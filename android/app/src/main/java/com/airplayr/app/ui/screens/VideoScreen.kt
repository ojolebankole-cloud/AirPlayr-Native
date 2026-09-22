package com.airplayr.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.airplayr.app.core.data.VIDEO_COMPLETION_FRACTION
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.cast.CastButton
import com.airplayr.app.ui.components.AirPlayrPremiumMediaActionSheet
import com.airplayr.app.ui.components.AirPlayrMediaMenuAction
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import com.airplayr.app.ui.video.VideoPlayerViewModel
import com.airplayr.app.ui.video.VideoPlayerViewModelFactory
import java.util.Locale
import kotlinx.coroutines.delay

internal enum class VideoFitMode(val label: String, val resizeMode: Int) {
    FIT("Fit", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom", androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
}

@OptIn(UnstableApi::class)
@Composable
fun VideoScreen(container: AppContainer, id: String) {
    val vm: VideoPlayerViewModel = viewModel(factory = VideoPlayerViewModelFactory(container.mediaLibrary, container.structured, container.stats))
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val savePlaybackHistory = settings?.savePlaybackHistory ?: true
    val autoEnterPiP = settings?.autoEnterPiPOnBackground ?: false
    val allowsPiP = settings?.allowsPictureInPicture ?: true
    val autoHideMs = settings?.autoHideMs ?: 4000
    val defaultSpeed = (settings?.defaultSpeed?.toFloat() ?: 1f).coerceIn(0.25f, 4f)
    val defaultVolume = (settings?.defaultVolume?.toFloat() ?: 1f).coerceIn(0f, 1f)
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

    var player by remember { mutableStateOf<Player?>(null) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var favorite by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var muted by remember(video?.id, muteOnStart) { mutableStateOf(muteOnStart) }
    var speed by remember(video?.id, defaultSpeed) { mutableFloatStateOf(defaultSpeed) }
    var repeatA by remember { mutableStateOf<Long?>(null) }
    var repeatB by remember { mutableStateOf<Long?>(null) }
    var subtitleOn by remember { mutableStateOf(false) }
    var fitMode by remember(video?.id, defaultFitMode) { mutableStateOf(defaultFitMode) }
    var isFullscreen by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var moreOpen by remember { mutableStateOf(false) }

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
        subtitleOn = true
        controlsVisible = true
        interactionTick++
    }

    fun touch() {
        controlsVisible = true
        interactionTick++
    }

    fun togglePlay() { player?.let { if (it.isPlaying) it.pause() else it.play() }; touch() }
    fun seekBy(deltaMs: Long) { player?.let { it.seekTo((it.currentPosition + deltaMs).coerceIn(0L, (it.duration.takeIf { d -> d > 0 } ?: durationMs))) }; touch() }
    fun toggleMute() {
        player?.let { p ->
            muted = !muted
            p.volume = if (muted) 0f else defaultVolume
        }
        touch()
    }
    fun toggleSubtitles() {
        val p = player ?: return
        val parameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, subtitleOn)
            .build()
        p.trackSelectionParameters = parameters
        subtitleOn = !subtitleOn
        touch()
    }
    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        val activity = context as? Activity ?: return
        if (isFullscreen) {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        touch()
    }
    fun rotate() {
        val activity = context as? Activity ?: return
        activity.requestedOrientation = if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        touch()
    }

    DisposableEffect(id) {
        com.airplayr.app.core.playback.service.AirPlayrPlaybackService.enterVideoMode()
        onDispose { com.airplayr.app.core.playback.service.AirPlayrPlaybackService.exitVideoMode() }
    }
    LaunchedEffect(id) { vm.load(id) }
    LaunchedEffect(video?.id) { favorite = video?.id?.let(vm::isFavorite) ?: false }

    LaunchedEffect(video?.id, resume) {
        val item = video ?: return@LaunchedEffect
        playerError = null
        container.videoController.connectAsync { ready ->
            if (item.id != video?.id) return@connectAsync
            player = ready
            ready.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playerError = error.errorCodeName.ifBlank { error.message ?: "Video playback failed" }
                    controlsVisible = true
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) vm.completePlayback(item)
                }
            })
            ready.volume = if (muted) 0f else defaultVolume
            ready.setPlaybackSpeed(speed)
            container.videoController.playVideo(ready, item, resume)
            vm.beginPlayback(item, savePlaybackHistory)
        }
    }

    LaunchedEffect(player, video?.id) {
        val item = video ?: return@LaunchedEffect
        while (player != null) {
            val p = player ?: break
            val currentDuration = p.duration.takeIf { it > 0 } ?: item.durationMs
            positionMs = p.currentPosition.coerceAtLeast(0L)
            durationMs = currentDuration.coerceAtLeast(0L)
            bufferedMs = p.bufferedPosition.coerceAtLeast(0L)
            if (repeatA != null && repeatB != null && positionMs >= repeatB!!) {
                p.seekTo(repeatA!!)
            }
            vm.trackPlayback(item, positionMs, durationMs, p.isPlaying)
            if (durationMs > 0L && positionMs.toDouble() / durationMs.toDouble() >= VIDEO_COMPLETION_FRACTION) vm.completePlayback(item)
            delay(3000L)
        }
    }

    LaunchedEffect(player, controlsVisible, locked, interactionTick, autoHideMs) {
        if (!locked && controlsVisible && player?.isPlaying == true && autoHideMs > 0) {
            delay(autoHideMs.toLong())
            controlsVisible = false
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

    DisposableEffect(video?.id) {
        onDispose {
            val item = video
            val p = player
            if (item != null && p != null) vm.finishPlayback(item, p.currentPosition, p.duration.takeIf { it > 0 } ?: item.durationMs, p.isPlaying)
            p?.pause()
            container.videoController.release()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        player?.let { p ->
            AndroidVideoSurface(p, fitMode, Modifier.fillMaxSize())
        }

        // Gesture layer stays underneath the controls, so artwork/video taps remain instantaneous.
        Box(
            Modifier.fillMaxSize().pointerInput(player, locked) {
                detectTapGestures(
                    onTap = { if (!locked) controlsVisible = !controlsVisible else { locked = false; controlsVisible = true }; interactionTick++ },
                    onDoubleTap = { offset ->
                        if (!locked) {
                            val width = size.width
                            if (offset.x < width / 2f) seekBy(-10_000L) else seekBy(10_000L)
                        }
                    },
                )
            },
        )

        VideoControlsOverlay(
            title = video?.title?.ifBlank { video?.filename }.orEmpty(),
            isPlaying = player?.isPlaying == true,
            controlsVisible = controlsVisible && !locked,
            locked = locked,
            muted = muted,
            speed = speed,
            subtitleOn = subtitleOn,
            fitMode = fitMode,
            isFullscreen = isFullscreen,
            positionMs = positionMs,
            durationMs = durationMs,
            bufferedMs = bufferedMs,
            repeatA = repeatA,
            repeatB = repeatB,
            bookmarks = bookmarks,
            favorite = favorite,
            onBack = {
                if (isFullscreen) toggleFullscreen() else (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
            },
            onMore = { moreOpen = true; touch() },
            onPlayPause = ::togglePlay,
            onReplay = { seekBy(-10_000L) },
            onForward = { seekBy(10_000L) },
            onMute = ::toggleMute,
            onSubtitle = {
                if (subtitleOn) toggleSubtitles() else subtitleLauncher.launch(arrayOf("text/vtt", "application/x-subrip", "text/plain"))
            },
            onPiP = { if (Build.VERSION.SDK_INT >= 26 && allowsPiP) runCatching { (context as? Activity)?.enterPictureInPictureMode() }; touch() },
            onRotate = ::rotate,
            onFullscreen = ::toggleFullscreen,
            onLock = { locked = !locked; controlsVisible = !locked; interactionTick++ },
            onFit = { fitMode = VideoFitMode.entries[(fitMode.ordinal + 1) % VideoFitMode.entries.size]; touch() },
            onSpeed = {
                val speeds = listOf(.25f, .5f, .75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f)
                speed = speeds[(speeds.indexOfFirst { it >= speed }.coerceAtLeast(0) + 1) % speeds.size]
                player?.setPlaybackSpeed(speed)
                touch()
            },
            onRepeat = {
                when {
                    repeatA == null -> repeatA = positionMs
                    repeatB == null -> repeatB = durationMs.coerceAtLeast(positionMs)
                    else -> { repeatA = null; repeatB = null }
                }
                touch()
            },
            onBookmark = { vm.bookmark(id, positionMs, formatTime(positionMs)); touch() },
            onFavorite = { video?.let { vm.toggleFavorite(it); favorite = !favorite }; touch() },
            onSeek = { fraction -> player?.seekTo((durationMs * fraction).toLong()); touch() },
            onBookmarkSeek = { player?.seekTo(it); touch() },
            onBookmarkRemove = { vm.removeBookmark(id, it); touch() },
            onUnlock = { locked = false; controlsVisible = true; interactionTick++ },
        )

        if (playerError != null) {
            Surface(
                Modifier.align(Alignment.Center).padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = .72f),
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    Text("Playback error", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    Text(playerError.orEmpty(), color = Color.White.copy(alpha = .78f), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    TextButton(onClick = { player?.let { it.prepare(); it.play(); playerError = null } }, modifier = Modifier.padding(top = 4.dp)) { Text("Retry") }
                }
            }
        }
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
                        AirPlayrMediaMenuAction("Add bookmark", Icons.Default.BookmarkAdd, { vm.bookmark(id, positionMs, formatTime(positionMs)); moreOpen = false }),
                    ),
                    listOf(
                        AirPlayrMediaMenuAction("Speed ${"%.2f".format(Locale.US, speed).trimEnd('0').trimEnd('.') }×", Icons.Default.Speed, {
                            val speeds = listOf(.25f, .5f, .75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f)
                            speed = speeds[(speeds.indexOfFirst { it >= speed }.coerceAtLeast(0) + 1) % speeds.size]
                            player?.setPlaybackSpeed(speed)
                            moreOpen = false
                        }),
                        AirPlayrMediaMenuAction("Subtitles", Icons.Default.ClosedCaption, {
                            if (subtitleOn) toggleSubtitles() else subtitleLauncher.launch(arrayOf("text/vtt", "application/x-subrip", "text/plain"))
                            moreOpen = false
                        }),
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
            }
        },
        update = { view -> view.player = player; view.resizeMode = fitMode.resizeMode },
        modifier = modifier,
    )
}

@Composable
private fun VideoControlsOverlay(
    title: String,
    isPlaying: Boolean,
    controlsVisible: Boolean,
    locked: Boolean,
    muted: Boolean,
    speed: Float,
    subtitleOn: Boolean,
    fitMode: VideoFitMode,
    isFullscreen: Boolean,
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    repeatA: Long?,
    repeatB: Long?,
    bookmarks: List<com.airplayr.app.core.data.db.VideoBookmarkEntity>,
    favorite: Boolean,
    onBack: () -> Unit,
    onMore: () -> Unit,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    onForward: () -> Unit,
    onMute: () -> Unit,
    onSubtitle: () -> Unit,
    onPiP: () -> Unit,
    onRotate: () -> Unit,
    onFullscreen: () -> Unit,
    onLock: () -> Unit,
    onFit: () -> Unit,
    onSpeed: () -> Unit,
    onRepeat: () -> Unit,
    onBookmark: () -> Unit,
    onFavorite: () -> Unit,
    onSeek: (Float) -> Unit,
    onBookmarkSeek: (Long) -> Unit,
    onBookmarkRemove: (Long) -> Unit,
    onUnlock: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val white = Color.White
    val scrim = Color.Black.copy(alpha = .60f)
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    if (locked) {
        Surface(onClick = onUnlock, modifier = Modifier.align(Alignment.CenterEnd).padding(18.dp), shape = CircleShape, color = Color.Black.copy(alpha = .58f)) {
            Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, "Unlock controls", tint = white, modifier = Modifier.size(25.dp)) }
        }
        return
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = controlsVisible,
        modifier = Modifier.fillMaxSize(),
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(120)),
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(115.dp).align(Alignment.TopCenter).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(scrim, Color.Transparent))))
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VideoTopButton(Icons.Default.ArrowBack, "Back", onBack)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(title.ifBlank { "Video" }, color = white, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (isFullscreen) "FULL SCREEN" else "AIRPLAYR VIDEO", color = white.copy(alpha = .68f), fontSize = 10.sp, letterSpacing = 1.1.sp)
                }
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { CastButton(Modifier.size(42.dp)) }
                VideoTopButton(if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, if (muted) "Unmute" else "Mute", onMute)
                VideoTopButton(Icons.Default.ScreenRotation, "Rotate", onRotate)
                VideoTopButton(if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Fullscreen", onFullscreen)
                VideoTopButton(Icons.Default.MoreVert, "More", onMore)
            }

            Row(
                Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VideoRoundControl(Icons.Default.Replay10, "10 seconds back", onReplay, 58.dp)
                VideoRoundControl(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", onPlayPause, 78.dp, primary = true)
                VideoRoundControl(Icons.Default.Forward10, "10 seconds forward", onForward, 58.dp)
            }

            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 94.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniVideoAction(if (repeatA == null) Icons.Default.Repeat else if (repeatB == null) Icons.Default.LooksOne else Icons.Default.RepeatOne, if (repeatA == null) "Set A" else if (repeatB == null) "Set B" else "Clear A-B", onRepeat, active = repeatA != null)
                MiniVideoAction(Icons.Default.ClosedCaption, if (subtitleOn) "Subtitles on" else "Subtitles", onSubtitle, active = subtitleOn)
                MiniVideoAction(Icons.Default.Speed, "Speed ${"%.2f".format(Locale.US, speed).trimEnd('0').trimEnd('.') }×", onSpeed, active = speed != 1f)
                MiniVideoAction(Icons.Default.BookmarkAdd, "Bookmark", onBookmark)
                MiniVideoAction(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (favorite) "Favorited" else "Favorite", onFavorite, active = favorite)
                MiniVideoAction(Icons.Default.PictureInPictureAlt, "Picture in picture", onPiP)
            }

            if (bookmarks.isNotEmpty()) {
                LazyRow(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 18.dp, vertical = 78.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(bookmarks, key = { "${it.mediaId}:${it.positionMs}" }) { bookmark ->
                        AssistChip(
                            onClick = { onBookmarkSeek(bookmark.positionMs) },
                            label = { Text(bookmark.label ?: formatTime(bookmark.positionMs), color = Color.White, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color.Black.copy(alpha = .58f), labelColor = Color.White),
                            trailingIcon = {
                                IconButton(onClick = { onBookmarkRemove(bookmark.positionMs) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Remove bookmark", tint = Color.White, modifier = Modifier.size(14.dp)) }
                            },
                        )
                    }
                }
            }

            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = .72f)).padding(horizontal = 18.dp, vertical = 10.dp).navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(positionMs), color = white, fontSize = 11.sp, fontFamily = AirPlayrFonts.OpenSans, modifier = Modifier.width(54.dp))
                    Slider(value = progress, onValueChange = onSeek, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = white, activeTrackColor = white, inactiveTrackColor = white.copy(alpha = .28f)))
                    Text(formatTime(durationMs), color = white, fontSize = 11.sp, fontFamily = AirPlayrFonts.OpenSans, modifier = Modifier.width(54.dp))
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    VideoBottomButton(Icons.Default.Lock, "Lock controls", onLock)
                    VideoBottomButton(Icons.Default.Replay10, "Replay 10", onReplay)
                    VideoBottomButton(Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", onPlayPause, prominent = true, playing = isPlaying)
                    VideoBottomButton(Icons.Default.Forward10, "Forward 10", onForward)
                    VideoBottomButton(Icons.Default.AspectRatio, fitMode.label, onFit)
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
private fun VideoRoundControl(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, size: Dp, primary: Boolean = false) {
    Surface(onClick = onClick, modifier = Modifier.size(size), shape = CircleShape, color = if (primary) LocalAirPlayrColors.current.primary else Color.Black.copy(alpha = .50f), tonalElevation = 2.dp) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, tint = if (primary) LocalAirPlayrColors.current.onPrimary else Color.White, modifier = Modifier.size(if (primary) 38.dp else 28.dp)) }
    }
}

@Composable
private fun MiniVideoAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, active: Boolean = false) {
    Surface(onClick = onClick, modifier = Modifier.size(48.dp), shape = CircleShape, color = if (active) LocalAirPlayrColors.current.primary.copy(alpha = .92f) else Color.Black.copy(alpha = .36f)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, tint = if (active) LocalAirPlayrColors.current.onPrimary else Color.White, modifier = Modifier.size(23.dp)) }
    }
}

@Composable
private fun VideoBottomButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, prominent: Boolean = false, playing: Boolean = false) {
    Surface(onClick = onClick, modifier = Modifier.size(if (prominent) 60.dp else 48.dp), shape = CircleShape, color = if (prominent) Color.White.copy(alpha = .96f) else Color.Transparent) {
        Box(contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Default.Pause else icon, label, tint = if (prominent) Color.Black else Color.White, modifier = Modifier.size(if (prominent) 28.dp else 24.dp)) }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    return if (hours > 0L) String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds) else String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
