package com.airplayr.app.core.playback.service

import android.content.Context
import android.net.Uri
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/** Reads the common ReplayGain tags used by AirPlayr without changing file metadata. */
object ReplayGainReader {
    private val cache = ConcurrentHashMap<String, Tags>()
    data class Tags(val trackDb: Double? = null, val albumDb: Double? = null, val peak: Double? = null)

    fun read(context: Context, uriString: String): Tags {
        cache[uriString]?.let { return it }
        val uri = Uri.parse(uriString)
        val temp = File.createTempFile("airplayr-rg-", ".audio", context.cacheDir)
        return try {
            val input = when (uri.scheme) {
                "file" -> uri.path?.let(::File)?.inputStream()
                else -> context.contentResolver.openInputStream(uri)
            } ?: return Tags()
            input.use { source -> FileOutputStream(temp).use { output -> source.copyTo(output) } }
            val tag = AudioFileIO.read(temp).tag ?: return Tags()
            fun custom(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
                runCatching { tag.getFirst(key)?.trim()?.takeIf { it.isNotEmpty() } }.getOrNull()
            }
            Tags(
                trackDb = parseDb(custom("REPLAYGAIN_TRACK_GAIN", "replaygain_track_gain", "REPLAYGAIN_TRACKGAIN")),
                albumDb = parseDb(custom("REPLAYGAIN_ALBUM_GAIN", "replaygain_album_gain", "REPLAYGAIN_ALBUMGAIN")),
                peak = parseNumber(custom("REPLAYGAIN_TRACK_PEAK", "replaygain_track_peak", "REPLAYGAIN_ALBUM_PEAK")),
            ).also { cache[uriString] = it }
        } catch (_: Throwable) {
            Tags()
        } finally {
            temp.delete()
        }
    }

    private fun parseDb(value: String?): Double? = parseNumber(value?.removeSuffix("dB")?.trim())
    private fun parseNumber(value: String?): Double? = value?.toDoubleOrNull()?.takeIf { it.isFinite() }
}
