package com.airplayr.app.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airplayr.app.core.data.PlaybackStatsRepository
import com.airplayr.app.core.data.StructuredRepository
import com.airplayr.app.core.data.VIDEO_COMPLETION_FRACTION
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaLibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class VideoPlayerViewModel(
    private val library: MediaLibraryRepository,
    private val structured: StructuredRepository,
    private val stats: PlaybackStatsRepository,
) : ViewModel() {
    private val _video = MutableStateFlow<MediaItem?>(null)
    val video: StateFlow<MediaItem?> = _video.asStateFlow()
    private val _resume = MutableStateFlow(0L)
    val resume: StateFlow<Long> = _resume.asStateFlow()

    private val sessionMutex = Mutex()
    @Volatile private var requestedSessionId: String? = null
    private var activeSessionId: String? = null
    private var completedSessionId: String? = null
    private var startJob: Job? = null

    fun load(id: String) {
        viewModelScope.launch {
            _video.value = library.get(id)
            _resume.value = structured.videoProgress(id).first()?.positionMs ?: 0L
        }
    }

    /** Starts durable history + statistics exactly once for the active video. */
    fun beginPlayback(item: MediaItem, saveHistory: Boolean) {
        if (requestedSessionId == item.id) return
        requestedSessionId = item.id
        completedSessionId = null
        startJob = viewModelScope.launch {
            sessionMutex.withLock {
                if (activeSessionId == item.id) return@withLock
                activeSessionId?.let { previous ->
                    if (previous != item.id) stats.finish()
                }
                activeSessionId = item.id
                if (saveHistory) structured.recordPlay(item)
                stats.start(item)
            }
        }
    }

    private suspend fun awaitSessionStart(item: MediaItem) {
        if (requestedSessionId == item.id) startJob?.join()
    }

    /** Tracks actual playing time and persists continue-watching progress. */
    fun trackPlayback(item: MediaItem, positionMs: Long, durationMs: Long, isPlaying: Boolean) {
        viewModelScope.launch {
            awaitSessionStart(item)
            sessionMutex.withLock {
                if (activeSessionId != item.id) return@withLock
                // Progress is useful while paused too, but statistics must count playback time only
                // while the video is actually playing.
                structured.saveVideoProgress(item.id, positionMs, durationMs)
                if (isPlaying) stats.track(item, positionMs)
                if (durationMs > 0L && positionMs.toDouble() / durationMs.toDouble() >= VIDEO_COMPLETION_FRACTION) {
                    completedSessionId = item.id
                }
            }
        }
    }

    fun completePlayback(item: MediaItem) {
        viewModelScope.launch {
            awaitSessionStart(item)
            sessionMutex.withLock {
                if (activeSessionId != item.id || completedSessionId == item.id) return@withLock
                structured.markCompleted(item.id)
                completedSessionId = item.id
            }
        }
    }

    /** Flushes the final statistics delta and closes this video session. */
    fun finishPlayback(item: MediaItem?, positionMs: Long, durationMs: Long, isPlaying: Boolean) {
        val id = item?.id ?: requestedSessionId ?: return
        val pendingStart = if (id == requestedSessionId) startJob else null
        viewModelScope.launch {
            pendingStart?.join()
            sessionMutex.withLock {
                if (activeSessionId != id) return@withLock
                item?.let {
                    structured.saveVideoProgress(it.id, positionMs, durationMs)
                    if (isPlaying) stats.track(it, positionMs)
                }
                stats.finish()
                activeSessionId = null
                requestedSessionId = null
                completedSessionId = null
                startJob = null
            }
        }
    }

    fun bookmark(id: String, position: Long, label: String?) {
        viewModelScope.launch { structured.addBookmark(id, position, label) }
    }

    fun removeBookmark(id: String, position: Long) {
        viewModelScope.launch { structured.removeBookmark(id, position) }
    }

    suspend fun isFavorite(id: String): Boolean = structured.isFavorite(id)
    fun toggleFavorite(item: MediaItem) { viewModelScope.launch { structured.toggleFavorite(item) } }

}

class VideoPlayerViewModelFactory(
    private val library: MediaLibraryRepository,
    private val structured: StructuredRepository,
    private val stats: PlaybackStatsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        VideoPlayerViewModel(library, structured, stats) as T
}
