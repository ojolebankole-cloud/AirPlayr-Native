package com.airplayr.app.ui.components

import com.airplayr.app.ui.theme.AirPlayrIcons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.playback.VideoQueueStore
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.theme.AirPlayrRadii
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.launch

private data class MenuEntry(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String,
    val value: String? = null,
    val active: Boolean = false,
)

@Composable
fun MenuDrawer(
    visible: Boolean,
    musicCount: Int,
    videoCount: Int,
    folderCount: Int = 0,
    playback: PlaybackController,
    latestHistory: com.airplayr.app.core.data.db.PlaybackHistoryEntity? = null,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    onScanMedia: () -> Unit,
    onAbout: () -> Unit,
    onSupport: () -> Unit,
    onQueueToggle: (() -> Unit)? = null,
    onVideoQueue: (() -> Unit)? = null,
) {
    if (!visible) return

    val colors = LocalAirPlayrColors.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val drawerWidth = (configuration.screenWidthDp * 0.87f).coerceAtMost(430f).dp
    val drawerWidthPx = with(density) { drawerWidth.toPx() }
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    val musicQueue by playback.queueItems.collectAsStateWithLifecycle()
    val videoQueue by VideoQueueStore.queue.collectAsStateWithLifecycle()
    val latest = latestHistory

    var playingQueueExpanded by rememberSaveable { mutableStateOf(false) }
    var musicQueueSheetOpen by remember { mutableStateOf(false) }
    // The drawer is intentionally immediate: no open/close settle animation is used.
    // Dragging is still supported, but releasing a close gesture dismisses immediately.
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableFloatStateOf(0f) }

    val displayedFraction = dragFraction
    val dragState = rememberDraggableState { delta ->
        if (drawerWidthPx <= 0f) return@rememberDraggableState
        dragFraction = (dragFraction + delta / drawerWidthPx).coerceIn(-1f, 0f)
    }

    val entriesLibrary = listOf(
        MenuEntry("Library", AirPlayrIcons.Library, "home", "${musicCount + videoCount}"),
        MenuEntry("Folders", Icons.Outlined.FolderOpen, "folders", "$folderCount folders"),
        MenuEntry("Music", AirPlayrIcons.Music, "music", "$musicCount songs"),
        MenuEntry("Videos", AirPlayrIcons.Video, "videos", "$videoCount videos"),
    )
    val entriesPlayback = listOf(
        MenuEntry("Playing queue", Icons.Outlined.QueueMusic, "queue"),
        MenuEntry("Equalizer", Icons.Outlined.Equalizer, "equalizer"),
    )
    val entriesInsights = listOf(
        MenuEntry("Listening & watching", Icons.Outlined.ShowChart, "listening-watching-stats"),
        MenuEntry("Scan media", Icons.Outlined.History, "__scan"),
    )
    val entriesAirPlayr = listOf(
        MenuEntry("Settings", Icons.Outlined.Settings, "settings"),
        MenuEntry("About AirPlayr", Icons.Outlined.Info, "__about"),
        MenuEntry("Support AirPlayr", Icons.Default.Favorite, "__support"),
    )

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .background(colors.scrim.copy(alpha = 0.52f))
                .clickable(onClick = onClose),
        )

        Card(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .navigationBarsPadding()
                .graphicsLayer { translationX = displayedFraction * drawerWidthPx }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = { dragging = 1f },
                    onDragStopped = { velocity ->
                        val close = dragFraction < -0.30f || velocity < -900f
                        dragging = 0f
                        if (close) {
                            dragFraction = -1f
                            onClose()
                        } else {
                            dragFraction = 0f
                        }
                    },
                ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                topEnd = AirPlayrRadii.xl,
                bottomEnd = AirPlayrRadii.xl,
                topStart = 0.dp,
                bottomStart = 0.dp,
            ),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                val isPlaying = playbackState.isPlaying && playbackState.currentId != null
                val title = playbackState.currentTitle ?: latest?.title ?: "Nothing playing yet"
                val artist = playbackState.currentArtist ?: latest?.artist ?: "Start playing your library"
                val artwork = playbackState.artworkUri ?: latest?.artworkUri
                Box(Modifier.fillMaxWidth().height(184.dp)) {
                    ArtworkBackdrop(
                        artwork,
                        Modifier.fillMaxSize(),
                        alpha = 1f,
                        scale = 1.08f,
                        targetSizePx = 320,
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.52f to Color.Transparent,
                                    1f to colors.surfaceContainer,
                                )
                            )
                    )
                    Row(
                        Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                            Artwork(artwork, Modifier.fillMaxSize(), 14.dp, title, MediaType.AUDIO, AirPlayrIcons.Music, isPlaying = isPlaying)
                        }
                        Column(Modifier.weight(1f).padding(start = 13.dp)) {
                            Text(if (isPlaying) "CURRENTLY PLAYING" else "LAST PLAYED", style = MaterialTheme.typography.labelSmall, color = colors.textPrimary)
                            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontFamily = AirPlayrFonts.SmoochSans), maxLines = 1)
                            Text(artist, style = MaterialTheme.typography.bodySmall.copy(fontFamily = AirPlayrFonts.SmoochSans), maxLines = 1, color = colors.textPrimary)
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).size(1.dp).background(colors.outlineVariant.copy(alpha = .65f)))

                LazyColumn(
                    state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    section(entriesLibrary, "YOUR LIBRARY", colors, onClose, onNavigate, onScanMedia, onAbout, onSupport)
                    item {
                        Text("PLAYBACK", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
                    }
                    item {
                        DrawerRow(entriesPlayback[0], colors) {
                            playingQueueExpanded = !playingQueueExpanded
                        }
                    }
                    item {
                        AnimatedVisibility(
                            visible = playingQueueExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            QueueDrawerCards(
                                musicCount = musicQueue.size,
                                videoCount = videoQueue.size,
                                onMusic = { musicQueueSheetOpen = true },
                                onVideo = { onVideoQueue?.invoke(); onClose() },
                            )
                        }
                    }
                    item {
                        DrawerRow(entriesPlayback[1], colors) {
                            onClose()
                            onNavigate(entriesPlayback[1].route)
                        }
                    }
                    section(entriesInsights, "INSIGHTS & TOOLS", colors, onClose, onNavigate, onScanMedia, onAbout, onSupport)
                    section(entriesAirPlayr, "AIR PLAYR", colors, onClose, onNavigate, onScanMedia, onAbout, onSupport)
                    item { MaterialInfoCard("Made for your media", "Music first. Video when you want it.", colors) }
                }
            }
        }
        if (musicQueueSheetOpen) {
            MusicQueueSheet(playback, onDismiss = { musicQueueSheetOpen = false })
        }
    }
}

private fun LazyListScope.section(
    entries: List<MenuEntry>,
    title: String,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    onScanMedia: () -> Unit,
    onAbout: () -> Unit,
    onSupport: () -> Unit,
    onQueueToggle: (() -> Unit)? = null,
) {
    item {
        Text(title, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
    }
    items(entries) { item ->
        DrawerRow(item, colors) {
            when (item.route) {
                "__scan" -> { onClose(); onScanMedia() }
                "__about" -> { onClose(); onAbout() }
                "__support" -> { onClose(); onSupport() }
                "queue" -> { onQueueToggle?.invoke() }
                else -> { onClose(); onNavigate(item.route) }
            }
        }
    }
}

@Composable
private fun DrawerRow(item: MenuEntry, colors: com.airplayr.app.ui.theme.AirPlayrColors, onClick: () -> Unit) {
    val rowShape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth()
            .background(if (item.active) colors.primaryContainer else Color.Transparent, rowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).background(if (item.active) colors.primary.copy(alpha = .12f) else colors.surfaceContainerHigh, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(item.icon, item.label, tint = when { item.route == "__support" -> Color.Red; item.active -> colors.primary; else -> colors.textPrimary }, modifier = Modifier.size(23.dp)) }
        Text(item.label, style = MaterialTheme.typography.titleMedium.copy(fontFamily = AirPlayrFonts.SmoochSans), color = colors.textPrimary, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        item.value?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = colors.textPrimary) }
    }
}

@Composable
private fun QueueDrawerCards(musicCount: Int, videoCount: Int, onMusic: () -> Unit, onVideo: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(Modifier.fillMaxWidth().clickable(onClick = onMusic), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(colors.primaryContainer, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = colors.primary) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) { Text("Music queue", fontFamily = AirPlayrFonts.SmoochSans, style = MaterialTheme.typography.titleMedium); Text("$musicCount ${if (musicCount == 1) "song" else "songs"}", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall) }
                Icon(Icons.Default.PlayArrow, null, tint = colors.primary)
            }
        }
        Card(Modifier.fillMaxWidth().clickable(onClick = onVideo), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(colors.secondaryContainer, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.VideoLibrary, null, tint = colors.secondary) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) { Text("Video queue", fontFamily = AirPlayrFonts.SmoochSans, style = MaterialTheme.typography.titleMedium); Text("$videoCount ${if (videoCount == 1) "video" else "videos"}", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall) }
                Icon(Icons.Default.PlayArrow, null, tint = colors.secondary)
            }
        }
    }
}

@Composable
private fun MaterialInfoCard(title: String, subtitle: String, colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp)
            .background(colors.primaryContainer, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).background(colors.surfaceContainerHigh, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
            Icon(AirPlayrIcons.Music, null, tint = colors.primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}
