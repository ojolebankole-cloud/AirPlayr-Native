package com.airplayr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaLibraryRepository
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.playback.VideoQueueStore
import com.airplayr.app.ui.theme.LocalAirPlayrColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoQueueSheet(
    library: MediaLibraryRepository,
    onPlay: (MediaItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val queue by VideoQueueStore.queue.collectAsStateWithLifecycle()
    val currentId by VideoQueueStore.currentId.collectAsStateWithLifecycle()
    var addDialog by remember { mutableStateOf(false) }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentIndex = queue.indexOfFirst { it.id == currentId }
    val current = queue.getOrNull(currentIndex)
    val upcoming = if (currentIndex >= 0) queue.drop(currentIndex + 1) else queue

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Video queue", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (upcoming.isEmpty()) "Nothing waiting after this video" else "${upcoming.size} waiting",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { addDialog = true }) { Icon(Icons.Default.Add, "Add videos") }
                IconButton(onClick = { VideoQueueStore.clearUpNext() }) { Icon(Icons.Default.ClearAll, "Clear up next") }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
            }

            if (current != null) {
                QueueVideoCard(current, "NOW PLAYING", highlighted = true, onClick = { onPlay(current) })
                Spacer(Modifier.height(14.dp))
            }

            if (upcoming.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QueueMusic, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Your video queue is empty", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                        Text("Add videos and they will play automatically in order.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        Button(onClick = { addDialog = true }, modifier = Modifier.padding(top = 16.dp)) {
                            Icon(Icons.Default.Add, null)
                            Text("Add videos", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            } else {
                Text("UP NEXT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                LazyColumn(Modifier.fillMaxWidth().height(330.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(upcoming, key = { it.id }) { item ->
                        VideoQueueRow(
                            item = item,
                            onPlay = { onPlay(item) },
                            onRemove = { VideoQueueStore.remove(item.id) },
                            onMoveUp = { VideoQueueStore.moveUpNext(item.id, -1) },
                            onMoveDown = { VideoQueueStore.moveUpNext(item.id, 1) },
                        )
                    }
                }
            }
        }
    }

    if (addDialog) {
        AddVideoDialog(
            library = library,
            currentQueue = queue,
            onAdd = { item, playNext -> if (playNext) VideoQueueStore.addNext(listOf(item)) else VideoQueueStore.add(listOf(item)) },
            onDismiss = { addDialog = false },
        )
    }
}

@Composable
private fun QueueVideoCard(item: MediaItem, label: String, highlighted: Boolean, onClick: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = if (highlighted) colors.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(item.displayArtworkUri(), Modifier.size(62.dp).clip(RoundedCornerShape(16.dp)), 16.dp, item.title, item.mediaType, com.airplayr.app.ui.theme.AirPlayrIcons.Video)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (highlighted) colors.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.title.ifBlank { item.filename }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artist.orEmpty().ifBlank { item.album.orEmpty().ifBlank { "Video" } }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.PlayArrow, null, tint = colors.primary)
        }
    }
}

@Composable
private fun VideoQueueRow(item: MediaItem, onPlay: () -> Unit, onRemove: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(item.displayArtworkUri(), Modifier.size(52.dp).clip(RoundedCornerShape(13.dp)), 13.dp, item.title, item.mediaType, com.airplayr.app.ui.theme.AirPlayrIcons.Video)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(item.title.ifBlank { item.filename }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artist.orEmpty().ifBlank { "Video" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowUpward, "Move up", Modifier.size(18.dp)) }
            IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDownward, "Move down", Modifier.size(18.dp)) }
            IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, "Play now", Modifier.size(21.dp)) }
            IconButton(onClick = onRemove) { Icon(Icons.Default.RemoveCircleOutline, "Remove", Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun AddVideoDialog(
    library: MediaLibraryRepository,
    currentQueue: List<MediaItem>,
    onAdd: (MediaItem, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val videos by library.observe(MediaType.VIDEO).collectAsStateWithLifecycle(initialValue = emptyList())
    var search by remember { mutableStateOf("") }
    val filtered = remember(videos, search, currentQueue) {
        val q = search.trim()
        videos.filter { item -> currentQueue.none { it.id == item.id } && (q.isBlank() || item.title.contains(q, true) || item.filename.contains(q, true) || item.artist.orEmpty().contains(q, true)) }.take(80)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add videos") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(value = search, onValueChange = { search = it }, singleLine = true, label = { Text("Search videos") }, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.fillMaxWidth().height(330.dp).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered, key = { it.id }) { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title.ifBlank { item.filename }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.artist.orEmpty().ifBlank { "Video" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onAdd(item, false) }) { Text("Add") }
                            TextButton(onClick = { onAdd(item, true) }) { Text("Next") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
