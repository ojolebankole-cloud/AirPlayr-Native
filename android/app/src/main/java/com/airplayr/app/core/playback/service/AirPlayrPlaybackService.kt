package com.airplayr.app.core.playback.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.cast.CastPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.DefaultMediaNotificationProvider
import com.airplayr.app.MainActivity
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.data.media.MediaLibraryRepository
import com.airplayr.app.core.data.preferences.PreferencesRepository
import com.airplayr.app.core.playback.queue.QueueStore
import com.airplayr.app.core.qa.PlaybackHealthMonitor
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Sole owner of ExoPlayer + MediaLibrarySession. UI never owns the player. */
@OptIn(UnstableApi::class)
class AirPlayrPlaybackService : MediaLibraryService() {
    companion object {
        const val MEDIA_NOTIFICATION_CHANNEL_ID = "airplayr_music_playback"
        const val MEDIA_NOTIFICATION_ID = 24071
        private const val COMMAND_FORCE_NEXT = "airplayr.notification.NEXT"
        private const val COMMAND_NOTIFICATION_PREVIOUS = "airplayr.notification.PREVIOUS"
        private const val COMMAND_NOTIFICATION_NEXT = "airplayr.notification.NEXT_ITEM"

        @Volatile
        private var activeInstance: AirPlayrPlaybackService? = null

        /**
         * Video playback is deliberately outside the music MediaSession. While a video is
         * on-screen, suspend the music session playlist so Android has no music session media
         * to surface in the notification/media-controls area. The persisted music snapshot is
         * kept on disk so music can be resumed later by the music player.
         */
        fun enterVideoMode() {
            activeInstance?.enterVideoModeInternal()
        }

        fun exitVideoMode() {
            activeInstance?.exitVideoModeInternal()
        }
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val libraryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var queueStore: QueueStore
    private lateinit var preferences: PreferencesRepository
    private lateinit var dsp: DspRepository
    private lateinit var structured: StructuredRepository
    private lateinit var library: MediaLibraryRepository
    private lateinit var search: SearchRepository
    private lateinit var stats: com.airplayr.app.core.data.PlaybackStatsRepository
    private val localEffects = AudioEffectsEngine()
    private val secondaryEffects = AudioEffectsEngine()
    @Volatile private var currentAudioSessionId = 0
    private lateinit var localPlayer: ExoPlayer
    private var secondaryPlayer: ExoPlayer? = null
    private lateinit var switchingPlayer: AirPlayrSwitchingPlayer
    private lateinit var player: androidx.media3.common.Player
    private var crossfadeJob: kotlinx.coroutines.Job? = null
    private var crossfadeInProgress = false
    private var replayGainMultiplier = 1f
    private var internalVolumeWrite = false
    @Volatile private var currentPauseAtEnd = false
    @Volatile private var latestDspState: com.airplayr.app.core.playback.dsp.DspState? = null
    private lateinit var activePlaybackPlayer: Player
    private var lastObservedMediaId: String? = null
    private lateinit var castPlayer: CastPlayer
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var mediaNotificationProvider: androidx.media3.session.MediaNotification.Provider
    private var hydrating = true
    private var lastSnapshotSignature: Int? = null
    @Volatile private var allowBackgroundPlayback = true
    @Volatile private var videoMode = false
    private val persistMutex = Mutex()

    private val mediaLibraryCallback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(SessionCommand(COMMAND_FORCE_NEXT, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_NOTIFICATION_PREVIOUS, Bundle.EMPTY))
                .add(SessionCommand(COMMAND_NOTIFICATION_NEXT, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                Player.Commands.Builder().addAllCommands().build(),
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ) = when (customCommand.customAction) {
            COMMAND_FORCE_NEXT, COMMAND_NOTIFICATION_NEXT -> {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                } else if (player.mediaItemCount > 0 && player.repeatMode == Player.REPEAT_MODE_ALL) {
                    player.seekToDefaultPosition(0)
                    player.play()
                }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            COMMAND_NOTIFICATION_PREVIOUS -> {
                if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                } else if (player.mediaItemCount > 0) {
                    player.seekTo(0)
                }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            else -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
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

        @OptIn(UnstableApi::class)
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ) = libraryScope.future {
            val saved = runCatching { queueStore.read() }.getOrNull()
            val domainItems = saved?.items.orEmpty().filter { it.mediaType == MediaType.AUDIO }
            val items = domainItems.map(MediaItemMapper::toExo)
            if (items.isEmpty()) {
                MediaSession.MediaItemsWithStartPosition(
                    emptyList(),
                    C.INDEX_UNSET,
                    C.TIME_UNSET,
                )
            } else {
                val startIndex = domainItems.indexOfFirst { it.id == saved?.currentId }
                    .takeIf { it >= 0 }
                    ?: saved?.currentIndex.orZero().coerceIn(0, items.lastIndex)

                // For metadata-only resumption, Media3 only needs the current item.
                // For actual playback resumption, restore the complete queue + position.
                val resumeItems = if (isForPlayback) items else listOf(items[startIndex])
                val resumeIndex = if (isForPlayback) startIndex else 0
                val resumePosition = if (isForPlayback) saved?.positionMs.orZero().coerceAtLeast(0L) else 0L

                // Restore queue playback mode before Media3 resumes the saved items.
                if (isForPlayback && ::player.isInitialized) {
                    player.shuffleModeEnabled = saved?.shuffle == true
                    player.repeatMode = saved?.repeatMode.orZero().coerceIn(
                        Player.REPEAT_MODE_OFF,
                        Player.REPEAT_MODE_ALL,
                    )
                }

                MediaSession.MediaItemsWithStartPosition(resumeItems, resumeIndex, resumePosition)
            }
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

    private val healthMonitor = PlaybackHealthMonitor()

    private val localAudioSessionListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            currentAudioSessionId = audioSessionId.coerceAtLeast(0)
            if (audioSessionId > 0) {
                serviceScope.launch {
                    val state = dsp.state.first()
                    if (localEffects.attach(audioSessionId).supported) localEffects.applyState(state)
                }
            } else {
                localEffects.release()
            }
        }
    }

    private val secondaryAudioSessionListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (audioSessionId > 0) {
                serviceScope.launch {
                    val state = dsp.state.first()
                    if (secondaryEffects.attach(audioSessionId).supported) secondaryEffects.applyState(state)
                }
            }
        }
    }

    private val listener = object : Player.Listener {

        override fun onEvents(player: Player, events: Player.Events) {
            healthMonitor.observe(player)
            if (events.contains(Player.EVENT_VOLUME_CHANGED) && !internalVolumeWrite) {
                // The MediaController owns the user-facing volume. ReplayGain is applied as
                // a multiplier when the active track changes, so a user volume adjustment
                // remains the base level for subsequent tracks.
                replayGainMultiplier = replayGainMultiplier.coerceAtLeast(0.001f)
            }
            if (hydrating) return
            if (events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED) ||
                events.contains(Player.EVENT_REPEAT_MODE_CHANGED) ||
                events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) ||
                events.contains(Player.EVENT_IS_PLAYING_CHANGED) ||
                events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                mediaLibrarySession?.setMediaButtonPreferences(notificationMediaButtons(player))
            }
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
                    val previousId = lastObservedMediaId
                    val currentId = player.currentMediaItem?.mediaId
                    lastObservedMediaId = currentId
                    serviceScope.launch {
                        val sleep = preferences.settings.first()
                        if (sleep.sleepPendingTrackId != null && sleep.sleepPendingTrackId == previousId) {
                            internalVolumeWrite = true
                            player.pause()
                            player.volume = sleep.sleepStartVolume.toFloat().coerceIn(0f, 1f)
                            internalVolumeWrite = false
                            preferences.cancelSleepTimer()
                        } else if (sleep.sleepPendingTrackId != null && sleep.sleepPendingTrackId != currentId) {
                            preferences.setSleepPendingTrack(null)
                        }
                    }
                    applyReplayGainForCurrent(player)
                    player.currentMediaItem?.let { current ->
                        toDomain(current)?.let { domain ->
                            serviceScope.launch(Dispatchers.IO) {
                                val saved = preferences.settings.first().savePlaybackHistory
                                if (saved) runCatching { structured.recordPlay(domain) }
                                runCatching { stats.start(library.get(domain.id) ?: domain) }
                            }
                        }
                    }
                } else if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) && player.playWhenReady) {
                    // A replay of the same media item does not emit MEDIA_ITEM_TRANSITION.
                    // Count a genuine play/resume event too, so the On repeat collection can
                    // become populated when the listener starts the same song again.
                    player.currentMediaItem?.let { current ->
                        toDomain(current)?.let { domain ->
                            serviceScope.launch(Dispatchers.IO) {
                                if (preferences.settings.first().savePlaybackHistory) {
                                    runCatching { structured.recordPlay(domain) }
                                }
                            }
                        }
                    }
                }
                persistSnapshot()
            }
        }
    }

    /**
     * Android 13+ media controls expose five playback slots. Media3's secondary slots let us
     * deliberately build the requested notification order instead of relying on the system's
     * default action ordering:
     *
     *     Shuffle · Previous · Play/Pause · Next · Repeat
     *
     * Shuffle and repeat use Media3's native player commands, while previous/next use session
     * commands so their buttons remain present even when the current timeline has no adjacent
     * item.
     */
    private fun notificationMediaButtons(source: Player): List<CommandButton> {
        val nextRepeatMode = when (source.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        return listOf(
            CommandButton.Builder(
                if (source.shuffleModeEnabled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF,
            )
                .setDisplayName("Shuffle")
                .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE)
                .setSlots(CommandButton.SLOT_BACK_SECONDARY)
                .build(),
            CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                .setDisplayName("Previous")
                .setSessionCommand(SessionCommand(COMMAND_NOTIFICATION_PREVIOUS, Bundle.EMPTY))
                .setSlots(CommandButton.SLOT_BACK)
                .build(),
            CommandButton.Builder(
                if (source.isPlaying || source.playWhenReady) CommandButton.ICON_PAUSE else CommandButton.ICON_PLAY,
            )
                .setDisplayName(if (source.isPlaying || source.playWhenReady) "Pause" else "Play")
                .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                .setSlots(CommandButton.SLOT_CENTRAL)
                .build(),
            CommandButton.Builder(CommandButton.ICON_NEXT)
                .setDisplayName("Next")
                .setSessionCommand(SessionCommand(COMMAND_NOTIFICATION_NEXT, Bundle.EMPTY))
                .setSlots(CommandButton.SLOT_FORWARD)
                .build(),
            CommandButton.Builder(
                when (source.repeatMode) {
                    Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
                    Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
                    else -> CommandButton.ICON_REPEAT_OFF
                },
            )
                .setDisplayName(
                    when (source.repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        else -> "Repeat off"
                    },
                )
                .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE, nextRepeatMode)
                .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
                .build(),
        )
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        queueStore = QueueStore(applicationContext)
        preferences = PreferencesRepository(applicationContext)
        dsp = DspRepository(applicationContext)
        val app = application as com.airplayr.app.AirPlayrApplication
        structured = app.container.structured
        library = app.container.mediaLibrary
        search = app.container.search
        stats = app.container.stats

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
        localPlayer.addListener(localAudioSessionListener)

        // Crossfade needs a second player only when it is actually used. Do not allocate the
        // second ExoPlayer on every existing-user launch when the common default is gapless/off.

        // Media3 CastPlayer already owns the local -> Cast handoff. Do not add a second
        // SessionAvailabilityListener that manually copies the timeline and switches players.
        // That duplicate handoff can race Media3's own remote-player transfer when a device is
        // selected from the Cast picker and can terminate the process on real devices.
        castPlayer = CastPlayer.Builder(this)
            .setLocalPlayer(localPlayer)
            .build()
        switchingPlayer = AirPlayrSwitchingPlayer(castPlayer)
        activePlaybackPlayer = castPlayer
        player = switchingPlayer
        player.addListener(healthMonitor)
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
        mediaLibrarySession?.setMediaButtonPreferences(notificationMediaButtons(player))
        mediaNotificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(MEDIA_NOTIFICATION_CHANNEL_ID)
            .setChannelName(com.airplayr.app.R.string.music_playback_channel_name)
            .setNotificationId(MEDIA_NOTIFICATION_ID)
            .build()
        setMediaNotificationProvider(mediaNotificationProvider)

        serviceScope.launch {
            preferences.settings
                .distinctUntilChanged()
                .collect { state ->
                    allowBackgroundPlayback = state.backgroundPlayback
                    val pauseAtEnd = state.crossfadeSeconds > 0.0 || !state.gaplessPlayback
                    currentPauseAtEnd = pauseAtEnd
                    localPlayer.setPauseAtEndOfMediaItems(pauseAtEnd)
                    secondaryPlayer?.setPauseAtEndOfMediaItems(pauseAtEnd)
                    // Media3 requires a non-null notification provider once one is installed.
                    // Notification visibility must therefore be handled separately from provider registration.
                    setMediaNotificationProvider(mediaNotificationProvider)
                }
        }
        serviceScope.launch {
            while (isActive) {
                delay(1_000L)
                if (!hydrating && player.mediaItemCount > 0) {
                    val current = player.currentMediaItem
                    if ((player.isPlaying || player.playWhenReady) && current != null) {
                        val domain = toDomain(current)
                        if (domain != null) runCatching { stats.track(library.get(domain.id) ?: domain, player.currentPosition) }
                    }
                    processSleepTimer()
                    processCrossfade()
                    persistSnapshot()
                }
            }
        }
        serviceScope.launch { restoreQueue() }
        serviceScope.launch {
            dsp.state.collect { state ->
                latestDspState = state
                applyDspTo(localPlayer, state)
                secondaryPlayer?.let { applyDspTo(it, state) }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    /**
     * Media3 normally owns notification publication. We still keep a hard guard here so a
     * stale notification cannot be recreated while a video is active. The player playlist is
     * also cleared when entering video mode, which is the Media3-supported way to remove the
     * media notification/session item immediately.
     */
    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (videoMode) {
            getSystemService(android.app.NotificationManager::class.java)
                ?.cancel(MEDIA_NOTIFICATION_ID)
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION") stopForeground(true)
            }
            return
        }
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    private fun enterVideoModeInternal() {
        videoMode = true
        if (!::player.isInitialized) return
        serviceScope.launch {
            // Capture the current music queue/position before detaching it from the session.
            runCatching { persistSnapshotNow() }
            player.pause()
            player.clearMediaItems()
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION") stopForeground(true)
            }
            getSystemService(android.app.NotificationManager::class.java)
                ?.cancel(MEDIA_NOTIFICATION_ID)
        }
    }

    private fun exitVideoModeInternal() {
        videoMode = false
        if (mediaLibrarySession != null) {
            triggerNotificationUpdate()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Capture the last queue/current-item/position before the task disappears. When
        // background playback is disabled, pause first and persist playWhenReady=false so the
        // cached relaunch state cannot incorrectly resurrect a track as actively playing.
        if (::player.isInitialized) {
            if (!allowBackgroundPlayback) {
                player.pause()
                player.playWhenReady = false
            }
            runCatching { kotlinx.coroutines.runBlocking { persistSnapshotNow() } }
        }
        if (allowBackgroundPlayback && player.playWhenReady) {
            super.onTaskRemoved(rootIntent)
        } else {
            player.pause()
            stopSelf()
        }
    }

    override fun onDestroy() {
        if (activeInstance === this) activeInstance = null
        runCatching { kotlinx.coroutines.runBlocking { stats.finish(); persistSnapshotNow() } }
        localEffects.release()
        secondaryEffects.release()
        if (::player.isInitialized) {
            player.removeListener(listener)
            player.removeListener(healthMonitor)
        }
        if (::localPlayer.isInitialized) localPlayer.removeListener(localAudioSessionListener)
        secondaryPlayer?.removeListener(secondaryAudioSessionListener)
        crossfadeJob?.cancel()
        mediaLibrarySession?.release()
        if (::castPlayer.isInitialized) castPlayer.release()
        secondaryPlayer?.release()
        secondaryPlayer = null
        if (::localPlayer.isInitialized) localPlayer.release()
        serviceScope.cancel()
        libraryScope.cancel()
        super.onDestroy()
    }

    private fun ensureSecondaryPlayer(): ExoPlayer {
        secondaryPlayer?.let { return it }
        val created = ExoPlayer.Builder(applicationContext)
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
        created.addListener(secondaryAudioSessionListener)
        created.setPauseAtEndOfMediaItems(currentPauseAtEnd)
        latestDspState?.let { applyDspTo(created, it) }
        secondaryPlayer = created
        return created
    }

    private suspend fun restoreQueue() {
        val saved = runCatching { queueStore.read() }.getOrNull()
        if (saved != null && saved.items.isNotEmpty()) {
            val audioItems = saved.items.filter { it.mediaType == MediaType.AUDIO }
            if (audioItems.isNotEmpty()) {
                val byIdIndex = saved.currentId?.let { currentId -> audioItems.indexOfFirst { it.id == currentId } } ?: -1
                val startIndex = (if (byIdIndex >= 0) byIdIndex else 0).coerceIn(0, audioItems.lastIndex)
                player.setMediaItems(audioItems.map(MediaItemMapper::toExo), startIndex, saved.positionMs)
                player.shuffleModeEnabled = saved.shuffle
                player.repeatMode = saved.repeatMode.coerceIn(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL)
                player.playWhenReady = saved.playWhenReady
            }
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

    private fun applyDspTo(target: ExoPlayer, state: com.airplayr.app.core.playback.dsp.DspState) {
        if (target.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) {
            val pitch = if (state.lockPitch) state.speed.toFloat() else com.airplayr.app.core.playback.pitchRatio(state.pitchSemitones)
            runCatching { target.setPlaybackParameters(androidx.media3.common.PlaybackParameters(state.speed.toFloat(), pitch)) }
        }
        val sessionId = target.audioSessionId
        if (sessionId > 0) {
            currentAudioSessionId = sessionId
            val engine = if (secondaryPlayer === target) secondaryEffects else localEffects
            if (engine.attach(sessionId).supported) engine.applyState(state)
        }
    }

    private fun isCasting(): Boolean = runCatching { castPlayer.isCastSessionAvailable() }.getOrDefault(false)

    private suspend fun processCrossfade() {
        if (crossfadeInProgress || isCasting() || !player.isPlaying) return
        val settings = runCatching { preferences.settings.first() }.getOrNull() ?: return
        val seconds = settings.crossfadeSeconds.coerceIn(0.0, 12.0)
        if (seconds <= 0.0) return
        val duration = player.duration
        if (duration <= 0L || player.currentMediaItem == null) return
        val remaining = duration - player.currentPosition
        val windowMs = (seconds * 1000.0).toLong().coerceAtLeast(250L)
        if (remaining > windowMs || player.nextMediaItemIndex == C.INDEX_UNSET) return
        crossfadeInProgress = true
        crossfadeJob?.cancel()
        crossfadeJob = serviceScope.launch {
            val source = player
            val secondary = ensureSecondaryPlayer()
            val target = if (activePlaybackPlayer === secondary) castPlayer else secondary
            val nextIndex = source.nextMediaItemIndex
            val items = (0 until source.mediaItemCount).map { source.getMediaItemAt(it) }
            if (nextIndex !in items.indices) { crossfadeInProgress = false; return@launch }
            val sourceVolume = source.volume.coerceIn(0f, 1f)
            val userVolume = (sourceVolume / replayGainMultiplier.coerceAtLeast(0.001f)).coerceIn(0f, 1f)
            val nextItem = source.getMediaItemAt(nextIndex)
            val targetGain = replayGainMultiplierFor(nextItem)
            target.setMediaItems(items, nextIndex, 0L)
            target.shuffleModeEnabled = source.shuffleModeEnabled
            target.repeatMode = Player.REPEAT_MODE_OFF
            if (target is ExoPlayer) applyDspTo(target, dsp.state.first())
            target.volume = 0f
            target.prepare()
            target.play()
            val started = System.currentTimeMillis()
            val total = windowMs.coerceAtMost(12_000L)
            while (isActive && System.currentTimeMillis() - started < total) {
                val t = ((System.currentTimeMillis() - started).toFloat() / total).coerceIn(0f, 1f)
                internalVolumeWrite = true
                source.volume = sourceVolume * (1f - t)
                target.volume = (userVolume * targetGain).coerceIn(0f, 1f) * t
                internalVolumeWrite = false
                delay(40L)
            }
            internalVolumeWrite = true
            target.volume = (userVolume * targetGain).coerceIn(0f, 1f)
            source.pause()
            source.volume = sourceVolume
            internalVolumeWrite = false
            switchingPlayer.switchTo(target)
            activePlaybackPlayer = target
            replayGainMultiplier = targetGain
            toDomain(nextItem)?.let { domain ->
                serviceScope.launch(Dispatchers.IO) {
                    val settingsNow = preferences.settings.first()
                    if (settingsNow.savePlaybackHistory) runCatching { structured.recordPlay(domain) }
                    runCatching { stats.start(library.get(domain.id) ?: domain) }
                }
            }
            crossfadeInProgress = false
        }
    }

    private suspend fun replayGainMultiplierFor(item: ExoMediaItem): Float {
        val settings = runCatching { preferences.settings.first() }.getOrNull() ?: return 1f
        if (settings.replayGainMode == "off") return 1f
        val domain = toDomain(item) ?: return 1f
        val tags = withContext(Dispatchers.IO) { runCatching { ReplayGainReader.read(applicationContext, domain.uri) }.getOrDefault(ReplayGainReader.Tags()) }
        return com.airplayr.app.core.playback.dsp.ReplayGainCalculator.multiplier(
            com.airplayr.app.core.playback.dsp.ReplayGainTags(tags.trackDb, tags.albumDb, tags.peak),
            settings.replayGainMode,
            settings.replayGainPreampDb,
            settings.replayGainPreventClipping,
        )
    }

    private fun applyReplayGainForCurrent(active: Player) {
        val current = active.currentMediaItem ?: return
        val previousMultiplier = replayGainMultiplier.coerceAtLeast(0.001f)
        serviceScope.launch {
            val multiplier = replayGainMultiplierFor(current)
            replayGainMultiplier = multiplier
            val base = (active.volume / previousMultiplier).coerceIn(0f, 1f)
            internalVolumeWrite = true
            active.volume = (base * multiplier).coerceIn(0f, 1f)
            internalVolumeWrite = false
        }
    }

    private suspend fun processSleepTimer() {
        val settings = preferences.settings.first()
        val deadline = settings.sleepDeadlineMs
        if (deadline <= 0L) return
        val currentId = player.currentMediaItem?.mediaId
        val pending = settings.sleepPendingTrackId
        if (pending != null && pending != currentId) {
            preferences.setSleepPendingTrack(null)
            return
        }
        if (pending != null) {
            val duration = player.duration
            if (duration > 0L && duration - player.currentPosition <= 10_000L) {
                if (settings.sleepFadeOut) fadeAndStop(settings.sleepStartVolume.toFloat()) else stopAndClearSleep()
            }
            return
        }
        if (System.currentTimeMillis() < deadline) return
        if (settings.sleepFinishCurrent && currentId != null) {
            preferences.setSleepPendingTrack(currentId)
            return
        }
        if (settings.sleepFadeOut) fadeAndStop(settings.sleepStartVolume.toFloat()) else stopAndClearSleep()
    }

    private suspend fun fadeAndStop(startVolume: Float) {
        val durationMs = 10_000L
        val started = System.currentTimeMillis()
        while (currentCoroutineContext().isActive && System.currentTimeMillis() - started < durationMs) {
            val t = ((System.currentTimeMillis() - started).toFloat() / durationMs).coerceIn(0f, 1f)
            internalVolumeWrite = true
            player.volume = (startVolume * (1f - t)).coerceIn(0f, 1f)
            internalVolumeWrite = false
            delay(100L)
        }
        internalVolumeWrite = true
        player.volume = 0f
        player.pause()
        player.volume = startVolume.coerceIn(0f, 1f)
        internalVolumeWrite = false
        stopAndClearSleep()
    }

    private suspend fun stopAndClearSleep() {
        player.pause()
        preferences.cancelSleepTimer()
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

private fun Int?.orZero(): Int = this ?: 0
private fun Long?.orZero(): Long = this ?: 0L
