package com.airplayr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicQueueSheet(
    playback: PlaybackController,
    onDismiss: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val state by playback.state.collectAsStateWithLifecycle()
    val queue by playback.queueItems.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 9.dp, bottom = 5.dp)
                    .width(62.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.textSecondary.copy(alpha = 0.62f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .navigationBarsPadding(),
        ) {
            QueueMiniPlayer(
                state = state,
                playback = playback,
                onOpenPlayer = onDismiss,
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                    .background(colors.surfaceContainerLow),
            ) {
                Text(
                    "Up next",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 18.dp),
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )

                if (queue.isEmpty()) {
                    QueueEmptyState(colors)
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(queue, state.currentQueueIndex) {
                        val target = (state.currentQueueIndex + 1).coerceAtMost(queue.lastIndex)
                        if (target >= 0) listState.scrollToItem(target)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(
                            items = queue,
                            key = { index, item -> "${item.id}:$index" },
                        ) { index, item ->
                            QueueItem(
                                index = index,
                                item = item,
                                isCurrent = index == state.currentQueueIndex,
                                colors = colors,
                                onPlay = { playback.playQueueIndex(index) },
                                onPlayNext = { playback.playNext(item) },
                                onRemove = { playback.removeFromQueue(index) },
                                onMove = { from, to -> playback.moveQueueItem(from, to) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueMiniPlayer(
    state: com.airplayr.app.ui.playback.PlaybackUiState,
    playback: PlaybackController,
    onOpenPlayer: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            state.artworkUri,
            Modifier.size(58.dp),
            18.dp,
            state.currentTitle,
            MediaType.AUDIO,
            AirPlayrIcons.Music,
            isPlaying = state.isPlaying,
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenPlayer),
        ) {
            Text(
                state.currentTitle ?: "Nothing playing",
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                state.currentArtist ?: "Unknown artist",
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = playback::skipPrevious, modifier = Modifier.size(44.dp)) {
            Icon(AirPlayrIcons.Previous, "Previous", tint = colors.textPrimary, modifier = Modifier.size(24.dp))
        }
        IconButton(onClick = playback::togglePlayPause, modifier = Modifier.size(44.dp)) {
            Icon(
                if (state.isPlaying) AirPlayrIcons.Pause else AirPlayrIcons.Play,
                if (state.isPlaying) "Pause" else "Play",
                tint = colors.textPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(onClick = playback::skipNext, modifier = Modifier.size(44.dp)) {
            Icon(AirPlayrIcons.Next, "Next", tint = colors.textPrimary, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun QueueItem(
    index: Int,
    item: MediaItem,
    isCurrent: Boolean,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val density = LocalDensity.current
    var menuOpen by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var lastTarget by remember(index) { mutableIntStateOf(index) }
    val rowHeightPx = with(density) { 92.dp.toPx() }

    Row(
        Modifier
            .fillMaxWidth()
            .height(94.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(78.dp),
            contentAlignment = Alignment.Center,
        ) {
            Artwork(
                item.artworkUri,
                Modifier.size(58.dp),
                17.dp,
                item.title,
                MediaType.AUDIO,
                AirPlayrIcons.Music,
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(27.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (index + 1).toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 8.dp),
        ) {
            Text(
                item.title,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(item.artist ?: "Unknown artist")
                    if (item.durationMs > 0) {
                        append("  ·  ")
                        append(formatDuration(item.durationMs))
                    }
                },
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.MoreVert, "Song options", tint = colors.textPrimary, modifier = Modifier.size(25.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Play now") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                    onClick = { menuOpen = false; onPlay() },
                )
                DropdownMenuItem(
                    text = { Text("Play next") },
                    leadingIcon = { Icon(Icons.Default.SkipNext, null) },
                    onClick = { menuOpen = false; onPlayNext() },
                )
                DropdownMenuItem(
                    text = { Text("Remove") },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                    onClick = { menuOpen = false; onRemove() },
                )
            }
        }

        Box(
            Modifier
                .size(44.dp)
                .pointerInput(index) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            dragOffsetPx = 0f
                            lastTarget = index
                        },
                        onDragCancel = {
                            dragOffsetPx = 0f
                            lastTarget = index
                        },
                        onDragEnd = {
                            dragOffsetPx = 0f
                            lastTarget = index
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetPx += dragAmount.y
                            val delta = (dragOffsetPx / rowHeightPx).roundToInt()
                            val target = (index + delta).coerceIn(0, Int.MAX_VALUE)
                            if (target != lastTarget) {
                                onMove(lastTarget, target)
                                lastTarget = target
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.DragHandle, "Drag to rearrange", tint = colors.textPrimary.copy(alpha = 0.78f), modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun QueueEmptyState(colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    Column(
        Modifier.fillMaxWidth().padding(top = 56.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(AirPlayrIcons.Queue, null, tint = colors.textSecondary, modifier = Modifier.size(48.dp))
        Text("Your music queue is empty", fontSize = 18.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 14.dp))
        Text("Add songs from your library to see them here.", fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 5.dp))
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
