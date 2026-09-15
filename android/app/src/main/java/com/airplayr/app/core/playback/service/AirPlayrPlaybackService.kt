package com.airplayr.app.core.playback.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.cast.CastPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.airplayr.app.MainActivity
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.data.media.MediaLibraryRepository
import com.airplayr.app.core.data.preferences.PreferencesRepository
import com.airplayr.app.core.playback.queue.QueueStore
import com.airplayr.app.core.data.StructuredRepository
import com.airplayr.app.core.playback.dsp.DspRepository
import com.airplayr.app.core.playback.dsp.AudioEffectsEngine
import com.airplayr.app.core.data.search.SearchRepository
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Sole owner of ExoPlayer + MediaLibrarySession. UI never owns the player. */
@OptIn(UnstableApi::class)
class AirPlayrPlaybackService : MediaLibraryService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val libraryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var queueStore: QueueStore
    private lateinit var preferences: PreferencesRepository
    private lateinit var dsp: DspRepository
    private lateinit var structured: StructuredRepository
    private lateinit var library: MediaLibraryRepository
    private lateinit var search: SearchRepository
    private val effects = AudioEffectsEngine()
    private lateinit var localPlayer: ExoPlayer
    private lateinit var player: androidx.media3.common.Player
    private lateinit var castPlayer: CastPlayer
    private var mediaLibrarySession: MediaLibrarySession? = null
    private var hydrating = true
    private var lastSnapshotSignature: Int? = null
    @Volatile private var allowBackgroundPlayback = true
    private val persistMutex = Mutex()

    private val mediaLibraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ) = Futures.immediateFuture(
            LibraryResult.ofItem(
                browseItem("root", "AirPlayr", isBrowsable = true, isPlayable = false),
                params,
            )
        )

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ) = when (parentId) {
            "root" -> Futures.immediateFuture(
                LibraryResult.ofItemList(
                    listOf(
                        browseItem("audio", "Music", isBrowsable = true, isPlayable = false),
                        browseItem("video", "Videos", isBrowsable = true, isPlayable = false),
                    ),
                    params,
                )
            )
            "audio", "video" -> libraryScope.future {
                val type = if (parentId == "audio") MediaType.AUDIO else MediaType.VIDEO
                val safePageSize = pageSize.coerceIn(1, 100)
                val items = library.page(type, safePageSize, page.coerceAtLeast(0) * safePageSize)
                    .map(MediaItemMapper::toExo)
                LibraryResult.ofItemList(items, params)
            }
            else -> Futures.immediateFuture(LibraryResult.ofItemList(emptyList(), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ) = libraryScope.future {
            val item = library.get(mediaId)
            if (item == null) {
                LibraryResult.ofError<ExoMediaItem>(LibraryResult.RESULT_ERROR_BAD_VALUE)
            } else {
                LibraryResult.ofItem(MediaItemMapper.toExo(item), null)
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ) = libraryScope.future {
            val count = search.search(query, limit = 200).first().size
            session.notifySearchResultChanged(browser, query, count, params)
            LibraryResult.ofVoid(params)
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ) = libraryScope.future {
            val safePageSize = pageSize.coerceIn(1, 100)
            val all = search.search(query, limit = 200).first()
            val from = (page.coerceAtLeast(0) * safePageSize).coerceAtMost(all.size)
            val to = (from + safePageSize).coerceAtMost(all.size)
            LibraryResult.ofItemList(all.subList(from, to).map(MediaItemMapper::toExo), params)
        }
    }

    private val listener = object : Player.Listener {

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (audioSessionId > 0) {
                serviceScope.launch {
                    val state = dsp.state.first()
                    effects.attach(audioSessionId)
                    effects.setEnabled(state.eqEnabled)
                    effects.setBass(state.bassMb)
                    effects.setLoudness(state.loudnessMb)
                    effects.setVirtualizer(state.virtualizerStrength)
                    if (state.eqPreset == "flat") {
                        effects.setFlat()
                    } else {
                        effects.setBands(state.bands)
                    }
                }
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (hydrating) return
            if (events.containsAny(
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_PLAYER_ERROR,
                )
            ) {
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    player.currentMediaItem?.let { current ->
                        toDomain(current)?.let { domain ->
                            serviceScope.launch(Dispatchers.IO) {
                                runCatching { structured.recordPlay(domain) }
                            }
                        }
                    }
                }
                persistSnapshot()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        queueStore = QueueStore(applicationContext)
        preferences = PreferencesRepository(applicationContext)
        dsp = DspRepository(applicationContext)
        val app = application as com.airplayr.app.AirPlayrApplication
        structured = app.container.structured
        library = app.container.mediaLibrary
        search = app.container.search

        localPlayer = ExoPlayer.Builder(applicationContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        castPlayer = CastPlayer.Builder(applicationContext)
            .setLocalPlayer(localPlayer)
            .build()
        player = castPlayer
        player.addListener(listener)

        val sessionActivity = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, mediaLibraryCallback)
            .setSessionActivity(sessionActivity)
            .build()

        serviceScope.launch {
            preferences.settings
                .distinctUntilChanged()
                .collect { state ->
                    allowBackgroundPlayback = state.backgroundPlayback
                    localPlayer.setPauseAtEndOfMediaItems(!state.gaplessPlayback)
                }
        }
        serviceScope.launch {
            while (isActive) {
                delay(5_000L)
                if (!hydrating && player.mediaItemCount > 0 && (player.isPlaying || player.playWhenReady)) {
                    persistSnapshot()
                }
            }
        }
        serviceScope.launch { restoreQueue() }
        serviceScope.launch {
            dsp.state.collect { state ->
                localPlayer.setPlaybackParameters(
                    androidx.media3.common.PlaybackParameters(
                        state.speed.toFloat(),
                        if (state.preservePitch) 1f else com.airplayr.app.core.playback.pitchRatio(state.pitchSemitones),
                    )
                )
                if (localPlayer.audioSessionId > 0) {
                    effects.attach(localPlayer.audioSessionId)
                    effects.setEnabled(state.eqEnabled)
                    effects.setBass(state.bassMb)
                    effects.setLoudness(state.loudnessMb)
                    effects.setVirtualizer(state.virtualizerStrength)
                    if (state.eqPreset == "flat") effects.setFlat() else effects.setBands(state.bands)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (allowBackgroundPlayback && player.playWhenReady) {
            super.onTaskRemoved(rootIntent)
        } else {
            player.pause()
            stopSelf()
        }
    }

    override fun onDestroy() {
        runCatching { kotlinx.coroutines.runBlocking { persistSnapshotNow() } }
        effects.release()
        if (::player.isInitialized) player.removeListener(listener)
        mediaLibrarySession?.release()
        if (::castPlayer.isInitialized) castPlayer.release()
        if (::localPlayer.isInitialized) localPlayer.release()
        serviceScope.cancel()
        libraryScope.cancel()
        super.onDestroy()
    }

    private suspend fun restoreQueue() {
        val saved = runCatching { queueStore.read() }.getOrNull()
        if (saved != null && saved.items.isNotEmpty()) {
            val byIdIndex = saved.currentId?.let { currentId -> saved.items.indexOfFirst { it.id == currentId } } ?: -1
            val startIndex = (if (byIdIndex >= 0) byIdIndex else saved.currentIndex).coerceIn(0, saved.items.lastIndex)
            player.setMediaItems(saved.items.map(MediaItemMapper::toExo), startIndex, saved.positionMs)
            player.shuffleModeEnabled = saved.shuffle
            player.repeatMode = saved.repeatMode.coerceIn(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL)
            player.playWhenReady = saved.playWhenReady
        }
        hydrating = false
    }

    private fun persistSnapshot() {
        if (!::player.isInitialized) return
        serviceScope.launch { persistSnapshotNow() }
    }

    private suspend fun persistSnapshotNow() {
        persistMutex.withLock {
            val position = player.currentPosition.coerceAtLeast(0L)
            val items = (0 until player.mediaItemCount)
                .map { index -> player.getMediaItemAt(index) }
                .mapNotNull(::toDomain)
            val index = player.currentMediaItemIndex.coerceAtLeast(0)
            val id = player.currentMediaItem?.mediaId
            val shuffle = player.shuffleModeEnabled
            val repeat = player.repeatMode
            val playWhenReady = player.playWhenReady
            val signature = 31 * items.fold(1) { acc, item -> 31 * acc + item.id.hashCode() } + index
                .let { 31 * it + shuffle.hashCode() }
                .let { 31 * it + repeat.hashCode() }
                .let { 31 * it + playWhenReady.hashCode() }
                .let { 31 * it + position.hashCode() }
            if (signature == lastSnapshotSignature) return@withLock
            lastSnapshotSignature = signature
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                runCatching { queueStore.save(items, index, id, position, shuffle, repeat, playWhenReady) }
            }
        }
    }

    private fun browseItem(id: String, title: String, isBrowsable: Boolean, isPlayable: Boolean): ExoMediaItem =
        ExoMediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(isBrowsable)
                    .setIsPlayable(isPlayable)
                    .build()
            )
            .build()

    private fun toDomain(item: ExoMediaItem): MediaItem? {
        val local = item.localConfiguration ?: return null
        val md = item.mediaMetadata
        val type = if (md.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO) MediaType.VIDEO else MediaType.AUDIO
        return MediaItem(
            id = item.mediaId,
            mediaStoreId = item.mediaId.substringAfterLast(':').toLongOrNull(),
            uri = local.uri.toString(),
            title = md.title?.toString().orEmpty().ifBlank { item.mediaId },
            filename = md.title?.toString().orEmpty().ifBlank { item.mediaId },
            artist = md.artist?.toString(),
            album = md.albumTitle?.toString(),
            albumArtist = null,
            durationMs = md.durationMs?.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L,
            width = 0,
            height = 0,
            sizeBytes = 0L,
            dateModified = 0L,
            dateAdded = 0L,
            mediaType = type,
            mimeType = null,
            albumId = null,
            artworkUri = md.artworkUri?.toString(),
            thumbnailUri = null,
            genre = null,
            sourceDirectoryUri = null,
            folderId = null,
            folder = null,
            path = null,
            relativePath = null,
        )
    }
}
