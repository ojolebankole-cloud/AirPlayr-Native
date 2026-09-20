package com.airplayr.app.ui.navigation

import com.airplayr.app.core.data.StructuredRepository
import com.airplayr.app.core.data.media.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class HomeStatsSnapshot(
    val listeningSeconds: Long = 0L,
    val completedVideos: Int = 0,
)

/**
 * Small app-process cache for the Home statistics card. Home only needs two aggregate values;
 * it must not run the full StatsScreen all-history report just to render a card.
 */
class HomeStatsCache(
    private val structuredProvider: () -> StructuredRepository,
) {
    private val mutex = Mutex()
    private val _summary = MutableStateFlow<HomeStatsSnapshot?>(null)
    val summary: StateFlow<HomeStatsSnapshot?> = _summary.asStateFlow()
    private var lastLoadedAt = 0L

    suspend fun refreshIfStale(force: Boolean = false) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && _summary.value != null && now - lastLoadedAt < CACHE_TTL_MS) return
            val value = withContext(Dispatchers.IO) {
                runCatching {
                    val structured = structuredProvider()
                    HomeStatsSnapshot(
                        listeningSeconds = structured.totalPlaybackSeconds(MediaType.AUDIO),
                        completedVideos = structured.completedVideoCount(),
                    )
                }.getOrNull()
            }
            if (value != null) {
                _summary.value = value
                lastLoadedAt = now
            }
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 15_000L
    }
}
