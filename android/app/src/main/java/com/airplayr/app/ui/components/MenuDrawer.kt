package com.airplayr.app.ui.components

import com.airplayr.app.ui.theme.AirPlayrIcons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackControllerFactory
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
    container: AppContainer,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    onScanMedia: () -> Unit,
    onAbout: () -> Unit,
    onSupport: () -> Unit,
) {
    if (!visible) return

    val colors = LocalAirPlayrColors.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val drawerWidth = (configuration.screenWidthDp * 0.87f).coerceAtMost(430f).dp
    val drawerWidthPx = with(density) { drawerWidth.toPx() }
    val playback: PlaybackController = viewModel(factory = PlaybackControllerFactory(container.appContext))
    val playbackState by playback.state.collectAsStateWithLifecycle()
    val recentHistory by container.structured.history(1).collectAsStateWithLifecycle(initialValue = emptyList())
    val latest = recentHistory.firstOrNull()

    val scope = rememberCoroutineScope()
    var showAbout by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    var dragging by remember { mutableFloatStateOf(0f) }
    var targetFraction by remember { mutableFloatStateOf(-1f) }
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(if (targetFraction < 0f) 180 else 300),
        label = "drawer",
    )

    // Opening animation starts fully off-screen and settles into the same 87%-width surface used by the reference.
    androidx.compose.runtime.LaunchedEffect(visible) {
        targetFraction = if (visible) 0f else -1f
        if (!visible) dragFraction = -1f
    }

    val displayedFraction = if (dragging > 0.5f) dragFraction else animatedFraction
    val dragState = rememberDraggableState { delta ->
        if (drawerWidthPx <= 0f) return@rememberDraggableState
        dragFraction = (dragFraction + delta / drawerWidthPx).coerceIn(-1f, 0f)
    }

    val entriesLibrary = listOf(
        MenuEntry("Library", Icons.Outlined.LibraryMusic, "home", "${musicCount + videoCount}"),
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
        MenuEntry("Support AirPlayr", Icons.Outlined.FavoriteBorder, "__support"),
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
                    onDragStarted = { dragging = 1f; dragFraction = animatedFraction },
                    onDragStopped = { velocity ->
                        val close = dragFraction < -0.30f || velocity < -900f
                        dragging = 0f
                        if (close) {
                            targetFraction = -1f
                            scope.launch { kotlinx.coroutines.delay(180); onClose() }
                        } else {
                            targetFraction = 0f
                        }
                    },
                ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                topEnd = AirPlayrRadii.xl,
                bottomEnd = AirPlayrRadii.xl,
                topStart = 0.dp,
                bottomStart = 0.dp,
            ),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer.copy(alpha = 0.94f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                val isPlaying = playbackState.isPlaying && playbackState.currentId != null
                val title = playbackState.currentTitle ?: latest?.title ?: "Nothing playing yet"
                val artist = playbackState.currentArtist ?: latest?.artist ?: "Start playing your library"
                val artwork = playbackState.artworkUri ?: latest?.artworkUri
                Box(Modifier.fillMaxWidth().height(184.dp)) {
                    ArtworkBackdrop(artwork, Modifier.fillMaxSize(), alpha = 0.42f, scale = 1.08f)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, colors.surfaceContainer.copy(alpha = 0.62f)))))
                    Row(
                        Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                            Artwork(artwork, Modifier.fillMaxSize(), 14.dp, title)
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
                    Modifier.fillMaxWidth().weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    section(entriesLibrary, "YOUR LIBRARY", colors, onClose, onNavigate, onScanMedia, { showAbout = true }, { showSupport = true })
                    section(entriesPlayback, "PLAYBACK", colors, onClose, onNavigate, onScanMedia, { showAbout = true }, { showSupport = true })
                    section(entriesInsights, "INSIGHTS & TOOLS", colors, onClose, onNavigate, onScanMedia, { showAbout = true }, { showSupport = true })
                    section(entriesAirPlayr, "AIR PLAYR", colors, onClose, onNavigate, onScanMedia, { showAbout = true }, { showSupport = true })
                    item { MaterialInfoCard("Made for your media", "Music first. Video when you want it.", colors) }
                }
            }
        }
        if (showAbout) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text("About AirPlayr") },
                text = { Text("AirPlayr is a music-first local media player. Your library, playback state and preferences stay on this device.") },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { showAbout = false }) { Text("Close") } },
            )
        }
        if (showSupport) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSupport = false },
                title = { Text("Support AirPlayr") },
                text = { Text("Thanks for supporting AirPlayr. Your feedback helps improve the player and its accessibility, stability and media features.") },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { showSupport = false }) { Text("Close") } },
            )
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
        ) { Icon(item.icon, item.label, tint = if (item.active) colors.primary else colors.textPrimary, modifier = Modifier.size(23.dp)) }
        Text(item.label, style = MaterialTheme.typography.titleMedium.copy(fontFamily = AirPlayrFonts.SmoochSans), color = colors.textPrimary, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        item.value?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = colors.textPrimary) }
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
