package com.airplayr.app.ui.screens

import androidx.activity.ComponentActivity
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airplayr.app.core.data.preferences.AirPlayrSettings
import androidx.media3.common.MimeTypes
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.video.VideoPlayerViewModel
import com.airplayr.app.ui.video.VideoPlayerViewModelFactory

@OptIn(UnstableApi::class)
@Composable fun VideoScreen(container: AppContainer, id: String) {
    val vm: VideoPlayerViewModel = viewModel(factory = VideoPlayerViewModelFactory(container.mediaLibrary, container.structured))
    val settingsState by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings = settingsState ?: return
    val video by vm.video.collectAsStateWithLifecycle(); val resume by vm.resume.collectAsStateWithLifecycle(); val context=LocalContext.current
    val bookmarks by remember(video?.id) { video?.id?.let(container.structured::bookmarks) ?: kotlinx.coroutines.flow.flowOf(emptyList()) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var controller by remember { mutableStateOf<Player?>(null) }; var playerError by remember { mutableStateOf<String?>(null) }; var speed by remember { mutableFloatStateOf(1f) }; var repeatA by remember { mutableStateOf<Long?>(null) }; var repeatB by remember { mutableStateOf<Long?>(null) }; var favorite by remember { mutableStateOf(false) }
    val subtitleLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri:Uri? -> if(uri!=null) { context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); controller?.let{c-> val base=c.currentMediaItem ?: return@let; val mime=when(context.contentResolver.getType(uri)){ "text/vtt"->MimeTypes.TEXT_VTT; "application/x-subrip","text/plain"->MimeTypes.APPLICATION_SUBRIP; else->MimeTypes.TEXT_VTT }; val sub=MediaItem.SubtitleConfiguration.Builder(uri).setMimeType(mime).setLanguage("und").setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build(); c.setMediaItem(base.buildUpon().setSubtitleConfigurations(listOf(sub)).build(),c.currentPosition); c.prepare(); c.play() } } }
    DisposableEffect(id) {
        com.airplayr.app.core.playback.service.AirPlayrPlaybackService.enterVideoMode()
        onDispose {
            com.airplayr.app.core.playback.service.AirPlayrPlaybackService.exitVideoMode()
        }
    }
    LaunchedEffect(id){vm.load(id)}
    LaunchedEffect(video?.id){ favorite = video?.id?.let { vm.isFavorite(it) } ?: false }
    LaunchedEffect(video?.id, resume){
        val item=video ?: return@LaunchedEffect
        playerError = null
        container.videoController.connectAsync { c->
            if(item.id==video?.id){
                controller=c
                c.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) { playerError = error.errorCodeName.ifBlank { error.message ?: "Video playback failed" } }
                })
                container.videoController.playVideo(c,item,resume)
            }
        }
    }
    LaunchedEffect(controller, repeatA, repeatB){ while(controller!=null){ val c=controller!!; if(repeatA!=null && repeatB!=null && c.currentPosition>=repeatB!!) c.seekTo(repeatA!!); vm.saveProgress(id,c.currentPosition,c.duration.takeIf{it>0}?:video?.durationMs?:0); kotlinx.coroutines.delay(3000) } }
    if(Build.VERSION.SDK_INT>=26){
        SideEffect{
            val w=(video?.width?:16).coerceAtLeast(1); val h=(video?.height?:9).coerceAtLeast(1)
            runCatching { (context as? Activity)?.setPictureInPictureParams(PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(w,h)).build()) }
        }
        DisposableEffect(settings.autoEnterPiPOnBackground, settings.allowsPictureInPicture, controller, video?.id) {
            val activity = context as? ComponentActivity
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && settings.autoEnterPiPOnBackground && settings.allowsPictureInPicture && controller?.isPlaying == true) {
                    runCatching { activity?.enterPictureInPictureMode() }
                }
            }
            activity?.lifecycle?.addObserver(observer)
            onDispose { activity?.lifecycle?.removeObserver(observer) }
        }
    }
    DisposableEffect(Unit){
        onDispose {
            controller?.pause()
            container.videoController.release()
        }
    }
    Column(Modifier.fillMaxSize()){
        controller?.let{ player ->
            if (playerError != null) {
                Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=androidx.compose.ui.Alignment.Center){
                    Text("Unable to play this video\n${playerError}", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                AndroidPlayerView(player,Modifier.fillMaxWidth().weight(1f))
            }
        } ?: Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=androidx.compose.ui.Alignment.Center){Text("Loading video…")}
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),horizontalArrangement=Arrangement.SpaceEvenly){
            IconButton({controller?.let{vm.bookmark(id,it.currentPosition,null)}}){Icon(Icons.Default.BookmarkAdd,"Add bookmark")}
            IconButton({repeatA=controller?.currentPosition}){Text("A")}; IconButton({repeatB=controller?.currentPosition}){Text("B")}; IconButton({repeatA=null;repeatB=null}){Icon(Icons.Default.Close,"Clear A-B")}
            IconButton({
                val speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 3.5f, 4f)
                val next = speeds[(speeds.indexOfFirst { it >= speed }.coerceAtLeast(0) + 1) % speeds.size]
                speed = next
                controller?.setPlaybackSpeed(speed)
            }){Icon(Icons.Default.Speed,"Speed ${speed}x")}
            IconButton({subtitleLauncher.launch(arrayOf("text/vtt","application/x-subrip","text/plain"))}){Text("CC")}
            IconButton({ if (Build.VERSION.SDK_INT >= 26 && settings.allowsPictureInPicture) runCatching { (context as? Activity)?.enterPictureInPictureMode() } }){Text("PiP")}
            IconButton({video?.let{vm.toggleFavorite(it);favorite=!favorite}}){Icon(Icons.Default.Favorite,if(favorite)"Unfavorite" else "Favorite")}
        }
        if (bookmarks.isNotEmpty()) {
            LazyRow(Modifier.fillMaxWidth().padding(horizontal=12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bookmarks, key = { "${it.mediaId}:${it.positionMs}" }) { mark ->
                    AssistChip(
                        onClick = { controller?.seekTo(mark.positionMs) },
                        label = { Text(mark.label ?: formatTime(mark.positionMs)) },
                        trailingIcon = { IconButton({ vm.removeBookmark(id, mark.positionMs) }) { Icon(Icons.Default.Close, "Remove bookmark") } },
                    )
                }
            }
        }
    }
}
@OptIn(UnstableApi::class)
@Composable private fun AndroidPlayerView(player:Player,modifier:Modifier){AndroidView(factory={ctx->PlayerView(ctx).apply{layoutParams=ViewGroup.LayoutParams(-1,-1);useController=true;setKeepContentOnPlayerReset(true);this.player=player}},update={it.player=player},modifier=modifier)}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val h = total / 3600L
    val m = (total % 3600L) / 60L
    val sec = total % 60L
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}
