@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
import com.airplayr.app.core.lyrics.LyricsTrack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFavoritePlaylistSheet(container: AppContainer, items: List<MediaItem>, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val playlists by container.structured.playlists().collectAsStateWithLifecycle(emptyList())
    val favorites by container.structured.favorites().collectAsStateWithLifecycle(emptyList())
    val itemIds = remember(items) { items.map { it.id }.distinct() }
    val mediaType = items.firstOrNull()?.mediaType ?: MediaType.AUDIO
    val compatible = remember(playlists, mediaType) {
        playlists.filter { it.mediaType == mediaType.value }.sortedByDescending { it.updatedAt }
    }
    val referenceSheet = Color(0xFFFFF8E7)
    val referenceCard = Color(0xFFFFFFFF)
    val referenceText = Color(0xFF171717)
    val referenceSecondary = Color(0xFF6F6B61)
    val referenceAccent = Color(0xFFFFE58A)
    val referenceAccentDark = Color(0xFF6F5900)
    var selectedPlaylistIds by remember(items) { mutableStateOf(emptySet<String>()) }
    var addToFavorites by remember(items) { mutableStateOf(itemIds.isNotEmpty() && itemIds.all { id -> favorites.any { it.mediaId == id } }) }
    var sortDescending by remember { mutableStateOf(true) }
    var createOpen by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var playlistItemIds by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    LaunchedEffect(compatible.map { it.id }) {
        playlistItemIds = compatible.associate { playlist -> playlist.id to container.structured.playlistIds(playlist.id) }
    }

    val ordered = remember(compatible, sortDescending) {
        if (sortDescending) compatible.sortedByDescending { it.updatedAt } else compatible.sortedBy { it.updatedAt }
    }
    val allSelected = selectedPlaylistIds.isNotEmpty() || addToFavorites

    fun finish() {
        if (working || itemIds.isEmpty()) return
        working = true
        scope.launch {
            if (addToFavorites) {
                items.forEach { item ->
                    if (!container.structured.isFavorite(item.id)) container.structured.toggleFavorite(item)
                }
            }
            selectedPlaylistIds.forEach { playlistId ->
                itemIds.forEach { id -> container.structured.addToPlaylist(playlistId, id) }
            }
            val count = (if (addToFavorites) 1 else 0) + selectedPlaylistIds.size
            AppFeedback.showSnackbar(if (count == 0) "Nothing selected." else "Added to $count destination${if (count == 1) "" else "s"}.")
            working = false
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = referenceSheet,
        tonalElevation = 0.dp,
        dragHandle = { Box(Modifier.padding(top = 8.dp).size(width = 62.dp, height = 5.dp).background(Color(0xFF77746C), RoundedCornerShape(50))) },
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.padding(horizontal = 28.dp)) {
                Text("Add to playlist", color = referenceText, fontFamily = AirPlayrFonts.OpenSans, fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }

            if (items.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = referenceCard,
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Artwork(
                            items.first().displayArtworkUri(),
                            Modifier.size(72.dp), 18.dp, items.first().title, items.first().mediaType,
                            if (mediaType == MediaType.VIDEO) AirPlayrIcons.Video else AirPlayrIcons.Music,
                            isPlaying = false,
                        )
                        Column(Modifier.weight(1f).padding(start = 18.dp)) {
                            Text(
                                items.first().title,
                                color = referenceText,
                                fontFamily = AirPlayrFonts.OpenSans,
                                fontSize = 20.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            Text(
                                items.first().artist.orEmpty().ifBlank { if (mediaType == MediaType.VIDEO) "Video" else "Unknown artist" },
                                color = referenceSecondary,
                                fontFamily = AirPlayrFonts.OpenSans,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            if (items.size > 1) Text("+${items.size - 1} more selected", color = referenceSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                shape = RoundedCornerShape(28.dp),
                color = referenceAccent,
                onClick = { createOpen = true },
            ) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(56.dp), shape = RoundedCornerShape(18.dp), color = Color(0xFF2A2100)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
                    }
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text("Create new playlist", color = referenceText, fontFamily = AirPlayrFonts.OpenSans, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Start with ${if (itemIds.size == 1) "this ${if (mediaType == MediaType.VIDEO) "video" else "song"}" else "these ${if (mediaType == MediaType.VIDEO) "videos" else "songs"}"}", color = referenceSecondary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = referenceText, modifier = Modifier.size(28.dp))
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Your playlists", color = referenceText, fontFamily = AirPlayrFonts.OpenSans, fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = { sortDescending = !sortDescending },
                    shape = RoundedCornerShape(28.dp),
                    color = referenceCard,
                ) {
                    Row(Modifier.padding(horizontal = 17.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("DATE MODIFIED", color = referenceText, fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, letterSpacing = 1.1.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(if (sortDescending) " ↓" else " ↑", color = referenceText, fontSize = 16.sp)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                shape = RoundedCornerShape(28.dp),
                color = referenceCard,
            ) {
                Column {
                    // Favorites is a first-class destination in AirPlayr even though it is backed by
                    // the Favorites table rather than the playlist table.
                    val favoritesSelected = addToFavorites
                    Row(
                        Modifier.fillMaxWidth().clickable { addToFavorites = !addToFavorites }.padding(horizontal = 26.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(Modifier.size(52.dp), shape = RoundedCornerShape(17.dp), color = Color(0xFFC6F0D1)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Favorite, null, tint = Color(0xFF0C5B2A), modifier = Modifier.size(28.dp)) }
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            Text("Favorites", color = referenceText, fontFamily = AirPlayrFonts.OpenSans, fontSize = 18.sp)
                            Text("${favorites.count { it.mediaType == mediaType.value }} ${if (mediaType == MediaType.VIDEO) "Videos" else "Songs"}", color = referenceSecondary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp)
                            if (favoritesSelected) AddedBadge()
                        }
                        SelectionCircle(favoritesSelected, referenceAccentDark)
                    }
                    ordered.forEachIndexed { index, playlist ->
                        if (index > 0) HorizontalDivider(color = Color(0xFFE8E5DD))
                        val selected = selectedPlaylistIds.contains(playlist.id)
                        val ids = playlistItemIds[playlist.id].orEmpty()
                        val alreadyAdded = ids.isNotEmpty() && itemIds.all(ids::contains)
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedPlaylistIds = if (selected) selectedPlaylistIds - playlist.id else selectedPlaylistIds + playlist.id }.padding(horizontal = 26.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val artwork = playlist.artworkUri
                            if (!artwork.isNullOrBlank()) Artwork(artwork, Modifier.size(52.dp), 17.dp, playlist.name, mediaType, if (mediaType == MediaType.VIDEO) AirPlayrIcons.Video else AirPlayrIcons.Music, false)
                            else Surface(Modifier.size(52.dp), shape = RoundedCornerShape(17.dp), color = Color(0xFFDCCCE0)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(if (mediaType == MediaType.VIDEO) Icons.Default.VideoLibrary else Icons.Default.QueueMusic, null, tint = Color(0xFF4D3158), modifier = Modifier.size(27.dp)) } }
                            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                                Text(playlist.name, color = referenceText, fontFamily = AirPlayrFonts.OpenSans, fontSize = 18.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text("${ids.size} ${if (mediaType == MediaType.VIDEO) "Videos" else "Songs"} · ${java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(java.util.Date(playlist.updatedAt))}", color = referenceSecondary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp)
                                if (alreadyAdded) AddedBadge()
                            }
                            SelectionCircle(selected, referenceAccentDark)
                        }
                    }
                    if (ordered.isEmpty()) {
                        Text("No ${if (mediaType == MediaType.VIDEO) "video" else "music"} playlists yet. Create one above.", color = referenceSecondary, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0E8D4), contentColor = referenceAccentDark),
                ) { Text("Cancel", fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
                Button(
                    onClick = ::finish,
                    enabled = allSelected && !working,
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B6200), contentColor = Color.White),
                ) { Text(if (working) "Adding…" else "Done", fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    if (createOpen) {
        AlertDialog(
            onDismissRequest = { if (!working) createOpen = false },
            title = { Text("Create new playlist") },
            text = { OutlinedTextField(newName, { newName = it }, label = { Text(if (mediaType == MediaType.VIDEO) "Video playlist name" else "Playlist name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(enabled = newName.trim().isNotEmpty() && !working, onClick = {
                    working = true
                    scope.launch {
                        val playlist = container.structured.createPlaylist(newName.trim(), mediaType.value)
                        itemIds.forEach { container.structured.addToPlaylist(playlist.id, it) }
                        AppFeedback.showSnackbar("Added to ${playlist.name}.")
                        working = false
                        createOpen = false
                        onDismiss()
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { createOpen = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SelectionCircle(selected: Boolean, accent: Color) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(2.dp, if (selected) accent else Color(0xFF8D897D)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (selected) Surface(Modifier.size(18.dp), shape = androidx.compose.foundation.shape.CircleShape, color = accent) {}
        }
    }
}

@Composable
private fun AddedBadge() {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE3EEE7), modifier = Modifier.padding(top = 4.dp)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("✓", color = Color(0xFF527363), fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text("ALREADY ADDED", color = Color(0xFF527363), fontSize = 10.sp, letterSpacing = 0.7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
                if (item.mediaType == MediaType.VIDEO) Modifier.width(140.dp).height(79.dp) else Modifier.size(110.dp),
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
    val service = container.lyricsRepository
    var lyrics by remember(item.id) { mutableStateOf("") }
    var loading by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) { lyrics = withContext(Dispatchers.IO) { service.getCached(item.id)?.plainLyrics.orEmpty() } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Edit lyrics", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 24.sp, color = colors.textPrimary)
            Text(item.title, color = colors.textPrimary)
            OutlinedTextField(lyrics, { lyrics = it }, Modifier.fillMaxWidth().height(260.dp), label = { Text("Lyrics") })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !loading, onClick = { scope.launch { loading = true; val result = withContext(Dispatchers.IO) { service.fetch(item.id, LyricsTrack(item.title, item.artist.orEmpty(), item.album.orEmpty(), item.durationMs / 1000.0), true) }; lyrics = result?.plainLyrics.orEmpty(); loading = false } }, modifier = Modifier.weight(1f)) { Text(if (loading) "Searching…" else "Fetch") }
                Button(onClick = { scope.launch {
                    val old = withContext(Dispatchers.IO) { service.getCached(item.id) }
                    val syncedText = if (Regex("\\[\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?\\]").containsMatchIn(lyrics)) lyrics else null
                    val result = LyricsService.Result("AirPlayr", item.title, item.artist.orEmpty(), item.album.orEmpty(), item.durationMs / 1000.0, lyrics.ifBlank { null }, syncedText, if (syncedText != null) service.parseLrc(syncedText) else emptyList(), old?.confidence ?: 0)
                    withContext(Dispatchers.IO) { service.save(item.id, result) }
                    AppFeedback.showSnackbar("Lyrics saved.")
                    onDismiss()
                } }, modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}
