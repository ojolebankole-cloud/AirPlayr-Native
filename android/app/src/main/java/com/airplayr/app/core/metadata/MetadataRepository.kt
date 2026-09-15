package com.airplayr.app.core.metadata

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

private val Context.metadataDataStore by preferencesDataStore("airplayr_metadata")
private val CACHE_KEY = stringPreferencesKey("itunes_search_cache_v1")
private val OVERRIDES_KEY = stringPreferencesKey("media_metadata_overrides_v1")
private const val TTL_MS = 6 * 60 * 60 * 1000L
private const val MAX_RESULTS = 10
private const val MAX_CACHE_ENTRIES = 100

class MetadataRepository(private val context: Context) {
    private val inFlight = ConcurrentHashMap<String, kotlinx.coroutines.Deferred<List<MetadataMatch>>>()

    suspend fun search(item: MediaItem, country: String = "US", force: Boolean = false): List<MetadataMatch> {
        val term = listOf(item.title, item.artist, item.album).map { it?.trim().orEmpty() }.filter { it.isNotBlank() }.joinToString(" ")
        if (term.isBlank()) return emptyList()
        val key = "$country:${term.lowercase()}"
        val now = System.currentTimeMillis()
        if (!force) readCache(now)[key]?.let { return it }
        inFlight[key]?.let { return it.await() }
        return coroutineScope {
            val task = async(Dispatchers.IO) { fetch(term, item, country) }
            inFlight[key] = task
            try {
                val results = task.await()
                writeCache(key, CachedMetadata(now, results))
                results
            } finally { inFlight.remove(key) }
        }
    }

    suspend fun saveOverride(itemId: String, match: MetadataMatch) {
        context.metadataDataStore.edit { prefs ->
            val root = runCatching { JSONObject(prefs[OVERRIDES_KEY].orEmpty()) }.getOrElse { JSONObject() }
            root.put(itemId, match.toJson())
            prefs[OVERRIDES_KEY] = root.toString()
        }
    }

    suspend fun getOverride(itemId: String): MetadataMatch? = context.metadataDataStore.data.first()[OVERRIDES_KEY].orEmpty().let { raw ->
        runCatching { JSONObject(raw).optJSONObject(itemId)?.let(MetadataMatch::fromJson) }.getOrNull()
    }

    suspend fun clearOverrides() { context.metadataDataStore.edit { it.remove(OVERRIDES_KEY) } }

    suspend fun removeOverride(itemId: String) {
        context.metadataDataStore.edit { prefs ->
            val root = runCatching { JSONObject(prefs[OVERRIDES_KEY].orEmpty()) }.getOrElse { JSONObject() }
            root.remove(itemId); prefs[OVERRIDES_KEY] = root.toString()
        }
    }

    suspend fun overlay(item: MediaItem): MediaItem {
        val o = getOverride(item.id) ?: return item
        return item.copy(
            title = o.title.ifBlank { item.title },
            artist = o.artist.ifBlank { item.artist },
            album = o.album.ifBlank { item.album },
            albumArtist = o.albumArtist.ifBlank { item.albumArtist },
            genre = o.genre.ifBlank { item.genre },
        )
    }

    suspend fun downloadArtwork(match: MetadataMatch, cacheKey: String = "${match.provider}-${match.id}"): Uri? = withContext(Dispatchers.IO) {
        val remote = highResolutionArtwork(match.artworkUrl) ?: return@withContext null
        val directory = File(context.filesDir, "airplayr_artwork").apply { mkdirs() }
        val target = File(directory, "${fnv1a(cacheKey)}.jpg")
        if (target.exists() && target.length() > 2048) return@withContext Uri.fromFile(target)
        val connection = (URL(remote).openConnection() as HttpURLConnection).apply { connectTimeout = 9000; readTimeout = 9000; requestMethod = "GET"; setRequestProperty("Accept", "image/*") }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) return@withContext null
            connection.inputStream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            if (target.length() <= 2048) { target.delete(); null } else Uri.fromFile(target)
        } finally { connection.disconnect() }
    }

    suspend fun applyToMediaStore(item: MediaItem, match: MetadataMatch): Boolean = withContext(Dispatchers.IO) {
        val mediaId = item.mediaStoreId ?: return@withContext false
        val collection = if (item.mediaType == MediaType.AUDIO) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            match.title.takeIf { it.isNotBlank() }?.let { put(MediaStore.MediaColumns.TITLE, it) }
            if (item.mediaType == MediaType.AUDIO) {
                match.artist.takeIf { it.isNotBlank() }?.let { put(MediaStore.Audio.AudioColumns.ARTIST, it) }
                match.album.takeIf { it.isNotBlank() }?.let { put(MediaStore.Audio.AudioColumns.ALBUM, it) }
                match.albumArtist.takeIf { it.isNotBlank() }?.let { put(MediaStore.Audio.AudioColumns.ALBUM_ARTIST, it) }
                match.genre.takeIf { it.isNotBlank() }?.let { put(MediaStore.Audio.AudioColumns.GENRE, it) }
                match.trackNumber.takeIf { it > 0 }?.let { put(MediaStore.Audio.AudioColumns.TRACK, it) }
                match.discNumber.takeIf { it > 0 }?.let { put(MediaStore.Audio.AudioColumns.DISC_NUMBER, it) }
                match.year.takeIf { it.length >= 4 }?.toIntOrNull()?.let { put(MediaStore.Audio.AudioColumns.YEAR, it) }
            }
        }
        val ok = runCatching { context.contentResolver.update(Uri.withAppendedPath(collection, mediaId.toString()), values, null, null) > 0 }.getOrDefault(false)
        if (ok) saveOverride(item.id, match)
        ok
    }

    private fun fetch(term: String, item: MediaItem, country: String): List<MetadataMatch> {
        val query = "term=${URLEncoder.encode(term, Charsets.UTF_8.name())}&country=${URLEncoder.encode(country, Charsets.UTF_8.name())}&media=music&entity=song&limit=$MAX_RESULTS"
        val connection = (URL("https://itunes.apple.com/search?$query").openConnection() as HttpURLConnection).apply { connectTimeout = 9000; readTimeout = 9000; requestMethod = "GET"; setRequestProperty("Accept", "application/json") }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) return emptyList()
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val results = json.optJSONArray("results") ?: JSONArray()
            return buildList {
                for (i in 0 until results.length()) {
                    val obj = results.optJSONObject(i) ?: continue
                    val id = obj.optLong("trackId", 0L).takeIf { it > 0 } ?: obj.optLong("collectionId", 0L).takeIf { it > 0 } ?: continue
                    add(MetadataMatch(
                        provider = "itunes", id = id.toString(), title = obj.optString("trackName", obj.optString("collectionName", "")), artist = obj.optString("artistName", ""), album = obj.optString("collectionName", ""), albumArtist = obj.optString("artistName", ""), genre = obj.optString("primaryGenreName", ""), year = obj.optString("releaseDate", "").take(4), trackNumber = obj.optInt("trackNumber", 0), trackTotal = obj.optInt("trackCount", 0), discNumber = obj.optInt("discNumber", 0), discTotal = obj.optInt("discCount", 0), durationMs = obj.optLong("trackTimeMillis", 0L), artworkUrl = obj.optString("artworkUrl100", "").takeIf { it.isNotBlank() }, sourceUrl = obj.optString("trackViewUrl", obj.optString("collectionViewUrl", "")).takeIf { it.isNotBlank() }, score = score(item, obj)
                    ))
                }
            }.sortedByDescending { it.score }.take(MAX_RESULTS)
        } finally { connection.disconnect() }
    }

    private fun score(item: MediaItem, obj: JSONObject): Int {
        fun n(s: String?) = s.orEmpty().trim().lowercase()
        val title = n(item.title); val artist = n(item.artist); val album = n(item.album)
        val rt = n(obj.optString("trackName", obj.optString("collectionName"))); val ra = n(obj.optString("artistName")); val ral = n(obj.optString("collectionName"))
        var score = 0
        if (title.isNotBlank()) score += if (rt == title) 55 else if (rt.contains(title)) 30 else 0
        if (artist.isNotBlank()) score += if (ra == artist) 30 else if (ra.contains(artist)) 15 else 0
        if (album.isNotBlank()) score += if (ral == album) 15 else if (ral.contains(album)) 8 else 0
        if (item.durationMs > 0) { val delta = abs(item.durationMs - obj.optLong("trackTimeMillis", 0L)); if (delta < 4000) score += 8 else if (delta < 10000) score += 4 }
        return score
    }

    private suspend fun readCache(now: Long): Map<String, List<MetadataMatch>> { val raw = runCatching { context.metadataDataStore.data.first()[CACHE_KEY].orEmpty() }.getOrDefault(""); val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap(); val out = LinkedHashMap<String, List<MetadataMatch>>(); root.keys().forEach { key -> val record = root.optJSONObject(key) ?: return@forEach; if (now - record.optLong("at", 0L) >= TTL_MS) return@forEach; val arr = record.optJSONArray("results") ?: return@forEach; out[key] = buildList { for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(MetadataMatch.fromJson(it)) } } }; return out }
    private suspend fun writeCache(key: String, record: CachedMetadata) { context.metadataDataStore.edit { prefs -> val root = runCatching { JSONObject(prefs[CACHE_KEY].orEmpty()) }.getOrElse { JSONObject() }; root.put(key, JSONObject().put("at", record.at).put("results", JSONArray(record.results.map { it.toJson() }))); while (root.length() > MAX_CACHE_ENTRIES) root.remove(root.keys().next()); prefs[CACHE_KEY] = root.toString() } }
    private fun highResolutionArtwork(url: String?): String? = url?.replace(Regex("\\b\\d+x\\d+(?:bb)?(?:-\\d+)?(?=\\.[a-z]+(?:\\?|$))", RegexOption.IGNORE_CASE), "600x600bb-60")
    private fun fnv1a(value: String): String { var hash = 0x811c9dc5.toInt(); value.forEach { hash = (hash xor it.code) * 16777619 }; return Integer.toUnsignedString(hash, 16) }
}

data class CachedMetadata(val at: Long, val results: List<MetadataMatch>)
data class MetadataMatch(
    val provider: String, val id: String, val title: String, val artist: String, val album: String, val albumArtist: String, val genre: String, val year: String,
    val trackNumber: Int, val trackTotal: Int, val discNumber: Int, val discTotal: Int, val durationMs: Long, val artworkUrl: String?, val sourceUrl: String?, val score: Int,
) {
    fun toJson() = JSONObject().apply { put("provider", provider); put("id", id); put("title", title); put("artist", artist); put("album", album); put("albumArtist", albumArtist); put("genre", genre); put("year", year); put("trackNumber", trackNumber); put("trackTotal", trackTotal); put("discNumber", discNumber); put("discTotal", discTotal); put("durationMs", durationMs); put("artworkUrl", artworkUrl); put("sourceUrl", sourceUrl); put("score", score) }
    companion object { fun fromJson(o: JSONObject) = MetadataMatch(o.optString("provider"), o.optString("id"), o.optString("title"), o.optString("artist"), o.optString("album"), o.optString("albumArtist"), o.optString("genre"), o.optString("year"), o.optInt("trackNumber"), o.optInt("trackTotal"), o.optInt("discNumber"), o.optInt("discTotal"), o.optLong("durationMs"), o.optString("artworkUrl").takeIf { it.isNotBlank() }, o.optString("sourceUrl").takeIf { it.isNotBlank() }, o.optInt("score")) }
}
