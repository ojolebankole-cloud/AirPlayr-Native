package com.airplayr.app.core.intelligence

import com.airplayr.app.core.data.db.PlaybackHistoryEntity
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicMixGeneratorTest {
    private val date = LocalDate.of(2026, 9, 24)
    private val songs = listOf(
        song("gospel", "Gospel"), song("highlife", "Highlife"), song("afro", "Afrobeat"),
        song("pop", "Pop"), song("alte", "Alté"), song("hiphop", "Hip Hop"),
        song("trap", "Trap"), song("rnb", "R&B"), song("rock", "Rock"),
    )

    @Test
    fun sameDayAndMixStayDeterministic() {
        val config = MusicMixGenerator.configForHour(14)
        val first = MusicMixGenerator.dailyMix(config, songs, emptyList(), emptyList(), date = date)
        val second = MusicMixGenerator.dailyMix(config, songs, emptyList(), emptyList(), date = date)
        assertEquals(first.map { it.id }, second.map { it.id })
    }

    @Test
    fun nextDayChangesDailyIdentity() {
        val config = MusicMixGenerator.configForHour(14)
        val first = MusicMixGenerator.dailyMix(config, songs, emptyList(), emptyList(), date = date)
        val second = MusicMixGenerator.dailyMix(config, songs, emptyList(), emptyList(), date = date.plusDays(1))
        assertNotEquals(first.map { it.id }, second.map { it.id })
    }

    @Test
    fun missingPriorityGenreFallsBackToAvailableLibrary() {
        val config = MusicMixGenerator.configForHour(6)
        val result = MusicMixGenerator.dailyMix(config, listOf(song("rock", "Rock")), emptyList(), emptyList(), date = date)
        assertEquals(listOf("rock"), result.map { it.id })
    }

    @Test
    fun timeOfDayPriorityRulesMatchProductSpec() {
        assertEquals(listOf("Gospel", "Highlife", "Afrobeat"), MusicMixGenerator.configForHour(6).priorityGenres)
        assertEquals(listOf("Pop"), MusicMixGenerator.configForHour(10).priorityGenres)
        assertEquals(listOf("Afrobeat"), MusicMixGenerator.configForHour(14).priorityGenres)
        assertEquals(listOf("Alté"), MusicMixGenerator.configForHour(18).priorityGenres)
        assertEquals(listOf("Hip Hop", "Trap"), MusicMixGenerator.configForHour(21).priorityGenres)
        assertTrue(MusicMixGenerator.configForHour(23).priorityGenres.isEmpty())
        assertEquals(listOf("R&B"), MusicMixGenerator.configForHour(3).priorityGenres)
    }

    @Test
    fun rockYourWorldIncludesWholeLibraryWithRepeatedSongsFirst() {
        val history = listOf(
            PlaybackHistoryEntity("rock", "audio", "rock", "Artist", "Album", null, 1L, 5, false, null),
            PlaybackHistoryEntity("pop", "audio", "pop", "Artist", "Album", null, 1L, 2, false, null),
        )
        val result = MusicMixGenerator.rockYourWorldMix(songs, history, emptyList(), date = date)
        assertEquals(songs.map { it.id }.toSet(), result.map { it.id }.toSet())
        val repeatedIds = result.take(2).map { it.id }.toSet()
        assertTrue("rock" in repeatedIds)
        assertTrue("pop" in repeatedIds)
    }

    @Test
    fun genreFilterUsesOnlyGenresPresentInLibrary() {
        val config = MusicMixGenerator.configForHour(14)
        val result = MusicMixGenerator.dailyMix(config, songs, emptyList(), emptyList(), selectedGenre = "Afrobeat", date = date)
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.genre == "Afrobeat" })
        assertTrue(MusicMixGenerator.availableGenres(songs).contains("Afrobeat"))
    }

    private fun song(id: String, genre: String) = MediaItem(
        id = id, mediaStoreId = null, uri = "content://airplayr/$id", title = id, filename = "$id.mp3",
        artist = "Artist", album = "Album", albumArtist = "Artist", durationMs = 180_000L,
        width = 0, height = 0, sizeBytes = 1L, dateModified = 1L, dateAdded = 1L,
        mediaType = MediaType.AUDIO, mimeType = "audio/mpeg", albumId = null,
        artworkUri = null, thumbnailUri = null, genre = genre, sourceDirectoryUri = null,
        folderId = null, folder = null, path = null, relativePath = null,
    )
}
