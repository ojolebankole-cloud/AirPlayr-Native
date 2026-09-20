package com.airplayr.app.ui.navigation

import android.content.Context
import com.airplayr.app.core.data.StructuredRepository
import org.json.JSONObject
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
    val watchingSeconds: Long = 0L,
    val musicPlays: Int = 0,
    val videoPlays: Int = 0,
    val completedVideos: Int = 0,
    val topTrack: String = "—",
    val topVideo: String = "—",
)

/**
 * Small persistent cache for the Home statistics card. It keeps only lightweight aggregates,
 * so Home can paint the last-known values immediately and refresh them in the background.
 */
class HomeStatsCache(
    context: Context,
    private val structuredProvider: () -> StructuredRepository,
) {
    private val mutex = Mutex()
    private val prefs = context.applicationContext.getSharedPreferences("airplayr_home_stats", Context.MODE_PRIVATE)
    private val _summary = MutableStateFlow(loadCached())
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
                        watchingSeconds = structured.totalPlaybackSeconds(MediaType.VIDEO),
                        musicPlays = structured.totalPlaybackCount(MediaType.AUDIO),
                        videoPlays = structured.totalPlaybackCount(MediaType.VIDEO),
                        completedVideos = structured.completedVideoCount(),
                        topTrack = structured.topPlaybackTitle(MediaType.AUDIO) ?: "—",
                        topVideo = structured.topPlaybackTitle(MediaType.VIDEO) ?: "—",
                    )
                }.getOrNull()
            }
            if (value != null) {
                _summary.value = value
                prefs.edit().putString("snapshot", JSONObject()
                    .put("listeningSeconds", value.listeningSeconds)
                    .put("watchingSeconds", value.watchingSeconds)
                    .put("musicPlays", value.musicPlays)
                    .put("videoPlays", value.videoPlays)
                    .put("completedVideos", value.completedVideos)
                    .put("topTrack", value.topTrack)
                    .put("topVideo", value.topVideo)
                    .toString()).apply()
                lastLoadedAt = now
            }
        }
    }

    private fun loadCached(): HomeStatsSnapshot? = runCatching {
        val o = prefs.getString("snapshot", null)?.let(::JSONObject) ?: return null
        HomeStatsSnapshot(
            listeningSeconds = o.optLong("listeningSeconds"),
            watchingSeconds = o.optLong("watchingSeconds"),
            musicPlays = o.optInt("musicPlays"),
            videoPlays = o.optInt("videoPlays"),
            completedVideos = o.optInt("completedVideos"),
            topTrack = o.optString("topTrack", "—"),
            topVideo = o.optString("topVideo", "—"),
        )
    }.getOrNull()

    companion object {
        private const val CACHE_TTL_MS = 15_000L
    }
}
