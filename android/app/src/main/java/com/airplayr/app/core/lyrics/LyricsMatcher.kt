package com.airplayr.app.core.lyrics

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object LyricsMatcher {
    const val MIN_ACCEPT_SCORE = 80

    fun score(track: LyricsTrack, candidate: LyricsService.Result): Int {
        val t = similarity(track.title, candidate.trackName, Field.TITLE)
        val a = similarity(track.artist, candidate.artistName, Field.ARTIST)
        val album = if (track.album.isBlank() || candidate.albumName.isBlank()) 0.5 else similarity(track.album, candidate.albumName, Field.ALBUM)
        val duration = if (track.duration <= 0 || candidate.duration <= 0) 0.5 else when (val diff = abs(track.duration - candidate.duration)) {
            in 0.0..1.0 -> 1.0
            in 1.0..2.0 -> .96
            in 2.0..3.0 -> .90
            in 3.0..5.0 -> .72
            in 5.0..7.0 -> .40
            else -> 0.0
        }
        if (t < .90 || a < .90) return 0
        if (track.album.isNotBlank() && candidate.albumName.isNotBlank() && album < .45) return 0
        if (track.duration > 0 && candidate.duration > 0 && duration < .40) return 0
        return (t * 35 + a * 40 + album * 10 + duration * 10 + if (candidate.cues.isNotEmpty()) 5 else 0).roundToInt()
    }

    private enum class Field { TITLE, ARTIST, ALBUM }

    private fun similarity(a: String, b: String, field: Field): Double {
        val aa = normalize(a, field)
        val bb = normalize(b, field)
        if (aa.isBlank() || bb.isBlank()) return 0.0
        if (aa == bb) return 1.0
        val sa = aa.split(' ').filter { it.isNotBlank() }.toSet()
        val sb = bb.split(' ').filter { it.isNotBlank() }.toSet()
        if (sa.isEmpty() || sb.isEmpty()) return 0.0
        return 2.0 * sa.intersect(sb).size / (sa.size + sb.size).toDouble()
    }

    private fun normalize(input: String, field: Field): String {
        var value = Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFKD)
            .replace("[\\u0300-\\u036f]".toRegex(), "")
        if (field == Field.TITLE) {
            value = value.replace("\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)".toRegex(), " ")
                .replace("\\s*\\[(?:feat\\.?|ft\\.?|featuring|with)\\b[^]]*\\]".toRegex(), " ")
        }
        return value.replace("\\b(feat\\.?|ft\\.?|featuring|with)\\b".toRegex(), " ")
            .replace("&", " ")
            .replace("[^\\p{L}\\p{N}\\s]+".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
