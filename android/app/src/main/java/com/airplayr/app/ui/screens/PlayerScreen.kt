package com.airplayr.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.asExternal
import com.airplayr.app.core.playback.REPEAT_ALL
import com.airplayr.app.core.playback.REPEAT_OFF
import com.airplayr.app.core.playback.REPEAT_ONE
import com.airplayr.app.core.playback.dsp.DspRepository
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.AirPlayrSheetScaffold
import com.airplayr.app.ui.components.Artwork
import com.airplayr.app.ui.components.LyricsEditorSheet
import com.airplayr.app.ui.components.LyricsSheet
import com.airplayr.app.ui.components.SleepTimerSheet
import com.airplayr.app.ui.components.SpeedPitchSheet
import com.airplayr.app.ui.components.TagEditorSheet
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackControllerFactory
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private enum class PlayerSheet { More, Lyrics, Sleep, Speed, Details, Tags, LyricsEdit, Playlist, QueueSaved }

@Composable
fun PlayerScreen(appContext: Context, onBack: () -> Unit, onEqualizer: () -> Unit, onQueue: () -> Unit, onNavigate: (String) -> Unit) {
    val colors = LocalAirPlayrColors.current
    val config = LocalConfiguration.current
    val controller: PlaybackController = viewModel(factory = PlaybackControllerFactory(appContext))
    val state by controller.state.collectAsStateWithLifecycle()
    val app = appContext.applicationContext as? com.airplayr.app.AirPlayrApplication
    val container: AppContainer? = app?.container
    val dspRepo = remember(appContext) { DspRepository(appContext) }
    val dsp by dspRepo.state.collectAsStateWithLifecycle(initialValue = com.airplayr.app.core.playback.dsp.DspState())
    var sheet by remember { mutableStateOf<PlayerSheet?>(null) }
    var favorite by remember(state.currentId) { mutableStateOf(false) }
    var currentItem by remember(state.currentId) { mutableStateOf<MediaItem?>(null) }
    var slider by remember { mutableFloatStateOf(0f) }
    var seeking by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(controller.volume()) }
    val scope = rememberCoroutineScope()
    val preferences = remember(appContext) { PreferencesRepository(appContext) }
    val settings by preferences.settings.collectAsStateWithLifecycle(initialValue = com.airplayr.app.core.data.preferences.AirPlayrSettings(
        theme = "system", highContrast = false, reduceMotion = false, hapticFeedback = true, touchFeedback = true,
        resumeMode = "auto", defaultSpeed = 1.0, crossfadeSeconds = 0.0, gaplessPlayback = true,
        replayGainMode = "off", replayGainPreampDb = 0.0, replayGainPreventClipping = true,
        defaultAspect = "contain", autoHideMs = 4000, keepAwake = true, subtitlesEnabledDefault = true,
        subtitleSize = 1.0, subtitlePosition = "bottom", subtitleBg = true, subtitleColor = "#FFFFFF", subtitleDelayMs = 0,
        rememberSubtitleTrack = true, preferExternalSubtitles = true, rememberAudioTrack = true, defaultVolume = 1.0, muteOnStart = false,
        gestureDoubleTapSeek = true, gestureVolume = true, gestureBrightness = true, gestureLongPress2x = true, gesturePinchZoom = true,
        gestureHorizontalSeek = true, seekStepSeconds = 10, librarySort = "recent", librarySortOrder = "desc", libraryLayout = "grid",
        gridColumns = 0, autoRefreshLibrary = true, showNowPlayingNotification = true, allowsPictureInPicture = true,
        autoEnterPiPOnBackground = false, backgroundPlayback = true, savePlaybackHistory = true, showContinueWatching = true,
        showRecentlyPlayed = true, sleepDeadlineMs = 0L, sleepFinishCurrent = false, sleepFadeOut = true, sleepStartVolume = 1.0,
        sleepPendingTrackId = null
    ))
    var timerNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settings.sleepDeadlineMs) {
        while (settings.sleepDeadlineMs > 0L) {
            timerNow = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val timerRemaining = ((settings.sleepDeadlineMs - timerNow).coerceAtLeast(0L) / 1000L)
    var deletePending by remember { mutableStateOf<MediaItem?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            scope.launch { container?.mediaLibrary?.refreshAll() }
        }
    }

    LaunchedEffect(state.currentId) {
        slider = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
        seeking = false
        if (state.currentId != null && container != null) {
            favorite = runCatching { container.structured.isFavorite(state.currentId!!) }.getOrDefault(false)
            currentItem = runCatching { container.database.mediaItemDao().getById(state.currentId!!)?.asExternal() }.getOrNull()
        } else { favorite = false; currentItem = null }
    }
    LaunchedEffect(state.positionMs, state.durationMs) {
        if (!seeking) slider = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
    }

    fun setFav() {
        scope.launch { currentItem?.let { item -> if (container != null) favorite = container.structured.toggleFavorite(item) } }
    }

    val artSizeDp = min(max(config.screenWidthDp - 56, 240), (config.screenHeightDp * 0.34f).toInt()).coerceAtLeast(240).dp
    val progressDuration = state.durationMs.coerceAtLeast(1L)
    val repeatLabel = when (state.repeatMode) { REPEAT_ONE -> "Repeat one"; REPEAT_ALL -> "Repeat all"; else -> "Repeat off" }

    Box(Modifier.testTag(com.airplayr.app.core.qa.AirPlayrQaTags.PLAYER).fillMaxSize().background(Brush.verticalGradient(listOf(colors.surfaceContainerLowest, colors.background)))) {
        Column(
            Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().height(54.dp).padding(top = 0.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag(com.airplayr.app.core.qa.AirPlayrQaTags.PLAYER_BACK).size(54.dp)) {
                    Icon(AirPlayrIcons.Back, "Back", tint = colors.primary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(onClick = { sheet = PlayerSheet.Lyrics }, modifier = Modifier.testTag(com.airplayr.app.core.qa.AirPlayrQaTags.PLAYER_LYRICS).width(142.dp).height(54.dp).shadow(14.dp, RoundedCornerShape(27.dp)), shape = RoundedCornerShape(27.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHighest)) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(AirPlayrIcons.Lyrics, null, tint = colors.primary, modifier = Modifier.size(26.dp))
                            Text("Lyrics", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    IconButton(onClick = { sheet = PlayerSheet.More }, modifier = Modifier.testTag(com.airplayr.app.core.qa.AirPlayrQaTags.PLAYER_MORE).padding(start = 8.dp)) {
                        Icon(AirPlayrIcons.More, "More playback options", tint = colors.primary, modifier = Modifier.size(28.dp))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Artwork(state.artworkUri, Modifier.size(artSizeDp).shadow(12.dp, RoundedCornerShape(min(34f, artSizeDp.value * 0.055f).dp)), min(34f, artSizeDp.value * 0.055f).dp, state.currentTitle)

            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.currentTitle ?: "Nothing playing", style = MaterialTheme.typography.displayLarge.copy(fontFamily = AirPlayrFonts.SmoochSans), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(state.currentArtist ?: "Unknown Artist", style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 23.sp), color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(time(state.positionMs), style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = colors.textSecondary)
                    Text(time(state.durationMs), style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = colors.textSecondary)
                }
                Slider(
                    modifier = Modifier.testTag(com.airplayr.app.core.qa.AirPlayrQaTags.PLAYER_SEEK),
                    value = slider,
                    onValueChange = { seeking = true; slider = it.coerceIn(0f, 1f) },
                    onValueChangeFinished = { seeking = false; controller.seekTo((slider * progressDuration).toLong().coerceIn(0L, progressDuration)) },
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                )
            }

            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                PlayerControl(AirPlayrIcons.Previous, "Previous", colors.primary) { controller.skipPrevious() }
                PlayerPlayControl(state.isPlaying, colors.onPrimary, colors.primaryContainer, controller::togglePlayPause)
                PlayerControl(AirPlayrIcons.Next, "Next", colors.primary) { controller.skipNext() }
            }

            Row(Modifier.fillMaxWidth(0.82f).padding(top = 55.dp, bottom = 18.dp).offset(y = (-24).dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(AirPlayrIcons.VolumeDown, null, tint = colors.primary, modifier = Modifier.size(25.dp))
                Slider(value = volume, onValueChange = { volume = it; controller.setVolume(it) }, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                Icon(AirPlayrIcons.VolumeUp, null, tint = colors.primary, modifier = Modifier.size(25.dp))
            }

            val mediaDetails = currentItem?.mimeType?.substringAfterLast('/', currentItem?.mimeType ?: "")?.uppercase().orEmpty()
            if (mediaDetails.isNotBlank()) Text(mediaDetails, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))

            Row(
                Modifier.fillMaxWidth(0.84f).heightIn(min = 58.dp).padding(horizontal = 0.dp, vertical = 5.dp).offset(y = 10.dp).background(colors.surfaceContainerHighest, RoundedCornerShape(22.dp)).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerAction(AirPlayrIcons.Shuffle, "Shuffle", state.shuffle, colors) { controller.toggleShuffle() }
                PlayerAction(if (favorite) AirPlayrIcons.Favorite else AirPlayrIcons.FavoriteBorder, if (favorite) "Remove favorite" else "Add favorite", false, colors, onClick = ::setFav)
                PlayerAction(AirPlayrIcons.Sleep, "Sleep timer", false, colors) { sheet = PlayerSheet.Sleep }
                PlayerAction(AirPlayrIcons.Queue, "Queue", false, colors, onClick = onQueue)
                PlayerAction(AirPlayrIcons.Repeat, repeatLabel, state.repeatMode != REPEAT_OFF, colors) { controller.cycleRepeat() }
            }
        }

        deletePending?.let { item ->
            AlertDialog(
                onDismissRequest = { deletePending = null },
                title = { Text("Delete from device?") },
                text = { Text("Delete ${item.title} from this device. Android may ask you to confirm the deletion.") },
                dismissButton = { TextButton(onClick = { deletePending = null }) { Text("Cancel") } },
                confirmButton = {
                    TextButton(onClick = {
                        val storeId = item.mediaStoreId
                        if (storeId == null) {
                            deletePending = null
                        } else {
                            val base = if (item.mediaType == com.airplayr.app.core.data.media.MediaType.AUDIO) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            val uri = android.content.ContentUris.withAppendedId(base, storeId)
                            runCatching {
                                if (Build.VERSION.SDK_INT >= 30) {
                                    val request = MediaStore.createDeleteRequest(appContext.contentResolver, listOf(uri))
                                    deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                } else {
                                    appContext.contentResolver.delete(uri, null, null)
                                    scope.launch { container?.mediaLibrary?.refreshAll() }
                                }
                            }
                            deletePending = null
                        }
                    }) { Text("Delete", color = colors.error) }
                },
            )
        }

        when (sheet) {
            PlayerSheet.Lyrics -> LyricsSheet(appContext, state, { sheet = null }, { controller.seekTo((it * 1000.0).toLong()) }, { sheet = PlayerSheet.LyricsEdit })
            PlayerSheet.Sleep -> SleepTimerSheet(
                currentMinutes = if (settings.sleepDeadlineMs > 0L) kotlin.math.max(5, kotlin.math.ceil(timerRemaining / 60.0).toInt()) else 30,
                remainingSeconds = timerRemaining,
                finishCurrent = settings.sleepFinishCurrent,
                fadeOut = settings.sleepFadeOut,
                onDismiss = { sheet = null },
                onApply = { minutes, finishCurrent, fadeOut -> controller.startSleepTimer(minutes, finishCurrent, fadeOut) },
                onCancelTimer = { controller.cancelSleepTimer() },
            )
            PlayerSheet.Speed -> SpeedPitchSheet(dsp, controller) { sheet = null }
            PlayerSheet.Details -> DetailsSheetContent(currentItem, { sheet = null }, { sheet = PlayerSheet.Tags })
            PlayerSheet.More -> MoreSheet(
                item = currentItem,
                controller = controller,
                onClose = { sheet = null },
                onEqualizer = { sheet = null; onEqualizer() },
                onSpeed = { sheet = PlayerSheet.Speed },
                onLyrics = { sheet = PlayerSheet.Lyrics },
                onLyricsEdit = { sheet = PlayerSheet.LyricsEdit },
                onSleep = { sheet = PlayerSheet.Sleep },
                onTags = { sheet = PlayerSheet.Tags },
                onDetails = { sheet = PlayerSheet.Details },
                onSaveQueue = { scope.launch { controller.saveCurrentQueue(); sheet = PlayerSheet.QueueSaved } },
                onPlaylist = { sheet = PlayerSheet.Playlist },
                onShare = {
                    val item = currentItem
                    if (item != null) {
                        runCatching {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = item.mimeType ?: "audio/*"
                                putExtra(Intent.EXTRA_TEXT, "${item.title}${item.artist?.let { " · $it" }.orEmpty()}\n\nShared from AirPlayr")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            appContext.startActivity(Intent.createChooser(intent, "Share song").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                    sheet = null
                },
                onDelete = { deletePending = currentItem; sheet = null },
                onNavigate = { route -> sheet = null; onNavigate(route) },
            )
            PlayerSheet.QueueSaved -> AlertDialog(onDismissRequest = { sheet = null }, title = { Text("Queue saved") }, text = { Text("The current playing queue is already persisted on this device and will be restored with the playback session.") }, confirmButton = { TextButton(onClick = { sheet = null }) { Text("Close") } })
            PlayerSheet.Playlist -> currentItem?.let { item -> PlayerPlaylistSheet(container, item) { sheet = null } }
            PlayerSheet.Tags -> currentItem?.let { TagEditorSheet(appContext, it, com.airplayr.app.core.metadata.MetadataRepository(appContext), { sheet = null }) { sheet = null } }
            PlayerSheet.LyricsEdit -> currentItem?.let { LyricsEditorSheet(appContext, it, { sheet = null }) }
            null -> Unit
        }
    }
}

@Composable
private fun PlayerControl(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(58.dp).testTag(when (label) { "Previous" -> com.airplayr.app.core.qa.AirPlayrQaTags.PLAYER_PREVIOUS; "Next" -> com.airplayr.app.core.qa.AirPlayrQaTags.PLAYER_NEXT; else -> "airplayr.player.control.$label" })) { Icon(icon, label, tint = tint, modifier = Modifier.size(34.dp)) }
}

@Composable
private fun PlayerPlayControl(playing: Boolean, iconTint: androidx.compose.ui.graphics.Color, background: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(72.dp).testTag(com.airplayr.app.core.qa.AirPlayrQaTags.PLAYER_PLAY)) {
        Box(Modifier.size(72.dp).shadow(16.dp, CircleShape).background(background, CircleShape), contentAlignment = Alignment.Center) {
            Icon(if (playing) AirPlayrIcons.Pause else AirPlayrIcons.Play, if (playing) "Pause" else "Play", tint = iconTint, modifier = Modifier.size(38.dp))
        }
    }
}

@Composable
private fun PlayerAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, colors: com.airplayr.app.ui.theme.AirPlayrColors, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
        Box(Modifier.size(52.dp).background(if (selected) colors.primaryContainer else androidx.compose.ui.graphics.Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = colors.primary, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun MoreSheet(
    item: MediaItem?,
    controller: PlaybackController,
    onClose: () -> Unit,
    onEqualizer: () -> Unit,
    onSpeed: () -> Unit,
    onLyrics: () -> Unit,
    onLyricsEdit: () -> Unit,
    onSleep: () -> Unit,
    onTags: () -> Unit,
    onDetails: () -> Unit,
    onSaveQueue: () -> Unit,
    onPlaylist: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    AirPlayrSheetScaffold("Playback options", "NOW PLAYING", onClose) {
        MoreGroup(listOf(
            "Equalizer" to onEqualizer,
            "Speed & Pitch" to onSpeed,
            "Save playing queue" to onSaveQueue,
            "Play next" to { item?.let(controller::playNext); onClose() },
            "Play last" to { item?.let(controller::playLast); onClose() },
            "Clear playing queue" to { controller.clearQueue(); onClose() },
        ), colors)
        MoreGroup(listOf(
            "Go to album" to { item?.album?.takeIf { it.isNotBlank() }?.let { onNavigate("album/${java.net.URLEncoder.encode(it, "UTF-8")}") } ?: onClose() },
            "Go to artist" to { item?.artist?.takeIf { it.isNotBlank() }?.let { onNavigate("artist/${java.net.URLEncoder.encode(it, "UTF-8")}") } ?: onClose() },
            "Go to album artist" to { item?.albumArtist?.takeIf { it.isNotBlank() }?.let { onNavigate("artist/${java.net.URLEncoder.encode(it, "UTF-8")}") } ?: onClose() },
            "Go to folder" to { val id = item?.folderId ?: item?.folder ?: item?.relativePath; id?.takeIf { it.isNotBlank() }?.let { onNavigate("music-folder/${java.net.URLEncoder.encode(it, "UTF-8")}") } ?: onClose() },
        ), colors)
        MoreGroup(listOf(
            "Add to playlist" to onPlaylist,
            "Tag editor" to onTags,
            "Edit lyrics" to onLyricsEdit,
        ), colors)
        MoreGroup(listOf(
            "Details" to onDetails,
            "Share" to onShare,
            "Delete from device" to onDelete,
            "Sleep timer" to onSleep,
            "Lyrics" to onLyrics,
        ), colors)
    }
}

@Composable
private fun MoreActionIcon(label: String): androidx.compose.ui.graphics.vector.ImageVector = when (label) {
    "Equalizer" -> AirPlayrIcons.Equalizer
    "Speed & Pitch" -> Icons.Default.Speed
    "Save playing queue" -> Icons.Default.Save
    "Play next", "Play last" -> Icons.Default.SkipNext
    "Clear playing queue" -> Icons.Default.PlaylistRemove
    "Go to album" -> Icons.Default.Album
    "Go to artist", "Go to album artist" -> Icons.Default.Person
    "Go to folder" -> Icons.Default.Folder
    "Add to playlist" -> Icons.Default.List
    "Tag editor", "Edit lyrics" -> Icons.Default.Edit
    "Details" -> Icons.Default.Info
    "Share" -> Icons.Default.Share
    "Delete from device" -> Icons.Default.Delete
    "Sleep timer" -> AirPlayrIcons.Sleep
    "Lyrics" -> AirPlayrIcons.Lyrics
    else -> AirPlayrIcons.More
}

@Composable
private fun MoreGroup(items: List<Pair<String, () -> Unit>>, colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
        Column {
            items.forEachIndexed { index, (label, action) ->
                Row(Modifier.fillMaxWidth().height(72.dp).clickable(onClick = action).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { Icon(MoreActionIcon(label), null, tint = colors.primary, modifier = Modifier.size(22.dp)) }
                    Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                    Text("›", style = MaterialTheme.typography.titleLarge, color = colors.primary)
                }
                if (index != items.lastIndex) Spacer(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant.copy(alpha = .4f)))
            }
        }
    }
}


@Composable
private fun PlayerPlaylistSheet(container: AppContainer?, item: MediaItem, onClose: () -> Unit) {
    if (container == null) return
    val colors = LocalAirPlayrColors.current
    val playlists by container.structured.playlists().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val preferences = remember(appContext) { PreferencesRepository(appContext) }
    val settings by preferences.settings.collectAsStateWithLifecycle(initialValue = com.airplayr.app.core.data.preferences.AirPlayrSettings(
        theme = "system", highContrast = false, reduceMotion = false, hapticFeedback = true, touchFeedback = true,
        resumeMode = "auto", defaultSpeed = 1.0, crossfadeSeconds = 0.0, gaplessPlayback = true,
        replayGainMode = "off", replayGainPreampDb = 0.0, replayGainPreventClipping = true,
        defaultAspect = "contain", autoHideMs = 4000, keepAwake = true, subtitlesEnabledDefault = true,
        subtitleSize = 1.0, subtitlePosition = "bottom", subtitleBg = true, subtitleColor = "#FFFFFF", subtitleDelayMs = 0,
        rememberSubtitleTrack = true, preferExternalSubtitles = true, rememberAudioTrack = true, defaultVolume = 1.0, muteOnStart = false,
        gestureDoubleTapSeek = true, gestureVolume = true, gestureBrightness = true, gestureLongPress2x = true, gesturePinchZoom = true,
        gestureHorizontalSeek = true, seekStepSeconds = 10, librarySort = "recent", librarySortOrder = "desc", libraryLayout = "grid",
        gridColumns = 0, autoRefreshLibrary = true, showNowPlayingNotification = true, allowsPictureInPicture = true,
        autoEnterPiPOnBackground = false, backgroundPlayback = true, savePlaybackHistory = true, showContinueWatching = true,
        showRecentlyPlayed = true, sleepDeadlineMs = 0L, sleepFinishCurrent = false, sleepFadeOut = true, sleepStartVolume = 1.0,
        sleepPendingTrackId = null
    ))
    var timerNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settings.sleepDeadlineMs) {
        while (settings.sleepDeadlineMs > 0L) {
            timerNow = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val timerRemaining = ((settings.sleepDeadlineMs - timerNow).coerceAtLeast(0L) / 1000L)
    AirPlayrSheetScaffold("Add to playlist", "PLAYLISTS", onClose) {
        if (playlists.isEmpty()) {
            Text("No playlists yet", style = MaterialTheme.typography.titleMedium)
            Text("Create a playlist from the Music tab first.", color = colors.textSecondary)
        } else {
            playlists.forEach { playlist ->
                Row(Modifier.fillMaxWidth().height(64.dp).clickable { scope.launch { container.structured.addToPlaylist(playlist.id, item.id); onClose() } }, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(colors.primaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(AirPlayrIcons.Queue, null, tint = colors.primary) }
                    Text(playlist.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    Icon(AirPlayrIcons.Next, null, tint = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun DetailsSheetContent(item: MediaItem?, onClose: () -> Unit, onTags: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    AirPlayrSheetScaffold("Track details", "MUSIC", onClose) {
        if (item == null) {
            Text("No track is currently loaded.")
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Artwork(item.artworkUri, Modifier.size(74.dp), 18.dp, item.title)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                    Text(item.artist.orEmpty().ifBlank { "Unknown artist" }, color = colors.textSecondary)
                }
            }
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLine("Album", item.album)
                    DetailLine("Genre", item.genre)
                    DetailLine("Duration", time(item.durationMs))
                    DetailLine("Size", formatBytes(item.sizeBytes))
                    DetailLine("Format", item.mimeType)
                    DetailLine("Location", item.path ?: item.relativePath)
                }
            }
            Button(onClick = onTags, modifier = Modifier.fillMaxWidth()) { Text("Edit metadata") }
        }
    }
}

@Composable private fun DetailLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) Row(Modifier.fillMaxWidth()) { Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(84.dp)); Text(value, style = MaterialTheme.typography.bodyMedium) }
}

private fun time(ms: Long): String { val s = ms.coerceAtLeast(0) / 1000; return "%02d:%02d".format(s / 60, s % 60) }
private fun formatBytes(n: Long): String { if (n < 1024) return "$n B"; val kb = n / 1024.0; if (kb < 1024) return "%.1f KB".format(kb); val mb = kb / 1024.0; if (mb < 1024) return "%.1f MB".format(mb); return "%.1f GB".format(mb / 1024.0) }
