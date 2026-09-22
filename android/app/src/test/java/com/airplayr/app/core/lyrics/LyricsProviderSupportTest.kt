package com.airplayr.app.core.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsProviderSupportTest {
    @Test
    fun lrcParser_buildsOrderedCues() {
        val cues = LyricsProviderSupport.parseLrc("[00:01.00]One\n[00:03.50]Two")
        assertEquals(2, cues.size)
        assertEquals(1.0, cues[0].start, 0.001)
        assertEquals(3.5, cues[0].end, 0.001)
        assertEquals("Two", cues[1].text)
    }

    @Test
    fun ttmlParser_convertsTimedParagraphs() {
        val ttml = """
            <tt xmlns=\"http://www.w3.org/ns/ttml\"><body><div>
              <p begin=\"00:00:01.50\">Hello <span>world</span></p>
              <p begin=\"3s\">Again</p>
            </div></body></tt>
        """.trimIndent()
        val lrc = LyricsProviderSupport.ttmlToLrc(ttml).orEmpty()
        assertTrue(lrc.contains("[00:01.50]Hello world"))
        assertTrue(lrc.contains("[00:03.00]Again"))
    }

    @Test
    fun resultDefaultsSyncLevelFromLyricsFormat() {
        val plain = LyricsProviderSupport.result("test", "Song", "Artist", "Album", 120.0, plain = "text")!!
        val synced = LyricsProviderSupport.result("test", "Song", "Artist", "Album", 120.0, synced = "[00:01.00]text")!!
        assertEquals(1, plain.syncLevel)
        assertEquals(2, synced.syncLevel)
    }
}
