package com.airplayr.app.core.lyrics

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

object LyricsProviderSupport {
    fun result(
        provider: String,
        title: String,
        artist: String,
        album: String,
        duration: Double,
        plain: String? = null,
        synced: String? = null,
        syncLevel: Int = if (!synced.isNullOrBlank()) 2 else if (!plain.isNullOrBlank()) 1 else 0,
    ): LyricsService.Result? {
        if (title.isBlank() || artist.isBlank() || (plain.isNullOrBlank() && synced.isNullOrBlank())) return null
        val cues = parseLrc(synced.orEmpty())
        return LyricsService.Result(provider, title.trim(), artist.trim(), album.trim(), duration, plain?.takeIf { it.isNotBlank() }, synced?.takeIf { it.isNotBlank() }, cues, 0, syncLevel)
    }

    fun parseLrc(text: String): List<LyricsService.Cue> {
        if (text.isBlank()) return emptyList()
        val entries = mutableListOf<Pair<Double, String>>()
        val tag = "\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]".toRegex()
        text.replace("\r", "").lines().forEach lineLoop@{ raw ->
            val matches = tag.findAll(raw).toList()
            if (matches.isEmpty()) return@lineLoop
            val body = raw.replace(tag, "").trim()
            if (body.isBlank()) return@lineLoop
            matches.forEach matchLoop@{ m ->
                val minute = m.groupValues[1].toIntOrNull() ?: return@matchLoop
                val second = m.groupValues[2].toIntOrNull() ?: return@matchLoop
                if (second >= 60) return@matchLoop
                val fraction = m.groupValues.getOrNull(3).orEmpty().let {
                    when (it.length) { 1 -> it.toDouble() / 10; 2 -> it.toDouble() / 100; 3 -> it.toDouble() / 1000; else -> 0.0 }
                }
                entries += minute * 60 + second + fraction to body
            }
        }
        entries.sortBy { it.first }
        val out = mutableListOf<LyricsService.Cue>()
        entries.forEach { (start, line) ->
            val previous = out.lastOrNull()
            if (previous != null && kotlin.math.abs(previous.start - start) < .001) {
                if (previous.text != line) out[out.lastIndex] = previous.copy(text = previous.text + "\n" + line)
            } else out += LyricsService.Cue(start, Double.POSITIVE_INFINITY, line)
        }
        return out.mapIndexed { index, cue -> cue.copy(end = out.getOrNull(index + 1)?.start ?: Double.POSITIVE_INFINITY) }
    }

    /** Converts common Apple/BetterLyrics TTML into line-level LRC. */
    fun ttmlToLrc(ttml: String): String? = runCatching {
        if (ttml.isBlank()) return null
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setXIncludeAware(false)
            isExpandEntityReferences = false
        }
        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(ttml.toByteArray(Charsets.UTF_8)))
        val nodes = document.getElementsByTagNameNS("*", "p")
        val lines = mutableListOf<Pair<Double, String>>()
        for (i in 0 until nodes.length) {
            val p = nodes.item(i) as? Element ?: continue
            val begin = parseTime(p.getAttribute("begin")) ?: continue
            val text = p.textContent.replace("\\s+".toRegex(), " ").trim()
            if (text.isNotBlank()) lines += begin to text
        }
        if (lines.isEmpty()) return null
        lines.sortedBy { it.first }.joinToString("\n") { (seconds, text) -> "[${formatTime(seconds)}]$text" }
    }.getOrNull()

    private fun parseTime(value: String): Double? {
        if (value.isBlank()) return null
        val s = value.trim()
        if (s.endsWith("ms")) return s.removeSuffix("ms").toDoubleOrNull()?.div(1000.0)
        if (s.endsWith("s")) return s.removeSuffix("s").toDoubleOrNull()
        val parts = s.split(':')
        return when (parts.size) {
            2 -> parts[0].toDoubleOrNull()?.times(60)?.plus(parts[1].toDoubleOrNull() ?: return null)
            3 -> parts[0].toDoubleOrNull()?.times(3600)?.plus(parts[1].toDoubleOrNull()?.times(60) ?: return null)?.plus(parts[2].toDoubleOrNull() ?: return null)
            else -> s.toDoubleOrNull()
        }
    }

    private fun formatTime(seconds: Double): String {
        val totalHundredths = (seconds.coerceAtLeast(0.0) * 100).toInt()
        val minutes = totalHundredths / 6000
        val remainder = totalHundredths % 6000
        val sec = remainder / 100
        val hundredths = remainder % 100
        return "%02d:%02d.%02d".format(minutes, sec, hundredths)
    }
}
