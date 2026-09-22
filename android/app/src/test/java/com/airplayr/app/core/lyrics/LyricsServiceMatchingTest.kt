package com.airplayr.app.core.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsServiceMatchingTest {
    @Test
    fun featureAndArtistCreditVariants_areAccepted() {
        val candidate = LyricsService.Result(
            provider = "LRCLIB",
            trackName = "Different Species (feat. Gunna)",
            artistName = "Offset & Gunna",
            albumName = "KIARI",
            duration = 184.0,
            plainLyrics = "lyrics",
            syncedLyrics = null,
            cues = emptyList(),
            confidence = 0,
        )

        val score = LyricsService.metadataScoreForTest(
            title = "Different Species",
            artist = "Offset, Gunna",
            album = "KIARI",
            duration = 184.0,
            candidate = candidate,
        )

        assertTrue("expected a valid metadata match, got score=$score", score >= 80)
    }

    @Test
    fun differentArtist_isRejected() {
        val candidate = LyricsService.Result(
            provider = "LRCLIB",
            trackName = "Different Species",
            artistName = "Another Artist",
            albumName = "KIARI",
            duration = 184.0,
            plainLyrics = "lyrics",
            syncedLyrics = null,
            cues = emptyList(),
            confidence = 0,
        )

        val score = LyricsService.metadataScoreForTest(
            title = "Different Species",
            artist = "Offset, Gunna",
            album = "KIARI",
            duration = 184.0,
            candidate = candidate,
        )

        assertEquals(0, score)
    }

    @Test
    fun materiallyDifferentDuration_isRejected() {
        val candidate = LyricsService.Result(
            provider = "LRCLIB",
            trackName = "Different Species",
            artistName = "Offset & Gunna",
            albumName = "KIARI",
            duration = 220.0,
            plainLyrics = "lyrics",
            syncedLyrics = null,
            cues = emptyList(),
            confidence = 0,
        )

        val score = LyricsService.metadataScoreForTest(
            title = "Different Species",
            artist = "Offset, Gunna",
            album = "KIARI",
            duration = 184.0,
            candidate = candidate,
        )

        assertEquals(0, score)
    }
}
