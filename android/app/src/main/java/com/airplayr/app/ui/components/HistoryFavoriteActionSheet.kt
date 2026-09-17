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
    val colors = LocalAirPlayrColors.current
    val isVideo = item.mediaType == MediaType.VIDEO
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceContainerLow,
        tonalElevation = 8.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Artwork(item.artworkUri ?: item.thumbnailUri, Modifier.size(64.dp), 14.dp, item.title)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(item.title.ifBlank { if (isVideo) "Video" else "Song" }, color = colors.textPrimary, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, maxLines = 1)
                    Text(listOfNotNull(item.artist, item.album).joinToString("  ·  ").ifBlank { if (isVideo) "Video" else "Unknown artist" }, color = colors.textSecondary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, maxLines = 1)
                }
                IconButton(onClick = onFavorite) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (favorite) "Remove from favorites" else "Add to favorites", tint = colors.primary) }
            }
            ActionGroup {
                ActionButton("Play next", Icons.Default.SkipNext, onPlayNext)
                ActionButton("Add to queue", Icons.Default.QueueMusic, onAddQueue)
                ActionButton("Add to playlist", Icons.Default.PlaylistAdd, onPlaylist)
            }
            if (!isVideo) {
                ActionGroup {
                    ActionButton("Go to album", Icons.Default.Album, onGoAlbum)
                    ActionButton("Go to artist", Icons.Default.Person, onGoArtist)
                    ActionButton("Go to album artist", Icons.Default.People, onGoArtist)
                    ActionButton("Go to folder", Icons.Default.Folder, onGoFolder)
                }
                ActionGroup {
                    ActionButton("Tag editor", Icons.Default.Edit, onEdit)
                    ActionButton("Edit lyrics", AirPlayrIcons.Music, onLyrics)
                }
            }
            ActionGroup {
                if (!isVideo && onBlacklist != null) ActionButton("Blacklist", Icons.Default.Block, onBlacklist)
                ActionButton("Details", Icons.Default.Info, onDetails)
                ActionButton("Share", Icons.Default.Share, {
                    val send = Intent(Intent.ACTION_SEND).apply { type = if (isVideo) "video/*" else "audio/*"; putExtra(Intent.EXTRA_TEXT, item.uri) }
                    runCatching { container.appContext.startActivity(Intent.createChooser(send, "Share ${item.title}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    onClose()
                })
                ActionButton("Delete from device", Icons.Default.Delete, onDelete, destructive = true)
            }
        }
    }
}

@Composable
fun ActionGroup(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAirPlayrColors.current
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), content = content)
    }
}

@Composable
fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, destructive: Boolean = false) {
    val colors = LocalAirPlayrColors.current
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (destructive) colors.error else colors.primary)
            Spacer(Modifier.width(14.dp))
            Text(label, color = if (destructive) colors.error else colors.textPrimary, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFavoriteDetailsSheet(item: MediaItem, onDismiss: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Artwork(item.artworkUri ?: item.thumbnailUri, Modifier.size(110.dp), 20.dp, item.title)
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
