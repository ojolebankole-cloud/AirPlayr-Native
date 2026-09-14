package com.airplayr.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.ui.navigation.LibraryViewModel

@Composable
fun LibraryScreen(title: String, type: MediaType, viewModel: LibraryViewModel, onOpen: (MediaItem) -> Unit = {}) {
    val items = when (type) {
        MediaType.AUDIO -> viewModel.audio.collectAsStateWithLifecycle().value
        MediaType.VIDEO -> viewModel.video.collectAsStateWithLifecycle().value
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 6.dp))
            Text(
                "${items.size} cached items",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        if (items.isEmpty()) {
            item {
                Text(
                    "No media is cached yet. Grant media access and refresh from Home.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            }
        } else {
            items(items, key = { it.id }) { item ->
                MediaRow(item, onOpen)
            }
        }
    }
}

@Composable
private fun MediaRow(item: MediaItem, onOpen: (MediaItem) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(item) }) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = if (item.mediaType == MediaType.AUDIO) Icons.Default.AudioFile else Icons.Default.VideoFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    listOfNotNull(item.artist?.takeUnless { it.isBlank() }, item.album?.takeUnless { it.isBlank() }, item.filename.takeIf { it != item.title }).joinToString(" • ")
                        .ifBlank { item.uri },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}
