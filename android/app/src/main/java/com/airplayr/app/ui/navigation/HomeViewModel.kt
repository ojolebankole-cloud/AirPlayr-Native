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
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Application-scoped Home data. It stays warm while the user moves through the app, so
 * returning to Home consumes an already-built snapshot instead of rebuilding the Home graph.
 */
class HomeRepository(
    private val mediaLibraryProvider: () -> MediaLibraryRepository,
    private val structuredProvider: () -> StructuredRepository,
    private val applicationScope: CoroutineScope,
    private val snapshotStore: HomeSnapshotStore,
) {
    private val initialSnapshot = snapshotStore.load()
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(
        initialSnapshot?.let { buildHomeState(it.inputs, it.lookup) } ?: HomeUiState()
    )
    val state: StateFlow<HomeUiState> = _state

    private val _ready = kotlinx.coroutines.flow.MutableStateFlow(initialSnapshot != null)
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
                    // If a render-ready snapshot already exists, keep it on screen until the
                    // first live Room lookup is complete. This prevents a snapshot -> empty/partial
                    // state flash during process re-entry.
                    if (initialSnapshot == null) {
                        _state.value = buildHomeState(inputs, emptyMap()).copy(folderCount = currentFolderCount.value)
                    }
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
                // Persist the bounded, render-ready Home inputs after Room enrichment. The next
                // process launch can paint the previous Home immediately while this observer
                // reconciles it with the live database in the background.
                snapshotStore.save(inputs, lookup)
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


/**
 * Small, bounded last-known Home snapshot. This is deliberately separate from Room: it is only
 * a presentation cache used to eliminate the empty-Home frame on process re-entry. Room remains
 * the source of truth and reconciles the snapshot immediately on the application IO scope.
 */
class HomeSnapshotStore(context: Context) {
    private val prefs = context.getSharedPreferences("airplayr_home_snapshot", Context.MODE_PRIVATE)
    private val key = "snapshot_v2"

    data class Snapshot(val inputs: HomeInputs, val lookup: Map<String, MediaItem>)

    fun load(): Snapshot? = runCatching {
        val root = prefs.getString(key, null)?.let(::JSONObject) ?: return null
        val inputs = HomeInputs(
            audio = root.getJSONArray("audio").toMediaItems(),
            video = root.getJSONArray("video").toMediaItems(),
            history = root.getJSONArray("history").toHistory(),
            repeated = root.getJSONArray("repeated").toHistory(),
            videoProgress = root.getJSONArray("videoProgress").toVideoProgress(),
            audioCount = root.optInt("audioCount", 0),
            videoCount = root.optInt("videoCount", 0),
        )
        val lookup = root.optJSONArray("lookup")?.toMediaItems()?.associateBy { it.id }.orEmpty()
        Snapshot(inputs, lookup)
    }.getOrNull()

    fun save(inputs: HomeInputs, lookup: Map<String, MediaItem>) {
        runCatching {
            val root = JSONObject()
                .put("audio", JSONArray(inputs.audio.map(::mediaToJson)))
                .put("video", JSONArray(inputs.video.map(::mediaToJson)))
                .put("history", JSONArray(inputs.history.map(::historyToJson)))
                .put("repeated", JSONArray(inputs.repeated.map(::historyToJson)))
                .put("videoProgress", JSONArray(inputs.videoProgress.map(::videoProgressToJson)))
                .put("audioCount", inputs.audioCount)
                .put("videoCount", inputs.videoCount)
                .put("lookup", JSONArray(lookup.values.map(::mediaToJson)))
            prefs.edit().putString(key, root.toString()).apply()
        }
    }

    private fun mediaToJson(item: MediaItem) = JSONObject().apply {
        put("id", item.id); put("mediaStoreId", item.mediaStoreId ?: JSONObject.NULL); put("uri", item.uri)
        put("title", item.title); put("filename", item.filename); put("artist", item.artist ?: JSONObject.NULL)
        put("album", item.album ?: JSONObject.NULL); put("albumArtist", item.albumArtist ?: JSONObject.NULL)
        put("durationMs", item.durationMs); put("width", item.width); put("height", item.height); put("sizeBytes", item.sizeBytes)
        put("dateModified", item.dateModified); put("dateAdded", item.dateAdded); put("mediaType", item.mediaType.value)
        put("mimeType", item.mimeType ?: JSONObject.NULL); put("albumId", item.albumId ?: JSONObject.NULL)
        put("artworkUri", item.artworkUri ?: JSONObject.NULL); put("thumbnailUri", item.thumbnailUri ?: JSONObject.NULL)
        put("genre", item.genre ?: JSONObject.NULL); put("sourceDirectoryUri", item.sourceDirectoryUri ?: JSONObject.NULL)
        put("folderId", item.folderId ?: JSONObject.NULL); put("folder", item.folder ?: JSONObject.NULL)
        put("path", item.path ?: JSONObject.NULL); put("relativePath", item.relativePath ?: JSONObject.NULL)
    }

    private fun historyToJson(item: PlaybackHistoryEntity) = JSONObject().apply {
        put("mediaId", item.mediaId); put("mediaType", item.mediaType); put("title", item.title)
        put("artist", item.artist ?: JSONObject.NULL); put("album", item.album ?: JSONObject.NULL)
        put("artworkUri", item.artworkUri ?: JSONObject.NULL); put("playedAt", item.playedAt); put("playCount", item.playCount)
        put("completed", item.completed); put("completedAt", item.completedAt ?: JSONObject.NULL)
    }

    private fun videoProgressToJson(item: VideoProgressEntity) = JSONObject().apply {
        put("mediaId", item.mediaId); put("positionMs", item.positionMs); put("durationMs", item.durationMs)
        put("completed", item.completed); put("updatedAt", item.updatedAt)
    }

    private fun JSONArray.toMediaItems(): List<MediaItem> = buildList {
        for (i in 0 until length()) optJSONObject(i)?.let { o ->
            add(MediaItem(
                id = o.optString("id"), mediaStoreId = o.longOrNull("mediaStoreId"), uri = o.optString("uri"),
                title = o.optString("title"), filename = o.optString("filename"), artist = o.stringOrNull("artist"),
                album = o.stringOrNull("album"), albumArtist = o.stringOrNull("albumArtist"), durationMs = o.optLong("durationMs"),
                width = o.optInt("width"), height = o.optInt("height"), sizeBytes = o.optLong("sizeBytes"),
                dateModified = o.optLong("dateModified"), dateAdded = o.optLong("dateAdded"), mediaType = MediaType.from(o.optString("mediaType")),
                mimeType = o.stringOrNull("mimeType"), albumId = o.longOrNull("albumId"), artworkUri = o.stringOrNull("artworkUri"),
                thumbnailUri = o.stringOrNull("thumbnailUri"), genre = o.stringOrNull("genre"), sourceDirectoryUri = o.stringOrNull("sourceDirectoryUri"),
                folderId = o.stringOrNull("folderId"), folder = o.stringOrNull("folder"), path = o.stringOrNull("path"), relativePath = o.stringOrNull("relativePath"),
            ))
        }
    }

    private fun JSONArray.toHistory(): List<PlaybackHistoryEntity> = buildList {
        for (i in 0 until length()) optJSONObject(i)?.let { o ->
            add(PlaybackHistoryEntity(
                mediaId = o.optString("mediaId"), mediaType = o.optString("mediaType"), title = o.optString("title"),
                artist = o.stringOrNull("artist"), album = o.stringOrNull("album"), artworkUri = o.stringOrNull("artworkUri"),
                playedAt = o.optLong("playedAt"), playCount = o.optInt("playCount"), completed = o.optBoolean("completed"),
                completedAt = o.longOrNull("completedAt"),
            ))
        }
    }

    private fun JSONArray.toVideoProgress(): List<VideoProgressEntity> = buildList {
        for (i in 0 until length()) optJSONObject(i)?.let { o ->
            add(VideoProgressEntity(o.optString("mediaId"), o.optLong("positionMs"), o.optLong("durationMs"), o.optBoolean("completed"), o.optLong("updatedAt")))
        }
    }

    private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)
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

data class HomeInputs(
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
    // History is durable user data; it must remain renderable even when a media-store row was
    // removed, moved, or is temporarily inaccessible. The history row contains enough identity
    // and presentation data to build a non-playable placeholder until the item returns.
    fun historicalItem(h: PlaybackHistoryEntity): MediaItem = MediaItem(
        id = h.mediaId, mediaStoreId = null, uri = h.mediaId, title = h.title, filename = h.title,
        artist = h.artist, album = h.album, albumArtist = h.artist, durationMs = 0L, width = 0, height = 0,
        sizeBytes = 0L, dateModified = 0L, dateAdded = h.playedAt,
        mediaType = MediaType.from(h.mediaType), mimeType = null, albumId = null,
        artworkUri = h.artworkUri.takeIf { h.mediaType == MediaType.AUDIO.value },
        thumbnailUri = h.artworkUri.takeIf { h.mediaType == MediaType.VIDEO.value },
        genre = null, sourceDirectoryUri = null, folderId = null, folder = null,
        path = null, relativePath = null,
    )
    val historyLookup = buildMap {
        putAll(lookup)
        inputs.history.forEach { h -> if (!containsKey(h.mediaId)) put(h.mediaId, historicalItem(h)) }
    }
    val recentPlayedAlbums = audioHistory.mapNotNull { historyLookup[it.mediaId] }
        .groupBy { (it.album ?: "Unknown album") to (it.albumArtist ?: it.artist ?: "") }
        .map { (key, songs) -> key.first to songs }
        .take(10)
    val onRepeat = inputs.repeated.mapNotNull { historyLookup[it.mediaId] }.distinctBy { it.id }.take(10)
    val recentArtists = audioHistory.mapNotNull { historyLookup[it.mediaId] }
        .groupBy { it.artist?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown artist" }
        .map { it.key to it.value }
        .take(10)
    return HomeUiState(
        recentSongs = inputs.audio.take(10),
        lookup = historyLookup,
        recentPlayedAlbums = recentPlayedAlbums,
        onRepeat = onRepeat,
        recentArtists = recentArtists,
        currentlyWatching = inputs.videoProgress.filter { !it.completed && it.positionMs > 0 }
            .sortedByDescending { it.updatedAt }.mapNotNull { historyLookup[it.mediaId] }.take(10),
        completedWatching = videoHistory.filter { it.completed }
            .sortedByDescending { it.completedAt ?: it.playedAt }
            .mapNotNull { historyLookup[it.mediaId] }
            .distinctBy { it.id }
            .take(10),
        recentVideos = inputs.video.take(10),
        recentlyPlayedVideos = videoHistory
            .asSequence()
            .filter { it.mediaType == MediaType.VIDEO.value }
            .sortedByDescending { it.playedAt }
            .mapNotNull { historyLookup[it.mediaId] }
            .filter { it.mediaType == MediaType.VIDEO }
            .distinctBy { it.id }
            .take(10)
            .toList(),
        history = inputs.history,
        audioCount = inputs.audioCount,
        videoCount = inputs.videoCount,
        folderCount = 0,
    )
}

