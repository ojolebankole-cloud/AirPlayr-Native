package com.airplayr.app.ui.components

import com.airplayr.app.ui.theme.AirPlayrIcons
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.lyrics.LyricsService
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.feedback.AppFeedback
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFavoriteActionSheet(
    item: MediaItem,
    favorite: Boolean,
    playing: Boolean = false,
    container: AppContainer,
    onClose: () -> Unit,
    onFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddQueue: () -> Unit,
    onPlaylist: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
    onBlacklist: (() -> Unit)? = null,
    onGoAlbum: () -> Unit,
    onGoArtist: () -> Unit,
    onGoFolder: () -> Unit,
    onEdit: () -> Unit,
    onLyrics: () -> Unit,
) {
    val isVideo = item.mediaType == MediaType.VIDEO
    val musicGroups = listOf(
        listOf(
            AirPlayrMediaMenuAction("Play next", Icons.Default.SkipNext, onPlayNext),
            AirPlayrMediaMenuAction("Add to queue", Icons.Default.QueueMusic, onAddQueue),
            AirPlayrMediaMenuAction("Add to playlist", Icons.Default.PlaylistAdd, onPlaylist),
        ),
        listOf(
            AirPlayrMediaMenuAction("Go to album", Icons.Default.Album, onGoAlbum, enabled = !item.album.isNullOrBlank()),
            AirPlayrMediaMenuAction("Go to artist", Icons.Default.Person, onGoArtist, enabled = !item.artist.isNullOrBlank()),
            AirPlayrMediaMenuAction("Go to album artist", Icons.Default.People, onGoArtist, enabled = !item.albumArtist.isNullOrBlank()),
            AirPlayrMediaMenuAction("Go to folder", Icons.Default.Folder, onGoFolder, enabled = !item.folderId.isNullOrBlank() || !item.folder.isNullOrBlank() || !item.relativePath.isNullOrBlank()),
        ),
        listOf(
            AirPlayrMediaMenuAction("Tag editor", Icons.Default.Edit, onEdit),
            AirPlayrMediaMenuAction("Edit lyrics", AirPlayrIcons.Lyrics, onLyrics),
        ),
    )
    val commonGroup = buildList {
        if (!isVideo && onBlacklist != null) add(AirPlayrMediaMenuAction("Blacklist", Icons.Default.Block, onBlacklist))
        add(AirPlayrMediaMenuAction("Details", Icons.Default.Info, onDetails))
        add(AirPlayrMediaMenuAction("Share", Icons.Default.Share, {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = if (isVideo) item.mimeType ?: "video/*" else item.mimeType ?: "audio/*"
                putExtra(Intent.EXTRA_TEXT, item.uri)
                putExtra(Intent.EXTRA_TITLE, item.title)
            }
            runCatching { container.appContext.startActivity(Intent.createChooser(send, "Share ${item.title}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            onClose()
        }))
        add(AirPlayrMediaMenuAction("Delete from device", Icons.Default.Delete, onDelete, destructive = true))
    }
    AirPlayrPremiumMediaActionSheet(
        item = item,
        playing = playing,
        favorite = favorite,
        onFavorite = onFavorite,
        groups = if (isVideo) listOf(commonGroup) else musicGroups + listOf(commonGroup),
        onDismiss = onClose,
    )
}

@Composable
fun HistoryFavoritePlaylistSheet(container: AppContainer, items: List<MediaItem>, onDismiss: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    val scope = rememberCoroutineScope()
    val playlists by container.structured.playlists().collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }
    val itemIds = remember(items) { items.map { it.id }.distinct() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Add to playlist", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 24.sp, color = colors.textPrimary)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("New playlist") }, singleLine = true)
            Button(enabled = name.trim().isNotEmpty(), onClick = { scope.launch { val p = container.structured.createPlaylist(name.trim()); itemIds.forEach { container.structured.addToPlaylist(p.id, it) }; AppFeedback.showSnackbar("Added to ${p.name}."); onDismiss() } }, modifier = Modifier.fillMaxWidth()) { Text("Create playlist") }
            if (playlists.isNotEmpty()) Text("YOUR PLAYLISTS", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 12.sp, letterSpacing = 1.1.sp, color = colors.textSecondary)
            playlists.forEach { p -> TextButton(onClick = { scope.launch { itemIds.forEach { container.structured.addToPlaylist(p.id, it) }; AppFeedback.showSnackbar("Added to ${p.name}."); onDismiss() } }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.QueueMusic, null, tint = colors.primary); Spacer(Modifier.width(14.dp)); Text(p.name, color = colors.textPrimary) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFavoriteDetailsSheet(item: MediaItem, playing: Boolean = false, onDismiss: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Artwork(
                item.artworkUri ?: item.thumbnailUri,
                Modifier.size(110.dp),
                20.dp,
                item.title,
                item.mediaType,
                if (item.mediaType == MediaType.VIDEO) AirPlayrIcons.Video else AirPlayrIcons.Music,
                isPlaying = playing,
            )
            Text(item.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 25.sp, color = colors.textPrimary)
            Text(listOfNotNull(item.artist, item.album, item.genre).joinToString(" • ").ifBlank { item.filename }, color = colors.textSecondary)
            Text("${item.sizeBytes / 1024 / 1024} MB · ${item.durationMs / 1000}s", color = colors.textSecondary)
            Text("${item.mimeType ?: "Unknown media type"}", color = colors.textSecondary, fontSize = 12.sp)
            Text("${item.uri}", color = colors.textSecondary, fontSize = 11.sp, maxLines = 2)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFavoriteTagEditorSheet(container: AppContainer, item: MediaItem, onDismiss: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    val scope = rememberCoroutineScope()
    var title by remember(item.id) { mutableStateOf(item.title) }
    var artist by remember(item.id) { mutableStateOf(item.artist.orEmpty()) }
    var album by remember(item.id) { mutableStateOf(item.album.orEmpty()) }
    var genre by remember(item.id) { mutableStateOf(item.genre.orEmpty()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tag editor", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 24.sp, color = colors.textPrimary)
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
            OutlinedTextField(artist, { artist = it }, Modifier.fillMaxWidth(), label = { Text("Artist") }, singleLine = true)
            OutlinedTextField(album, { album = it }, Modifier.fillMaxWidth(), label = { Text("Album") }, singleLine = true)
            OutlinedTextField(genre, { genre = it }, Modifier.fillMaxWidth(), label = { Text("Genre") }, singleLine = true)
            Button(onClick = {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { runCatching {
                        val id = item.mediaStoreId ?: return@runCatching false
                        val base = if (item.mediaType == MediaType.AUDIO) android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.TITLE, title.trim())
                            if (item.mediaType == MediaType.AUDIO) {
                                put(android.provider.MediaStore.Audio.AudioColumns.ARTIST, artist.trim())
                                put(android.provider.MediaStore.Audio.AudioColumns.ALBUM, album.trim())
                                put(android.provider.MediaStore.Audio.AudioColumns.GENRE, genre.trim())
                            }
                        }
                        container.appContext.contentResolver.update(Uri.withAppendedPath(base, id.toString()), values, null, null) > 0
                    }.getOrDefault(false) }
                    if (ok) { runCatching { container.mediaLibrary.deltaRefresh(item.mediaType) }; AppFeedback.showSnackbar("Metadata saved."); onDismiss() } else AppFeedback.showSnackbar("Metadata could not be saved.")
                }
            }, Modifier.fillMaxWidth()) { Text("Save changes") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFavoriteLyricsSheet(container: AppContainer, item: MediaItem, onDismiss: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val service = remember(item.id) { LyricsService(context) }
    var lyrics by remember(item.id) { mutableStateOf("") }
    var loading by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) { lyrics = withContext(Dispatchers.IO) { service.getCached(item.id)?.plainLyrics.orEmpty() } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Edit lyrics", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 24.sp, color = colors.textPrimary)
            Text(item.title, color = colors.textPrimary)
            OutlinedTextField(lyrics, { lyrics = it }, Modifier.fillMaxWidth().height(260.dp), label = { Text("Lyrics") })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !loading, onClick = { scope.launch { loading = true; val result = withContext(Dispatchers.IO) { service.fetch(item.id, item.title, item.artist.orEmpty(), item.album, item.durationMs / 1000.0, true) }; lyrics = result?.plainLyrics.orEmpty(); loading = false } }, modifier = Modifier.weight(1f)) { Text(if (loading) "Searching…" else "Fetch") }
                Button(onClick = { scope.launch { val old = withContext(Dispatchers.IO) { service.getCached(item.id) }; val result = LyricsService.Result(old?.provider ?: "AirPlayr", item.title, item.artist.orEmpty(), item.album.orEmpty(), item.durationMs / 1000.0, lyrics.ifBlank { null }, old?.syncedLyrics, old?.cues.orEmpty(), old?.confidence ?: 0); withContext(Dispatchers.IO) { service.save(item.id, result) }; AppFeedback.showSnackbar("Lyrics saved."); onDismiss() } }, modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}
}
