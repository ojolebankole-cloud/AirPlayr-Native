package com.airplayr.app.ui.screens

import com.airplayr.app.ui.theme.LocalAirPlayrColors
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.components.AirPlayrCard
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.components.AirPlayrMediaRow
import com.airplayr.app.ui.components.AirPlayrScreenHeader
import com.airplayr.app.ui.components.AirPlayrSectionEmpty
import kotlinx.coroutines.launch

@Composable
fun FeatureCollectionScreen(
    title: String,
    subtitle: String,
    loader: suspend () -> List<com.airplayr.app.core.data.media.MediaItem>,
    playback: com.airplayr.app.ui.playback.PlaybackController,
) {
    var items by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<com.airplayr.app.core.data.media.MediaItem>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(title) { items = runCatching { loader() }.getOrDefault(emptyList()) }
    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        ListTopBar(title, subtitle)
        if (items.isEmpty()) EmptyNativeState("Nothing here yet", "Use AirPlayr for a while and this collection will fill from your local library.")
        else androidx.compose.foundation.lazy.LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
            item { Text("${items.size} items", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                androidx.compose.foundation.layout.Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    items.take(8).forEach { item ->
                        androidx.compose.foundation.layout.Column(Modifier.width(150.dp).clickable { if (item.mediaType == MediaType.AUDIO) playback.play(item) }) {
                            com.airplayr.app.ui.components.Artwork(item.artworkUri ?: item.thumbnailUri, Modifier.size(150.dp), 18.dp, item.title)
                            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            Text(item.artist ?: "Unknown artist", style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(items.drop(8), key = { it.id }) { item -> MediaListCard(item) { if (item.mediaType == MediaType.AUDIO) playback.play(item) } }
        }
    }
}

@Composable
fun QueueScreen(appContext: android.content.Context) {
    val playback: PlaybackController = androidx.lifecycle.viewmodel.compose.viewModel(factory = com.airplayr.app.ui.playback.PlaybackControllerFactory(appContext))
    val state by playback.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ListTopBar("Queue", "${state.queueSize} queued items")
        AirPlayrCard(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(26.dp), containerColor = LocalAirPlayrColors.current.surfaceSolid, elevation = 5.dp) {
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
fun FoldersScreen(container: AppContainer, libraryViewModel: LibraryViewModel, onOpen: (String) -> Unit) {
    val audio by libraryViewModel.audio.collectAsStateWithLifecycle()
    val video by libraryViewModel.video.collectAsStateWithLifecycle()
    val folders = (audio + video).groupBy { it.folder ?: it.relativePath ?: "Unknown folder" }.toList().sortedBy { it.first.lowercase() }
    Column(Modifier.fillMaxSize()) {
        ListTopBar("Folders", "${folders.size} media folders")
        if (folders.isEmpty()) EmptyNativeState("No folders yet", "Grant media access or add a media folder in Settings.")
        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(folders, key = { it.first }) { (folder, items) ->
                AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), containerColor = LocalAirPlayrColors.current.surfaceSolid, elevation = 5.dp, onClick = { onOpen(java.net.URLEncoder.encode(folder, "UTF-8")) }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(LocalAirPlayrColors.current.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Folder, null, tint = LocalAirPlayrColors.current.primary, modifier = Modifier.size(26.dp))
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(folder, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = LocalAirPlayrColors.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${items.size} media items · local folder", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = LocalAirPlayrColors.current.textSecondary, maxLines = 1)
                        }
                        Icon(Icons.Default.ArrowForward, null, tint = LocalAirPlayrColors.current.primary, modifier = Modifier.size(22.dp))
                    }
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
    container: AppContainer? = null,
) {
    val audio by libraryViewModel.audio.collectAsStateWithLifecycle()
    val video by libraryViewModel.video.collectAsStateWithLifecycle()
    val history by container?.structured?.history(200)?.collectAsStateWithLifecycle(initialValue = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val audioMap = remember(audio) { audio.associateBy { it.id } }
    val data: List<MediaItem> = when (type) {
        "songs" -> audio.sortedByDescending { it.dateAdded }.take(100)
        "albums" -> history.filter { it.mediaType == MediaType.AUDIO.value }.mapNotNull { audioMap[it.mediaId] }.groupBy { (it.album ?: "Unknown album") to (it.albumArtist ?: it.artist ?: "") }.values.mapNotNull { it.firstOrNull() }.take(100)
        "artists" -> history.filter { it.mediaType == MediaType.AUDIO.value }.mapNotNull { audioMap[it.mediaId] }.groupBy { it.artist ?: "Unknown artist" }.values.mapNotNull { it.firstOrNull() }.take(100)
        "videos-added" -> video.sortedByDescending { it.dateAdded }.take(100)
        else -> history.filter { it.mediaType == MediaType.VIDEO.value }.mapNotNull { h -> video.firstOrNull { it.id == h.mediaId } }.take(100)
    }
    Column(Modifier.fillMaxSize()) {
        ListTopBar(when (type) { "songs" -> "Recently added songs"; "albums" -> "Recently played albums"; "artists" -> "Recent Artists"; "videos-added" -> "Recently added videos"; else -> "Recently played videos" }, "${data.size} local items")
        if (data.isEmpty()) EmptyNativeState("Nothing here yet", "AirPlayr will build this section as your library and listening history grows.")
        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(data, key = { it.id }) { item -> MediaListCard(item) { if (item.mediaType == MediaType.AUDIO) playback.play(item) else onOpen(item.id) } } }
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
                AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 8.dp), shape = RoundedCornerShape(22.dp), containerColor = LocalAirPlayrColors.current.surfaceSolid, elevation = 5.dp, onClick = { onOpen(playlist.id) }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QueueMusic, null, tint = LocalAirPlayrColors.current.primary, modifier = Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(playlist.name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = LocalAirPlayrColors.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Open playlist", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = LocalAirPlayrColors.current.textSecondary)
                        }
                    }
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
fun EmptyNativeState(title: String, message: String) = AirPlayrSectionEmpty(title, message)

@Composable
private fun MediaListCard(item: MediaItem, onPlay: () -> Unit) {
    AirPlayrMediaRow(item, onOpen = onPlay, compact = false)
}

@Composable
private fun ListTopBar(title: String, subtitle: String) = AirPlayrScreenHeader(title, subtitle)

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
                AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 8.dp), shape = RoundedCornerShape(22.dp), containerColor = LocalAirPlayrColors.current.surfaceSolid, elevation = 5.dp, onClick = { onOpen(java.net.URLEncoder.encode(name, "UTF-8")) }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (title.lowercase()) {
                            "albums" -> Icons.Default.Album
                            "artists" -> Icons.Default.Person
                            "genres" -> Icons.Default.LocalOffer
                            "folders" -> Icons.Default.Folder
                            else -> Icons.Default.LibraryMusic
                        }
                        Icon(icon, null, tint = LocalAirPlayrColors.current.primary, modifier = Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = LocalAirPlayrColors.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${items.size} items", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = LocalAirPlayrColors.current.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
