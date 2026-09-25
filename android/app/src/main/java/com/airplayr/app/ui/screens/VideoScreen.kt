@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.airplayr.app.ui.screens

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.text.CueGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.airplayr.app.core.data.VIDEO_COMPLETION_FRACTION
import com.airplayr.app.core.data.media.MediaItem as AppMediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.playback.VideoQueueStore
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.AirPlayrMediaMenuAction
import com.airplayr.app.ui.components.AirPlayrPremiumMediaActionSheet
import com.airplayr.app.ui.components.VideoQueueSheet
import com.airplayr.app.ui.components.VideoSubtitleSettingsSheet
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import com.airplayr.app.ui.video.VideoPlayerViewModel
import com.airplayr.app.ui.video.VideoPlayerViewModelFactory
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.delay

internal enum class VideoFitMode(val label: String, val resizeMode: Int) {
    FIT("Fit", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
}

@OptIn(UnstableApi::class)
@Composable
fun VideoScreen(
    container: AppContainer,
    id: String,
    onCast: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
) {
    val vm: VideoPlayerViewModel = viewModel(factory = VideoPlayerViewModelFactory(container.mediaLibrary, container.structured, container.stats))
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val savePlaybackHistory = settings?.savePlaybackHistory ?: true
    val autoEnterPiP = settings?.autoEnterPiPOnBackground ?: false
    val allowsPiP = settings?.allowsPictureInPicture ?: true
    val autoHideMs = settings?.autoHideMs ?: 4000
    val defaultSpeed = (settings?.defaultSpeed?.toFloat() ?: 1f).coerceIn(0.25f, 4f)
    val defaultVolume = (settings?.defaultVolume?.toFloat() ?: 1f).coerceIn(0f, 1f)
    val seekStepMs = ((settings?.seekStepSeconds ?: 10) * 1000L).coerceIn(5_000L, 30_000L)
    val muteOnStart = settings?.muteOnStart ?: false
    val defaultFitMode = when (settings?.defaultAspect) {
        "cover" -> VideoFitMode.ZOOM
        "fill" -> VideoFitMode.FILL
        else -> VideoFitMode.FIT
    }
    val video by vm.video.collectAsStateWithLifecycle()
    val resume by vm.resume.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val bookmarks by remember(video?.id) { video?.id?.let(container.structured::bookmarks) ?: kotlinx.coroutines.flow.flowOf(emptyList()) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val queue by VideoQueueStore.queue.collectAsStateWithLifecycle()
    val shuffleQueue by VideoQueueStore.shuffleQueue.collectAsStateWithLifecycle()
    val repeatQueue by VideoQueueStore.repeatQueue.collectAsStateWithLifecycle()

    var player by remember { mutableStateOf<Player?>(null) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var favorite by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var muted by remember(video?.id, muteOnStart) { mutableStateOf(muteOnStart) }
    var speed by remember(video?.id, defaultSpeed) { mutableFloatStateOf(defaultSpeed) }
    val subtitleOnState = remember(video?.id, settings?.subtitlesEnabledDefault, settings?.rememberSubtitleTrack) {
        val defaultOn = settings?.subtitlesEnabledDefault ?: true
        mutableStateOf(
            if (settings?.rememberSubtitleTrack == true && video?.id != null) {
                context.getSharedPreferences("airplayr_video_subtitles", 0)
                    .getBoolean("enabled:${video!!.id}", defaultOn)
            } else {
                defaultOn
            }
        )
    }
    var subtitleCues by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingSubtitleCues by remember { mutableStateOf<List<String>?>(null) }
    var fitMode by remember(video?.id, defaultFitMode) { mutableStateOf(defaultFitMode) }
    val isFullscreenState = remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var moreOpen by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }
    var subtitleSettingsOpen by remember { mutableStateOf(false) }
    var audioTracksOpen by remember { mutableStateOf(false) }
    var volumeOverlay by remember { mutableStateOf(false) }
    var volumeOverlayValue by remember { mutableIntStateOf((VideoGestureSupport.musicVolumeFraction(context) * 100f).roundToInt().coerceIn(0, 100)) }
    var brightnessFeedback by remember { mutableStateOf<String?>(null) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var resumePromptVisible by remember(video?.id) { mutableStateOf(false) }
    var playbackInitializedForId by remember { mutableStateOf<String?>(null) }
    var decoderLabel by remember(video?.id) { mutableStateOf<String?>(null) }
    val pinchZoomState = remember(video?.id, settings?.gesturePinchZoom) { mutableFloatStateOf(1f) }
    var longPressBoostActive by remember(video?.id) { mutableStateOf(false) }
    val activity = context as? Activity
    val originalRequestedOrientation = remember { activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }

    fun touch() {
        controlsVisible = true
        interactionTick++
    }

    fun showSeekFeedback(text: String) {
        seekFeedback = text
        touch()
    }

    fun startOver() {
        val item = video ?: return
        player?.seekTo(0L)
        vm.startOver(item.id)
        resumePromptVisible = false
        touch()
    }

    fun seekBy(deltaMs: Long) {
        player?.let { p ->
            val max = p.duration.takeIf { it > 0 } ?: durationMs
            p.seekTo((p.currentPosition + deltaMs).coerceIn(0L, max.coerceAtLeast(0L)))
        }
        showSeekFeedback(if (deltaMs >= 0) "+${deltaMs / 1000}s" else "${deltaMs / 1000}s")
    }

    fun togglePlay() {
        player?.let { if (it.isPlaying) it.pause() else it.play() }
        touch()
    }

    fun toggleMute() {
        player?.let { p ->
            muted = !muted
            p.volume = if (muted) 0f else defaultVolume
            volumeOverlayValue = if (muted) 0 else ((VideoGestureSupport.musicVolumeFraction(context) * 100f).roundToInt().coerceIn(0, 100))
            volumeOverlay = true
        }
        touch()
    }

    fun toggleSubtitles() {
        val p = player ?: return
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, subtitleOnState.value)
            .build()
        subtitleOnState.value = !subtitleOnState.value
        if (settings?.rememberSubtitleTrack == true && video?.id != null) {
            context.getSharedPreferences("airplayr_video_subtitles", 0).edit().putBoolean("enabled:${video!!.id}", subtitleOnState.value).apply()
        }
        touch()
    }

    fun toggleFullscreen() {
        isFullscreenState.value = !isFullscreenState.value
        val activity = context as? Activity ?: return
        WindowMode.setFullscreen(activity, isFullscreenState.value)
        touch()
    }

    fun rotate() {
        val activity = context as? Activity ?: return
        activity.requestedOrientation = if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        touch()
    }

    fun toggleVideoShuffle() { VideoQueueStore.toggleShuffle(); touch() }
    fun toggleVideoRepeat() { VideoQueueStore.toggleRepeat(); touch() }

    fun openQueueItem(item: AppMediaItem) {
        VideoQueueStore.setCurrent(item)
        queueOpen = false
        if (item.id != id) onNavigate("video/${item.id}") else touch()
    }

    val subtitleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val current = player ?: return@rememberLauncherForActivityResult
        val base = current.currentMediaItem ?: return@rememberLauncherForActivityResult
        val mime = when (context.contentResolver.getType(uri)) {
            "text/vtt" -> MimeTypes.TEXT_VTT
            "application/x-subrip", "text/plain" -> MimeTypes.APPLICATION_SUBRIP
            else -> MimeTypes.TEXT_VTT
        }
        val sub = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mime)
            .setLanguage("und")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        current.setMediaItem(base.buildUpon().setSubtitleConfigurations(listOf(sub)).build(), current.currentPosition)
        current.prepare()
        current.play()
        subtitleOnState.value = true
        touch()
    }

    DisposableEffect(id) {
        com.airplayr.app.core.playback.service.AirPlayrPlaybackService.enterVideoMode()
        onDispose { com.airplayr.app.core.playback.service.AirPlayrPlaybackService.exitVideoMode() }
    }
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        onDispose { activity?.let { VideoGestureSupport.restoreWindowBrightness(it, originalBrightness) } }
    }
    DisposableEffect(settings?.keepAwake) {
        val window = (context as? Activity)?.window
        if (settings?.keepAwake == true) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { if (settings?.keepAwake != true) return@onDispose; window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    LaunchedEffect(id) { playbackInitializedForId = null; vm.load(id) }
    LaunchedEffect(video?.id) {
        favorite = video?.id?.let { vm.isFavorite(it) } ?: false
        video?.let { VideoQueueStore.setCurrent(it) }
    }

    LaunchedEffect(video?.id, resume) {
        val item = video ?: return@LaunchedEffect
        if (playbackInitializedForId == item.id) return@LaunchedEffect
        // Wait for the persisted resume position to arrive before initializing the native player.
        // Once initialized, later progress writes must never recreate/reset the player.
        playerError = null
        resumePromptVisible = resume > 5_000L && resume.toDouble() < (item.durationMs.coerceAtLeast(1L) * VIDEO_COMPLETION_FRACTION)
        playbackInitializedForId = item.id
        container.videoController.connectAsync { ready ->
            if (item.id != video?.id) return@connectAsync
            player = ready
            ready.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playerError = error.errorCodeName.ifBlank { error.message ?: "Video playback failed" }
                    controlsVisible = true
                }
                override fun onCues(cueGroup: CueGroup) {
                    val next = cueGroup.cues.mapNotNull { it.text?.toString()?.trim()?.takeIf(String::isNotBlank) }
                    if ((settings?.subtitleDelayMs ?: 0) <= 0) {
                        subtitleCues = next
                    } else {
                        subtitleCues = emptyList()
                        pendingSubtitleCues = next
                    }
                }
                override fun onTracksChanged(tracks: Tracks) {
                    if (settings?.rememberAudioTrack != true || video?.id == null) return
                    val savedIndex = context.getSharedPreferences("airplayr_video_audio", 0).getInt("track:${video!!.id}", -1)
                    if (savedIndex < 0) return
                    val group = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSupported } ?: return
                    if (savedIndex < group.length && !group.isTrackSelected(savedIndex)) {
                        ready.trackSelectionParameters = ready.trackSelectionParameters.buildUpon()
                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, savedIndex))
                            .build()
                    }
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        vm.completePlayback(item)
                        VideoQueueStore.next(item.id)?.let { next ->
                            if (next.id == item.id) {
                                // A looping single-item/last-item sequence restarts in-place instead
                                // of stacking another identical navigation destination.
                                ready.seekTo(0L)
                                ready.play()
                            } else {
                                onNavigate("video/${next.id}")
                            }
                        }
                    }
                }
            })
            (ready as? androidx.media3.exoplayer.ExoPlayer)?.addAnalyticsListener(object : AnalyticsListener {
                override fun onVideoDecoderInitialized(
                    eventTime: AnalyticsListener.EventTime,
                    decoderName: String,
                    initializedTimestampMs: Long,
                    initializationDurationMs: Long,
                ) {
                    val software = decoderName.contains("software", true) ||
                        decoderName.contains("ffmpeg", true) ||
                        decoderName.contains("google", true) ||
                        decoderName.contains("c2.android", true) ||
                        decoderName.contains(".sw", true) ||
                        decoderName.contains("sw.", true)
                    decoderLabel = if (software) "SW" else "HW"
                }
            })
            ready.volume = if (muted) 0f else defaultVolume
            ready.setPlaybackSpeed(speed)
            // Register the session before starting the native player so a very fast play/stop
            // interaction cannot lose the initial playback-history event.
            vm.beginPlayback(item, savePlaybackHistory)
            val startPosition = if (settings?.resumeMode == "start") 0L else resume
            container.videoController.playVideo(ready, item, startPosition)
        }
    }

    LaunchedEffect(pendingSubtitleCues, settings?.subtitleDelayMs) {
        val pending = pendingSubtitleCues ?: return@LaunchedEffect
        val delayMs = (settings?.subtitleDelayMs ?: 0).coerceAtLeast(0)
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs.toLong())
        subtitleCues = pending
        pendingSubtitleCues = null
    }

    LaunchedEffect(player, video?.id) {
        val item = video ?: return@LaunchedEffect
        while (player != null) {
            val p = player ?: break
            positionMs = p.currentPosition.coerceAtLeast(0L)
            durationMs = (p.duration.takeIf { it > 0 } ?: item.durationMs).coerceAtLeast(0L)
            bufferedMs = p.bufferedPosition.coerceAtLeast(0L)
            vm.trackPlayback(item, positionMs, durationMs, p.isPlaying)
            if (durationMs > 0L && positionMs.toDouble() / durationMs.toDouble() >= VIDEO_COMPLETION_FRACTION) vm.completePlayback(item)
            delay(650L)
        }
    }

    LaunchedEffect(player, controlsVisible, locked, interactionTick, autoHideMs) {
        if (!locked && controlsVisible && player?.isPlaying == true && autoHideMs > 0) {
            delay(autoHideMs.toLong())
            controlsVisible = false
        }
    }
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(900L)
            seekFeedback = null
        }
    }
    LaunchedEffect(volumeOverlay) {
        if (volumeOverlay) {
            delay(1400L)
            volumeOverlay = false
        }
    }
    LaunchedEffect(brightnessFeedback) {
        if (brightnessFeedback != null) {
            delay(1200L)
            brightnessFeedback = null
        }
    }

    if (Build.VERSION.SDK_INT >= 26) {
        SideEffect {
            val w = (video?.width ?: 16).coerceAtLeast(1)
            val h = (video?.height ?: 9).coerceAtLeast(1)
            runCatching { (context as? Activity)?.setPictureInPictureParams(PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(w, h)).build()) }
        }
        DisposableEffect(autoEnterPiP, allowsPiP, player, video?.id) {
            val activity = context as? ComponentActivity
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && autoEnterPiP && allowsPiP && player?.isPlaying == true) {
                    runCatching { activity?.enterPictureInPictureMode() }
                }
            }
            activity?.lifecycle?.addObserver(observer)
            onDispose { activity?.lifecycle?.removeObserver(observer) }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val item = video
            val p = player
            if (item != null && p != null) vm.finishPlayback(item, p.currentPosition, p.duration.takeIf { it > 0 } ?: item.durationMs, p.isPlaying)
            p?.pause()
            container.videoController.release()
            val leavingActivity = context as? Activity
            runCatching {
                leavingActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        player?.let { p -> AndroidVideoSurface(p, fitMode, Modifier.fillMaxSize().graphicsLayer(scaleX = pinchZoomState.floatValue, scaleY = pinchZoomState.floatValue)) }

        if (subtitleOnState.value && subtitleCues.isNotEmpty()) {
            VideoSubtitleOverlay(
                cues = subtitleCues,
                sizeScale = settings?.subtitleSize ?: 1.0,
                textColor = parseSubtitleColor(settings?.subtitleColor),
                background = settings?.subtitleBg ?: true,
                top = settings?.subtitlePosition == "top",
                controlsVisible = controlsVisible,
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(player, locked, settings?.gestureDoubleTapSeek, settings?.gestureLongPress2x) {
                    detectTapGestures(
                        onPress = {
                            tryAwaitRelease()
                            if (longPressBoostActive) {
                                longPressBoostActive = false
                                speed = defaultSpeed
                                player?.setPlaybackSpeed(defaultSpeed)
                            }
                        },
                        onTap = {
                            if (!locked) {
                                controlsVisible = !controlsVisible
                                interactionTick++
                            }
                        },
                        onDoubleTap = {
                            if (!locked && settings?.gestureDoubleTapSeek != false) {
                                player?.let { if (it.isPlaying) it.pause() else it.play() }
                                controlsVisible = true
                                interactionTick++
                            }
                        },
                        onLongPress = {
                            if (!locked && settings?.gestureLongPress2x == true) {
                                longPressBoostActive = true
                                speed = 2f
                                player?.setPlaybackSpeed(2f)
                            }
                        },
                    )
                }
                .pointerInput(player, locked, settings?.gestureVolume, settings?.gestureBrightness, settings?.gestureHorizontalSeek, durationMs) {
                    var start = androidx.compose.ui.geometry.Offset.Zero
                    var axis = 0
                    var accumulatedX = 0f
                    var accumulatedY = 0f
                    detectDragGestures(
                        onDragStart = { start = it; axis = 0; accumulatedX = 0f; accumulatedY = 0f },
                        onDragEnd = { axis = 0 },
                        onDragCancel = { axis = 0 },
                        onDrag = { change, dragAmount ->
                            if (locked) return@detectDragGestures
                            change.consume()
                            accumulatedX += dragAmount.x
                            accumulatedY += dragAmount.y
                            if (axis == 0 && (abs(accumulatedX) > 12f || abs(accumulatedY) > 12f)) {
                                axis = if (abs(accumulatedX) > abs(accumulatedY)) 1 else 2
                            }
                            when (axis) {
                                1 -> if (settings?.gestureHorizontalSeek != false && size.width > 0) {
                                    val maxSeek = if (durationMs > 0L) (durationMs / 20L).coerceIn(10_000L, 60_000L) else 10_000L
                                    val delta = (dragAmount.x / size.width.toFloat() * maxSeek).roundToLong()
                                    if (delta != 0L) seekBy(delta)
                                }
                                2 -> if (size.height > 0) {
                                    val startOnLeft = start.x < size.width / 2f
                                    if (startOnLeft && settings?.gestureVolume != false) {
                                        val fraction = VideoGestureSupport.adjustMusicVolume(context, -dragAmount.y / size.height.toFloat())
                                        volumeOverlayValue = (fraction * 100f).roundToInt().coerceIn(0, 100)
                                        volumeOverlay = true
                                    } else if (!startOnLeft && settings?.gestureBrightness != false) {
                                        val activity = context as? Activity
                                        if (activity != null) {
                                            val current = VideoGestureSupport.currentWindowBrightness(activity)
                                            val next = (current - dragAmount.y / size.height.toFloat()).coerceIn(0.02f, 1f)
                                            VideoGestureSupport.setWindowBrightness(activity, next)
                                            brightnessFeedback = "${(next * 100f).roundToInt()}%"
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
                .pointerInput(player, locked, settings?.gesturePinchZoom) {
                    if (settings?.gesturePinchZoom == true) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (!locked) pinchZoomState.floatValue = (pinchZoomState.floatValue * zoom).coerceIn(1f, 3f)
                        }
                    }
                }

        )

        VideoControlsOverlay(
            title = video?.title?.ifBlank { video?.filename }.orEmpty(),
            subtitle = video?.artist.orEmpty().ifBlank { video?.album.orEmpty() },
            isPlaying = player?.isPlaying == true,
            controlsVisible = controlsVisible && !locked,
            locked = locked,
            muted = muted,
            speed = speed,
            subtitleOn = subtitleOnState.value,
            isFullscreen = isFullscreenState.value,
            isLandscape = (context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE),
            positionMs = positionMs,
            durationMs = durationMs,
            bufferedMs = bufferedMs,
            queueCount = queue.count { it.id != id },
            shuffleQueue = shuffleQueue,
            repeatQueue = repeatQueue,
            qualityLabel = player?.videoSize?.let { videoQualityLabel(it.width, it.height, decoderLabel) }
                ?: video?.let { videoQualityLabel(it.width, it.height, decoderLabel) }
                ?: "",
            onBack = { if (isFullscreenState.value) toggleFullscreen() else (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() },
            onCast = onCast,
            onAudioTracks = { audioTracksOpen = true; touch() },
            onEqualizer = { onNavigate("equalizer") },
            onQueue = { queueOpen = true; touch() },
            onBookmark = { video?.let { vm.bookmark(it.id, positionMs, formatVideoTime(positionMs)) }; touch() },
            onMore = { moreOpen = true; touch() },
            onPlayPause = ::togglePlay,
            onPrevious = { VideoQueueStore.previous(id)?.let(::openQueueItem) ?: player?.seekTo(0L); touch() },
            onNext = { VideoQueueStore.next(id)?.let(::openQueueItem) ?: touch() },
            onMute = ::toggleMute,
            onBack10 = { seekBy(-seekStepMs) },
            onForward10 = { seekBy(seekStepMs) },
            onSubtitle = {
                if (subtitleOnState.value) toggleSubtitles() else subtitleLauncher.launch(arrayOf("text/vtt", "application/x-subrip", "text/plain"))
            },
            onSubtitleSettings = { subtitleSettingsOpen = true },
            onPiP = { if (Build.VERSION.SDK_INT >= 26 && allowsPiP) runCatching { (context as? Activity)?.enterPictureInPictureMode() }; touch() },
            onRotate = ::rotate,
            onFullscreen = ::toggleFullscreen,
            onLock = { locked = true; controlsVisible = false; interactionTick++ },
            onFit = {
                fitMode = VideoFitMode.entries[(fitMode.ordinal + 1) % VideoFitMode.entries.size]
                touch()
            },
            onSpeed = {
                val speeds = listOf(.25f, .5f, .75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f)
                speed = speeds[(speeds.indexOfFirst { it >= speed }.coerceAtLeast(0) + 1) % speeds.size]
                player?.setPlaybackSpeed(speed)
                touch()
            },
            onSeek = { fraction -> player?.seekTo((durationMs * fraction).toLong()); touch() },
        )

        if (locked) {
            GlassIconButton(
                Icons.Default.Lock,
                "Unlock controls",
                Modifier.align(Alignment.CenterEnd).padding(18.dp),
                onClick = {
                    locked = false
                    controlsVisible = true
                    interactionTick++
                },
            )
        }

        if (resumePromptVisible && resume > 5_000L) {
            ResumeVideoPrompt(
                positionMs = resume,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 132.dp, start = 20.dp, end = 20.dp),
                onResume = { resumePromptVisible = false; touch() },
                onStartOver = ::startOver,
            )
        }

        seekFeedback?.let { text ->
            GlassPill(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp)) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(formatVideoTime(positionMs), color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
            }
        }

        if (volumeOverlay) {
            VideoVolumeOverlay(
                value = volumeOverlayValue,
                muted = muted,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp),
            )
        }

        brightnessFeedback?.let { value ->
            VideoBrightnessOverlay(value, Modifier.align(Alignment.CenterEnd).padding(end = 22.dp))
        }

        playerError?.let { error ->
            Surface(Modifier.align(Alignment.Center).padding(24.dp), shape = RoundedCornerShape(24.dp), color = Color.Black.copy(alpha = .72f)) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    Text("Playback error", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    Text(error, color = Color.White.copy(alpha = .78f), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    TextButton(onClick = { player?.prepare(); player?.play(); playerError = null }, modifier = Modifier.padding(top = 4.dp)) { Text("Retry") }
                }
            }
        }
    }

    if (queueOpen) {
        VideoQueueSheet(container.mediaLibrary, ::openQueueItem) { queueOpen = false }
    }
    if (subtitleSettingsOpen) {
        VideoSubtitleSettingsSheet(container) { subtitleSettingsOpen = false }
    }
    if (audioTracksOpen) {
        VideoAudioTrackSheet(
            player = player,
            rememberSelection = settings?.rememberAudioTrack == true,
            rememberKey = video?.id,
            onDismiss = { audioTracksOpen = false },
        )
    }
    if (moreOpen) {
        val item = video
        if (item != null) {
            AirPlayrPremiumMediaActionSheet(
                item = item,
                favorite = favorite,
                playing = player?.isPlaying == true,
                groups = listOf(
                    listOf(
                        AirPlayrMediaMenuAction("Play / pause", if (player?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow, { togglePlay(); moreOpen = false }),
                        AirPlayrMediaMenuAction("Video queue", Icons.Default.QueueMusic, { moreOpen = false; queueOpen = true }),
                        AirPlayrMediaMenuAction(if (shuffleQueue) "Shuffle: On" else "Shuffle: Off", Icons.Default.Shuffle, { moreOpen = false; toggleVideoShuffle() }),
                        AirPlayrMediaMenuAction(if (repeatQueue) "Repeat: On" else "Repeat: Off", Icons.Default.Repeat, { moreOpen = false; toggleVideoRepeat() }),
                        AirPlayrMediaMenuAction("Equalizer", Icons.Default.GraphicEq, { moreOpen = false; onNavigate("equalizer") }),
                    ),
                    listOf(
                        AirPlayrMediaMenuAction("Subtitles", Icons.Default.ClosedCaption, { moreOpen = false; subtitleSettingsOpen = true }),
                        AirPlayrMediaMenuAction("Add subtitle file", Icons.Default.Subtitles, { moreOpen = false; subtitleLauncher.launch(arrayOf("text/vtt", "application/x-subrip", "text/plain")) }),
                        AirPlayrMediaMenuAction("Fit: ${fitMode.label}", Icons.Default.AspectRatio, {
                            fitMode = VideoFitMode.entries[(fitMode.ordinal + 1) % VideoFitMode.entries.size]
                            moreOpen = false
                            touch()
                        }),
                    ),
                    listOf(
                        AirPlayrMediaMenuAction("Speed ${"%.2f".format(Locale.US, speed).trimEnd('0').trimEnd('.') }×", Icons.Default.Speed, {
                            val speeds = listOf(.25f, .5f, .75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f)
                            speed = speeds[(speeds.indexOfFirst { it >= speed }.coerceAtLeast(0) + 1) % speeds.size]
                            player?.setPlaybackSpeed(speed)
                            moreOpen = false
                        }),
                        AirPlayrMediaMenuAction("Add bookmark", Icons.Default.BookmarkAdd, { vm.bookmark(id, positionMs, formatVideoTime(positionMs)); moreOpen = false }),
                    ),
                ),
                onFavorite = { vm.toggleFavorite(item); favorite = !favorite },
                onDismiss = { moreOpen = false },
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun AndroidVideoSurface(player: Player, fitMode: VideoFitMode, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(-1, -1)
                useController = false
                setKeepContentOnPlayerReset(true)
                resizeMode = fitMode.resizeMode
                this.player = player
                setBackgroundColor(android.graphics.Color.BLACK)
                findViewById<View>(androidx.media3.ui.R.id.exo_subtitles)?.visibility = View.GONE
            }
        },
        update = { view ->
            view.player = player
            view.resizeMode = fitMode.resizeMode
            view.findViewById<View>(androidx.media3.ui.R.id.exo_subtitles)?.visibility = View.GONE
        },
        modifier = modifier,
    )
}

@Composable
private fun VideoControlsOverlay(
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    controlsVisible: Boolean,
    locked: Boolean,
    muted: Boolean,
    speed: Float,
    subtitleOn: Boolean,
    isFullscreen: Boolean,
    isLandscape: Boolean,
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    queueCount: Int,
    shuffleQueue: Boolean,
    repeatQueue: Boolean,
    qualityLabel: String,
    onBack: () -> Unit,
    onCast: () -> Unit,
    onAudioTracks: () -> Unit,
    onEqualizer: () -> Unit,
    onQueue: () -> Unit,
    onBookmark: () -> Unit,
    onMore: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMute: () -> Unit,
    onBack10: () -> Unit,
    onForward10: () -> Unit,
    onSubtitle: () -> Unit,
    onSubtitleSettings: () -> Unit,
    onPiP: () -> Unit,
    onRotate: () -> Unit,
    onFullscreen: () -> Unit,
    onLock: () -> Unit,
    onFit: () -> Unit,
    onSpeed: () -> Unit,
    onSeek: (Float) -> Unit,
) {
    if (!controlsVisible || locked) return
    AnimatedVisibility(visible = true, modifier = Modifier.fillMaxSize(), enter = fadeIn(tween(120)), exit = fadeOut(tween(90))) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(145.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .62f), Color.Transparent))))
            Box(Modifier.fillMaxWidth().height(180.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .72f)))))

            BoxWithConstraints(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)) {
                val compactHeader = maxWidth < 600.dp
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    GlassIconButton(Icons.Default.ArrowBack, "Back", Modifier.size(54.dp), onBack)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 2.dp)) {
                        Text(title.ifBlank { "Video" }, color = Color.White, fontFamily = AirPlayrFonts.SmoochSans, fontSize = if (compactHeader) 16.sp else 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(subtitle.ifBlank { "AIRPLAYR VIDEO" }, color = Color.White.copy(alpha = .65f), fontSize = 10.sp, letterSpacing = 1.1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (qualityLabel.isNotBlank()) Text(qualityLabel, color = Color.White.copy(alpha = .82f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (compactHeader) {
                        GlassGroup {
                            GlassAction(Icons.Default.Cast, "Cast", onCast)
                            GlassAction(Icons.Default.Headphones, "Audio", onAudioTracks)
                            GlassPill { Text("${trimSpeed(speed)}×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            GlassAction(Icons.Default.MoreHoriz, "More", onMore)
                        }
                    } else {
                        GlassGroup {
                            GlassAction(Icons.Default.Cast, "Cast", onCast)
                            GlassAction(Icons.Default.Audiotrack, "Audio tracks", onAudioTracks)
                            GlassAction(if (subtitleOnState.value) Icons.Default.ClosedCaption else Icons.Default.SubtitlesOff, "Subtitles", onSubtitle)
                        }
                        Spacer(Modifier.width(6.dp))
                        GlassIconButton(Icons.Default.GraphicEq, "Equalizer", Modifier.size(48.dp), onEqualizer)
                        Spacer(Modifier.width(6.dp))
                        GlassPill {
                            Text("${trimSpeed(speed)}×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(6.dp))
                        GlassIconButton(Icons.Default.BookmarkBorder, "Bookmark", Modifier.size(48.dp), onBookmark)
                        Spacer(Modifier.width(6.dp))
                        GlassIconButton(Icons.Default.QueueMusic, if (queueCount > 0) "Queue, $queueCount next" else "Queue", Modifier.size(48.dp), onQueue)
                        Spacer(Modifier.width(6.dp))
                        GlassIconButton(Icons.Default.MoreHoriz, "More", Modifier.size(48.dp), onMore)
                    }
                }
            }

            // Reference layout: transport controls live in the lower playback rail,
            // never in the visual centre of the video frame. This keeps the source aspect
            // ratio untouched while making the play button easy to reach with one hand.
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.Default.SkipPrevious, "Previous", Modifier.size(58.dp), onPrevious)
                GlassIconButton(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (isPlaying) "Pause" else "Play",
                    Modifier.size(76.dp),
                    onPlayPause,
                    primary = false,
                    iconSize = 36.dp,
                )
                GlassIconButton(Icons.Default.SkipNext, "Next", Modifier.size(58.dp), onNext)
            }

            val portrait = !isLandscape
            GlassGroup(
                Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 18.dp, bottom = if (portrait) 104.dp else 18.dp)
            ) {
                GlassAction(Icons.Default.Lock, "Lock controls", onLock)
                GlassAction(if (muted) Icons.Default.VolumeOff else Icons.Default.Headphones, if (muted) "Unmute" else "Audio", onMute)
            }

            GlassGroup(
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 18.dp, bottom = if (portrait) 104.dp else 18.dp)
            ) {
                GlassAction(Icons.Default.PictureInPictureAlt, "Picture in picture", onPiP)
                GlassAction(Icons.Default.ScreenRotation, "Portrait / landscape", onRotate)
                GlassAction(if (isFullscreen) Icons.Default.ZoomOutMap else Icons.Default.ZoomInMap, "Zoom", onFit)
            }

            if (portrait) {
                GlassGroup(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 104.dp)) {
                    GlassAction(if (shuffleQueue) Icons.Default.Shuffle else Icons.Default.Shuffle, if (shuffleQueue) "Shuffle on" else "Shuffle off", {
                        if (shuffleQueue) VideoQueueStore.setShuffle(false) else VideoQueueStore.setShuffle(true)
                    })
                    GlassAction(if (repeatQueue) Icons.Default.RepeatOne else Icons.Default.Repeat, if (repeatQueue) "Repeat on" else "Repeat off", {
                        if (repeatQueue) VideoQueueStore.setRepeat(false) else VideoQueueStore.setRepeat(true)
                    })
                }
            }

            VideoProgressBar(
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedMs = bufferedMs,
                onSeek = onSeek,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 22.dp, end = 22.dp, bottom = 82.dp),
            )
        }
    }
}

@Composable
private fun ResumeVideoPrompt(
    positionMs: Long,
    modifier: Modifier = Modifier,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = .78f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .16f)),
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Continue watching", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Resume from ${formatVideoTime(positionMs)}", color = Color.White.copy(alpha = .68f), fontSize = 11.sp)
            }
            TextButton(onClick = onStartOver) { Text("Start over", color = Color.White.copy(alpha = .78f)) }
            Button(
                onClick = onResume,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(3.dp))
                Text("Resume")
            }
        }
    }
}

@Composable
private fun VideoProgressBar(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val buffered = if (durationMs > 0L) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(formatVideoTime(positionMs), color = Color.White.copy(alpha = .92f), fontSize = 11.sp, modifier = Modifier.width(52.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(34.dp)
                    .pointerInput(durationMs) {
                        detectTapGestures { offset ->
                            if (size.width > 0) onSeek((offset.x / size.width).coerceIn(0f, 1f))
                        }
                    }
                    .pointerInput(durationMs) {
                        detectDragGestures(
                            onDragStart = { offset -> if (size.width > 0) onSeek((offset.x / size.width).coerceIn(0f, 1f)) },
                            onDrag = { change, _ ->
                                change.consume()
                                if (size.width > 0) onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                            },
                        )
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = .24f)))
                Box(Modifier.fillMaxWidth(buffered).height(5.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = .38f)))
                Box(Modifier.fillMaxWidth(progress).height(5.dp).clip(RoundedCornerShape(5.dp)).background(Color.White))
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .padding(horizontal = 1.dp),
                ) {
                    Surface(
                        modifier = Modifier
                            .size(14.dp)
                            .offset(x = ((maxWidth - 14.dp).coerceAtLeast(0.dp) * progress))
                            .align(Alignment.CenterStart),
                        shape = CircleShape,
                        color = Color.White,
                        tonalElevation = 0.dp,
                    ) {}
                }
            }
            Text(formatVideoTime(durationMs), color = Color.White.copy(alpha = .78f), fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
        }
    }
}

@Composable
internal fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    primary: Boolean = false,
    iconSize: Dp = 23.dp,
) {
    val background = if (primary) Color.White.copy(alpha = .90f) else Color.Black.copy(alpha = .34f)
    val tint = if (primary) Color.Black else Color.White
    Surface(
        onClick = onClick,
        modifier = modifier.border(1.dp, Color.White.copy(alpha = .16f), CircleShape),
        shape = CircleShape,
        color = background,
        tonalElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, tint = tint, modifier = Modifier.size(iconSize)) }
    }
}

@Composable
internal fun GlassGroup(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = modifier.border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(30.dp)),
        shape = RoundedCornerShape(30.dp),
        color = Color.Black.copy(alpha = .34f),
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            content = content,
        )
    }
}

@Composable
internal fun GlassAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(46.dp), shape = CircleShape, color = Color.Transparent, tonalElevation = 0.dp) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
internal fun GlassPill(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = modifier.border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = .34f),
        tonalElevation = 0.dp,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
internal fun VideoSubtitleOverlay(
    cues: List<String>,
    sizeScale: Double,
    textColor: Color,
    background: Boolean,
    top: Boolean,
    controlsVisible: Boolean,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp).then(if (top) Modifier.statusBarsPadding().padding(top = 92.dp) else Modifier.navigationBarsPadding().padding(bottom = if (controlsVisible) 112.dp else 48.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (top) Arrangement.Top else Arrangement.Bottom,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (background) Color.Black.copy(alpha = .56f) else Color.Transparent,
        ) {
            Text(
                cues.joinToString("\n"),
                color = textColor,
                fontSize = (18.0 * sizeScale).sp,
                lineHeight = (23.0 * sizeScale).sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
internal fun VideoBrightnessOverlay(value: String, modifier: Modifier) {
    GlassPill(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Default.Brightness6, null, tint = Color.White, modifier = Modifier.size(23.dp))
            Box(Modifier.width(7.dp).height(112.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = .20f)), contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.fillMaxWidth().fillMaxHeight((value.removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0f).coerceIn(0f, 1f)).clip(RoundedCornerShape(8.dp)).background(Color.White))
            }
            Text(value, color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
internal fun VideoVolumeOverlay(value: Int, muted: Boolean, modifier: Modifier) {
    GlassPill(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(23.dp))
            Box(Modifier.width(7.dp).height(112.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = .20f)), contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.fillMaxWidth().fillMaxHeight(value / 100f).clip(RoundedCornerShape(8.dp)).background(Color.White))
            }
            Text("$value%", color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
private fun VideoAudioTrackSheet(player: Player?, rememberSelection: Boolean, rememberKey: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val tracks = player?.currentTracks?.groups.orEmpty().filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Audio track", style = MaterialTheme.typography.headlineSmall)
                    Text("Choose the audio stream supplied by the video.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
            }
            if (tracks.isEmpty()) {
                Text("No alternate audio tracks were found.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 26.dp))
            } else {
                tracks.forEachIndexed { groupIndex, group ->
                    for (trackIndex in 0 until group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        val selected = group.isTrackSelected(trackIndex)
                        val label = format.label?.toString()?.takeIf { it.isNotBlank() }
                            ?: format.language?.takeIf { it.isNotBlank() }?.uppercase(Locale.getDefault())
                            ?: "Audio ${groupIndex + 1}"
                        val detail = listOfNotNull(format.language?.takeIf { it.isNotBlank() }, format.codecs?.takeIf { it.isNotBlank() }, format.channelCount.takeIf { it > 0 }?.let { "$it ch" }).distinct().joinToString(" · ")
                        Surface(
                            onClick = {
                                player?.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                    .build()
                                if (rememberSelection && rememberKey != null) {
                                    context.getSharedPreferences("airplayr_video_audio", 0).edit()
                                        .putInt("track:$rememberKey", trackIndex)
                                        .apply()
                                }
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                    if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal object WindowMode {
    fun setFullscreen(activity: Activity, fullscreen: Boolean) {
        val controller = androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (fullscreen) {
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

internal fun parseSubtitleColor(value: String?): Color = runCatching { Color(android.graphics.Color.parseColor(value ?: "#FFFFFF")) }.getOrDefault(Color.White)
private fun videoQualityLabel(width: Int, height: Int, decoder: String?): String {
    val quality = when {
        height >= 2160 -> "4K"
        height >= 1440 -> "2K"
        height >= 1080 -> "FHD"
        height >= 720 -> "HD"
        height > 0 -> "SD"
        else -> "AUTO"
    }
    return listOfNotNull(decoder, quality).joinToString(" · ")
}

internal fun trimSpeed(value: Float): String = "%.2f".format(Locale.US, value).trimEnd('0').trimEnd('.')

internal fun formatVideoTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000L
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    return if (hours > 0L) String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds) else String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
