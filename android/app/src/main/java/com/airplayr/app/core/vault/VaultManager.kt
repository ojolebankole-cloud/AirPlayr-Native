package com.airplayr.app.core.vault

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.annotation.RequiresApi
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Device-bound private video vault. Vault media is encrypted at rest and moved out of MediaStore.
 * The password alone is not sufficient: the wrapping secret is protected by Android Keystore on
 * the same installation/device.
 */
class VaultManager(private val context: Context) {
    companion object {
        private const val PREFS = "airplayr_vault"
        private const val ENTRIES = "entries"
        private const val SALT = "salt"
        private const val PASSWORD_WRAP = "password_wrap"
        private const val DEVICE_WRAP = "device_wrap"
        private const val DEVICE_FINGERPRINT = "device_fingerprint"
        private const val KEY_ALIAS = "airplayr_vault_device_key_v1"
        private const val PBKDF2_ROUNDS = 210_000
        private const val KEY_BYTES = 32
        private const val GCM_IV_BYTES = 12

        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= 23
    }

    data class Entry(
        val id: String,
        val title: String,
        val artist: String?,
        val filename: String,
        val mimeType: String?,
        val durationMs: Long,
        val sizeBytes: Long,
        val addedAt: Long,
        val encryptedName: String,
        val thumbnailName: String?,
    )

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val random = SecureRandom()
    private val mediaDir = File(context.filesDir, "vault/media").apply { mkdirs() }
    private val thumbDir = File(context.filesDir, "vault/thumbs").apply { mkdirs() }
    private val cacheDir = File(context.cacheDir, "vault_playback").apply { mkdirs() }
    @Volatile private var unlockedMaster: ByteArray? = null

    val configured: Boolean get() = prefs.contains(SALT) && prefs.contains(PASSWORD_WRAP) && prefs.contains(DEVICE_WRAP)
    val unlocked: Boolean get() = unlockedMaster != null

    fun lock() {
        unlockedMaster?.fill(0)
        unlockedMaster = null
    }

    fun deviceFingerprint(): String = sha256Hex((Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown") + ":" + context.packageName).take(16).uppercase()

    suspend fun setup(password: String): Boolean = withContext(Dispatchers.IO) {
        if (configured || password.length < 6) return@withContext false
        val deviceSecret = randomBytes(KEY_BYTES)
        val master = randomBytes(KEY_BYTES)
        val salt = randomBytes(16)
        val deviceWrapped = wrapWithDeviceKey(deviceSecret)
        val passwordKey = derivePasswordKey(password, deviceSecret, salt)
        val passwordWrapped = encryptBytes(passwordKey, master)
        prefs.edit()
            .putString(SALT, b64(salt))
            .putString(DEVICE_WRAP, b64(deviceWrapped))
            .putString(PASSWORD_WRAP, b64(passwordWrapped))
            .putString(DEVICE_FINGERPRINT, deviceFingerprint())
            .putString(ENTRIES, "[]")
            .apply()
        unlockedMaster = master
        true
    }

    suspend fun unlock(password: String): Boolean = withContext(Dispatchers.IO) {
        if (!configured || password.length < 6) return@withContext false
        if (prefs.getString(DEVICE_FINGERPRINT, null) != deviceFingerprint()) return@withContext false
        return@withContext runCatching {
            val salt = b64d(prefs.getString(SALT, "")!!)
            val deviceSecret = unwrapWithDeviceKey(b64d(prefs.getString(DEVICE_WRAP, "")!!))
            val key = derivePasswordKey(password, deviceSecret, salt)
            val master = decryptBytes(key, b64d(prefs.getString(PASSWORD_WRAP, "")!!))
            if (master.size != KEY_BYTES) error("Invalid vault key")
            unlockedMaster = master
            true
        }.getOrDefault(false)
    }

    fun entries(): List<Entry> = readEntries().sortedByDescending { it.addedAt }

    suspend fun stageMove(item: MediaItem): Entry? = withContext(Dispatchers.IO) {
        if (!unlocked || item.mediaType != MediaType.VIDEO) return@withContext null
        val master = unlockedMaster?.clone() ?: return@withContext null
        val safeBase = item.id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80)
        val encryptedName = "${safeBase}_${System.currentTimeMillis()}.vault"
        val encrypted = File(mediaDir, encryptedName)
        val source = context.contentResolver.openInputStream(Uri.parse(item.uri)) ?: return@withContext null
        return@withContext runCatching {
            source.use { input -> encryptStream(input, encrypted, master) }
            val thumbName = createThumbnail(item, safeBase)
            Entry(item.id, item.title.ifBlank { item.filename }, item.artist, item.filename, item.mimeType, item.durationMs, item.sizeBytes, System.currentTimeMillis(), encryptedName, thumbName)
        }.getOrElse {
            encrypted.delete()
            null
        }.also { master.fill(0) }
    }

    suspend fun finalizeMove(entry: Entry) = withContext(Dispatchers.IO) { saveEntries(readEntries().filterNot { it.id == entry.id } + entry) }

    suspend fun rollbackStaged(entry: Entry) = withContext(Dispatchers.IO) {
        File(mediaDir, entry.encryptedName).delete()
        entry.thumbnailName?.let { File(thumbDir, it).delete() }
    }

    suspend fun decryptToCache(id: String): Pair<File, Entry>? = withContext(Dispatchers.IO) {
        val master = unlockedMaster?.clone() ?: return@withContext null
        val entry = readEntries().firstOrNull { it.id == id } ?: return@withContext null
        val encrypted = File(mediaDir, entry.encryptedName)
        if (!encrypted.exists()) return@withContext null
        val extension = entry.filename.substringAfterLast('.', "mp4").replace(Regex("[^A-Za-z0-9]"), "")
        val out = File(cacheDir, "${entry.id.hashCode().toUInt()}.$extension")
        runCatching {
            decryptStream(encrypted, out, master)
            master.fill(0)
            out to entry
        }.getOrElse {
            master.fill(0)
            out.delete()
            null
        }
    }

    fun thumbnailFile(entry: Entry): File? = entry.thumbnailName?.let { File(thumbDir, it).takeIf(File::exists) }

    suspend fun deleteEntry(id: String): Boolean = withContext(Dispatchers.IO) {
        val old = readEntries().firstOrNull { it.id == id } ?: return@withContext false
        File(mediaDir, old.encryptedName).delete()
        old.thumbnailName?.let { File(thumbDir, it).delete() }
        saveEntries(readEntries().filterNot { it.id == id })
        true
    }

    private fun createThumbnail(item: MediaItem, base: String): String? {
        if (Build.VERSION.SDK_INT < 29) return null
        return runCatching {
            val bitmap = context.contentResolver.loadThumbnail(Uri.parse(item.uri), android.util.Size(640, 360), null)
            val fileName = "$base.jpg"
            FileOutputStream(File(thumbDir, fileName)).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
            bitmap.recycle()
            fileName
        }.getOrNull()
    }

    private fun encryptStream(input: InputStream, target: File, master: ByteArray) {
        val iv = ByteArray(GCM_IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(master, "AES"), GCMParameterSpec(128, iv))
        FileOutputStream(target).use { fos ->
            fos.write(iv)
            CipherOutputStream(fos, cipher).use { out ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } >= 0) if (read > 0) out.write(buffer, 0, read)
            }
        }
    }

    private fun decryptStream(source: File, target: File, master: ByteArray) {
        FileInputStream(source).use { fis ->
            val iv = ByteArray(GCM_IV_BYTES)
            if (fis.read(iv) != iv.size) error("Invalid vault media")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(master, "AES"), GCMParameterSpec(128, iv))
            CipherInputStream(fis, cipher).use { input ->
                FileOutputStream(target).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } >= 0) if (read > 0) out.write(buffer, 0, read)
                }
            }
        }
    }

    private fun wrapWithDeviceKey(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deviceKey())
        return cipher.iv + cipher.doFinal(bytes)
    }

    private fun unwrapWithDeviceKey(blob: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deviceKey(), GCMParameterSpec(128, blob.copyOfRange(0, GCM_IV_BYTES)))
        return cipher.doFinal(blob.copyOfRange(GCM_IV_BYTES, blob.size))
    }

    private fun deviceKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(KEY_ALIAS, android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build())
        return generator.generateKey()
    }

    private fun derivePasswordKey(password: String, deviceSecret: ByteArray, salt: ByteArray): SecretKeySpec {
        val mixedSalt = MessageDigest.getInstance("SHA-256").digest(salt + deviceSecret)
        val algorithm = if (Build.VERSION.SDK_INT >= 26) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"
        val spec = PBEKeySpec(password.toCharArray(), mixedSalt, PBKDF2_ROUNDS, KEY_BYTES * 8)
        val bytes = SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    private fun encryptBytes(key: SecretKeySpec, plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.iv + cipher.doFinal(plain)
    }

    private fun decryptBytes(key: SecretKeySpec, blob: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob.copyOfRange(0, GCM_IV_BYTES)))
        return cipher.doFinal(blob.copyOfRange(GCM_IV_BYTES, blob.size))
    }

    private fun readEntries(): List<Entry> = runCatching {
        val array = JSONArray(prefs.getString(ENTRIES, "[]") ?: "[]")
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(Entry(o.getString("id"), o.getString("title"), o.optString("artist").takeIf { it.isNotBlank() }, o.getString("filename"), o.optString("mimeType").takeIf { it.isNotBlank() }, o.optLong("durationMs"), o.optLong("sizeBytes"), o.optLong("addedAt"), o.getString("encryptedName"), o.optString("thumbnailName").takeIf { it.isNotBlank() }))
            }
        }
    }.getOrDefault(emptyList())

    private fun saveEntries(entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { e -> array.put(JSONObject().apply {
            put("id", e.id); put("title", e.title); put("artist", e.artist ?: ""); put("filename", e.filename); put("mimeType", e.mimeType ?: ""); put("durationMs", e.durationMs); put("sizeBytes", e.sizeBytes); put("addedAt", e.addedAt); put("encryptedName", e.encryptedName); put("thumbnailName", e.thumbnailName ?: "")
        }) }
        prefs.edit().putString(ENTRIES, array.toString()).apply()
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)
    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun b64d(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)
    private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
