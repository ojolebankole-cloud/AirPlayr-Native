@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.airplayr.app.ui.screens

import com.airplayr.app.ui.theme.AirPlayrIcons
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
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
    playback: PlaybackController,
    onOpenVideo: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val scope = rememberCoroutineScope()
    val music by container.mediaLibrary.observe(MediaType.AUDIO).collectAsStateWithLifecycle(emptyList())
    val videos by container.mediaLibrary.observe(MediaType.VIDEO).collectAsStateWithLifecycle(emptyList())
    val favoriteRows by container.structured.favorites().collectAsStateWithLifecycle(emptyList())
    val playbackState by playback.state.collectAsStateWithLifecycle()
    val lookup = remember(music, videos) { (music + videos).associateBy { it.id } }
    val items = remember(favoriteRows, lookup) { favoriteRows.sortedByDescending { it.addedAt }.mapNotNull { lookup[it.mediaId] } }
    val songs = remember(items) { items.filter { it.mediaType == MediaType.AUDIO } }
    val likedVideos = remember(items) { items.filter { it.mediaType == MediaType.VIDEO } }
    val albums = remember(songs) { songs.groupBy { (it.album ?: "Unknown album").trim() to (it.albumArtist ?: it.artist ?: "") }.values.map { it.first() }.take(20) }
    val favoriteSet = remember(favoriteRows) { favoriteRows.map { it.mediaId }.toSet() }
    var headerMenu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var playlistTarget by remember { mutableStateOf<List<MediaItem>?>(null) }
    var detailsTarget by remember { mutableStateOf<MediaItem?>(null) }
    var editTarget by remember { mutableStateOf<MediaItem?>(null) }
    var lyricsTarget by remember { mutableStateOf<MediaItem?>(null) }
    var deleteTarget by remember { mutableStateOf<MediaItem?>(null) }

    fun playSong(item: MediaItem) {
        val index = songs.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        if (songs.isNotEmpty()) { playback.playQueue(songs, index); onOpenPlayer() }
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 190.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).heightIn(min = 66.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary, modifier = Modifier.size(30.dp)) }
                    Text("Favorites", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 29.sp, color = colors.textPrimary)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { headerMenu = true }) { Icon(Icons.Default.MoreVert, "Favorites options", tint = colors.primary, modifier = Modifier.size(25.dp)) }
                }
            }
            if (items.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(horizontal = 45.dp).padding(top = 90.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.FavoriteBorder, null, Modifier.size(56.dp), tint = colors.primary); Text("No favorites yet", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 26.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 18.dp)); Text("Songs and videos you like will appear here.", color = colors.textPrimary, modifier = Modifier.padding(top = 8.dp)) } }
            if (songs.isNotEmpty()) {
                item { Text("Liked songs", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 24.dp).padding(top = 25.dp, bottom = 10.dp)) }
                items(songs.size, key = { "song:${songs[it].id}" }) { index ->
                    val item = songs[index]
                    val playing = playbackState.isPlaying && playbackState.currentId == item.id
                    Row(Modifier.fillMaxWidth().clickable { playSong(item) }.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Artwork(item.artworkUri ?: item.thumbnailUri, Modifier.size(64.dp), 17.dp, item.title)
                        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) { Text(item.title.ifBlank { "Unknown song" }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = colors.textPrimary, maxLines = 1); Text(listOfNotNull(item.artist, item.durationMs.takeIf { it > 0 }?.let(::formatDuration)).joinToString("  ·  ").ifBlank { "Unknown artist" }, color = colors.textSecondary, fontSize = 13.sp, maxLines = 1) }
                        if (playing) Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { repeat(3) { n -> Box(Modifier.width(4.dp).height((14 + n * 5).dp).background(colors.primary, RoundedCornerShape(4.dp))) } }
                        else IconButton(onClick = { selected = item }) { Icon(Icons.Default.MoreVert, "Song options", tint = colors.primary) }
                    }
                }
            }
            if (albums.isNotEmpty()) {
                item { Text("Liked albums", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 24.dp).padding(top = 25.dp, bottom = 10.dp)) }
                item { Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) { albums.forEach { album -> Column(Modifier.width(170.dp).clickable { onNavigate("album/${Uri.encode(album.album ?: "Unknown album")}") }) { Artwork(album.artworkUri ?: album.thumbnailUri, Modifier.size(170.dp), 27.dp, album.title); Text(album.album ?: "Unknown album", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, color = colors.textPrimary, maxLines = 1, modifier = Modifier.padding(top = 7.dp)); Text(album.artist ?: "Unknown artist", color = colors.textSecondary, fontSize = 12.sp, maxLines = 1) } } } }
            }
            if (likedVideos.isNotEmpty()) {
                item { Text("Liked videos", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 24.dp).padding(top = 25.dp, bottom = 10.dp)) }
                items(likedVideos.size, key = { "video:${likedVideos[it].id}" }) { index ->
                    val item = likedVideos[index]
                    Row(Modifier.fillMaxWidth().clickable { onOpenVideo(item.id) }.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Artwork(item.thumbnailUri ?: item.uri, Modifier.width(122.dp).height(69.dp), 13.dp, item.title, MediaType.VIDEO, AirPlayrIcons.Video)
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text(item.title.ifBlank { "Video" }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = colors.textPrimary, maxLines = 1); Text("${item.artist ?: "Video"}${if (item.durationMs > 0) "  ·  ${formatDuration(item.durationMs)}" else ""}", color = colors.textSecondary, fontSize = 13.sp, maxLines = 1) }
                        IconButton(onClick = { selected = item }) { Icon(Icons.Default.MoreVert, "Video options", tint = colors.primary) }
                    }
                }
            }
        }
        if (songs.isNotEmpty()) FloatingActionButton(onClick = { playSong(songs.first()) }, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = if (playbackState.currentId != null && playbackState.currentMediaType == MediaType.AUDIO) AIRPLAYR_FLOATING_PLAY_BOTTOM_WITH_MINI.dp else AIRPLAYR_FLOATING_PLAY_BOTTOM.dp), containerColor = colors.primary, contentColor = colors.onPrimary, shape = RoundedCornerShape(22.dp)) { Icon(Icons.Default.PlayArrow, "Play favorites", modifier = Modifier.size(31.dp)) }
    }

    if (headerMenu) {
        ModalBottomSheet(onDismissRequest = { headerMenu = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Favorites", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 24.sp, color = colors.textPrimary)
                TextButton(onClick = { playback.playQueue(songs.shuffled()); headerMenu = false; onOpenPlayer() }, enabled = songs.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth()) { Icon(Icons.Default.Shuffle, null, tint = colors.primary); Spacer(Modifier.width(14.dp)); Text("Shuffle", color = colors.textPrimary) } }
                TextButton(onClick = { songs.forEach(playback::addToQueue); headerMenu = false; AppFeedback.showSnackbar("Favorites added to the playing queue.") }, enabled = songs.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth()) { Icon(Icons.Default.QueueMusic, null, tint = colors.primary); Spacer(Modifier.width(14.dp)); Text("Add to queue", color = colors.textPrimary) } }
                TextButton(onClick = { songs.forEach(playback::playNext); headerMenu = false; AppFeedback.showSnackbar("Favorites added as next.") }, enabled = songs.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth()) { Icon(Icons.Default.SkipNext, null, tint = colors.primary); Spacer(Modifier.width(14.dp)); Text("Play next", color = colors.textPrimary) } }
                TextButton(onClick = { headerMenu = false; playlistTarget = songs }, enabled = songs.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth()) { Icon(Icons.Default.PlaylistAdd, null, tint = colors.primary); Spacer(Modifier.width(14.dp)); Text("Add to playlist", color = colors.textPrimary) } }
            }
        }
    }

    selected?.let { item ->
        HistoryFavoriteActionSheet(item, favoriteSet.contains(item.id), container, { selected = null },
            onFavorite = { scope.launch { val active = container.structured.toggleFavorite(item); AppFeedback.showSnackbar(if (active) "Added to favorites." else "Removed from favorites."); selected = null } },
            onPlayNext = { if (item.mediaType == MediaType.AUDIO) playback.playNext(item) else AppFeedback.showSnackbar("Video queue is not available in the audio queue."); selected = null },
            onAddQueue = { if (item.mediaType == MediaType.AUDIO) playback.addToQueue(item) else AppFeedback.showSnackbar("Video queue is not available in the audio queue."); selected = null },
            onPlaylist = { playlistTarget = listOf(item); selected = null }, onDetails = { detailsTarget = item; selected = null }, onDelete = { deleteTarget = item; selected = null },
            onBlacklist = null, onGoAlbum = { item.album?.let { onNavigate("album/${Uri.encode(it)}") }; selected = null }, onGoArtist = { item.artist?.let { onNavigate("artist/${Uri.encode(it)}") }; selected = null }, onGoFolder = { item.relativePath?.let { onNavigate("music-folder/${Uri.encode(it)}") }; selected = null },
            onEdit = { editTarget = item; selected = null }, onLyrics = { lyricsTarget = item; selected = null })
    }
    playlistTarget?.let { list -> HistoryFavoritePlaylistSheet(container, list) { playlistTarget = null } }
    detailsTarget?.let { HistoryFavoriteDetailsSheet(it) { detailsTarget = null } }
    editTarget?.let { HistoryFavoriteTagEditorSheet(container, it) { editTarget = null } }
    lyricsTarget?.let { HistoryFavoriteLyricsSheet(container, it) { lyricsTarget = null } }
    deleteTarget?.let { item ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete from device?") }, text = { Text("Delete ${item.title} from this device. Android may ask you to confirm the deletion.") }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }, confirmButton = { TextButton(onClick = { deleteTarget = null; scope.launch(Dispatchers.IO) { val ok = runCatching { val base = if (item.mediaType == MediaType.AUDIO) android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI; container.appContext.contentResolver.delete(Uri.withAppendedPath(base, item.mediaStoreId?.toString() ?: return@runCatching false), null, null) > 0 }.getOrDefault(false); if (ok) runCatching { container.mediaLibrary.deltaRefresh(item.mediaType) }; withContext(Dispatchers.Main.immediate) { AppFeedback.showSnackbar(if (ok) "${item.title} deleted from device." else "Unable to delete ${item.title}.") } } }) { Text("Delete", color = colors.error) } })
    }
}

private fun formatDuration(ms: Long): String { val total = (ms / 1000L).coerceAtLeast(0L); return "${total / 60}:${(total % 60).toString().padStart(2, '0')}" }
