package com.airplayr.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import kotlinx.coroutines.launch

@Composable
fun FeatureCollectionScreen(
    title: String,
    subtitle: String,
    loader: suspend () -> List<MediaItem>,
    playback: PlaybackController,
) {
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    LaunchedEffect(title) { items = runCatching { loader() }.getOrDefault(emptyList()) }
    Column(Modifier.fillMaxSize()) {
        ListTopBar(title, subtitle)
        if (items.isEmpty()) {
            EmptyNativeState("Nothing here yet", "Use AirPlayr for a while and this collection will fill from your local library.")
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("${items.size} items", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(items, key = { it.id }) { item ->
                    MediaListCard(item) { playback.play(item) }
                }
            }
        }
    }
}

@Composable
fun QueueScreen(appContext: android.content.Context) {
    val playback: PlaybackController = androidx.lifecycle.viewmodel.compose.viewModel(factory = com.airplayr.app.ui.playback.PlaybackControllerFactory(appContext))
    val state by playback.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ListTopBar("Queue", "${state.queueSize} queued items")
        Card(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.currentTitle ?: "Nothing playing", style = MaterialTheme.typography.titleLarge)
                Text(state.currentArtist.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (state.queueSize == 0) "Queue is empty" else "Playing ${state.currentQueueIndex + 1} of ${state.queueSize}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = playback::skipPrevious, enabled = state.queueSize > 0) { Text("Previous") }
                    Button(onClick = playback::togglePlayPause, enabled = state.currentId != null) { Text(if (state.isPlaying) "Pause" else "Play") }
                    Button(onClick = playback::skipNext, enabled = state.queueSize > 0) { Text("Next") }
                }
                OutlinedButton(onClick = playback::clearQueue, enabled = state.queueSize > 0) { Text("Clear queue") }
            }
        }
    }
}

@Composable
fun StatsScreen(container: AppContainer) {
    val history by container.structured.history(500).collectAsStateWithLifecycle(initialValue = emptyList())
    val videos by container.structured.allVideoProgress().collectAsStateWithLifecycle(initialValue = emptyList())
    val favorites by container.structured.favorites().collectAsStateWithLifecycle(initialValue = emptyList())
    val totalPlays = history.sumOf { it.playCount }
    val completed = history.count { it.completed }
    Column(Modifier.fillMaxSize()) {
        ListTopBar("Listening & watching stats", "Your local AirPlayr activity")
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { StatCard("Music plays", totalPlays.toString(), Icons.Default.TrendingUp) }
            item { StatCard("Favorite songs", favorites.size.toString(), Icons.Default.Star) }
            item { StatCard("Completed listening sessions", completed.toString(), Icons.Default.History) }
            item { StatCard("Video progress records", videos.size.toString(), Icons.Default.GraphicEq) }
        }
    }
}

@Composable
fun FoldersScreen(container: AppContainer, libraryViewModel: LibraryViewModel, onOpen: (String) -> Unit) {
    val audio by libraryViewModel.audio.collectAsStateWithLifecycle()
    val video by libraryViewModel.video.collectAsStateWithLifecycle()
    val folders = (audio + video).groupBy { it.folder ?: it.relativePath ?: "Unknown folder" }.toList().sortedBy { it.first.lowercase() }
    Column(Modifier.fillMaxSize()) {
        ListTopBar("Folders", "${folders.size} media folders")
        if (folders.isEmpty()) EmptyNativeState("No folders yet", "Grant media access or add a media folder in Settings.")
        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(folders, key = { it.first }) { (folder, items) ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(java.net.URLEncoder.encode(folder, "UTF-8")) }) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Folder, null) },
                        headlineContent = { Text(folder) },
                        supportingContent = { Text("${items.size} media items") },
                    )
                }
            }
        }
    }
}

@Composable
fun BrowseFilterScreen(
    title: String,
    value: String,
    type: MediaType,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
) {
    val source by (if (type == MediaType.AUDIO) libraryViewModel.audio else libraryViewModel.video).collectAsStateWithLifecycle()
    val filtered = remember(source, value) {
        source.filter {
            when (title.lowercase()) {
                "artists" -> it.artist.equals(value, true)
                "albums" -> it.album.equals(value, true)
                "genres" -> it.genre.equals(value, true)
                "folders" -> (it.folder ?: it.relativePath ?: "").equals(value, true)
                else -> true
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        ListTopBar(value, "${filtered.size} items")
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered, key = { it.id }) { item -> MediaListCard(item) { playback.play(item) } }
        }
    }
}

@Composable
fun RecentScreen(
    type: String,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    onOpen: (String) -> Unit,
) {
    val source by (if (type == "videos") libraryViewModel.video else libraryViewModel.audio).collectAsStateWithLifecycle()
    val data = source.sortedByDescending { it.dateAdded }
    Column(Modifier.fillMaxSize()) {
        ListTopBar("Recently added ${type}", "${data.size} local items")
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(data, key = { it.id }) { item -> MediaListCard(item) { if (item.mediaType == MediaType.AUDIO) playback.play(item) else onOpen(item.id) } }
        }
    }
}

@Composable
fun MyPlaylistsScreen(container: AppContainer, onOpen: (String) -> Unit) {
    val playlists by container.structured.playlists().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        ListTopBar("My playlists", "Your archive, your rules")
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { container.structured.createPlaylist("New playlist") } }) { Text("Create playlist") }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(playlists, key = { it.id }) { playlist ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(playlist.id) }) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.List, null) },
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text("Open playlist") },
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(container: AppContainer, id: String, playback: PlaybackController) {
    val library by container.mediaLibrary.observe(MediaType.AUDIO).collectAsStateWithLifecycle(initialValue = emptyList())
    var name by remember { mutableStateOf("Playlist") }
    var ids by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(id) {
        val p = container.database.userDataDao().playlist(id)
        name = p?.name ?: "Playlist"
        ids = container.structured.playlistIds(id)
    }
    val items = library.filter { it.id in ids }.sortedBy { ids.indexOf(it.id) }
    Column(Modifier.fillMaxSize()) {
        ListTopBar(name, "${items.size} songs")
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Button(onClick = { playback.playQueue(items) }, enabled = items.isNotEmpty()) { Text("Play playlist") } }
            items(items, key = { it.id }) { item -> MediaListCard(item) { playback.play(item) } }
        }
    }
}

@Composable
fun EmptyNativeState(title: String, message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.MusicNote, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MediaListCard(item: MediaItem, onPlay: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onPlay)) {
        ListItem(
            leadingContent = { Icon(if (item.mediaType == MediaType.VIDEO) Icons.Default.GraphicEq else Icons.Default.MusicNote, null) },
            headlineContent = { Text(item.title, maxLines = 1) },
            supportingContent = { Text(listOfNotNull(item.artist, item.album).joinToString(" · ").ifBlank { item.filename }, maxLines = 1) },
            trailingContent = { IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, "Play") } },
        )
    }
}

@Composable
private fun ListTopBar(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(Modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = { Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(value, style = MaterialTheme.typography.headlineMedium) },
            supportingContent = { Text(title) },
        )
    }
}


@Composable
fun BrowseIndexScreen(
    title: String,
    type: MediaType,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    onOpen: (String) -> Unit,
) {
    val source by (if (type == MediaType.AUDIO) libraryViewModel.audio else libraryViewModel.video).collectAsStateWithLifecycle()
    val keyer: (MediaItem) -> String = when (title.lowercase()) {
        "albums" -> { item -> item.album?.takeIf { it.isNotBlank() } ?: "Unknown album" }
        "artists" -> { item -> item.artist?.takeIf { it.isNotBlank() } ?: "Unknown artist" }
        "genres" -> { item -> item.genre?.takeIf { it.isNotBlank() } ?: "Unknown genre" }
        "folders" -> { item -> item.folder?.takeIf { it.isNotBlank() } ?: item.relativePath ?: "Unknown folder" }
        else -> { item -> item.title }
    }
    val groups = source.groupBy(keyer).toList().sortedBy { it.first.lowercase() }
    Column(Modifier.fillMaxSize()) {
        ListTopBar(title, "${groups.size} ${title.lowercase()}")
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups, key = { it.first }) { (name, items) ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(java.net.URLEncoder.encode(name, "UTF-8")) }) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Album, null) },
                        headlineContent = { Text(name) },
                        supportingContent = { Text("${items.size} items") },
                    )
                }
            }
        }
    }
}
