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


/**
 * Application-scoped last-known library state. It stores references to the immutable lists already
 * held by the live ViewModel rather than making a second copy of the library. This is intentionally
 * in-memory only: Room remains the source of truth and updates replace the snapshot as soon as they
 * arrive.
 */
class LibraryWarmCache {
    // Soft references make this a real warm cache rather than a permanent duplicate of a large
    // media library. Android may reclaim these lists under memory pressure; Room will then simply
    // repopulate them in the normal background path.
    @Volatile private var audioRef: java.lang.ref.SoftReference<List<MediaItem>>? = null
    @Volatile private var videoRef: java.lang.ref.SoftReference<List<MediaItem>>? = null
    @Volatile private var historyRef: java.lang.ref.SoftReference<List<PlaybackHistoryEntity>>? = null
    @Volatile private var favoritesRef: java.lang.ref.SoftReference<List<FavoriteEntity>>? = null
    @Volatile private var playlistsRef: java.lang.ref.SoftReference<List<MusicPlaylistEntity>>? = null
    @Volatile var folderCount: Int? = null
    @Volatile var features: Map<String, List<MediaItem>> = emptyMap()

    var audio: List<MediaItem>?
        get() = audioRef?.get()
        set(value) { audioRef = value?.let { javaSoftReference(it) } }
    var video: List<MediaItem>?
        get() = videoRef?.get()
        set(value) { videoRef = value?.let { javaSoftReference(it) } }
    var history: List<PlaybackHistoryEntity>?
        get() = historyRef?.get()
        set(value) { historyRef = value?.let { javaSoftReference(it) } }
    var favorites: List<FavoriteEntity>?
        get() = favoritesRef?.get()
        set(value) { favoritesRef = value?.let { javaSoftReference(it) } }
    var playlists: List<MusicPlaylistEntity>?
        get() = playlistsRef?.get()
        set(value) { playlistsRef = value?.let { javaSoftReference(it) } }

    private fun <T> javaSoftReference(value: T): java.lang.ref.SoftReference<T> = java.lang.ref.SoftReference(value)
}

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
    private val warmCache: LibraryWarmCache? = null,
) : ViewModel() {
    // Nullable warm sources let the snapshot distinguish "not loaded yet" from a real empty
    // library. The public non-null flows keep existing consumers simple, while snapshot.ready
    // becomes a genuine first-emission barrier for screens that must not flash an empty state.
    private val audioSource = repository.observe(MediaType.AUDIO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, warmCache?.audio)
    private val videoSource = repository.observe(MediaType.VIDEO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, warmCache?.video)

    private val emptyHistory = kotlinx.coroutines.flow.flowOf(emptyList<PlaybackHistoryEntity>())
    private val emptyFavorites = kotlinx.coroutines.flow.flowOf(emptyList<FavoriteEntity>())
    private val emptyPlaylists = kotlinx.coroutines.flow.flowOf(emptyList<MusicPlaylistEntity>())

    private val resolvedStructured = structured
    private val historySource = (resolvedStructured?.history(100) ?: emptyHistory)
        .stateIn(viewModelScope, SharingStarted.Eagerly, warmCache?.history)
    private val favoritesSource = (resolvedStructured?.favorites() ?: emptyFavorites)
        .stateIn(viewModelScope, SharingStarted.Eagerly, warmCache?.favorites)
    private val playlistsSource = (resolvedStructured?.playlists() ?: emptyPlaylists)
        .stateIn(viewModelScope, SharingStarted.Eagerly, warmCache?.playlists)

    val audio: StateFlow<List<MediaItem>> = audioSource.filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, warmCache?.audio ?: emptyList())
    val video: StateFlow<List<MediaItem>> = videoSource.filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, warmCache?.video ?: emptyList())
    val history: StateFlow<List<PlaybackHistoryEntity>> = historySource.filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, warmCache?.history ?: emptyList())
    val favorites: StateFlow<List<FavoriteEntity>> = favoritesSource.filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, warmCache?.favorites ?: emptyList())
    val playlists: StateFlow<List<MusicPlaylistEntity>> = playlistsSource.filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, warmCache?.playlists ?: emptyList())
    val folderCount: StateFlow<Int> = repository.observeFolderCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, warmCache?.folderCount ?: 0)

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
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        warmCache?.let { cache ->
            if (cache.audio != null && cache.video != null && cache.history != null && cache.favorites != null && cache.playlists != null) {
                LibrarySnapshot(
                    audio = cache.audio!!,
                    video = cache.video!!,
                    history = cache.history!!,
                    favorites = cache.favorites!!,
                    playlists = cache.playlists!!,
                    ready = true,
                )
            } else null
        } ?: LibrarySnapshot(),
    )

    private val featureCache = MutableStateFlow(warmCache?.features ?: emptyMap())
    val cachedFeatures: StateFlow<Map<String, List<MediaItem>>> = featureCache

    init {
        // Keep the application-level warm cache pointing at the same immutable list instances.
        // This avoids serialisation/copying while making Activity/ViewModel recreation instant.
        viewModelScope.launch {
            snapshot.collect { value ->
                if (!value.ready || warmCache == null) return@collect
                warmCache.audio = value.audio
                warmCache.video = value.video
                warmCache.history = value.history
                warmCache.favorites = value.favorites
                warmCache.playlists = value.playlists
            }
        }
        viewModelScope.launch {
            folderCount.collect { value -> if (warmCache != null) warmCache.folderCount = value }
        }
        viewModelScope.launch {
            featureCache.collect { value -> if (warmCache != null) warmCache.features = value }
        }
    }

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
    private val warmCache: LibraryWarmCache? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LibraryViewModel(repository, structured, warmCache) as T
}
