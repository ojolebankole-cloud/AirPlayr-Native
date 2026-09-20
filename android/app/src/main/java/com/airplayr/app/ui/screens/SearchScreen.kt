@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.airplayr.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.lyrics.LyricsService
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.AirPlayrSheetScaffold
import com.airplayr.app.ui.components.AirPlayrMediaArtwork
import com.airplayr.app.ui.components.Artwork
import com.airplayr.app.ui.components.displayArtworkUri
import com.airplayr.app.ui.feedback.AppFeedback
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackControllerFactory
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private data class SearchEntry(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val item: MediaItem? = null,
    val routeKey: String? = null,
)

private val FILTERS = listOf(
    "all" to "All",
    "songs" to "Songs",
    "albums" to "Albums",
    "artists" to "Artists",
    "playlists" to "Playlists",
    "genres" to "Genres",
    "videos" to "Videos",
)

private fun normalize(value: String?): String =
    java.text.Normalizer.normalize(value.orEmpty().lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

private fun matches(entry: SearchEntry, query: String): Boolean {
    val tokens = normalize(query).split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return true
    val values = listOf(entry.title, entry.subtitle, entry.item?.artist, entry.item?.album, entry.item?.genre, entry.item?.filename)
        .filterNotNull().map(::normalize)
    return tokens.all { token -> values.any { it.contains(token) } }
}

@Composable
fun SearchScreen(
    container: AppContainer,
    libraryViewModel: LibraryViewModel,
    onOpen: (MediaItem) -> Unit,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
    val focus = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val playback: PlaybackController = viewModel(factory = PlaybackControllerFactory(container.appContext))
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var playlistTarget by remember { mutableStateOf<MediaItem?>(null) }
    var detailsSheet by remember { mutableStateOf<MediaItem?>(null) }
    var deletePending by remember { mutableStateOf<MediaItem?>(null) }
    var lyricsTarget by remember { mutableStateOf<MediaItem?>(null) }
    var tagTarget by remember { mutableStateOf<MediaItem?>(null) }

    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val audio = snapshot.audio
    val video = snapshot.video
    val history = snapshot.history
    val playlists = snapshot.playlists
    val favoriteIds = snapshot.favorites
    val favoriteIdSet = remember(favoriteIds) { favoriteIds.map { it.mediaId }.toSet() }
    val playbackState by playback.stableState.collectAsStateWithLifecycle()

    LaunchedEffect(query) {
        delay(180)
        debounced = query.trim()
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val lookup = remember(audio, video) { (audio + video).associateBy { it.id } }
    val entries = remember(snapshot.audio, snapshot.video, snapshot.playlists) {
        val songs = audio.map {
            SearchEntry(it.id, "song", it.title.ifBlank { it.filename }, it.artist ?: "Unknown artist", it)
        }
        val albums = audio.filter { !it.album.isNullOrBlank() }
            .groupBy { (it.album ?: "").trim().lowercase(Locale.ROOT) }
            .values.map { group ->
                val first = group.first()
                SearchEntry("album:${group.first().album}", "album", group.first().album.orEmpty(),
                    "${first.artist ?: "Unknown artist"} · ${group.size} songs", first, group.first().album)
            }
        val artists = audio.filter { !it.artist.isNullOrBlank() }
            .groupBy { (it.artist ?: "").trim().lowercase(Locale.ROOT) }
            .values.map { group ->
                val first = group.first()
                SearchEntry("artist:${group.first().artist}", "artist", group.first().artist.orEmpty(),
                    "${group.size} songs", first, group.first().artist)
            }
        val genres = audio.filter { !it.genre.isNullOrBlank() }
            .groupBy { (it.genre ?: "").trim().lowercase(Locale.ROOT) }
            .values.map { group ->
                val first = group.first()
                SearchEntry("genre:${group.first().genre}", "genre", group.first().genre.orEmpty(),
                    "${group.size} songs", first, group.first().genre)
            }
        val pls = playlists.map {
            SearchEntry("playlist:${it.id}", "playlist", it.name, "Playlist", null, it.id)
        }
        val videos = video.map {
            SearchEntry(it.id, "video", it.title.ifBlank { it.filename }, it.artist ?: "Video", it)
        }
        songs + albums + artists + pls + genres + videos
    }

    val recentItems = remember(snapshot.history, snapshot.audio, lookup) {
        val recentArtists = mutableListOf<SearchEntry>()
        val seenArtists = mutableSetOf<String>()
        history.forEach { h ->
            val artistName = h.artist?.trim().takeUnless { it.isNullOrBlank() } ?: return@forEach
            val key = normalize(artistName)
            if (!seenArtists.add(key)) return@forEach
            val first = audio.firstOrNull { normalize(it.artist) == key } ?: return@forEach
            recentArtists += SearchEntry(
                "artist:${first.artist}",
                "artist",
                first.artist.orEmpty(),
                "${audio.count { normalize(it.artist) == key }} songs",
                first,
                first.artist,
            )
            if (recentArtists.size == 3) return@forEach
        }
        val recentMedia = history.take(8).mapNotNull { h ->
            lookup[h.mediaId]?.let {
                SearchEntry(
                    it.id,
                    if (it.mediaType == MediaType.VIDEO) "video" else "song",
                    it.title.ifBlank { it.filename },
                    it.artist ?: if (it.mediaType == MediaType.VIDEO) "Video" else "Unknown artist",
                    it,
                )
            }
        }
        (recentArtists + recentMedia).distinctBy { "${it.type}:${it.id}" }
    }

    val filtered = remember(entries, debounced, filter) {
        val base = when (filter) {
            "songs" -> entries.filter { it.type == "song" }
            "albums" -> entries.filter { it.type == "album" }
            "artists" -> entries.filter { it.type == "artist" }
            "playlists" -> entries.filter { it.type == "playlist" }
            "genres" -> entries.filter { it.type == "genre" }
            "videos" -> entries.filter { it.type == "video" }
            else -> entries
        }
        if (debounced.isBlank()) base else base.filter { matches(it, debounced) }
    }
    val display = if (debounced.isBlank()) {
        if (recentItems.isNotEmpty()) recentItems else filtered.take(12)
    } else filtered

    fun openEntry(entry: SearchEntry) {
        when (entry.type) {
            "song", "video" -> entry.item?.let(onOpen)
            "album" -> entry.routeKey?.let { onNavigate("album/${Uri.encode(it)}") }
            "artist" -> entry.routeKey?.let { onNavigate("artist/${Uri.encode(it)}") }
            "genre" -> entry.routeKey?.let { onNavigate("genre/${Uri.encode(it)}") }
            "playlist" -> entry.routeKey?.let { onNavigate("music-playlist/$it") }
        }
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ChevronLeft, "Back", tint = colors.primary)
            }
            Surface(
                Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceContainerHigh,
                tonalElevation = 0.dp,
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = colors.primary, modifier = Modifier.size(21.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp).focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            color = colors.textPrimary, fontSize = 17.sp, fontFamily = AirPlayrFonts.OpenSans
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text(
                                "Search your music",
                                color = colors.textSecondary,
                                fontSize = 15.sp,
                                fontFamily = AirPlayrFonts.SmoochSans,
                            )
                            inner()
                        },
                    )
                    if (query.isNotEmpty()) IconButton(
                        onClick = { query = ""; focus.clearFocus() },
                        modifier = Modifier.size(32.dp),
                    ) { Icon(Icons.Default.Close, "Clear", tint = colors.primary) }
                }
            }
        }

        Row(
            Modifier.horizontalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FILTERS.forEach { (key, label) ->
                val active = filter == key
                FilterChip(
                    selected = active,
                    onClick = { filter = key },
                    label = { Text(label, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 15.sp) },
                    shape = RoundedCornerShape(23.dp),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (debounced.isNotBlank()) {
                            if (filter == "all") "RESULTS" else FILTERS.first { it.first == filter }.second.uppercase(Locale.ROOT)
                        } else if (recentItems.isNotEmpty()) "RECENTLY PLAYED" else "YOUR LIBRARY",
                        fontFamily = AirPlayrFonts.SmoochSans,
                        fontSize = 12.sp,
                        letterSpacing = 1.1.sp,
                        color = colors.textPrimary,
                    )
                    if (debounced.isNotBlank()) Text(
                        "${filtered.size}",
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (!snapshot.ready) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 56.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
            } else items(display, key = { "${it.type}:${it.id}" }) { entry ->
                val playing = entry.item?.let { it.mediaType == MediaType.AUDIO && playbackState.isPlaying && playbackState.currentId == it.id } == true
                SearchResultRow(
                    entry = entry,
                    colors = colors,
                    playing = playing,
                    onClick = { openEntry(entry) },
                    onMore = { entry.item?.let { selected = it } },
                )
            }
            if (snapshot.ready && display.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Search, null, Modifier.size(42.dp), tint = colors.primary)
                        Text(
                            if (debounced.isNotBlank()) "No results found" else "Your library is empty",
                            fontFamily = AirPlayrFonts.SmoochSans,
                            fontSize = 22.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Text(
                            if (debounced.isNotBlank()) "Nothing matched “$debounced”." else "Your recently played and available media will appear here.",
                            fontFamily = AirPlayrFonts.OpenSans,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }
        }
    }

    if (selected != null) {
        SearchActionSheet(
            item = selected!!,
            favorite = favoriteIdSet.contains(selected!!.id),
            playing = playbackState.isPlaying && playbackState.currentId == selected!!.id,
            container = container,
            onClose = { selected = null },
            onPlayNext = {
                selected?.let { item ->
                    selected = null
                    playback.playNext(item)
                    AppFeedback.showSnackbar("Added to playing queue as next.")
                }
            },
            onAddQueue = {
                selected?.let { item ->
                    selected = null
                    playback.addToQueue(item)
                    AppFeedback.showSnackbar("Added to the playing queue.")
                }
            },
            onFavorite = {
                selected?.let { item ->
                    scope.launch {
                        val active = container.structured.toggleFavorite(item)
                        AppFeedback.showSnackbar(if (active) "Added to favorites." else "Removed from favorites.")
                        selected = null
                    }
                }
            },
            onPlaylist = { playlistTarget = selected; selected = null },
            onDetails = { detailsSheet = selected; selected = null },
            onDelete = { deletePending = selected; selected = null },
            onGoAlbum = {
                selected?.album?.let { onNavigate("album/${Uri.encode(it)}") }
                selected = null
            },
            onGoArtist = {
                selected?.artist?.let { onNavigate("artist/${Uri.encode(it)}") }
                selected = null
            },
            onGoFolder = {
                selected?.relativePath?.takeIf { it.isNotBlank() }?.let { onNavigate("music-folder/${Uri.encode(it)}") }
                selected = null
            },
            onEditMetadata = {
                val target = selected
                selected = null
                tagTarget = target
            },
            onEditLyrics = {
                val target = selected
                selected = null
                lyricsTarget = target
            },
        )
    }

    playlistTarget?.let { item ->
        SearchPlaylistSheet(
            container = container,
            item = item,
            onDismiss = { playlistTarget = null },
        )
    }

    lyricsTarget?.let { item ->
        SearchLyricsSheet(
            container = container,
            item = item,
            onDismiss = { lyricsTarget = null },
        )
    }

    tagTarget?.let { item ->
        SearchTagEditorSheet(
            container = container,
            item = item,
            onDismiss = { tagTarget = null },
        )
    }

    detailsSheet?.let { item ->
        AirPlayrSheetScaffold("Details", "MEDIA", onDismiss = { detailsSheet = null }) {
            Artwork(
                item.displayArtworkUri(),
                Modifier.size(92.dp),
                18.dp,
                item.title,
                item.mediaType,
                if (item.mediaType == MediaType.VIDEO) AirPlayrIcons.Video else AirPlayrIcons.Music,
                isPlaying = playbackState.isPlaying && playbackState.currentId == item.id,
            )
            Text(item.title, style = MaterialTheme.typography.titleLarge)
            Text(listOfNotNull(item.artist, item.album, item.genre).joinToString(" • ").ifBlank { item.filename }, color = colors.textSecondary)
            Text("${item.sizeBytes / 1024 / 1024} MB · ${item.durationMs / 1000}s", color = colors.textSecondary)
        }
    }

    deletePending?.let { item ->
        AlertDialog(
            onDismissRequest = { deletePending = null },
            title = { Text("Delete from device?") },
            text = { Text("Delete ${item.title} from this device. Android may ask you to confirm the deletion.") },
            dismissButton = { TextButton(onClick = { deletePending = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = {
                    deletePending = null
                    scope.launch(Dispatchers.IO) {
                        val ok = runCatching {
                            val base = if (item.mediaType == MediaType.AUDIO) android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            container.appContext.contentResolver.delete(
                                Uri.withAppendedPath(base, item.mediaStoreId?.toString() ?: return@runCatching false),
                                null, null
                            ) > 0
                        }.getOrDefault(false)
                        if (ok) runCatching { container.mediaLibrary.deltaRefresh(item.mediaType) }
                        withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                            if (ok) {
                                AppFeedback.showSnackbar("${item.title} deleted from device.")
                            } else {
                                AppFeedback.showSnackbar("Unable to delete ${item.title}.")
                            }
                        }
                    }
                }) { Text("Delete", color = colors.error) }
            },
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val total = (durationMs / 1000L).coerceAtLeast(0L)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

@Composable
private fun SearchResultRow(
    entry: SearchEntry,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    playing: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entry.item != null) {
            AirPlayrMediaArtwork(
                artworkUri = entry.item.displayArtworkUri(),
                modifier = Modifier.size(64.dp),
                radius = 16.dp,
                title = entry.title,
                mediaType = entry.item.mediaType,
                fallbackIcon = when (entry.type) {
                    "artist" -> AirPlayrIcons.Artist
                    "album" -> AirPlayrIcons.AlbumDisc
                    "video" -> AirPlayrIcons.Video
                    else -> AirPlayrIcons.Music
                },
                playing = playing,
                onPlay = if (entry.type == "song" || entry.type == "video") onClick else null,
                onMore = if (entry.type == "song" || entry.type == "video") onMore else null,
            )
        } else {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when (entry.type) {
                        "playlist" -> Icons.Default.QueueMusic
                        "genre" -> Icons.Default.LocalOffer
                        "artist" -> AirPlayrIcons.Artist
                        "album" -> AirPlayrIcons.AlbumDisc
                        else -> AirPlayrIcons.Music
                    },
                    null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(entry.title, color = colors.textPrimary, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.subtitle, color = colors.textPrimary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SearchActionSheet(
    item: MediaItem,
    favorite: Boolean,
    playing: Boolean,
    container: AppContainer,
    onClose: () -> Unit,
    onPlayNext: () -> Unit,
    onAddQueue: () -> Unit,
    onFavorite: () -> Unit,
    onPlaylist: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
    onGoAlbum: () -> Unit,
    onGoArtist: () -> Unit,
    onGoFolder: () -> Unit,
    onEditMetadata: () -> Unit,
    onEditLyrics: () -> Unit,
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
            Artwork(
                item.displayArtworkUri(),
                Modifier.size(64.dp),
                14.dp,
                item.title,
                item.mediaType,
                if (item.mediaType == MediaType.VIDEO) AirPlayrIcons.Video else AirPlayrIcons.Music,
                isPlaying = playing,
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(item.title.ifBlank { if (isVideo) "Video" else "Song" }, color = colors.textPrimary, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (isVideo) "${item.artist ?: "Video"}${if (item.durationMs > 0) " · ${formatDuration(item.durationMs)}" else ""}"
                    else listOfNotNull(item.artist ?: "Unknown artist", item.album).joinToString(" · "),
                    color = colors.textPrimary,
                    fontFamily = AirPlayrFonts.OpenSans,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (favorite) "Remove from favorites" else "Add to favorites", tint = colors.primary)
            }
        }

        ActionGroup {
            ActionButton("Play next", Icons.Default.PlaylistPlay, onPlayNext)
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
                ActionButton("Tag editor", Icons.Default.Edit, onEditMetadata)
                ActionButton("Edit lyrics", AirPlayrIcons.Lyrics, onEditLyrics)
            }
        }

            ActionGroup {
                ActionButton("Details", Icons.Default.Info, onDetails)
                ActionButton("Share", Icons.Default.Share, {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = item.mimeType ?: "text/plain"
                        putExtra(Intent.EXTRA_TEXT, item.uri)
                        putExtra(Intent.EXTRA_TITLE, item.title)
                    }
                    runCatching {
                        container.appContext.startActivity(
                            Intent.createChooser(send, "Share ${item.title}")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    onClose()
                })
                ActionButton("Delete from device", Icons.Default.Delete, onDelete, destructive = true)
            }
        }
    }
}

@Composable
private fun ActionGroup(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAirPlayrColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), content = content)
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, destructive: Boolean = false) {
    val colors = LocalAirPlayrColors.current
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (destructive) colors.error else colors.primary)
            Spacer(Modifier.width(14.dp))
            Text(label, color = if (destructive) colors.error else colors.textPrimary)
        }
    }
}


@Composable
private fun SearchTagEditorSheet(
    container: AppContainer,
    item: MediaItem,
    onDismiss: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val scope = rememberCoroutineScope()
    var title by remember(item.id) { mutableStateOf(item.title) }
    var artist by remember(item.id) { mutableStateOf(item.artist.orEmpty()) }
    var album by remember(item.id) { mutableStateOf(item.album.orEmpty()) }
    var genre by remember(item.id) { mutableStateOf(item.genre.orEmpty()) }

    AirPlayrSheetScaffold("Tag editor", "MUSIC", onDismiss) {
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
        OutlinedTextField(artist, { artist = it }, Modifier.fillMaxWidth(), label = { Text("Artist") }, singleLine = true)
        OutlinedTextField(album, { album = it }, Modifier.fillMaxWidth(), label = { Text("Album") }, singleLine = true)
        OutlinedTextField(genre, { genre = it }, Modifier.fillMaxWidth(), label = { Text("Genre") }, singleLine = true)
        Button(
            onClick = {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            val id = item.mediaStoreId ?: return@runCatching false
                            val base = if (item.mediaType == MediaType.AUDIO) android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            val values = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.TITLE, title.trim())
                                if (item.mediaType == MediaType.AUDIO) {
                                    put(android.provider.MediaStore.Audio.AudioColumns.ARTIST, artist.trim())
                                    put(android.provider.MediaStore.Audio.AudioColumns.ALBUM, album.trim())
                                    put("genre", genre.trim())
                                }
                            }
                            container.appContext.contentResolver.update(
                                Uri.withAppendedPath(base, id.toString()), values, null, null
                            ) > 0
                        }.getOrDefault(false)
                    }
                    if (ok) {
                        withContext(Dispatchers.IO) { runCatching { container.mediaLibrary.deltaRefresh(item.mediaType) } }
                        AppFeedback.showSnackbar("Metadata saved.")
                        onDismiss()
                    } else {
                        AppFeedback.showSnackbar("Metadata could not be saved.")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save changes") }
        Text("Changes are written to the device media library when Android permits the update.", color = colors.textSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun SearchLyricsSheet(
    container: AppContainer,
    item: MediaItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalAirPlayrColors.current
    val scope = rememberCoroutineScope()
    val service = remember(item.id) { LyricsService(context) }
    var lyrics by remember(item.id) { mutableStateOf("") }
    var status by remember(item.id) { mutableStateOf("") }
    var loading by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(item.id) {
        val cached = withContext(Dispatchers.IO) { service.getCached(item.id) }
        lyrics = cached?.plainLyrics.orEmpty()
        status = if (cached != null) "Saved lyrics loaded" else ""
    }

    AirPlayrSheetScaffold("Edit lyrics", "LYRICS", onDismiss) {
        Text(item.title, style = MaterialTheme.typography.titleLarge)
        Text(item.artist ?: "Unknown artist", color = colors.textSecondary)
        OutlinedTextField(
            value = lyrics,
            onValueChange = { lyrics = it },
            modifier = Modifier.fillMaxWidth().height(260.dp),
            label = { Text("Lyrics") },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        loading = true
                        status = "Searching…"
                        val result = withContext(Dispatchers.IO) {
                            service.fetch(item.id, item.title, item.artist.orEmpty(), item.album, item.durationMs / 1000.0, true)
                        }
                        lyrics = result?.plainLyrics.orEmpty()
                        status = if (result != null) "Lyrics found" else "Lyrics not found"
                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier.weight(1f),
            ) { Text(if (loading) "Searching…" else "Fetch") }
            Button(
                onClick = {
                    scope.launch {
                        val old = withContext(Dispatchers.IO) { service.getCached(item.id) }
                        val result = LyricsService.Result(
                            provider = old?.provider ?: "AirPlayr",
                            trackName = item.title,
                            artistName = item.artist.orEmpty(),
                            albumName = item.album.orEmpty(),
                            duration = item.durationMs / 1000.0,
                            plainLyrics = lyrics.ifBlank { null },
                            syncedLyrics = old?.syncedLyrics,
                            cues = old?.cues.orEmpty(),
                            confidence = old?.confidence ?: 0,
                        )
                        withContext(Dispatchers.IO) { service.save(item.id, result) }
                        status = "Lyrics saved"
                        AppFeedback.showSnackbar("Lyrics saved.")
                        onDismiss()
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Save") }
        }
        if (status.isNotBlank()) Text(status, color = colors.textSecondary)
    }
}

@Composable
private fun SearchPlaylistSheet(
    container: AppContainer,
    item: MediaItem,
    onDismiss: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val scope = rememberCoroutineScope()
    val playlists by container.structured.playlists().collectAsStateWithLifecycle(emptyList())
    var newName by remember { mutableStateOf("") }

    AirPlayrSheetScaffold("Add to playlist", "PLAYLIST", onDismiss) {
        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("New playlist") },
            singleLine = true,
        )
        Button(
            onClick = {
                scope.launch {
                    val playlist = container.structured.createPlaylist(newName)
                    container.structured.addToPlaylist(playlist.id, item.id)
                    AppFeedback.showSnackbar("Added to ${playlist.name}.")
                    newName = ""
                    onDismiss()
                }
            },
            enabled = newName.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Create playlist") }

        if (playlists.isNotEmpty()) {
            Text("YOUR PLAYLISTS", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 12.sp, letterSpacing = 1.1.sp)
            playlists.forEach { playlist ->
                TextButton(
                    onClick = {
                        scope.launch {
                            container.structured.addToPlaylist(playlist.id, item.id)
                            AppFeedback.showSnackbar("Added to ${playlist.name}.")
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QueueMusic, null, tint = colors.primary)
                        Spacer(Modifier.width(14.dp))
                        Text(playlist.name, color = colors.textPrimary)
                    }
                }
            }
        }
    }
}
