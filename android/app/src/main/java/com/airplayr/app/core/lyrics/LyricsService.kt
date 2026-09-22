package com.airplayr.app.core.lyrics

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Native counterpart of reference app's multi-provider lyrics service. */
class LyricsService(private val context: Context) {
    data class Cue(val start: Double, val end: Double, val text: String)
    data class Result(
        val provider: String,
        val trackName: String,
        val artistName: String,
        val albumName: String,
        val duration: Double,
        val plainLyrics: String?,
        val syncedLyrics: String?,
        val cues: List<Cue>,
        val confidence: Int,
    )

    private val prefs = context.getSharedPreferences("lyrics_cache", Context.MODE_PRIVATE)
    private val indexKey = "cached_ids"

    fun fetch(id: String, title: String, artist: String, album: String?, duration: Double, force: Boolean = false): Result? {
        if (id.isBlank() || title.isBlank() || artist.isBlank()) return null
        if (!force) getCached(id)?.let { cached -> if (matches(title, artist, album.orEmpty(), duration, cached)) return cached else clear(id) }
        val input = Input(title.trim(), artist.trim(), album.orEmpty().trim(), duration.coerceAtLeast(0.0))
        val providers = listOf<(Input) -> Result?>({ lrclib(it) }, { netease(it) }, { kugou(it) }, { lyricsOvh(it) })
        for (provider in providers) {
            val result = runCatching { provider(input) }.getOrNull() ?: continue
            if (matches(input.title, input.artist, input.album, input.duration, result)) {
                save(id, result)
                return result
            }
        }
        return null
    }

    fun getCached(id: String): Result? {
        val raw = prefs.getString(id, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val cues = parseLrc(o.optString("syncedLyrics"))
            Result(o.optString("provider"), o.optString("trackName"), o.optString("artistName"), o.optString("albumName"), o.optDouble("duration", 0.0), o.optString("plainLyrics").takeIf { it.isNotBlank() }, o.optString("syncedLyrics").takeIf { it.isNotBlank() }, cues, o.optInt("confidence", 0))
        }.getOrNull()
    }

    fun clear(id: String) {
        prefs.edit().remove(id).putStringSet(indexKey, cachedIds().filterNot { it == id }.toSet()).apply()
    }
    fun cachedIds(): Set<String> = prefs.getStringSet(indexKey, emptySet()).orEmpty()
    fun clearAll() { prefs.edit().clear().apply() }
    fun save(id: String, r: Result) {
        prefs.edit().putString(id, JSONObject().apply { put("provider", r.provider); put("trackName", r.trackName); put("artistName", r.artistName); put("albumName", r.albumName); put("duration", r.duration); put("plainLyrics", r.plainLyrics ?: ""); put("syncedLyrics", r.syncedLyrics ?: ""); put("confidence", r.confidence) }.toString()).putStringSet(indexKey, (cachedIds() + id).toSet()).apply()
    }

    private data class Input(val title: String, val artist: String, val album: String, val duration: Double)

    private fun lrclib(i: Input): Result? {
        val exact = params(
            mapOf(
                "track_name" to i.title,
                "artist_name" to i.artist,
                "album_name" to i.album,
                "duration" to i.duration.roundToString(),
            ),
        )
        getJson("https://lrclib.net/api/get?$exact")?.let { o ->
            normalize(
                "LRCLIB",
                o.optString("trackName"),
                o.optString("artistName"),
                o.optString("albumName"),
                o.optDouble("duration", i.duration),
                o.optString("plainLyrics"),
                o.optString("syncedLyrics"),
            )?.let { return it }
        }

        // /api/get is an exact-match endpoint and legitimately returns 404 for many tagged
        // files. Fall back to LRCLIB's search endpoint before moving to slower providers.
        val search = params(mapOf("q" to "${i.title} ${i.artist}", "track_name" to i.title, "artist_name" to i.artist))
        val results = getJsonArray("https://lrclib.net/api/search?$search") ?: return null
        val candidates = mutableListOf<Result>()
        for (index in 0 until minOf(results.length(), 20)) {
            val o = results.optJSONObject(index) ?: continue
            normalize(
                "LRCLIB",
                o.optString("trackName"),
                o.optString("artistName"),
                o.optString("albumName"),
                o.optDouble("duration", i.duration),
                o.optString("plainLyrics"),
                o.optString("syncedLyrics"),
            )?.let { candidates += it }
        }
        return candidates.maxByOrNull { score(i.title, i.artist, i.album, i.duration, it) }
    }

    private fun netease(i: Input): Result? {
        val q = params(mapOf("s" to "${i.title} ${i.artist}", "type" to "1", "limit" to "10", "offset" to "0"))
        val songs = getJson("https://music.163.com/api/cloudsearch/pc?$q")?.optJSONObject("result")?.optJSONArray("songs") ?: return null
        val candidates = mutableListOf<Result>()
        for (n in 0 until minOf(songs.length(), 10)) {
            val s = songs.optJSONObject(n) ?: continue
            val id = s.optLong("id", -1L); if (id <= 0) continue
            val lyric = getJson("https://music.163.com/api/song/lyric?id=$id&lv=-1&kv=-1&tv=-1")?.optJSONObject("lrc")?.optString("lyric").orEmpty()
            if (lyric.isBlank()) continue
            val ar = s.optJSONArray("ar")
            val artist = joinArtists(ar)
            val album = s.optJSONObject("al")?.optString("name").orEmpty()
            val duration = s.optLong("dt", 0L) / 1000.0
            normalize("NetEase", s.optString("name"), artist, album, duration, null, lyric)?.let { candidates += it }
        }
        return candidates.maxByOrNull { score(i.title, i.artist, i.album, i.duration, it) }
    }

    private fun kugou(i: Input): Result? {
        val q = params(mapOf("ver" to "1", "man" to "no", "client" to "web", "keyword" to "${i.artist} - ${i.title}", "duration" to (i.duration * 1000).roundToString()))
        val candidates = getJson("https://lyrics.kugou.com/search?$q")?.optJSONArray("candidates") ?: return null
        val accepted = mutableListOf<Result>()
        for (n in 0 until minOf(candidates.length(), 10)) {
            val c = candidates.optJSONObject(n) ?: continue
            val lyricQ = params(mapOf("id" to c.optString("id"), "accesskey" to c.optString("accesskey"), "fmt" to "lrc", "charset" to "utf8"))
            val encoded = getJson("https://lyrics.kugou.com/download?$lyricQ")?.optString("content").orEmpty()
            if (encoded.isBlank()) continue
            val lrc = runCatching { String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8) }.getOrNull() ?: continue
            normalize("Kugou", c.optString("song").ifBlank { c.optString("songname") }, c.optString("singer").ifBlank { c.optString("artist") }, c.optString("album"), c.optDouble("duration", 0.0) / 1000.0, null, lrc)?.let { accepted += it }
        }
        return accepted.maxByOrNull { score(i.title, i.artist, i.album, i.duration, it) }
    }

    private fun lyricsOvh(i: Input): Result? {
        val raw = getText("https://api.lyrics.ovh/v1/${enc(i.artist)}/${enc(i.title)}")?.trim().orEmpty()
        if (raw.isBlank()) return null
        return normalize("lyrics.ovh", i.title, i.artist, i.album, i.duration, raw, null)
    }

    private fun normalize(provider: String, title: String, artist: String, album: String, duration: Double, plain: String?, synced: String?): Result? {
        if (title.isBlank() || artist.isBlank()) return null
        val cues = parseLrc(synced.orEmpty())
        val base = Result(provider, title.trim(), artist.trim(), album.trim(), duration, plain?.takeIf { it.isNotBlank() }, synced?.takeIf { it.isNotBlank() }, cues, 0)
        return base.copy(confidence = score(title, artist, album, duration, base))
    }

    private fun matches(title: String, artist: String, album: String, duration: Double, r: Result): Boolean = score(title, artist, album, duration, r) >= 80

    private fun score(title: String, artist: String, album: String, duration: Double, candidate: Result): Int {
        val t = similarity(title, candidate.trackName)
        val a = similarity(artist, candidate.artistName)
        val alb = if (album.isBlank() || candidate.albumName.isBlank()) 0.5 else similarity(album, candidate.albumName)
        val d = if (duration <= 0 || candidate.duration <= 0) 0.5 else when (val diff = abs(duration - candidate.duration)) { in 0.0..1.0 -> 1.0; in 1.0..2.0 -> .96; in 2.0..3.0 -> .90; in 3.0..5.0 -> .72; in 5.0..7.0 -> .40; else -> 0.0 }
        if (t < .90 || a < .90) return 0
        if (album.isNotBlank() && candidate.albumName.isNotBlank() && alb < .45) return 0
        if (duration > 0 && candidate.duration > 0 && d < .40) return 0
        return (t * 35 + a * 40 + alb * 10 + d * 10 + if (candidate.cues.isNotEmpty()) 5 else 0).roundToInt()
    }

    private fun similarity(a: String, b: String): Double {
        val aa = normalizeText(a); val bb = normalizeText(b); if (aa.isBlank() || bb.isBlank()) return 0.0; if (aa == bb) return 1.0
        val sa = aa.split(' ').filter { it.isNotBlank() }.toSet(); val sb = bb.split(' ').filter { it.isNotBlank() }.toSet(); if (sa.isEmpty() || sb.isEmpty()) return 0.0
        return 2.0 * sa.intersect(sb).size / (sa.size + sb.size).toDouble()
    }

    private fun normalizeText(s: String): String = java.text.Normalizer.normalize(s.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFKD).replace("[\\u0300-\\u036f]".toRegex(), "").replace("&", " and ").replace("\\b(feat|ft|featuring|with)\\b".toRegex(), " and ").replace("[()[\\]{}\"'`.,!?_:;|/\\\\-]+".toRegex(), " ").replace("\\s+".toRegex(), " ").trim()

    fun parseLrc(text: String): List<Cue> {
        if (text.isBlank()) return emptyList()
        val entries = mutableListOf<Pair<Double, String>>()
        text.replace("\r", "").lines().forEach { raw ->
            val matches = "\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]".toRegex().findAll(raw).toList(); if (matches.isEmpty()) return@forEach
            val body = raw.replace("\\[(?:\\d{1,3}):(?:\\d{2})(?:[.:]\\d{1,3})?\\]".toRegex(), "").trim(); if (body.isBlank()) return@forEach
            matches.forEach { m -> val min = m.groupValues[1].toIntOrNull() ?: return@forEach; val sec = m.groupValues[2].toIntOrNull() ?: return@forEach; if (sec >= 60) return@forEach; val frac = m.groupValues.getOrNull(3).orEmpty().let { when (it.length) { 1 -> it.toDouble()/10; 2 -> it.toDouble()/100; 3 -> it.toDouble()/1000; else -> 0.0 } }; entries += min*60+sec+frac to body }
        }
        entries.sortBy { it.first }
        val out = mutableListOf<Cue>()
        entries.forEach { (start, textLine) ->
            val prev = out.lastOrNull(); if (prev != null && abs(prev.start-start) < .001) { if (prev.text != textLine) out[out.lastIndex] = prev.copy(text = prev.text + "\n" + textLine) } else out += Cue(start, Double.POSITIVE_INFINITY, textLine) }
        return out.mapIndexed { index, cue -> cue.copy(end = out.getOrNull(index + 1)?.start ?: Double.POSITIVE_INFINITY) }
    }

    private fun getJson(url: String): JSONObject? = runCatching {
        val body = getText(url) ?: return@runCatching null
        JSONObject(body)
    }.getOrNull()

    private fun getJsonArray(url: String): JSONArray? = runCatching {
        val body = getText(url) ?: return@runCatching null
        JSONArray(body)
    }.getOrNull()

    private fun getText(url: String): String? {
        val connection = runCatching { URL(url).openConnection() as HttpURLConnection }.getOrNull() ?: return null
        return try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.useCaches = true
            connection.setRequestProperty("User-Agent", "AirPlayr/1.0")
            connection.setRequestProperty("Accept", "application/json,text/plain,*/*")
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream ?: return null
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }.takeIf { it.isNotBlank() && connection.responseCode in 200..299 }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
    private fun params(map: Map<String, String>): String = map.filterValues { it.isNotBlank() }.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }
    private fun enc(v: String) = URLEncoder.encode(v, "UTF-8")
    private fun JSONArray?.joinNames(): String = buildString { this@joinNames?.let { for (i in 0 until it.length()) { if (i > 0) append(" & "); append(it.optJSONObject(i)?.optString("name").orEmpty()) } } }
    private fun joinArtists(arr: JSONArray?): String = arr.joinNames()
    private fun Double.roundToString() = kotlin.math.round(this).toInt().toString()
}
