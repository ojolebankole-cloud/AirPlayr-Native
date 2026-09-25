package com.airplayr.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.text.CueGroup
import androidx.media3.common.Player
import com.airplayr.app.di.AppContainer
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun ExternalVideoScreen(
    container: AppContainer,
    uri: String,
    mimeType: String?,
    title: String,
    onCast: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val autoHideMs = settings?.autoHideMs ?: 4000
    val defaultVolume = (settings?.defaultVolume?.toFloat() ?: 1f).coerceIn(0f, 1f)
    val seekStepMs = ((settings?.seekStepSeconds ?: 10) * 1000L).coerceIn(5_000L, 30_000L)
    val muteOnStart = settings?.muteOnStart ?: false
    val defaultFitMode = when (settings?.defaultAspect) {
        "cover" -> VideoFitMode.ZOOM
        "fill" -> VideoFitMode.FILL
        else -> VideoFitMode.FIT
    }
    var player by remember(uri) { mutableStateOf<Player?>(null) }
    var muted by remember(uri, muteOnStart) { mutableStateOf(muteOnStart) }
    var controlsVisible by remember { mutableStateOf(true) }
    var fullscreen by remember { mutableStateOf(false) }
    var fitMode by remember(uri, defaultFitMode) { mutableStateOf(defaultFitMode) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var interaction by remember { mutableIntStateOf(0) }
    var subtitleOn by remember(uri, settings?.subtitlesEnabledDefault, settings?.rememberSubtitleTrack) {
        val defaultOn = settings?.subtitlesEnabledDefault ?: true
        if (settings?.rememberSubtitleTrack == true) context.getSharedPreferences("airplayr_video_subtitles", 0).getBoolean("enabled:external:$uri", defaultOn) else defaultOn
    }
    var cues by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingCues by remember { mutableStateOf<List<String>?>(null) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var volumeOverlay by remember { mutableStateOf(false) }
    var volumeOverlayValue by remember { mutableIntStateOf((VideoGestureSupport.musicVolumeFraction(context) * 100f).roundToInt().coerceIn(0, 100)) }
    var brightnessFeedback by remember { mutableStateOf<String?>(null) }
    var pinchZoom by remember(uri, settings?.gesturePinchZoom) { mutableFloatStateOf(1f) }
    var longPressBoostActive by remember(uri) { mutableStateOf(false) }

    fun touch() { controlsVisible = true; interaction++ }
    fun seek(delta: Long) {
        player?.let { p -> p.seekTo((p.currentPosition + delta).coerceAtLeast(0L).coerceAtMost(p.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)) }
        seekFeedback = if (delta >= 0) "+${delta / 1000}s" else "${delta / 1000}s"
        touch()
    }
    fun toggleFullscreen() {
        fullscreen = !fullscreen
        WindowMode.setFullscreen(context as? Activity ?: return, fullscreen)
        touch()
    }
    fun toggleMute() {
        muted = !muted
        player?.volume = if (muted) 0f else defaultVolume
        volumeOverlayValue = if (muted) 0 else ((VideoGestureSupport.musicVolumeFraction(context) * 100f).roundToInt().coerceIn(0, 100))
        volumeOverlay = true
        touch()
    }
    fun toggleSubtitles() {
        val p = player ?: return
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, subtitleOn).build()
        subtitleOn = !subtitleOn
        if (settings?.rememberSubtitleTrack == true) context.getSharedPreferences("airplayr_video_subtitles", 0).edit().putBoolean("enabled:external:$uri", subtitleOn).apply()
        touch()
    }

    LaunchedEffect(Unit) { com.airplayr.app.core.playback.service.AirPlayrPlaybackService.enterVideoMode() }
    LaunchedEffect(uri) {
        container.videoController.connectAsync { ready ->
            player = ready
            ready.addListener(object : Player.Listener {
                override fun onCues(group: CueGroup) {
                    val next = group.cues.mapNotNull { it.text?.toString()?.trim()?.takeIf(String::isNotBlank) }
                    if ((settings?.subtitleDelayMs ?: 0) <= 0) cues = next else { cues = emptyList(); pendingCues = next }
                }
            })
            ready.volume = if (muted) 0f else defaultVolume
            container.videoController.playExternal(ready, uri, mimeType, title)
        }
    }
    LaunchedEffect(pendingCues, settings?.subtitleDelayMs) {
        val pending = pendingCues ?: return@LaunchedEffect
        val delayMs = (settings?.subtitleDelayMs ?: 0).coerceAtLeast(0)
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs.toLong())
        cues = pending
        pendingCues = null
    }

    LaunchedEffect(player) {
        while (player != null) {
            val p = player ?: break
            position = p.currentPosition.coerceAtLeast(0L)
            duration = p.duration.takeIf { it > 0 } ?: 0L
            delay(300L)
        }
    }
    LaunchedEffect(player, interaction, controlsVisible, autoHideMs) {
        if (controlsVisible && player?.isPlaying == true && autoHideMs > 0) {
            delay(autoHideMs.toLong())
            controlsVisible = false
        }
    }
    LaunchedEffect(seekFeedback) { if (seekFeedback != null) { delay(900L); seekFeedback = null } }
    LaunchedEffect(volumeOverlay) { if (volumeOverlay) { delay(1400L); volumeOverlay = false } }
    LaunchedEffect(brightnessFeedback) { if (brightnessFeedback != null) { delay(1200L); brightnessFeedback = null } }
    DisposableEffect(Unit) {
        onDispose {
            player?.pause()
            container.videoController.release()
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            com.airplayr.app.core.playback.service.AirPlayrPlaybackService.exitVideoMode()
        }
    }
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        onDispose { activity?.let { VideoGestureSupport.restoreWindowBrightness(it, originalBrightness) } }
    }
    DisposableEffect(settings?.keepAwake) {
        val window = (context as? Activity)?.window
        if (settings?.keepAwake == true) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { if (settings?.keepAwake == true) window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    if (Build.VERSION.SDK_INT >= 26) SideEffect {
        runCatching { (context as? Activity)?.setPictureInPictureParams(PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(16, 9)).build()) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)
        .pointerInput(player, settings?.gestureDoubleTapSeek, settings?.gestureLongPress2x) {
            detectTapGestures(
                onPress = {
                    tryAwaitRelease()
                    if (longPressBoostActive) { longPressBoostActive = false; player?.setPlaybackSpeed(settings?.defaultSpeed?.toFloat()?.coerceIn(.25f, 4f) ?: 1f) }
                },
                onTap = { controlsVisible = !controlsVisible; interaction++ },
                onDoubleTap = { if (settings?.gestureDoubleTapSeek != false) { player?.let { if (it.isPlaying) it.pause() else it.play() }; controlsVisible = true; interaction++ } },
                onLongPress = {
                    if (settings?.gestureLongPress2x == true) { longPressBoostActive = true; player?.setPlaybackSpeed(2f) }
                },
            )
        }
        .pointerInput(player, settings?.gestureVolume, settings?.gestureBrightness, settings?.gestureHorizontalSeek, duration) {
            var gestureStart = androidx.compose.ui.geometry.Offset.Zero
            var gestureAxis = 0
            var gestureDx = 0f
            var gestureDy = 0f
            detectDragGestures(
                onDragStart = { gestureStart = it; gestureAxis = 0; gestureDx = 0f; gestureDy = 0f },
                onDragEnd = { gestureAxis = 0 },
                onDragCancel = { gestureAxis = 0 },
                onDrag = { change, dragAmount ->
                    change.consume()
                    gestureDx += dragAmount.x
                    gestureDy += dragAmount.y
                    if (gestureAxis == 0 && (abs(gestureDx) > 12f || abs(gestureDy) > 12f)) gestureAxis = if (abs(gestureDx) > abs(gestureDy)) 1 else 2
                    when (gestureAxis) {
                        1 -> if (settings?.gestureHorizontalSeek != false && size.width > 0) {
                            val maxSeek = if (duration > 0L) (duration / 20L).coerceIn(10_000L, 60_000L) else 10_000L
                            val delta = (dragAmount.x / size.width.toFloat() * maxSeek).roundToLong()
                            if (delta != 0L) seek(delta)
                        }
                        2 -> if (size.height > 0) {
                            val left = gestureStart.x < size.width / 2f
                            if (left && settings?.gestureVolume != false) {
                                val fraction = VideoGestureSupport.adjustMusicVolume(context, -dragAmount.y / size.height.toFloat())
                                volumeOverlayValue = (fraction * 100f).roundToInt().coerceIn(0, 100)
                                volumeOverlay = true
                            } else if (!left && settings?.gestureBrightness != false) {
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
        .pointerInput(player, settings?.gesturePinchZoom) {
            if (settings?.gesturePinchZoom == true) {
                androidx.compose.foundation.gestures.detectTransformGestures { _, _, zoom, _ ->
                    pinchZoom = (pinchZoom * zoom).coerceIn(1f, 3f)
                }
            }
        }
        ) {
        player?.let { AndroidVideoSurface(it, fitMode, Modifier.fillMaxSize().graphicsLayer(scaleX = pinchZoom, scaleY = pinchZoom)) }
        if (subtitleOn && cues.isNotEmpty()) {
            VideoSubtitleOverlay(cues, settings?.subtitleSize ?: 1.0, parseSubtitleColor(settings?.subtitleColor), settings?.subtitleBg ?: true, settings?.subtitlePosition == "top", controlsVisible)
        }
        AnimatedVisibility(visible = controlsVisible, modifier = Modifier.fillMaxSize(), enter = fadeIn(tween(120)), exit = fadeOut(tween(90))) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(145.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .62f), Color.Transparent))))
                Box(Modifier.fillMaxWidth().height(180.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .72f)))))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
                    GlassIconButton(Icons.Default.ArrowBack, "Back", Modifier.size(54.dp), onClick = { (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() })
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 2.dp)) {
                        Text(title.ifBlank { "Video" }, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("EXTERNAL VIDEO", color = Color.White.copy(alpha = .65f), fontSize = 10.sp, letterSpacing = 1.1.sp)
                    }
                    GlassIconButton(Icons.Default.Cast, "Cast", Modifier.size(48.dp), onCast)
                    Spacer(Modifier.width(6.dp))
                    GlassIconButton(Icons.Default.GraphicEq, "Equalizer", Modifier.size(48.dp), onClick = { onNavigate("equalizer") })
                    Spacer(Modifier.width(6.dp))
                    GlassIconButton(if (subtitleOn) Icons.Default.ClosedCaption else Icons.Default.SubtitlesOff, "Subtitles", Modifier.size(48.dp), ::toggleSubtitles)
                    Spacer(Modifier.width(6.dp))
                    GlassIconButton(if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, "Mute", Modifier.size(48.dp), ::toggleMute)
                    Spacer(Modifier.width(6.dp))
                    GlassIconButton(Icons.Default.AspectRatio, "Fit: ${fitMode.label}", Modifier.size(48.dp), onClick = { fitMode = VideoFitMode.entries[(fitMode.ordinal + 1) % VideoFitMode.entries.size]; touch() })
                }
                Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(if (player?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow, if (player?.isPlaying == true) "Pause" else "Play", Modifier.size(82.dp), onClick = { player?.let { if (it.isPlaying) it.pause() else it.play(); touch() } }, primary = true, iconSize = 38.dp)
                }
                Row(Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 18.dp, bottom = 76.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(Icons.Default.Replay10, "Back 10 seconds", Modifier.size(50.dp), onClick = { seek(-10_000L) })
                    GlassIconButton(Icons.Default.VolumeUp, "Volume", Modifier.size(50.dp), ::toggleMute)
                    GlassIconButton(Icons.Default.ScreenRotation, "Rotate", Modifier.size(50.dp), onClick = {
                        val a = context as? Activity
                        a?.let { it.requestedOrientation = if (it.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                        touch()
                    })
                }
                Row(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 18.dp, bottom = 76.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(Icons.Default.Forward10, "Forward 10 seconds", Modifier.size(50.dp), onClick = { seek(10_000L) })
                    GlassIconButton(Icons.Default.PictureInPictureAlt, "Picture in picture", Modifier.size(50.dp), onClick = { if (Build.VERSION.SDK_INT >= 26) (context as? Activity)?.enterPictureInPictureMode(); touch() })
                    GlassIconButton(if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Fullscreen", Modifier.size(50.dp), ::toggleFullscreen)
                }
                Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)) {
                    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatVideoTime(position), color = Color.White, fontSize = 11.sp, modifier = Modifier.width(54.dp))
                        Box(Modifier.weight(1f).height(28.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = .26f), RoundedCornerShape(4.dp)))
                            Box(Modifier.fillMaxWidth(progress).height(4.dp).background(Color.White, RoundedCornerShape(4.dp)))
                            Box(Modifier.fillMaxWidth().height(28.dp).pointerInput(duration) { detectTapGestures { p -> if (size.width > 0) player?.seekTo((duration * (p.x / size.width).coerceIn(0f, 1f)).toLong()) } })
                        }
                        Text(formatVideoTime(duration), color = Color.White, fontSize = 11.sp, modifier = Modifier.width(54.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }
        seekFeedback?.let { text -> GlassPill(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp)) { Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp), tint = Color.White); Text(text, color = Color.White); Text(formatVideoTime(position), color = Color.White.copy(alpha = .7f), fontSize = 12.sp) } }
        if (volumeOverlay) VideoVolumeOverlay(volumeOverlayValue, muted, Modifier.align(Alignment.CenterEnd).padding(end = 22.dp))
        brightnessFeedback?.let { VideoBrightnessOverlay(it, Modifier.align(Alignment.CenterEnd).padding(end = 22.dp)) }
    }
}
