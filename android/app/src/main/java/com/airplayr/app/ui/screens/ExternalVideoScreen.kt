package com.airplayr.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.cast.CastButton
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ExternalVideoScreen(container: AppContainer, uri: String, mimeType: String?, title: String) {
    val context = LocalContext.current
    var player by remember(uri) { mutableStateOf<Player?>(null) }
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val autoHideMs = settings?.autoHideMs ?: 4000
    val defaultVolume = (settings?.defaultVolume?.toFloat() ?: 1f).coerceIn(0f, 1f)
    val muteOnStart = settings?.muteOnStart ?: false
    val defaultFitMode = when (settings?.defaultAspect) {
        "cover" -> VideoFitMode.ZOOM
        "fill" -> VideoFitMode.FILL
        else -> VideoFitMode.FIT
    }
    var muted by remember(uri, muteOnStart) { mutableStateOf(muteOnStart) }
    var controlsVisible by remember { mutableStateOf(true) }
    var fullscreen by remember { mutableStateOf(false) }
    var fitMode by remember(uri, defaultFitMode) { mutableStateOf(defaultFitMode) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var interaction by remember { mutableIntStateOf(0) }

    fun touch() { controlsVisible = true; interaction++ }
    fun seek(delta: Long) {
        player?.let { p -> p.seekTo((p.currentPosition + delta).coerceAtLeast(0L).coerceAtMost(p.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)) }
        touch()
    }
    fun toggleFullscreen() {
        fullscreen = !fullscreen
        val activity = context as? Activity ?: return
        if (fullscreen) {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).let { c ->
                c.hide(WindowInsetsCompat.Type.systemBars())
                c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        touch()
    }

    LaunchedEffect(Unit) { com.airplayr.app.core.playback.service.AirPlayrPlaybackService.enterVideoMode() }
    LaunchedEffect(uri) {
        container.videoController.connectAsync { ready ->
            player = ready
            ready.volume = if (muted) 0f else defaultVolume
            container.videoController.playExternal(ready, uri, mimeType, title)
        }
    }
    LaunchedEffect(player) {
        while (player != null) {
            val p = player ?: break
            position = p.currentPosition.coerceAtLeast(0L)
            duration = p.duration.takeIf { it > 0 } ?: 0L
            delay(400L)
        }
    }
    LaunchedEffect(player, interaction, controlsVisible, autoHideMs) {
        if (controlsVisible && player?.isPlaying == true && autoHideMs > 0) {
            delay(autoHideMs.toLong())
            controlsVisible = false
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            player?.pause()
            container.videoController.release()
            com.airplayr.app.core.playback.service.AirPlayrPlaybackService.exitVideoMode()
        }
    }
    if (Build.VERSION.SDK_INT >= 26) SideEffect {
        val w = 16
        val h = 9
        runCatching { (context as? Activity)?.setPictureInPictureParams(PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(w, h)).build()) }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(player) {
            detectTapGestures(
                onTap = { controlsVisible = !controlsVisible; interaction++ },
                onDoubleTap = { point -> if (point.x < size.width / 2f) seek(-10_000L) else seek(10_000L) },
            )
        },
    ) {
        player?.let { AndroidVideoSurface(it, fitMode, Modifier.fillMaxSize()) }
        AnimatedVisibility(visible = controlsVisible, modifier = Modifier.fillMaxSize(), enter = fadeIn(tween(120)), exit = fadeOut(tween(90))) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(110.dp).align(Alignment.TopCenter).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha = .64f), Color.Transparent))))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    VideoTopButton(Icons.Default.ArrowBack, "Back") { (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(title.ifBlank { "Video" }, color = Color.White, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("EXTERNAL VIDEO", color = Color.White.copy(alpha = .65f), fontSize = 10.sp, letterSpacing = 1.1.sp)
                    }
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { CastButton(Modifier.size(42.dp)) }
                    VideoTopButton(if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, if (muted) "Unmute" else "Mute") {
                        muted = !muted; player?.volume = if (muted) 0f else defaultVolume; touch()
                    }
                    VideoTopButton(if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Fullscreen", ::toggleFullscreen)
                }
                Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    VideoRoundControl(Icons.Default.Replay10, "Back 10") { seek(-10_000L) }
                    VideoRoundControl(if (player?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", { player?.let { if (it.isPlaying) it.pause() else it.play(); touch() } }, 78.dp, true)
                    VideoRoundControl(Icons.Default.Forward10, "Forward 10") { seek(10_000L) }
                }
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = .72f)).padding(horizontal = 18.dp, vertical = 10.dp).navigationBarsPadding()) {
                    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(position), color = Color.White, fontSize = 11.sp, modifier = Modifier.width(54.dp))
                        Slider(value = progress, onValueChange = { f -> player?.seekTo((duration * f).toLong()); touch() }, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = .28f)))
                        Text(formatTime(duration), color = Color.White, fontSize = 11.sp, modifier = Modifier.width(54.dp))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        VideoBottomButton(Icons.Default.Replay10, "Replay 10") { seek(-10_000L) }
                        VideoBottomButton(if (player?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow, if (player?.isPlaying == true) "Pause" else "Play", { player?.let { if (it.isPlaying) it.pause() else it.play(); touch() } }, prominent = true, playing = player?.isPlaying == true)
                        VideoBottomButton(Icons.Default.Forward10, "Forward 10") { seek(10_000L) }
                        VideoBottomButton(Icons.Default.AspectRatio, fitMode.label) {
                            fitMode = VideoFitMode.entries[(fitMode.ordinal + 1) % VideoFitMode.entries.size]
                            touch()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoTopButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(44.dp), shape = CircleShape, color = Color.Black.copy(alpha = .36f)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(23.dp)) }
    }
}

@Composable
private fun VideoRoundControl(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 58.dp, primary: Boolean = false) {
    val colors = com.airplayr.app.ui.theme.LocalAirPlayrColors.current
    Surface(onClick = onClick, modifier = Modifier.size(size), shape = CircleShape, color = if (primary) colors.primary else Color.Black.copy(alpha = .50f)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, tint = if (primary) colors.onPrimary else Color.White, modifier = Modifier.size(if (primary) 38.dp else 28.dp)) }
    }
}

@Composable
private fun VideoBottomButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, prominent: Boolean = false, playing: Boolean = false) {
    Surface(onClick = onClick, modifier = Modifier.size(if (prominent) 60.dp else 48.dp), shape = CircleShape, color = if (prominent) Color.White.copy(alpha = .96f) else Color.Transparent) {
        Box(contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Default.Pause else icon, label, tint = if (prominent) Color.Black else Color.White, modifier = Modifier.size(if (prominent) 28.dp else 24.dp)) }
    }
}

private fun formatTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000L
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    return if (hours > 0L) String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds) else String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
