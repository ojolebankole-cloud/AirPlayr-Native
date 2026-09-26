@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.airplayr.app.ui.screens
import com.airplayr.app.ui.airPlayrBottomContentPadding
import com.airplayr.app.ui.airPlayrFloatingPlayBottomPadding

import androidx.compose.foundation.lazy.items

import com.airplayr.app.ui.theme.AirPlayrIcons
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.playback.VideoQueueStore
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.AirPlayrLongListFastScrollHandle
import com.airplayr.app.ui.components.*
import com.airplayr.app.ui.feedback.AppFeedback
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FavoritesScreen(
    container: AppContainer,
    libraryViewModel: com.airplayr.app.ui.navigation.LibraryViewModel,
    playback: PlaybackController,
    onOpenVideo: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val scope = rememberCoroutineScope()
    val favoritesListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) }
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    var headerMenu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var selectedIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var playlistTarget by remember { mutableStateOf<List<MediaItem>?>(null) }
    var detailsTarget by remember { mutableStateOf<MediaItem?>(null) }
    var editTarget by remember { mutableStateOf<MediaItem?>(null) }
    var lyricsTarget by remember { mutableStateOf<MediaItem?>(null) }
    var deleteTarget by remember { mutableStateOf<MediaItem?>(null) }

    val lookup = remember(snapshot.audio, snapshot.video) {
        (snapshot.audio + snapshot.video).associateBy { it.id }
    }
    val items = remember(snapshot.favorites, lookup) {
        snapshot.favorites.sortedByDescending { it.addedAt }.mapNotNull { lookup[it.mediaId] }
    }
    val songs = remember(items) { items.filter { it.mediaType == MediaType.AUDIO } }
    val likedVideos = remember(items) { items.filter { it.mediaType == MediaType.VIDEO } }
    val albums = remember(songs) {
        songs.groupBy { (it.album ?: "Unknown album").trim() to (it.albumArtist ?: it.artist ?: "") }
            .values.mapNotNull { it.firstOrNull() }
            .take(20)
    }
    val favoriteSet = remember(snapshot.favorites) { snapshot.favorites.map { it.mediaId }.toSet() }

    fun songsForAlbum(album: MediaItem): List<MediaItem> = songs.filter {
        (it.album ?: "Unknown album").equals(album.album ?: "Unknown album", ignoreCase = true) &&
            (it.albumArtist ?: it.artist ?: "").equals(album.albumArtist ?: album.artist ?: "", ignoreCase = true)
    }

    fun playSong(item: MediaItem) {
        val index = songs.indexOfFirst { it.id == item.id }
        if (index >= 0) playback.playLoopingQueue(songs, index)
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = favoritesListState,
                contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp).heightIn(min = 66.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary, modifier = Modifier.size(30.dp)) }
                        Text("Favorites", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 29.sp, color = colors.textPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { headerMenu = true }) { Icon(Icons.Default.MoreVert, "Favorites options", tint = colors.primary, modifier = Modifier.size(25.dp)) }
                    }
                }

                if (selectedIds.isNotEmpty()) {
                    val chosen = items.filter { it.id in selectedIds }
                    item {
                        AirPlayrSelectionBar(
                            count = selectedIds.size,
                            onClear = { selectedIds = emptySet() },
                            onSelectAll = { selectedIds = items.map { it.id }.toSet() },
                            onPlay = if (songs.isNotEmpty()) ({ val chosenSongs = chosen.filter { it.mediaType == MediaType.AUDIO }; if (chosenSongs.isNotEmpty()) { playSong(chosenSongs.first()); selectedIds = emptySet() } }) else null,
                            onPlayNext = if (songs.isNotEmpty()) ({ chosen.filter { it.mediaType == MediaType.AUDIO }.forEach(playback::playNext); selectedIds = emptySet() }) else null,
                            onPlaylist = if (songs.isNotEmpty()) ({ val chosenSongs = chosen.filter { it.mediaType == MediaType.AUDIO }; if (chosenSongs.isNotEmpty()) { playlistTarget = chosenSongs } }) else null,
                            onLike = { scope.launch { chosen.forEach { if (it.id !in favoriteSet) container.structured.toggleFavorite(it) }; selectedIds = emptySet() } },
                        )
                    }
                }

                if (items.isEmpty()) {
                    item {
                        AirPlayrSectionEmpty("No favorites yet", "Songs and videos you like will appear here.")
                    }
                }

                if (songs.isNotEmpty()) {
                    item { Text("Liked songs", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) }
                    items(songs, key = { "song:${it.id}" }) { item ->
                        val playing = playbackState.isPlaying && playbackState.currentId == item.id
                        AirPlayrMediaRow(
                            item = item,
                            onOpen = { if (selectedIds.isNotEmpty()) selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id else playSong(item) },
                            onMore = { selected = item },
                            playing = playing,
                            selected = item.id in selectedIds,
                            onLongPress = { selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id },
                        )
                    }
                }

                if (albums.isNotEmpty()) {
                    item { Text("Liked albums", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) }
                    item {
                        Row(
                            Modifier.horizontalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }).padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            albums.forEach { album ->
                                val albumSongs = songsForAlbum(album)
                                val playing = playbackState.isPlaying && albumSongs.any { it.id == playbackState.currentId }
                                var menuOpen by rememberSaveable(album.album) { mutableStateOf(false) }
                                AirPlayrAlbumRailCard(
                                    artworkUri = album.displayArtworkUri(),
                                    albumTitle = album.album ?: "Unknown album",
                                    artist = album.albumArtist ?: album.artist,
                                    onOpen = { onNavigate("album/${Uri.encode(album.album ?: "Unknown album")}") },
                                    onPlay = { if (albumSongs.isNotEmpty()) playback.playQueue(albumSongs) },
                                    onMore = { menuOpen = true },
                                    playing = playing,
                                )
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Play album") },
                                        leadingIcon = { Icon(AirPlayrIcons.Play, null) },
                                        onClick = { menuOpen = false; if (albumSongs.isNotEmpty()) playback.playQueue(albumSongs) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Open album") },
                                        leadingIcon = { Icon(Icons.Default.Album, null) },
                                        onClick = { menuOpen = false; onNavigate("album/${Uri.encode(album.album ?: "Unknown album")}") },
                                    )
                                }
                            }
                        }
                    }
                }

                if (likedVideos.isNotEmpty()) {
                    item { Text("Liked videos", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) }
                    item {
                        AirPlayrVideoRail(
                            items = likedVideos,
                            selectedIds = selectedIds,
                            onOpen = { item ->
                                if (selectedIds.isNotEmpty()) selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                                else {
                                    VideoQueueStore.setLoopingQueue(likedVideos, item.id)
                                    onOpenVideo(item.id)
                                }
                            },
                            onMore = { item -> selected = item },
                            onLongPress = { item -> selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id },
                        )
                    }
                }
            }

        if (snapshot.ready && songs.isNotEmpty()) {
            FloatingActionButton(
                onClick = { playSong(songs.first()) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = airPlayrFloatingPlayBottomPadding()),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 10.dp),
            ) { Icon(Icons.Default.PlayArrow, "Play favorites", modifier = Modifier.size(31.dp)) }
        }
    }

    if (headerMenu) {
        AirPlayrFeatureCollectionActionSheet(
            title = "Favorites",
            onDismiss = { headerMenu = false },
            onShuffle = {
                headerMenu = false
                if (songs.isNotEmpty()) { playback.playQueue(songs.shuffled()); onOpenPlayer() }
            },
            onPlayNext = {
                headerMenu = false
                songs.forEach(playback::playNext)
                if (songs.isNotEmpty()) AppFeedback.showSnackbar("Favorites added as next.")
            },
            onAddToQueue = {
                headerMenu = false
                songs.forEach(playback::addToQueue)
                if (songs.isNotEmpty()) AppFeedback.showSnackbar("Favorites added to the playing queue.")
            },
            onAddToPlaylist = { headerMenu = false; playlistTarget = songs },
        )
        AirPlayrLongListFastScrollHandle(favoritesListState)
    }

    selected?.let { item ->
        HistoryFavoriteActionSheet(
            item = item,
            favorite = favoriteSet.contains(item.id),
            playing = playbackState.isPlaying && playbackState.currentId == item.id,
            container = container,
            onClose = { selected = null },
            onFavorite = { scope.launch { val active = container.structured.toggleFavorite(item); AppFeedback.showSnackbar(if (active) "Added to favorites." else "Removed from favorites."); selected = null } },
            onPlayNext = { if (item.mediaType == MediaType.AUDIO) playback.playNext(item) else AppFeedback.showSnackbar("Video queue is not available in the audio queue."); selected = null },
            onAddQueue = { if (item.mediaType == MediaType.AUDIO) playback.addToQueue(item) else AppFeedback.showSnackbar("Video queue is not available in the audio queue."); selected = null },
            onPlaylist = { playlistTarget = listOf(item); selected = null },
            onDetails = { detailsTarget = item; selected = null },
            onDelete = { deleteTarget = item; selected = null },
            onBlacklist = null,
            onGoAlbum = { item.album?.let { onNavigate("album/${Uri.encode(it)}") }; selected = null },
            onGoArtist = { item.artist?.let { onNavigate("artist/${Uri.encode(it)}") }; selected = null },
            onGoFolder = { item.relativePath?.let { onNavigate("music-folder/${Uri.encode(it)}") }; selected = null },
            onEdit = { editTarget = item; selected = null },
            onLyrics = { lyricsTarget = item; selected = null },
        )
    }
    playlistTarget?.let { list -> HistoryFavoritePlaylistSheet(container, list) { playlistTarget = null } }
    detailsTarget?.let { item -> MediaDetailsSheet(item, playing = playbackState.isPlaying && playbackState.currentId == item.id) { detailsTarget = null } }
    editTarget?.let { HistoryFavoriteTagEditorSheet(container, it) { editTarget = null } }
    lyricsTarget?.let { HistoryFavoriteLyricsSheet(container, it) { lyricsTarget = null } }
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete from device?") },
            text = { Text("Delete ${item.title} from this device. Android may ask you to confirm the deletion.") },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch(Dispatchers.IO) {
                        val ok = runCatching {
                            val base = if (item.mediaType == MediaType.AUDIO) android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            container.appContext.contentResolver.delete(
                                Uri.withAppendedPath(base, item.mediaStoreId?.toString() ?: return@runCatching false),
                                null,
                                null,
                            ) > 0
                        }.getOrDefault(false)
                        if (ok) runCatching { container.mediaLibrary.deltaRefresh(item.mediaType) }
                        withContext(Dispatchers.Main.immediate) { AppFeedback.showSnackbar(if (ok) "${item.title} deleted from device." else "Unable to delete ${item.title}.") }
                    }
                }) { Text("Delete", color = colors.error) }
            },
        )
    }
}

private fun formatDuration(ms: Long): String { val total = (ms / 1000L).coerceAtLeast(0L); return "${total / 60}:${(total % 60).toString().padStart(2, '0')}" }
