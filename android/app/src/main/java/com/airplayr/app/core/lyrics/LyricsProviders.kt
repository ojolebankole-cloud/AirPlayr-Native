package com.airplayr.app.core.lyrics

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class LrclibProvider : LyricsProvider {
    override val id = "LRCLIB"
    override fun fetch(track: LyricsTrack, http: LyricsHttpClient): LyricsService.Result? {
        val exact = http.params(mapOf("track_name" to track.title, "artist_name" to track.artist, "album_name" to track.album, "duration" to track.duration.roundToInt().toString()))
        http.getJson("https://lrclib.net/api/get?$exact")?.let { candidate(it) }?.let { return it }
        if (track.album.isNotBlank()) {
            val fallback = http.params(mapOf("track_name" to track.title, "artist_name" to track.artist, "duration" to track.duration.roundToInt().toString()))
            http.getJson("https://lrclib.net/api/get?$fallback")?.let { candidate(it) }?.let { return it }
        }
        val search = http.params(mapOf("q" to "${track.title} ${track.artist}", "track_name" to track.title, "artist_name" to track.artist, "album_name" to track.album))
        val results = http.getJsonArray("https://lrclib.net/api/search?$search") ?: return null
        return best(track, (0 until minOf(20, results.length())).mapNotNull { candidate(results.optJSONObject(it)) })
    }
    private fun candidate(o: JSONObject?): LyricsService.Result? = o?.let { LyricsProviderSupport.result(id, it.optString("trackName"), it.optString("artistName"), it.optString("albumName"), it.optDouble("duration", 0.0), it.optString("plainLyrics"), it.optString("syncedLyrics")) }
}

class NetEaseProvider : LyricsProvider {
    override val id = "NetEase"
    override fun fetch(track: LyricsTrack, http: LyricsHttpClient): LyricsService.Result? {
        val q = http.params(mapOf("s" to "${track.title} ${track.artist}", "type" to "1", "limit" to "10", "offset" to "0"))
        val songs = http.getJson("https://music.163.com/api/cloudsearch/pc?$q")?.optJSONObject("result")?.optJSONArray("songs") ?: return null
        val candidates = mutableListOf<LyricsService.Result>()
        for (i in 0 until minOf(10, songs.length())) {
            val song = songs.optJSONObject(i) ?: continue
            val id = song.optLong("id", -1L)
            if (id <= 0) continue
            val lyric = http.getJson("https://music.163.com/api/song/lyric?id=$id&lv=-1&kv=-1&tv=-1")?.optJSONObject("lrc")?.optString("lyric").orEmpty()
            if (lyric.isBlank()) continue
            val artists = song.optJSONArray("ar")?.let { arr -> (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank) }.joinToString(" & ") }.orEmpty()
            val album = song.optJSONObject("al")?.optString("name").orEmpty()
            val duration = song.optLong("dt", 0L) / 1000.0
            LyricsProviderSupport.result(this.id, song.optString("name"), artists, album, duration, synced = lyric)?.let(candidates::add)
        }
        return best(track, candidates)
    }
}

class KugouProvider : LyricsProvider {
    override val id = "Kugou"
    override fun fetch(track: LyricsTrack, http: LyricsHttpClient): LyricsService.Result? {
        val q = http.params(mapOf("ver" to "1", "man" to "yes", "client" to "web", "keyword" to "${track.artist} - ${track.title}", "duration" to (track.duration * 1000).roundToInt().toString()))
        val candidates = http.getJson("https://lyrics.kugou.com/search?$q")?.optJSONArray("candidates") ?: return null
        val results = mutableListOf<LyricsService.Result>()
        for (i in 0 until minOf(10, candidates.length())) {
            val c = candidates.optJSONObject(i) ?: continue
            val lyricQ = http.params(mapOf("ver" to "1", "client" to "pc", "id" to c.optString("id"), "accesskey" to c.optString("accesskey"), "fmt" to "lrc", "charset" to "utf8"))
            val encoded = http.getJson("https://lyrics.kugou.com/download?$lyricQ")?.optString("content").orEmpty()
            if (encoded.isBlank()) continue
            val lrc = runCatching { String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8) }.getOrNull() ?: continue
            val duration = c.optDouble("duration", 0.0).let { if (it > 1000) it / 1000.0 else it }
            LyricsProviderSupport.result(id, c.optString("song").ifBlank { c.optString("songname") }, c.optString("singer").ifBlank { c.optString("artist") }, c.optString("album"), duration, synced = lrc)?.let(results::add)
        }
        return best(track, results)
    }
}

class LyricsOvhProvider : LyricsProvider {
    override val id = "lyrics.ovh"
    override fun fetch(track: LyricsTrack, http: LyricsHttpClient): LyricsService.Result? {
        val json = http.getJson("https://api.lyrics.ovh/v1/${http.enc(track.artist)}/${http.enc(track.title)}") ?: return null
        val lyrics = json.optString("lyrics").trim()
        return LyricsProviderSupport.result(id, track.title, track.artist, track.album, track.duration, plain = lyrics)
    }
}

class BetterLyricsProvider : LyricsProvider {
    override val id = "BetterLyrics"
    override fun fetch(track: LyricsTrack, http: LyricsHttpClient): LyricsService.Result? {
        val q = http.params(mapOf("s" to track.title, "a" to track.artist, "d" to track.duration.roundToInt().toString(), "al" to track.album))
        val json = http.getJson("https://lyrics-api.boidu.dev/getLyrics?$q") ?: return null
        val ttml = json.optString("ttml").trim()
        val lrc = LyricsProviderSupport.ttmlToLrc(ttml) ?: return null
        return LyricsProviderSupport.result(id, track.title, track.artist, track.album, track.duration, synced = lrc, syncLevel = 2)
    }
}

class UnisonProvider : LyricsProvider {
    override val id = "Unison"
    override fun fetch(track: LyricsTrack, http: LyricsHttpClient): LyricsService.Result? {
        val q = http.params(mapOf("song" to track.title, "artist" to track.artist, "album" to track.album, "duration" to track.duration.toString()))
        val json = http.getJson("https://unison.boidu.dev/lyrics?$q") ?: return null
        val data = json.optJSONObject("data") ?: return null
        val format = data.optString("format").lowercase()
        val lyrics = data.optString("lyrics").trim()
        if (lyrics.isBlank()) return null
        return when (format) {
            "ttml" -> LyricsProviderSupport.ttmlToLrc(lyrics)?.let { LyricsProviderSupport.result(id, data.optString("song").ifBlank { track.title }, data.optString("artist").ifBlank { track.artist }, track.album, track.duration, synced = it, syncLevel = 2) }
            "lrc", "elrc" -> LyricsProviderSupport.result(id, data.optString("song").ifBlank { track.title }, data.optString("artist").ifBlank { track.artist }, track.album, track.duration, synced = lyrics, syncLevel = 2)
            else -> LyricsProviderSupport.result(id, data.optString("song").ifBlank { track.title }, data.optString("artist").ifBlank { track.artist }, track.album, track.duration, plain = lyrics)
        }
    }
}

class SimpMusicProvider : LyricsProvider {
    override val id = "SimpMusic"
    override fun fetch(track: LyricsTrack, http: LyricsHttpClient): LyricsService.Result? {
        val q = http.params(mapOf("q" to "${track.title} ${track.artist}", "limit" to "10"))
        val data = http.getJson("https://api-lyrics.simpmusic.org/v1/search?$q")?.optJSONArray("data") ?: return null
        val candidates = mutableListOf<LyricsService.Result>()
        for (i in 0 until minOf(10, data.length())) {
            val item = data.optJSONObject(i) ?: continue
            val title = item.optString("songTitle").ifBlank { item.optString("title") }
            val artist = item.optString("artistName").ifBlank { item.optString("artist") }
            val synced = item.optString("syncedLyrics").ifBlank { item.optString("syncedLyric") }
            val plain = item.optString("plainLyric").ifBlank { item.optString("plainLyrics") }
            val duration = item.optDouble("duration", track.duration)
            LyricsProviderSupport.result(id, title, artist, item.optString("albumName").ifBlank { track.album }, duration, plain = plain, synced = synced)?.let(candidates::add)
        }
        return best(track, candidates)
    }
}

class YouLyPlusProvider : LyricsProvider {
    override val id = "YouLyPlus"
    private val mirrors = listOf(
        "https://lyricsplus.binimum.org",
        "https://lyricsplus.atomix.one",
        "https://lyricsplus.prjktla.workers.dev",
        "https://lyricsplus-seven.vercel.app",
        "https://lyrics-plus-backend.vercel.app",
    )

    override fun fetch(track: LyricsTrack, http: LyricsHttpClient): LyricsService.Result? {
        for (base in mirrors) {
            val q = http.params(
                mapOf(
                    "title" to track.title,
                    "artist" to track.artist,
                    "album" to track.album,
                    "duration" to track.duration.toString(),
                ),
            )
            val json = http.getJson("$base/v2/lyrics/get?$q", mapOf("X-Client-Package" to "AirPlayr")) ?: continue
            parseV2(json, track)?.let { return it }
        }
        return null
    }

    private fun parseV2(json: JSONObject, track: LyricsTrack): LyricsService.Result? {
        val type = json.optString("type").uppercase()
        val title = json.optJSONObject("metadata")?.optString("song").orEmpty().ifBlank { track.title }
        val artist = json.optJSONObject("metadata")?.optString("artist").orEmpty().ifBlank { track.artist }
        val album = json.optJSONObject("metadata")?.optString("album").orEmpty().ifBlank { track.album }
        val duration = json.optJSONObject("metadata")?.optDouble("duration", track.duration) ?: track.duration
        val lines = json.optJSONArray("lyrics")
        if (lines != null && lines.length() > 0) {
            val lrc = if (type == "WORD") wordArrayToLrc(lines) else lineArrayToLrc(lines)
            if (!lrc.isNullOrBlank()) {
                return LyricsProviderSupport.result(id, title, artist, album, duration, synced = lrc, syncLevel = if (type == "WORD") 3 else 2)
            }
        }

        // Older mirrors can still return a string payload.
        val content = json.optString("lyrics").ifBlank { json.optString("content") }.trim()
        if (content.isBlank()) return null
        val syncLevel = if (type == "WORD" || content.matches(Regex("(?s).*<[^|>]+:[0-9.]+:[0-9.]+.*"))) 3 else 2
        return LyricsProviderSupport.result(id, title, artist, album, duration, synced = content, syncLevel = syncLevel)
    }

    private fun lineArrayToLrc(lines: JSONArray): String? {
        val out = StringBuilder()
        for (i in 0 until lines.length()) {
            val item = lines.optJSONObject(i) ?: continue
            val time = item.optDouble("time", -1.0)
            val text = item.optString("text").trim()
            if (time < 0 || text.isBlank()) continue
            out.append('[').append(formatMs(time)).append(']').append(text).append('\n')
        }
        return out.toString().trim().takeIf { it.isNotBlank() }
    }

    private fun wordArrayToLrc(words: JSONArray): String? {
        data class Word(val timeMs: Double, val text: String, val key: String)
        val parsed = mutableListOf<Word>()
        for (i in 0 until words.length()) {
            val item = words.optJSONObject(i) ?: continue
            val time = item.optDouble("time", -1.0)
            val text = item.optString("text").trim()
            if (time < 0 || text.isBlank()) continue
            val key = item.optJSONObject("element")?.optString("key").orEmpty().ifBlank { "line-$i" }
            parsed += Word(time, text, key)
        }
        if (parsed.isEmpty()) return null
        return parsed.groupBy { it.key }.values
            .sortedBy { it.minOf { word -> word.timeMs } }
            .joinToString("\n") { group ->
                val first = group.minBy { it.timeMs }
                "[${formatMs(first.timeMs)}]${group.joinToString(" ") { it.text }}"
            }
    }

    private fun formatMs(milliseconds: Double): String {
        val totalHundredths = (milliseconds.coerceAtLeast(0.0) / 10.0).toInt()
        val minutes = totalHundredths / 6000
        val remainder = totalHundredths % 6000
        return "%02d:%02d.%02d".format(minutes, remainder / 100, remainder % 100)
    }
}

class PaxsenixProvider : LyricsProvider {
    override val id = "Paxsenix"
    override fun fetch(track: LyricsTrack, http: LyricsHttpClient): LyricsService.Result? {
        // The proxy exposes a metadata-based Musixmatch route that does not require an
        // AirPlayr-owned API key. Prefer word timing, then fall back to line timing.
        val queries = listOf(
            mapOf("a" to track.artist, "d" to track.duration.roundToInt().toString(), "format" to "lrc", "t" to track.title, "type" to "word"),
            mapOf("a" to track.artist, "d" to track.duration.roundToInt().toString(), "format" to "lrc", "t" to track.title),
        )
        for (values in queries) {
            val q = http.params(values)
            val raw = http.getText("https://lyrics.paxsenix.org/musixmatch/lyrics?$q") ?: continue
            val parsed = parsePayload(raw)
            val synced = parsed?.optString("lyrics").orEmpty().ifBlank { parsed?.optString("lrc").orEmpty() }.ifBlank { parsed?.optString("syncedLyrics").orEmpty() }.ifBlank { raw.takeIf { it.contains(Regex("\\[\\d{1,3}:\\d{2}")) }.orEmpty() }.trim()
            if (synced.isBlank()) continue
            val level = if (values["type"] == "word") 3 else 2
            return LyricsProviderSupport.result(id, parsed?.optString("title").orEmpty().ifBlank { track.title }, parsed?.optString("artist").orEmpty().ifBlank { track.artist }, parsed?.optString("album").orEmpty().ifBlank { track.album }, parsed?.optDouble("duration", track.duration) ?: track.duration, synced = synced, syncLevel = level)
        }
        return null
    }

    private fun parsePayload(raw: String): JSONObject? = runCatching {
        val trimmed = raw.trim()
        val jsonText = when {
            trimmed.startsWith("{") -> trimmed
            trimmed.startsWith("[") -> return null
            else -> trimmed.substringAfter("(", trimmed).substringBeforeLast(")", trimmed)
        }
        JSONObject(jsonText)
    }.getOrNull()
}

class QqMusicProvider : LyricsProvider {
    override val id = "QQ Music"
    override fun fetch(track: LyricsTrack, http: LyricsHttpClient): LyricsService.Result? {
        val search = http.params(mapOf("w" to "${track.title} ${track.artist}", "p" to "1", "n" to "10", "format" to "json", "t" to "0"))
        val json = http.getJson("https://c.y.qq.com/soso/fcgi-bin/client_search_cp?$search") ?: return null
        val songs = json.optJSONObject("data")?.optJSONObject("song")?.optJSONArray("list") ?: return null
        for (i in 0 until minOf(10, songs.length())) {
            val song = songs.optJSONObject(i) ?: continue
            val mid = song.optString("songmid")
            if (mid.isBlank()) continue
            val lyricUrl = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=${http.enc(mid)}&format=json&nobase64=1"
            val lyric = http.getText(lyricUrl, mapOf("Referer" to "https://y.qq.com/portal/player.html"))?.let { parseJsonp(it) }?.optString("lyric").orEmpty().trim()
            if (lyric.isBlank()) continue
            val artist = song.optJSONArray("singer")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optString("name") }.joinToString(" & ") }.orEmpty()
            val album = song.optJSONObject("album")?.optString("name").orEmpty()
            val duration = song.optInt("interval", 0).toDouble()
            return LyricsProviderSupport.result(id, song.optString("songname").ifBlank { track.title }, artist.ifBlank { track.artist }, album, duration, synced = lyric)
        }
        return null
    }
    private fun parseJsonp(raw: String): JSONObject? = runCatching {
        val cleaned = raw.trim().removePrefix("MusicJsonCallback(").removeSuffix(")").removePrefix("callback(").removeSuffix(")")
        JSONObject(cleaned)
    }.getOrNull()
}

private fun best(track: LyricsTrack, candidates: List<LyricsService.Result>): LyricsService.Result? = candidates.maxByOrNull { LyricsMatcher.score(track, it) }
