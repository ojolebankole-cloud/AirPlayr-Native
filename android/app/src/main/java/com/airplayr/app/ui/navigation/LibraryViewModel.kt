package com.airplayr.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airplayr.app.core.data.StructuredRepository
import com.airplayr.app.core.data.db.FavoriteEntity
import com.airplayr.app.core.data.db.MusicPlaylistEntity
import com.airplayr.app.core.data.db.PlaybackHistoryEntity
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaLibraryRepository
import com.airplayr.app.core.data.media.MediaType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Application-screen library state.
 *
 * The Home pipeline is intentionally kept separate because it has bounded, Home-specific
 * projections. This ViewModel provides the corresponding warm, coherent source for the rest of
 * the application so History/Favorites/Search/Playlists do not each begin from independent
 * empty flows when the destination enters composition.
 */
data class LibraryUiState(
    val refreshing: Boolean = false,
    val lastRefreshAudio: Int? = null,
    val lastRefreshVideo: Int? = null,
    val error: String? = null,
)

data class LibrarySnapshot(
    val audio: List<MediaItem> = emptyList(),
    val video: List<MediaItem> = emptyList(),
    val history: List<PlaybackHistoryEntity> = emptyList(),
    val favorites: List<FavoriteEntity> = emptyList(),
    val playlists: List<MusicPlaylistEntity> = emptyList(),
    val ready: Boolean = false,
)

class LibraryViewModel(
    private val repository: MediaLibraryRepository,
    structured: StructuredRepository? = null,
) : ViewModel() {
    // Nullable warm sources let the snapshot distinguish "not loaded yet" from a real empty
    // library. The public non-null flows keep existing consumers simple, while snapshot.ready
    // becomes a genuine first-emission barrier for screens that must not flash an empty state.
    private val audioSource = repository.observe(MediaType.AUDIO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val videoSource = repository.observe(MediaType.VIDEO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val emptyHistory = kotlinx.coroutines.flow.flowOf(emptyList<PlaybackHistoryEntity>())
    private val emptyFavorites = kotlinx.coroutines.flow.flowOf(emptyList<FavoriteEntity>())
    private val emptyPlaylists = kotlinx.coroutines.flow.flowOf(emptyList<MusicPlaylistEntity>())

    private val resolvedStructured = structured
    private val historySource = (resolvedStructured?.history(100) ?: emptyHistory)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val favoritesSource = (resolvedStructured?.favorites() ?: emptyFavorites)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val playlistsSource = (resolvedStructured?.playlists() ?: emptyPlaylists)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val audio: StateFlow<List<MediaItem>> = audioSource.filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val video: StateFlow<List<MediaItem>> = videoSource.filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val history: StateFlow<List<PlaybackHistoryEntity>> = historySource.filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val favorites: StateFlow<List<FavoriteEntity>> = favoritesSource.filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val playlists: StateFlow<List<MusicPlaylistEntity>> = playlistsSource.filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val folderCount: StateFlow<Int> = repository.observeFolderCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** True only after all warm source flows have emitted their first coherent snapshot. */
    val snapshot: StateFlow<LibrarySnapshot> = combine(
        audioSource,
        videoSource,
        historySource,
        favoritesSource,
        playlistsSource,
    ) { a, v, h, f, p ->
        if (a == null || v == null || h == null || f == null || p == null) {
            LibrarySnapshot()
        } else {
            LibrarySnapshot(a, v, h, f, p, ready = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LibrarySnapshot())

    private val featureCache = MutableStateFlow<Map<String, List<MediaItem>>>(emptyMap())
    val cachedFeatures: StateFlow<Map<String, List<MediaItem>>> = featureCache

    /**
     * Loads a one-shot feature collection at most once per ViewModel lifetime. Re-entering a
     * feature destination therefore reuses the warm result instead of rebuilding from an empty
     * list first. A refresh explicitly replaces the cached value.
     */
    suspend fun loadFeature(key: String, loader: suspend () -> List<MediaItem>, force: Boolean = false) {
        if (!force && featureCache.value.containsKey(key)) return
        val result = runCatching { loader() }.getOrDefault(emptyList())
        featureCache.update { it + (key to result) }
    }

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state

    fun refresh() {
        if (_state.value.refreshing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(refreshing = true, error = null)
            runCatching { repository.deltaRefresh() }
                .onSuccess { result ->
                    _state.value = LibraryUiState(
                        refreshing = false,
                        lastRefreshAudio = result.audioCount,
                        lastRefreshVideo = result.videoCount,
                    )
                }
                .onFailure { error ->
                    _state.value = LibraryUiState(refreshing = false, error = error.message ?: "Library refresh failed")
                }
        }
    }

    fun onPermissionsChanged(granted: Boolean) {
        if (granted) refresh()
    }
}

class LibraryViewModelFactory(
    private val repository: MediaLibraryRepository,
    private val structured: StructuredRepository? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LibraryViewModel(repository, structured) as T
}
