@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.airplayr.app.ui.screens
import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.db.MusicPlaylistEntity
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.airPlayrBottomContentPadding
import com.airplayr.app.ui.components.AirPlayrCard
import com.airplayr.app.ui.components.AirPlayrMediaMenuAction
import com.airplayr.app.ui.components.AirPlayrMoreButton
import com.airplayr.app.ui.components.AirPlayrPremiumMediaActionSheet
import com.airplayr.app.ui.components.Artwork
import com.airplayr.app.ui.components.TagEditorSheet
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.theme.AirPlayrArtworkTokens
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val MUSIC_CATEGORIES = listOf(
    "songs" to "Songs",
    "likes" to "Likes",
    "albums" to "Albums",
    "playlists" to "Playlists",
    "artists" to "Artists",
    "genres" to "Genres",
    "folders" to "Folders",
)

private val MUSIC_SORTS = listOf(
    "recent" to "Recently added",
    "name" to "Name",
    "artist" to "Artist",
    "album" to "Album",
    "genre" to "Genre",
    "duration" to "Duration",
    "modified" to "Date modified",
)

private fun musicSortsFor(category: String): List<Pair<String, String>> = when (category) {
    "songs", "likes" -> MUSIC_SORTS
    "albums" -> MUSIC_SORTS.filter { it.first in setOf("recent", "name", "artist", "duration", "modified") }
    "artists" -> MUSIC_SORTS.filter { it.first in setOf("recent", "name") }
    "genres" -> MUSIC_SORTS.filter { it.first in setOf("recent", "name", "duration") }
    "folders" -> MUSIC_SORTS.filter { it.first in setOf("recent", "name", "modified") }
    "playlists" -> MUSIC_SORTS.filter { it.first in setOf("recent", "name") }
    else -> MUSIC_SORTS
}

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    libraryViewModel: LibraryViewModel,
    container: AppContainer,
    playback: PlaybackController,
    onNavigate: (String) -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val librarySnapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val songs = librarySnapshot.audio
    val favorites = librarySnapshot.favorites
    val playlists = librarySnapshot.playlists
    val playbackState by playback.stableState.collectAsStateWithLifecycle()

    var category by rememberSaveable { mutableStateOf("songs") }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var sortKey by rememberSaveable(settings?.librarySort) { mutableStateOf(settings?.librarySort ?: "recent") }
    var sortAscending by rememberSaveable(settings?.librarySortOrder) { mutableStateOf(settings?.librarySortOrder == "asc") }
    var grid by rememberSaveable(settings?.libraryLayout) { mutableStateOf(settings?.libraryLayout != "list") }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var moreFor by remember { mutableStateOf<MediaItem?>(null) }
    var playlistDialogFor by remember { mutableStateOf<MediaItem?>(null) }
    var editFor by remember { mutableStateOf<MediaItem?>(null) }
    var sortSheetOpen by remember { mutableStateOf(false) }
    val playlistCounts = remember { mutableStateMapOf<String, Int>() }
    val playlistItems = remember { mutableStateMapOf<String, List<String>>() }
    val scope = rememberCoroutineScope()
    val songListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) }

    LaunchedEffect(playlists, category) {
        if (category != "playlists") return@LaunchedEffect
        playlists.forEach { playlist ->
            val ids = container.structured.playlistIds(playlist.id)
            playlistItems[playlist.id] = ids
            playlistCounts[playlist.id] = ids.size
        }
    }

    LaunchedEffect(settings?.librarySort, settings?.librarySortOrder) {
        settings?.let {
            sortKey = it.librarySort
            sortAscending = it.librarySortOrder == "asc"
        }
    }

    val filter = query.trim()
    val categorySorts = remember(category) { musicSortsFor(category) }
    val effectiveSortKey = sortKey.takeIf { key -> categorySorts.any { it.first == key } } ?: "recent"
    val favoriteIds = remember(favorites) { favorites.map { it.mediaId }.toSet() }
    val filteredSongs = remember(songs, category, favoriteIds, filter, effectiveSortKey, sortAscending) {
        val base = if (category == "likes") songs.filter { it.id in favoriteIds } else songs
        sortMedia(
            base.filter { mediaMatches(it, filter) },
            effectiveSortKey,
            sortAscending,
        )
    }

    // Collection groupings are lazy: opening Songs should not group the entire library into
    // albums/artists/genres/folders before the user asks for those categories.
    val albums = remember(songs, filter, effectiveSortKey, sortAscending) {
        lazy {
            songs.filter { !it.album.isNullOrBlank() }
                .groupBy { it.album!!.trim() }
                .map { AlbumGroup(it.key, it.value) }
                .filter { filter.isBlank() || it.name.contains(filter, true) || it.artist.contains(filter, true) }
                .let { sortAlbumGroups(it, effectiveSortKey, sortAscending) }
        }
    }
    val artists = remember(songs, filter, effectiveSortKey, sortAscending) {
        lazy {
            songs.filter { !it.artist.isNullOrBlank() }
                .groupBy { it.artist!!.trim() }
                .map { CollectionGroup(it.key, it.value) }
                .filter { filter.isBlank() || it.name.contains(filter, true) }
                .let { sortCollectionGroups(it, effectiveSortKey, sortAscending) }
        }
    }
    val genres = remember(songs, filter, effectiveSortKey, sortAscending) {
        lazy {
            songs.groupBy { it.genre?.trim().takeUnless { genre -> genre.isNullOrBlank() } ?: "Other" }
                .map { CollectionGroup(it.key, it.value) }
                .filter { filter.isBlank() || it.name.contains(filter, true) }
                .let { sortCollectionGroups(it, effectiveSortKey, sortAscending) }
        }
    }
    val folders = remember(songs, filter, effectiveSortKey, sortAscending) {
        lazy {
            songs.groupBy { it.folder?.trim().takeUnless { folder -> folder.isNullOrBlank() } ?: "Music" }
                .map { CollectionGroup(it.key, it.value) }
                .filter { filter.isBlank() || it.name.contains(filter, true) }
                .let { sortCollectionGroups(it, effectiveSortKey, sortAscending) }
        }
    }
    val orderedPlaylists = remember(playlists, effectiveSortKey, sortAscending) {
        val sorted = when (effectiveSortKey) {
            "recent" -> playlists.sortedBy { it.updatedAt }
            else -> playlists.sortedBy { it.name.lowercase() }
        }
        if (sortAscending) sorted else sorted.asReversed()
    }

    fun play(items: List<MediaItem>, start: MediaItem? = items.firstOrNull()) {
        if (items.isNotEmpty() && start != null) {
            playback.playQueue(items, items.indexOfFirst { it.id == start.id }.coerceAtLeast(0))
        }
    }

    fun setSort(key: String, ascending: Boolean = sortAscending) {
        sortKey = key
        sortAscending = ascending
        scope.launch {
            container.preferences.setLibrarySort(key)
            container.preferences.setLibrarySortOrder(if (ascending) "asc" else "desc")
        }
        sortSheetOpen = false
    }

    Column(
        Modifier.fillMaxSize().background(colors.background),
    ) {
        MusicHeader(
            searchOpen = searchOpen,
            query = query,
            onQuery = { query = it },
            onSearch = { searchOpen = !searchOpen; if (!searchOpen) query = "" },
            onBack = { onNavigate("home") },
            grid = grid,
            onGrid = { value ->
                grid = value
                scope.launch { container.preferences.setLibraryLayout(if (value) "grid" else "list") }
            },
        )

        Row(
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberSaveable(saver = androidx.compose.foundation.ScrollState.Saver) { androidx.compose.foundation.ScrollState(0) })
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MUSIC_CATEGORIES.forEach { (key, label) ->
                FilterChip(
                    selected = category == key,
                    onClick = { category = key; selected = emptySet() },
                    label = { Text(label, fontWeight = if (category == key) FontWeight.SemiBold else FontWeight.Normal) },
                )
            }
        }

        MusicLibraryControlBar(
            category = category,
            count = when (category) {
                "songs", "likes" -> filteredSongs.size
                "albums" -> albums.value.size
                "artists" -> artists.value.size
                "genres" -> genres.value.size
                "folders" -> folders.value.size
                else -> playlists.size
            },
            sortKey = effectiveSortKey,
            sortAscending = sortAscending,
            grid = grid,
            onSort = { sortSheetOpen = true },
            onShuffle = if (category == "songs" || category == "likes") ({ play(filteredSongs.shuffled()) }) else null,
            onGrid = { value ->
                grid = value
                scope.launch { container.preferences.setLibraryLayout(if (value) "grid" else "list") }
            },
        )

        when (category) {
            "songs", "likes" -> {
                if (selected.isNotEmpty()) SelectionBar(
                    count = selected.size,
                    onClear = { selected = emptySet() },
                    onSelectAll = { selected = filteredSongs.map { it.id }.toSet() },
                    onPlay = {
                        val chosen = filteredSongs.filter { it.id in selected }
                        if (chosen.isNotEmpty()) { play(chosen); selected = emptySet() }
                    },
                    onPlayNext = { filteredSongs.filter { it.id in selected }.forEach(playback::playNext); selected = emptySet() },
                    onPlaylist = { filteredSongs.firstOrNull { it.id in selected }?.let { playlistDialogFor = it } },
                    onLike = { scope.launch { filteredSongs.filter { it.id in selected }.forEach { if (it.id !in favoriteIds) container.structured.toggleFavorite(it) }; selected = emptySet() } },
                    onEdit = { filteredSongs.firstOrNull { it.id in selected }?.let { editFor = it } },
                )

                if (grid) {
                    val songGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
                    Box(Modifier.weight(1f)) {
                    LazyVerticalGrid(
                        state = songGridState,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, top = 6.dp, end = 12.dp, bottom = airPlayrBottomContentPadding()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (filteredSongs.isEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                EmptyMusic("No songs found", if (category == "likes") "Like a song to see it here." else "Music on this device will appear here.")
                            }
                        }
                        items(filteredSongs, key = { it.id }) { item ->
                            NativeMusicTile(
                                item = item,
                                favorite = item.id in favoriteIds,
                                playing = playbackState.currentId == item.id && playbackState.isPlaying,
                                selected = item.id in selected,
                                onPress = { if (selected.isNotEmpty()) selected = toggleSelection(selected, item.id) else play(filteredSongs, item) },
                                onLongPress = { selected = toggleSelection(selected, item.id) },
                                onFavorite = { scope.launch { container.structured.toggleFavorite(item) } },
                                onMore = { moreFor = item },
                            )
                        }
                    }
                    AirPlayrFastScrollGridHandle(songGridState)
                    }
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = songListState,
                            contentPadding = PaddingValues(start = 10.dp, top = 6.dp, end = 10.dp, bottom = airPlayrBottomContentPadding()),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            musicSongItems(
                                filteredSongs = filteredSongs,
                                favoriteIds = favoriteIds,
                                selected = selected,
                                playbackState = playbackState,
                                onPress = { item -> if (selected.isNotEmpty()) selected = toggleSelection(selected, item.id) else play(filteredSongs, item) },
                                onLongPress = { item -> selected = toggleSelection(selected, item.id) },
                                onFavorite = { item -> scope.launch { container.structured.toggleFavorite(item) } },
                                onMore = { moreFor = it },
                            )
                        }
                        AirPlayrFastScrollHandle(songListState)
                    }
                }
            }
            "albums" -> CollectionGrid(
                grid = grid,
                data = albums.value,
                circular = false,
                onClick = { raw -> val name = rawName(raw); onNavigate("album/${encode(name)}") },
                modifier = Modifier.weight(1f),
            )
            "artists" -> CollectionGrid(
                grid = grid,
                data = artists.value,
                circular = true,
                onClick = { raw -> val name = rawName(raw); onNavigate("artist/${encode(name)}") },
                modifier = Modifier.weight(1f),
            )
            "genres" -> GenreGrid(genres.value, onNavigate, modifier = Modifier.weight(1f))
            "folders" -> CollectionGrid(
                grid = grid,
                data = folders.value,
                circular = false,
                onClick = { raw -> val name = rawName(raw); onNavigate("music-folder/${encode(name)}") },
                modifier = Modifier.weight(1f),
            )
            "playlists" -> PlaylistGrid(
                grid = grid,
                data = orderedPlaylists,
                counts = playlistCounts,
                playlistItems = playlistItems,
                songs = songs,
                onNavigate = onNavigate,
                onAdd = { scope.launch { val p = container.structured.createPlaylist("New playlist"); onNavigate("music-playlist/${p.id}") } },
                modifier = Modifier.weight(1f),
            )
        }
    }

    moreFor?.let { item ->
        AirPlayrPremiumMediaActionSheet(
            item = item,
            playing = playbackState.isPlaying && playbackState.currentId == item.id,
            favorite = favoriteIds.contains(item.id),
            onFavorite = { scope.launch { container.structured.toggleFavorite(item) } },
            groups = listOf(
                listOf(
                    AirPlayrMediaMenuAction("Play now", Icons.Default.PlayArrow, { moreFor = null; play(listOf(item), item) }),
                    AirPlayrMediaMenuAction("Play next", Icons.Default.SkipNext, { moreFor = null; playback.playNext(item) }),
                    AirPlayrMediaMenuAction("Add to queue", Icons.Default.QueueMusic, { moreFor = null; playback.addToQueue(item) }),
                ),
                listOf(
                    AirPlayrMediaMenuAction("Add to playlist", Icons.Default.PlaylistAdd, { moreFor = null; playlistDialogFor = item }),
                    AirPlayrMediaMenuAction("Edit song", Icons.Default.Edit, { moreFor = null; editFor = item }),
                ),
            ),
            onDismiss = { moreFor = null },
        )
    }

    playlistDialogFor?.let { item ->
        PlaylistPickerDialog(
            playlists = playlists,
            counts = playlistCounts,
            item = item,
            onDismiss = { playlistDialogFor = null },
            onSelect = { playlistId ->
                scope.launch {
                    container.structured.addToPlaylist(playlistId, item.id)
                    playlistDialogFor = null
                }
            },
        )
    }
    editFor?.let { item ->
        TagEditorSheet(
            appContext,
            item,
            com.airplayr.app.core.metadata.MetadataRepository(appContext),
            { editFor = null },
        ) {
            editFor = null
            libraryViewModel.refresh()
        }
    }

    if (sortSheetOpen) {
        MusicSortSheet(
            sorts = categorySorts,
            selectedKey = effectiveSortKey,
            ascending = sortAscending,
            onSelectKey = { setSort(it) },
            onAscending = { setSort(effectiveSortKey, true) },
            onDescending = { setSort(effectiveSortKey, false) },
            onDismiss = { sortSheetOpen = false },
        )
    }
}

private fun toggleSelection(selected: Set<String>, id: String): Set<String> =
    if (id in selected) selected - id else selected + id

private fun mediaMatches(item: MediaItem, filter: String): Boolean =
    filter.isBlank() || listOf(item.title, item.artist, item.album, item.folder, item.genre)
        .any { value -> value.orEmpty().contains(filter, true) }

private fun sortMedia(items: List<MediaItem>, key: String, ascending: Boolean): List<MediaItem> {
    val sorted = when (key) {
        "artist" -> items.sortedWith(compareBy<MediaItem> { it.artist.orEmpty().lowercase() }.thenBy { it.title.lowercase() })
        "album" -> items.sortedWith(compareBy<MediaItem> { it.album.orEmpty().lowercase() }.thenBy { it.title.lowercase() })
        "genre" -> items.sortedWith(compareBy<MediaItem> { it.genre.orEmpty().lowercase() }.thenBy { it.title.lowercase() })
        "duration" -> items.sortedWith(compareBy<MediaItem> { it.durationMs }.thenBy { it.title.lowercase() })
        "modified" -> items.sortedWith(compareBy<MediaItem> { it.dateModified }.thenBy { it.title.lowercase() })
        "recent" -> items.sortedWith(compareBy<MediaItem> { it.dateAdded }.thenBy { it.title.lowercase() })
        else -> items.sortedBy { it.title.lowercase() }
    }
    return if (ascending) sorted else sorted.asReversed()
}

private fun sortCollectionGroups(values: List<CollectionGroup>, key: String, ascending: Boolean): List<CollectionGroup> {
    val sorted = when (key) {
        "recent" -> values.sortedBy { it.songs.maxOfOrNull { song -> song.dateAdded } ?: 0L }
        "duration" -> values.sortedBy { it.songs.sumOf(MediaItem::durationMs) }
        "modified" -> values.sortedBy { it.songs.maxOfOrNull { song -> song.dateModified } ?: 0L }
        else -> values.sortedBy { it.name.lowercase() }
    }
    return if (ascending) sorted else sorted.asReversed()
}

private fun sortAlbumGroups(values: List<AlbumGroup>, key: String, ascending: Boolean): List<AlbumGroup> {
    val sorted = when (key) {
        "recent" -> values.sortedBy { it.songs.maxOfOrNull { song -> song.dateAdded } ?: 0L }
        "artist" -> values.sortedBy { it.artist.lowercase() }
        "duration" -> values.sortedBy { it.songs.sumOf(MediaItem::durationMs) }
        "modified" -> values.sortedBy { it.songs.maxOfOrNull { song -> song.dateModified } ?: 0L }
        else -> values.sortedBy { it.name.lowercase() }
    }
    return if (ascending) sorted else sorted.asReversed()
}

@Composable
private fun MusicLibraryControlBar(
    category: String,
    count: Int,
    sortKey: String,
    sortAscending: Boolean,
    grid: Boolean,
    onSort: () -> Unit,
    onShuffle: (() -> Unit)?,
    onGrid: (Boolean) -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val sortLabel = MUSIC_SORTS.firstOrNull { it.first == sortKey }?.second ?: "Recently added"
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                when (category) {
                    "likes" -> "$count liked"
                    "albums" -> "$count albums"
                    "artists" -> "$count artists"
                    "genres" -> "$count genres"
                    "folders" -> "$count folders"
                    "playlists" -> "$count playlists"
                    else -> "$count songs"
                },
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                "$sortLabel · ${if (sortAscending) "Ascending" else "Descending"}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        Surface(
            onClick = onSort,
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 0.dp,
            modifier = Modifier.height(42.dp),
        ) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sort, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Text("Sort", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 7.dp))
            }
        }
        if (onShuffle != null) {
            IconButton(onClick = onShuffle) { Icon(Icons.Default.Shuffle, "Shuffle", tint = colors.primary) }
        }
        IconButton(onClick = { onGrid(!grid) }) {
            Icon(if (grid) Icons.Default.GridView else Icons.Default.List, "Layout", tint = colors.primary)
        }
    }
}

@Composable
private fun MusicSortSheet(
    sorts: List<Pair<String, String>>,
    selectedKey: String,
    ascending: Boolean,
    onSelectKey: (String) -> Unit,
    onAscending: () -> Unit,
    onDescending: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("SORT YOUR LIBRARY", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, letterSpacing = 1.2.sp, color = colors.textSecondary)
            Text("Choose how Music is arranged", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            sorts.forEach { (key, label) ->
                Surface(
                    onClick = { onSelectKey(key) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (key == selectedKey) colors.primaryContainer else colors.surfaceContainer,
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (key == selectedKey) Icons.Default.Check else Icons.Default.Sort, null, tint = if (key == selectedKey) colors.primary else colors.textSecondary, modifier = Modifier.size(20.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 12.dp))
                        if (key == selectedKey) Text(if (ascending) "ASC" else "DESC", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(onClick = onAscending, shape = RoundedCornerShape(15.dp), color = if (ascending) colors.primary else colors.surfaceContainerHigh, modifier = Modifier.weight(1f).height(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("Ascending", color = if (ascending) colors.onPrimary else colors.textPrimary, fontWeight = FontWeight.SemiBold) }
                }
                Surface(onClick = onDescending, shape = RoundedCornerShape(15.dp), color = if (!ascending) colors.primary else colors.surfaceContainerHigh, modifier = Modifier.weight(1f).height(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("Descending", color = if (!ascending) colors.onPrimary else colors.textPrimary, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

private fun LazyListScope.musicSongItems(
    filteredSongs: List<MediaItem>,
    favoriteIds: Set<String>,
    selected: Set<String>,
    playbackState: com.airplayr.app.ui.playback.PlaybackUiState,
    onPress: (MediaItem) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onFavorite: (MediaItem) -> Unit,
    onMore: (MediaItem) -> Unit,
) {
    if (filteredSongs.isEmpty()) {
        item { EmptyMusic("No songs found", "Music on this device will appear here.") }
    }
    items(filteredSongs, key = { it.id }) { item ->
        NativeMusicRow(
            item = item,
            favorite = item.id in favoriteIds,
            playing = playbackState.currentId == item.id && playbackState.isPlaying,
            selected = item.id in selected,
            onPress = { onPress(item) },
            onLongPress = { onLongPress(item) },
            onFavorite = { onFavorite(item) },
            onMore = { onMore(item) },
        )
    }
}

@Composable
private fun AirPlayrFastScrollHandle(state: LazyListState) {
    val colors = LocalAirPlayrColors.current
    val scrollScope = rememberCoroutineScope()
    val total = state.layoutInfo.totalItemsCount
    val visible = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    if (total <= visible + 4) return

    val maxFirst = (total - visible).coerceAtLeast(1)
    val fraction = (state.firstVisibleItemIndex.toFloat() / maxFirst).coerceIn(0f, 1f)
    BoxWithConstraints(Modifier.fillMaxSize().padding(end = 4.dp, top = 18.dp, bottom = 72.dp)) {
        val trackHeight = maxHeight - 12.dp
        val thumbHeight = 52.dp
        val dragRangePx = with(LocalDensity.current) { (trackHeight - thumbHeight).toPx() }.coerceAtLeast(1f)
        val y = (trackHeight - thumbHeight).coerceAtLeast(0.dp) * fraction
        Box(Modifier.align(Alignment.TopEnd).width(30.dp).height(trackHeight), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier.offset { IntOffset(0, y.roundToPx()) }
                    .width(24.dp)
                    .height(thumbHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceContainerHighest.copy(alpha = 0.88f))
                    .pointerInput(total, visible, fraction) {
                        var dragFraction = fraction
                        detectDragGestures(
                            onDragStart = { dragFraction = fraction },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragFraction = (dragFraction + dragAmount.y / dragRangePx).coerceIn(0f, 1f)
                                scrollScope.launch { state.scrollToItem((dragFraction * maxFirst).roundToInt().coerceIn(0, maxFirst)) }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.DragHandle, "Drag to scroll", tint = colors.primary, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun AirPlayrFastScrollGridHandle(state: LazyGridState) {
    val colors = LocalAirPlayrColors.current
    val scrollScope = rememberCoroutineScope()
    val total = state.layoutInfo.totalItemsCount
    val visible = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    if (total <= visible + 4) return

    val maxFirst = (total - visible).coerceAtLeast(1)
    val fraction = (state.firstVisibleItemIndex.toFloat() / maxFirst).coerceIn(0f, 1f)
    BoxWithConstraints(Modifier.fillMaxSize().padding(end = 4.dp, top = 18.dp, bottom = 72.dp)) {
        val trackHeight = maxHeight - 12.dp
        val thumbHeight = 48.dp
        val dragRangePx = with(LocalDensity.current) { (trackHeight - thumbHeight).toPx() }.coerceAtLeast(1f)
        val y = (trackHeight - thumbHeight).coerceAtLeast(0.dp) * fraction
        Box(Modifier.align(Alignment.TopEnd).width(30.dp).height(trackHeight), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier.offset { IntOffset(0, y.roundToPx()) }
                    .width(22.dp)
                    .height(thumbHeight)
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.surfaceContainerHighest.copy(alpha = 0.84f))
                    .pointerInput(total, visible, fraction) {
                        var dragFraction = fraction
                        detectDragGestures(
                            onDragStart = { dragFraction = fraction },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragFraction = (dragFraction + dragAmount.y / dragRangePx).coerceIn(0f, 1f)
                                scrollScope.launch { state.scrollToItem((dragFraction * maxFirst).roundToInt().coerceIn(0, maxFirst)) }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.DragHandle, "Drag to scroll", tint = colors.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NativeMusicRow(
    item: MediaItem,
    favorite: Boolean,
    playing: Boolean,
    selected: Boolean,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Row(
        Modifier.fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.primaryContainer else colors.surfaceContainer)
            .combinedClickable(onClick = onPress, onLongClick = onLongPress)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(17.dp))) {
            Artwork(item.artworkUri, Modifier.fillMaxSize(), 17.dp, item.title, isPlaying = playing)
            if (selected) {
                Box(Modifier.fillMaxSize().background(colors.primary.copy(alpha = .76f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, "Selected", tint = colors.onPrimary, modifier = Modifier.size(22.dp))
                }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(item.artist, item.album).joinToString(" · ").ifBlank { "Unknown artist" } + " · " + formatDurationShort(item.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(38.dp)) {
            Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", tint = if (favorite) colors.primary else colors.textSecondary, modifier = Modifier.size(20.dp))
        }
        AirPlayrMoreButton(onMore, modifier = Modifier.size(42.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NativeMusicTile(
    item: MediaItem,
    favorite: Boolean,
    playing: Boolean,
    selected: Boolean,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Column(
        Modifier.combinedClickable(onClick = onPress, onLongClick = onLongPress)
            .background(if (selected) colors.primaryContainer else colors.surfaceContainer, RoundedCornerShape(22.dp))
            .padding(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(152.dp).clip(RoundedCornerShape(18.dp))) {
            Artwork(item.artworkUri, Modifier.fillMaxSize(), 18.dp, item.title, MediaType.AUDIO, AirPlayrIcons.Music, isPlaying = playing)
            if (selected) {
                Box(Modifier.fillMaxSize().background(colors.primary.copy(alpha = .14f)))
                Icon(Icons.Default.Check, "Selected", tint = colors.onPrimary, modifier = Modifier.size(27.dp).align(Alignment.BottomEnd).padding(6.dp).background(colors.primary, CircleShape).padding(4.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artist ?: "Unknown artist", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(34.dp)) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", tint = if (favorite) colors.primary else colors.textSecondary, modifier = Modifier.size(18.dp))
            }
            AirPlayrMoreButton(onMore, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun CollectionGrid(
    grid: Boolean,
    data: List<Any>,
    onClick: (Any) -> Unit,
    circular: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAirPlayrColors.current
    if (grid) {
        val gridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
        Box(modifier) {
        LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.fillMaxSize(),
            state = gridState,
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = airPlayrBottomContentPadding()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (data.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { EmptyMusic("Nothing here yet", "Your library will appear here as it grows.") }
            }
            items(data) { raw ->
                val (name, groupSongs) = groupParts(raw)
                PremiumCollectionTile(name, groupSongs, circular, onClick = { onClick(raw) })
            }
        }
        AirPlayrFastScrollGridHandle(gridState)
        }
    } else {
        val state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) }
        Box(modifier) {
            LazyColumn(
                state = state,
                contentPadding = PaddingValues(start = 10.dp, top = 8.dp, end = 10.dp, bottom = airPlayrBottomContentPadding()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (data.isEmpty()) item { EmptyMusic("Nothing here yet", "Your library will appear here as it grows.") }
                items(data) { raw ->
                    val (name, groupSongs) = groupParts(raw)
                    AirPlayrCard(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(21.dp),
                        containerColor = colors.surfaceContainer,
                        elevation = 0.dp,
                        onClick = { onClick(raw) },
                    ) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Artwork(
                                groupSongs.firstOrNull()?.artworkUri,
                                Modifier.size(66.dp),
                                if (circular) 33.dp else 16.dp,
                                name,
                                MediaType.AUDIO,
                                if (circular) AirPlayrIcons.Artist else AirPlayrIcons.AlbumDisc,
                                fallbackIconSize = 28.dp,
                                fallbackIconTint = colors.onSurfaceVariant.copy(alpha = AirPlayrArtworkTokens.fallbackIconAlpha),
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${groupSongs.size} songs", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                            Icon(Icons.Default.ArrowForwardIos, "Open", tint = colors.primary, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
            AirPlayrFastScrollHandle(state)
        }
    }
}

@Composable
private fun PremiumCollectionTile(name: String, songs: List<MediaItem>, circular: Boolean, onClick: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = {})) {
        Box(
            Modifier.fillMaxWidth().height(178.dp).clip(RoundedCornerShape(24.dp)).background(
                Brush.linearGradient(listOf(colors.surfaceContainerHigh, colors.surfaceContainer)),
            ),
        ) {
            Artwork(
                songs.firstOrNull()?.artworkUri,
                Modifier.fillMaxSize().padding(8.dp),
                if (circular) 83.dp else 20.dp,
                name,
                MediaType.AUDIO,
                if (circular) AirPlayrIcons.Artist else AirPlayrIcons.AlbumDisc,
                fallbackIconSize = 58.dp,
                fallbackIconTint = colors.onSurfaceVariant.copy(alpha = .55f),
            )
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().background(colors.surfaceContainerLowest.copy(alpha = .78f)).padding(horizontal = 13.dp, vertical = 10.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${songs.size} ${if (songs.size == 1) "song" else "songs"}", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun GenreGrid(data: List<CollectionGroup>, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAirPlayrColors.current
    val state = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    Box(modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = state,
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = airPlayrBottomContentPadding()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (data.isEmpty()) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { EmptyMusic("No genres yet", "Genre labels will appear here when your music metadata provides them.") }
            items(data, key = { it.name }) { genre ->
                GenrePremiumCard(genre) { onNavigate("genre/${encode(genre.name)}") }
            }
        }
        AirPlayrFastScrollGridHandle(state)
    }
}

@Composable
private fun GenrePremiumCard(group: CollectionGroup, onClick: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    val arts = group.songs.mapNotNull { it.artworkUri?.takeIf(String::isNotBlank) }.distinct().take(3)
    val accent = when ((group.name.hashCode() and Int.MAX_VALUE) % 5) {
        0 -> colors.primaryContainer
        1 -> colors.secondaryContainer
        2 -> colors.tertiaryContainer
        3 -> colors.surfaceContainerHigh
        else -> colors.primaryContainer.copy(alpha = .72f)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(25.dp),
        color = accent,
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.fillMaxWidth().height(174.dp)) {
            Row(Modifier.fillMaxSize().padding(13.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 5.dp)) {
                    Icon(Icons.Default.LocalOffer, null, tint = colors.primary, modifier = Modifier.size(25.dp))
                    Text(group.name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
                    Text("${group.songs.size} songs", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary, modifier = Modifier.padding(top = 5.dp))
                    Surface(shape = RoundedCornerShape(50), color = colors.surfaceContainerLowest.copy(alpha = .56f), modifier = Modifier.padding(top = 12.dp)) {
                        Text("OPEN GENRE", fontFamily = AirPlayrFonts.OpenSans, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = colors.primary, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                }
                Box(Modifier.width(104.dp).fillMaxSize()) {
                    when {
                        arts.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(AirPlayrIcons.Music, null, tint = colors.primary.copy(alpha = .58f), modifier = Modifier.size(54.dp))
                            }
                        }
                        else -> {
                            arts.forEachIndexed { index, uri ->
                                Artwork(
                                    uri,
                                    Modifier.size(74.dp).align(
                                        when (index) {
                                            0 -> Alignment.TopEnd
                                            1 -> Alignment.CenterEnd
                                            else -> Alignment.BottomEnd
                                        },
                                    ).padding(2.dp),
                                    14.dp,
                                    group.name,
                                    MediaType.AUDIO,
                                    AirPlayrIcons.Music,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistGrid(
    grid: Boolean,
    data: List<MusicPlaylistEntity>,
    counts: Map<String, Int>,
    playlistItems: Map<String, List<String>>,
    songs: List<MediaItem>,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAirPlayrColors.current
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your playlists", style = MaterialTheme.typography.titleMedium)
                Text("Curated by you", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
            Surface(onClick = onAdd, shape = RoundedCornerShape(15.dp), color = colors.primaryContainer, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, "Create playlist", tint = colors.primary) }
            }
        }
        if (grid) {
            val playlistGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
            Box(Modifier.weight(1f)) {
            LazyVerticalGrid(
                state = playlistGridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 10.dp, end = 12.dp, bottom = airPlayrBottomContentPadding()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(data, key = { it.id }) { playlist ->
                    PlaylistCard(playlist, counts[playlist.id] ?: 0, playlistItems[playlist.id].orEmpty(), songs, colors, onNavigate)
                }
            }
            AirPlayrFastScrollGridHandle(playlistGridState)
            }
        } else {
            val state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) }
            Box(Modifier.weight(1f)) {
                LazyColumn(
                    state = state,
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = airPlayrBottomContentPadding()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(data, key = { it.id }) { playlist ->
                        val ids = playlistItems[playlist.id].orEmpty()
                        Row(
                            Modifier.fillMaxWidth().background(colors.surfaceContainer, RoundedCornerShape(18.dp)).padding(10.dp).combinedClickable(onClick = { onNavigate("music-playlist/${playlist.id}") }, onLongClick = {}),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val art = songs.firstOrNull { it.id in ids && !it.artworkUri.isNullOrBlank() }
                            Artwork(art?.artworkUri, Modifier.size(66.dp), 16.dp, playlist.name, MediaType.AUDIO, AirPlayrIcons.AlbumDisc, fallbackIconSize = 28.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${counts[playlist.id] ?: 0} songs", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                            Icon(Icons.Default.ArrowForwardIos, null, tint = colors.primary, modifier = Modifier.size(15.dp))
                        }
                    }
                }
                AirPlayrFastScrollHandle(state)
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: MusicPlaylistEntity,
    count: Int,
    ids: List<String>,
    songs: List<MediaItem>,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    onNavigate: (String) -> Unit,
) {
    Column(Modifier.combinedClickable(onClick = { onNavigate("music-playlist/${playlist.id}") }, onLongClick = {})) {
        val arts = songs.filter { it.id in ids }.filter { !it.artworkUri.isNullOrBlank() }.take(4)
        Card(Modifier.fillMaxWidth().height(168.dp), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            if (arts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(AirPlayrIcons.Music, null, Modifier.size(42.dp), tint = colors.primary) }
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    arts.forEachIndexed { index, song ->
                        val cell = Modifier.then(if (index % 2 == 0) Modifier.padding(end = maxWidth / 2) else Modifier.padding(start = maxWidth / 2)).then(if (index / 2 == 0) Modifier.padding(bottom = maxHeight / 2) else Modifier.padding(top = maxHeight / 2))
                        Artwork(song.artworkUri, cell.fillMaxWidth().fillMaxHeight(), 0.dp, song.title, isPlaying = false)
                    }
                }
            }
        }
        Text(playlist.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
        Text("$count ${if (count == 1) "song" else "songs"}", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onPlaylist: () -> Unit,
    onLike: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHighest), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberSaveable(saver = androidx.compose.foundation.ScrollState.Saver) { androidx.compose.foundation.ScrollState(0) }).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClear) { Icon(Icons.Default.Close, "Clear", tint = colors.primary) }
            Text("$count selected", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 5.dp))
            IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, "Play selected", tint = colors.primary) }
            IconButton(onClick = onPlayNext) { Icon(Icons.Default.SkipNext, "Play selected next", tint = colors.primary) }
            IconButton(onClick = onPlaylist) { Icon(Icons.Default.QueueMusic, "Add to playlist", tint = colors.primary) }
            IconButton(onClick = onLike) { Icon(Icons.Default.FavoriteBorder, "Like selected", tint = colors.primary) }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit selected", tint = colors.primary) }
            TextButton(onClick = onSelectAll) { Text("Select all") }
        }
    }
}

@Composable
private fun MusicHeader(
    searchOpen: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    grid: Boolean,
    onGrid: (Boolean) -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Home", tint = colors.primary) }
            Column(Modifier.weight(1f).padding(horizontal = 5.dp)) {
                Text("AIR PLAYR", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 13.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                Text("Music", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            }
            Surface(shape = RoundedCornerShape(15.dp), color = colors.primaryContainer.copy(alpha = .65f), modifier = Modifier.height(38.dp)) {
                Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = colors.primary, modifier = Modifier.size(17.dp))
                    Text("LIBRARY", fontFamily = AirPlayrFonts.OpenSans, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = colors.primary, modifier = Modifier.padding(start = 5.dp))
                }
            }
        }
        if (searchOpen) {
            OutlinedTextField(
                query,
                onQuery,
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                placeholder = { Text("Search songs, artists, albums or genres") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { IconButton(onClick = onSearch) { Icon(Icons.Default.Close, "Close search") } },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun EmptyMusic(title: String, message: String) {
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(AirPlayrIcons.Music, null, Modifier.size(40.dp), tint = colors.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun PlaylistPickerDialog(
    playlists: List<MusicPlaylistEntity>,
    counts: Map<String, Int>,
    item: MediaItem,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column(Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                playlists.forEach { playlist ->
                    TextButton(onClick = { onSelect(playlist.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${playlist.name}  ·  ${counts[playlist.id] ?: 0} songs", modifier = Modifier.fillMaxWidth())
                    }
                }
                if (playlists.isEmpty()) Text("Create a playlist first.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private data class AlbumGroup(val name: String, val songs: List<MediaItem>) {
    val artist get() = songs.firstOrNull()?.artist.orEmpty()
}
private data class CollectionGroup(val name: String, val songs: List<MediaItem>)

private fun rawName(raw: Any): String = when (raw) {
    is AlbumGroup -> raw.name
    is CollectionGroup -> raw.name
    else -> "Music"
}

private fun groupParts(raw: Any): Pair<String, List<MediaItem>> = when (raw) {
    is AlbumGroup -> raw.name to raw.songs
    is CollectionGroup -> raw.name to raw.songs
    else -> "Unknown" to emptyList()
}

private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")
private fun formatDurationShort(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) / 1000L)
    return "%02d:%02d".format(seconds / 60L, seconds % 60L)
}
