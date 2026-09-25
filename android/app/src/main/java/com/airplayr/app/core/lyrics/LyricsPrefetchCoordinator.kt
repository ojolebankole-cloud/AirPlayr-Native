package com.airplayr.app.core.lyrics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Coordinates background lyrics warm-up from the centralized playback controller.
 * It deliberately does not own a second lyrics cache: every request goes through the
 * application-scoped LyricsRepository, which provides cache lookup and in-flight deduplication.
 */
class LyricsPrefetchCoordinator(
    private val repository: LyricsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun prefetch(id: String, track: LyricsTrack) {
        if (id.isBlank() || track.title.isBlank() || track.artist.isBlank()) return
        scope.launch {
            runCatching { repository.fetchShared(id, track, force = false).await() }
        }
    }
}
