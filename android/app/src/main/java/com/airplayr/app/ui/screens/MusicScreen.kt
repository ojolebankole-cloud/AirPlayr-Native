package com.airplayr.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackControllerFactory

@Composable
fun MusicScreen(libraryViewModel: LibraryViewModel, appContext: android.content.Context, onCategory: (String) -> Unit = {}) {
    val items by libraryViewModel.audio.collectAsStateWithLifecycle()
    val playback: PlaybackController = viewModel(factory = PlaybackControllerFactory(appContext))
    val playbackState by playback.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val categories = listOf("Songs", "Likes", "Albums", "Playlists", "Artists", "Genres", "Folders")
            items(categories) { label ->
                androidx.compose.material3.FilterChip(
                    selected = label == "Songs",
                    onClick = { onCategory(label.lowercase()) },
                    label = { Text(label) },
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Music", style = MaterialTheme.typography.headlineMedium)
                        Text("${items.size} cached items", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(enabled = items.isNotEmpty(), onClick = { playback.playQueue(items) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play all")
                    }
                }
            }
            itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                MusicRow(
                    item = item,
                    playing = playbackState.currentId == item.id && playbackState.isPlaying,
                    onPlay = { playback.playQueue(items, index) },
                    onQueue = { playback.addToQueue(item) },
                )
            }
        }
        if (playbackState.currentId != null) {
            NowPlayingBar(playbackState, playback)
        }
    }
}

@Composable
private fun MusicRow(item: MediaItem, playing: Boolean, onPlay: () -> Unit, onQueue: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(listOfNotNull(item.artist, item.album).joinToString(" • ").ifBlank { item.filename }, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = onQueue) { Icon(Icons.Default.AddToQueue, contentDescription = "Add to queue") }
            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (playing) "Pause" else "Play")
        }
    }
}

@Composable
private fun NowPlayingBar(state: com.airplayr.app.ui.playback.PlaybackUiState, controller: PlaybackController) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.currentTitle ?: "Playing", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(state.currentArtist ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text("${state.currentQueueIndex + 1}/${state.queueSize}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val fraction = if (state.durationMs > 0L) (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = controller::toggleShuffle) { Icon(Icons.Default.Shuffle, contentDescription = "Shuffle") }
                IconButton(onClick = controller::skipPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous") }
                IconButton(onClick = controller::togglePlayPause) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (state.isPlaying) "Pause" else "Play") }
                IconButton(onClick = controller::skipNext) { Icon(Icons.Default.SkipNext, contentDescription = "Next") }
                IconButton(onClick = controller::cycleRepeat) { Icon(Icons.Default.Repeat, contentDescription = "Repeat") }
                IconButton(onClick = controller::clearQueue) { Icon(Icons.Default.ClearAll, contentDescription = "Clear queue") }
            }
            Text("${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
