package com.airplayr.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModelProvider
import com.airplayr.app.core.data.StructuredRepository
import com.airplayr.app.core.data.db.PlaybackHistoryEntity
import com.airplayr.app.core.data.db.VideoProgressEntity
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaLibraryRepository
import com.airplayr.app.core.data.media.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application-scoped Home data. It stays warm while the user moves through the app, so
 * returning to Home consumes an already-built snapshot instead of rebuilding the Home graph.
 */
class HomeRepository(
    private val mediaLibraryProvider: () -> MediaLibraryRepository,
    private val structuredProvider: () -> StructuredRepository,
    private val applicationScope: CoroutineScope,
) {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private val _ready = kotlinx.coroutines.flow.MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    @Volatile
    private var started = false
    private var publishedInitialState = false
    private val currentFolderCount = kotlinx.coroutines.flow.MutableStateFlow(0)
    private var lastLookupFingerprint: Int? = null
    private var lastLookup: Map<String, MediaItem> = emptyMap()

    /**
     * Starts the Home data pipeline exactly once, on the application IO scope. Repository
     * construction itself is deliberately cheap: no Room DAO/Flow is touched until this method
     * is executed, so the Activity thread never has to open the database just to create Home.
     */
    @Synchronized
    fun start() {
        if (started) return
        started = true
        applicationScope.launch {
            // Resolve the Room-backed repositories only on the application IO dispatcher.
            // Constructing Home must never force a Room open on the Activity thread.
            val mediaLibrary = mediaLibraryProvider()
            val structured = structuredProvider()
            val recentAudio = mediaLibrary.observeRecent(MediaType.AUDIO, HOME_RECENT_LIMIT)
            val recentVideo = mediaLibrary.observeRecent(MediaType.VIDEO, HOME_RECENT_LIMIT)
            val history = structured.history(HOME_HISTORY_LIMIT)
            val repeated = structured.repeated(HOME_REPEAT_LIMIT)
            val videoProgress = structured.recentVideoProgress(HOME_VIDEO_PROGRESS_LIMIT)
            val audioCount = mediaLibrary.observeCount(MediaType.AUDIO)
            val videoCount = mediaLibrary.observeCount(MediaType.VIDEO)
            val folderCount = mediaLibrary.observeFolderCount()

            // Folder count is only needed by the drawer, not by the visible Home rails. Collect
            // it independently so a large-library DISTINCT scan can never hold up the first
            // Home content snapshot.
            applicationScope.launch {
                folderCount.collectLatest { folders ->
                    currentFolderCount.value = folders
                    val updated = _state.value.copy(folderCount = folders)
                    if (updated != _state.value) _state.value = updated
                }
            }

            combine(
                recentAudio,
                recentVideo,
                history,
                repeated,
                videoProgress,
                audioCount,
                videoCount,
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                HomeInputs(
                    audio = values[0] as List<MediaItem>,
                    video = values[1] as List<MediaItem>,
                    history = values[2] as List<PlaybackHistoryEntity>,
                    repeated = values[3] as List<PlaybackHistoryEntity>,
                    videoProgress = values[4] as List<VideoProgressEntity>,
                    audioCount = values[5] as Int,
                    videoCount = values[6] as Int,
                )
            }.distinctUntilChanged().collectLatest { inputs ->
                // Publish the bounded lists/counts first so Home has useful content as soon as
                // Room produces its snapshot. Enrichment happens off the UI thread and is then
                // swapped into the same app-scoped state.
                if (!publishedInitialState) {
                    _state.value = buildHomeState(inputs, emptyMap()).copy(folderCount = currentFolderCount.value)
                    publishedInitialState = true
                }

                val ids = buildSet {
                    addAll(inputs.audio.map { it.id })
                    addAll(inputs.video.map { it.id })
                    addAll(inputs.history.map { it.mediaId })
                    addAll(inputs.repeated.map { it.mediaId })
                    addAll(inputs.videoProgress.map { it.mediaId })
                }
                // Folder/count-only Room emissions do not change the media rows themselves.
                // Reuse the previous enrichment for those emissions instead of issuing another
                // SELECT ... IN (...) against the same IDs.
                val lookupFingerprint = listOf(
                    inputs.audio, inputs.video, inputs.history, inputs.repeated, inputs.videoProgress, ids
                ).hashCode()
                val lookup = if (lookupFingerprint == lastLookupFingerprint) {
                    lastLookup
                } else {
                    val loaded = if (ids.isEmpty()) emptyMap() else mediaLibrary.getByIds(ids).associateBy { it.id }
                    lastLookupFingerprint = lookupFingerprint
                    lastLookup = loaded
                    loaded
                }
                val newState = buildHomeState(inputs, lookup).copy(folderCount = currentFolderCount.value)
                if (newState != _state.value) _state.value = newState
                if (!_ready.value) _ready.value = true
            }
        }
    }

    /** Warm Home before the first navigation whenever possible. */
    suspend fun prewarm(timeoutMs: Long = 4_000L) {
        start()
        kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            _ready.first { it }
        }
    }

    companion object {
        private const val HOME_RECENT_LIMIT = 30
        private const val HOME_HISTORY_LIMIT = 100
        private const val HOME_REPEAT_LIMIT = 20
        private const val HOME_VIDEO_PROGRESS_LIMIT = 30
    }
}

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {
    val state: StateFlow<HomeUiState> = repository.state
}

class HomeViewModelFactory(
    private val repository: HomeRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(repository) as T
}

@Immutable
data class HomeUiState(
    val recentSongs: List<MediaItem> = emptyList(),
    val lookup: Map<String, MediaItem> = emptyMap(),
    val recentPlayedAlbums: List<Pair<String, List<MediaItem>>> = emptyList(),
    val onRepeat: List<MediaItem> = emptyList(),
    val recentArtists: List<Pair<String, List<MediaItem>>> = emptyList(),
    val currentlyWatching: List<MediaItem> = emptyList(),
    val completedWatching: List<MediaItem> = emptyList(),
    val recentVideos: List<MediaItem> = emptyList(),
    val recentlyPlayedVideos: List<MediaItem> = emptyList(),
    val history: List<PlaybackHistoryEntity> = emptyList(),
    val audioCount: Int = 0,
    val videoCount: Int = 0,
    val folderCount: Int = 0,
)

private data class HomeInputs(
    val audio: List<MediaItem>,
    val video: List<MediaItem>,
    val history: List<PlaybackHistoryEntity>,
    val repeated: List<PlaybackHistoryEntity>,
    val videoProgress: List<VideoProgressEntity>,
    val audioCount: Int,
    val videoCount: Int,
)

private fun buildHomeState(inputs: HomeInputs, lookup: Map<String, MediaItem>): HomeUiState {
    val audioHistory = inputs.history.filter { it.mediaType == MediaType.AUDIO.value }
    val videoHistory = inputs.history.filter { it.mediaType == MediaType.VIDEO.value }
    val recentPlayedAlbums = audioHistory.mapNotNull { lookup[it.mediaId] }
        .groupBy { (it.album ?: "Unknown album") to (it.albumArtist ?: it.artist ?: "") }
        .map { (key, songs) -> key.first to songs }
        .take(10)
    val onRepeat = inputs.repeated.mapNotNull { lookup[it.mediaId] }.distinctBy { it.id }.take(10)
    val recentArtists = audioHistory.mapNotNull { lookup[it.mediaId] }
        .groupBy { it.artist?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown artist" }
        .map { it.key to it.value }
        .take(10)
    return HomeUiState(
        recentSongs = inputs.audio.take(10),
        lookup = lookup,
        recentPlayedAlbums = recentPlayedAlbums,
        onRepeat = onRepeat,
        recentArtists = recentArtists,
        currentlyWatching = inputs.videoProgress.filter { !it.completed && it.positionMs > 0 }
            .sortedByDescending { it.updatedAt }.mapNotNull { lookup[it.mediaId] }.take(10),
        completedWatching = inputs.videoProgress.filter { it.completed }
            .sortedByDescending { it.updatedAt }.mapNotNull { lookup[it.mediaId] }.take(10),
        recentVideos = inputs.video.take(10),
        recentlyPlayedVideos = videoHistory.mapNotNull { lookup[it.mediaId] }.distinctBy { it.id }.take(10),
        history = inputs.history,
        audioCount = inputs.audioCount,
        videoCount = inputs.videoCount,
        folderCount = 0,
    )
}

