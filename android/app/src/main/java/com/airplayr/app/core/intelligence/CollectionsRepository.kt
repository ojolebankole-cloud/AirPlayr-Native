package com.airplayr.app.core.intelligence

import com.airplayr.app.core.data.StructuredRepository
import com.airplayr.app.core.data.db.PlaybackHistoryEntity
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaLibraryRepository
import com.airplayr.app.core.data.media.MediaType
import kotlinx.coroutines.flow.first

/** Music-first smart collections mirroring the reference app's library intelligence. */
class CollectionsRepository(private val library: MediaLibraryRepository, private val structured: StructuredRepository) {
    suspend fun recentlyAdded(limit: Int = 50): List<MediaItem> =
        library.page(MediaType.AUDIO, 250, 0).sortedByDescending { it.dateAdded }.take(limit)

    suspend fun recentlyPlayed(limit: Int = 50): List<PlaybackHistoryEntity> =
        structured.recent((limit * 3).coerceAtMost(500)).filter { it.mediaType == MediaType.AUDIO.value }.take(limit)

    suspend fun recentlyPlayedItems(limit: Int = 50): List<MediaItem> {
        val audio = library.page(MediaType.AUDIO, 250, 0).associateBy { it.id }
        return recentlyPlayed(limit).mapNotNull { audio[it.mediaId] }
    }

    suspend fun mostPlayed(limit: Int = 50): List<MediaItem> {
        val counts = structured.recent(500).filter { it.mediaType == MediaType.AUDIO.value }.associateBy({ it.mediaId }, { it.playCount })
        return library.page(MediaType.AUDIO, 250, 0).sortedWith(compareByDescending<MediaItem> { counts[it.id] ?: 0 }.thenBy { it.title.lowercase() }).take(limit)
    }

    suspend fun favorites(limit: Int = 50): List<MediaItem> {
        val ids = structured.favoriteIds()
        return library.page(MediaType.AUDIO, 250, 0).filter { it.id in ids }.sortedBy { it.title.lowercase() }.take(limit)
    }

    suspend fun continueWatching(limit: Int = 50): List<MediaItem> {
        val progress = structured.allVideoProgress().first().filter { !it.completed && it.positionMs > 0 }
            .sortedByDescending { it.updatedAt }.take(limit)
        val lookup = library.page(MediaType.VIDEO, 250, 0).associateBy { it.id }
        return progress.mapNotNull { lookup[it.mediaId] }
    }

    suspend fun unwatched(limit: Int = 50): List<MediaItem> {
        val completed = structured.allVideoProgress().first().filter { it.completed }.map { it.mediaId }.toSet()
        return library.page(MediaType.VIDEO, 250, 0).filterNot { it.id in completed }.take(limit)
    }

    suspend fun forYou(limit: Int = 40): List<MediaItem> =
        (favorites(limit) + recentlyAdded(limit) + recentlyPlayedItems(limit)).distinctBy { it.id }.take(limit)

    suspend fun rediscover(limit: Int = 40): List<MediaItem> =
        recentlyPlayedItems(limit)

    suspend fun rediscoverVideos(limit: Int = 40): List<MediaItem> {
        // Rediscover is intentionally driven by repeated watching, not simply the last video
        // opened. Aggregate the durable history counts and use recency only as the tie-breaker.
        val history = structured.recent(500).filter { it.mediaType == MediaType.VIDEO.value }
        val videos = library.page(MediaType.VIDEO, 250, 0).associateBy { it.id }
        val latestById = history.groupBy { it.mediaId }.mapValues { (_, rows) -> rows.maxByOrNull { it.playedAt } }
        val countById = history.groupBy { it.mediaId }.mapValues { (_, rows) -> rows.maxOfOrNull { it.playCount } ?: rows.size }
        return countById.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { latestById[it.key]?.playedAt ?: 0L })
            .mapNotNull { videos[it.key] }
            .take(limit)
    }

    suspend fun tunedIn(limit: Int = 40): List<MediaItem> =
        recentlyPlayedItems(limit)

    suspend fun rockYourWorld(limit: Int = 40): List<MediaItem> =
        (mostPlayed(limit * 2) + favorites(limit * 2) + recentlyAdded(limit * 2)).distinctBy { it.id }.take(limit)
}
