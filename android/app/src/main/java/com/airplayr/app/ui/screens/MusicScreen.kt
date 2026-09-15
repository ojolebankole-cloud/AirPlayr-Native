package com.airplayr.app.ui.screens

import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.airplayr.app.core.data.db.MusicPlaylistEntity
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.AirPlayrCard
import com.airplayr.app.ui.components.Artwork
import com.airplayr.app.ui.components.TagEditorSheet
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackControllerFactory
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors

private val MUSIC_CATEGORIES = listOf(
    "songs" to "Songs", "likes" to "Likes", "albums" to "Albums", "playlists" to "Playlists",
    "artists" to "Artists", "genres" to "Genres", "folders" to "Folders",
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicScreen(libraryViewModel: LibraryViewModel, appContext: android.content.Context, container: AppContainer, onNavigate: (String) -> Unit = {}) {
    val colors = LocalAirPlayrColors.current
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val songs by libraryViewModel.audio.collectAsStateWithLifecycle()
    val favorites by container.structured.favorites().collectAsStateWithLifecycle(initialValue = emptyList())
    val playlists by container.structured.playlists().collectAsStateWithLifecycle(initialValue = emptyList())
    val playback: PlaybackController = viewModel(factory = PlaybackControllerFactory(appContext))
    val playbackState by playback.state.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf("songs") }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var ascending by remember { mutableStateOf(true) }
    var grid by remember(settings?.libraryLayout) { mutableStateOf(settings?.libraryLayout != "list") }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var moreFor by remember { mutableStateOf<MediaItem?>(null) }
    var playlistDialogFor by remember { mutableStateOf<MediaItem?>(null) }
    var playlistName by remember { mutableStateOf("") }
    var editFor by remember { mutableStateOf<MediaItem?>(null) }
    val playlistCounts = remember { mutableStateMapOf<String, Int>() }
    val playlistItems = remember { mutableStateMapOf<String, List<String>>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { libraryViewModel.refresh() }

    LaunchedEffect(playlists) {
        playlists.forEach { playlist ->
            val ids = container.structured.playlistIds(playlist.id)
            playlistItems[playlist.id] = ids
            playlistCounts[playlist.id] = ids.size
        }
    }

    val filter = query.trim()
    val favoriteIds = remember(favorites) { favorites.map { it.mediaId }.toSet() }
    val filteredSongs = remember(songs, category, favoriteIds, filter, ascending) {
        val base = if (category == "likes") songs.filter { it.id in favoriteIds } else songs
        base.filter { filter.isBlank() || listOf(it.title, it.artist, it.album, it.folder, it.genre).any { v -> v.orEmpty().contains(filter, true) } }
            .sortedWith(compareBy<MediaItem> { it.title.lowercase() }.let { if (ascending) it else it.reversed() })
    }
    val albums = remember(songs, filter, ascending) { songs.filter { !it.album.isNullOrBlank() }.groupBy { it.album!!.trim() }.map { AlbumGroup(it.key, it.value) }.filter { filter.isBlank() || it.name.contains(filter,true) || it.artist.contains(filter,true) }.sortedBy { it.name.lowercase() }.let { if (ascending) it else it.reversed() } }
    val artists = remember(songs, filter, ascending) { songs.filter { !it.artist.isNullOrBlank() }.groupBy { it.artist!!.trim() }.map { CollectionGroup(it.key, it.value) }.filter { filter.isBlank() || it.name.contains(filter,true) }.sortedBy { it.name.lowercase() }.let { if (ascending) it else it.reversed() } }
    val genres = remember(songs, filter, ascending) { songs.groupBy { it.genre?.trim().takeUnless { g -> g.isNullOrBlank() } ?: "Other" }.map { CollectionGroup(it.key, it.value) }.filter { filter.isBlank() || it.name.contains(filter,true) }.sortedBy { it.name.lowercase() }.let { if (ascending) it else it.reversed() } }
    val folders = remember(songs, filter, ascending) { songs.groupBy { it.folder?.trim().takeUnless { g -> g.isNullOrBlank() } ?: "Music" }.map { CollectionGroup(it.key, it.value) }.filter { filter.isBlank() || it.name.contains(filter,true) }.sortedBy { it.name.lowercase() }.let { if (ascending) it else it.reversed() } }

    fun play(items: List<MediaItem>, start: MediaItem? = items.firstOrNull()) { if (items.isNotEmpty() && start != null) playback.playQueue(items, items.indexOfFirst { it.id == start.id }.coerceAtLeast(0)) }

    Column(Modifier.testTag(com.airplayr.app.core.qa.AirPlayrQaTags.MUSIC).fillMaxSize().background(colors.background)) {
        MusicHeader(searchOpen, query, { query = it }, { searchOpen = !searchOpen; if (searchOpen) query = "" }, { onNavigate("home") }, grid, { value -> grid = value; scope.launch { container.preferences.setLibraryLayout(if (value) "grid" else "list") } })
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MUSIC_CATEGORIES.forEach { (key,label) -> FilterChip(selected = category == key, onClick = { category = key; selected = emptySet() }, label = { Text(label) }) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(when(category) { "songs" -> "${filteredSongs.size} songs"; "likes" -> "${filteredSongs.size} liked"; "albums" -> "${albums.size} albums"; "playlists" -> "${playlists.size} playlists"; "artists" -> "${artists.size} artists"; "genres" -> "${genres.size} genres"; else -> "${folders.size} folders" }, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { ascending = !ascending }) { Icon(Icons.Default.Sort, "Sort", tint = colors.primary) }
            if (category == "songs" || category == "likes") IconButton(onClick = { play(filteredSongs.shuffled()) }) { Icon(Icons.Default.Shuffle, "Shuffle", tint = colors.primary) }
            if (category != "songs" && category != "likes") Icon(if (grid) Icons.Default.GridView else Icons.Default.List, "Layout", Modifier.size(24.dp).clickable { grid = !grid }, tint = colors.primary)
        }

        when (category) {
            "songs", "likes" -> {
                if (selected.isNotEmpty()) SelectionBar(
                    count = selected.size,
                    onClear = { selected = emptySet() },
                    onSelectAll = { selected = filteredSongs.map { it.id }.toSet() },
                    onPlay = { val chosen = filteredSongs.filter { it.id in selected }; if (chosen.isNotEmpty()) { play(chosen); selected = emptySet() } },
                    onPlayNext = { filteredSongs.filter { it.id in selected }.forEach(playback::playNext); selected = emptySet() },
                    onPlaylist = { filteredSongs.firstOrNull { it.id in selected }?.let { playlistDialogFor = it } },
                    onLike = { scope.launch { filteredSongs.filter { it.id in selected }.forEach { if (it.id !in favoriteIds) container.structured.toggleFavorite(it) }; selected = emptySet() } },
                    onEdit = { filteredSongs.firstOrNull { it.id in selected }?.let { editFor = it } },
                )
                if (grid) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (filteredSongs.isEmpty()) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            EmptyMusic("No songs found", if (category == "likes") "Like a song to see it here." else "Music on this device will appear here.")
                        }
                        items(filteredSongs, key = { it.id }) { item ->
                            NativeMusicTile(
                                item = item,
                                favorite = item.id in favoriteIds,
                                playing = playbackState.currentId == item.id && playbackState.isPlaying,
                                selected = item.id in selected,
                                onPress = { if (selected.isNotEmpty()) selected = if (item.id in selected) selected - item.id else selected + item.id else play(filteredSongs, item) },
                                onLongPress = { selected = if (item.id in selected) selected - item.id else selected + item.id },
                                onFavorite = { scope.launch { container.structured.toggleFavorite(item) } },
                                onMore = { moreFor = item },
                            )
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (filteredSongs.isEmpty()) item { EmptyMusic("No songs found", if (category == "likes") "Like a song to see it here." else "Music on this device will appear here.") }
                        items(filteredSongs, key = { it.id }) { item ->
                            NativeMusicRow(
                                item = item,
                                favorite = item.id in favoriteIds,
                                playing = playbackState.currentId == item.id && playbackState.isPlaying,
                                selected = item.id in selected,
                                onPress = { if (selected.isNotEmpty()) selected = if (item.id in selected) selected - item.id else selected + item.id else play(filteredSongs, item) },
                                onLongPress = { selected = if (item.id in selected) selected - item.id else selected + item.id },
                                onFavorite = { scope.launch { container.structured.toggleFavorite(item) } },
                                onMore = { moreFor = item },
                            )
                        }
                    }
                }
            }
            "albums" -> CollectionGrid(grid, albums, { raw -> val name = when(raw){is AlbumGroup->raw.name;is CollectionGroup->raw.name;else->"Unknown"}; onNavigate("album/${java.net.URLEncoder.encode(name, "UTF-8")}") }, modifier = Modifier.weight(1f))
            "artists" -> CollectionGrid(grid, artists, { raw -> val name = when(raw){is AlbumGroup->raw.name;is CollectionGroup->raw.name;else->"Unknown"}; onNavigate("artist/${java.net.URLEncoder.encode(name, "UTF-8")}") }, circular = true, modifier = Modifier.weight(1f))
            "genres" -> GenreGrid(genres, onNavigate, modifier = Modifier.weight(1f))
            "folders" -> CollectionGrid(grid, folders, { raw -> val name = when(raw){is AlbumGroup->raw.name;is CollectionGroup->raw.name;else->"Music"}; onNavigate("music-folder/${java.net.URLEncoder.encode(name, "UTF-8")}") }, modifier = Modifier.weight(1f))
            "playlists" -> PlaylistGrid(grid, playlists, playlistCounts, playlistItems, songs, onNavigate, { scope.launch { val p = container.structured.createPlaylist("New playlist"); onNavigate("music-playlist/${p.id}") } }, modifier = Modifier.weight(1f))
        }
    }

    moreFor?.let { item ->
        ActionDialog(item, favoriteIds.contains(item.id), onDismiss = { moreFor = null }, onPlay = { moreFor = null; play(listOf(item), item) }, onQueue = { moreFor = null; playback.addToQueue(item) }, onNext = { moreFor = null; playback.playNext(item) }, onFavorite = { moreFor = null; scope.launch { container.structured.toggleFavorite(item) } }, onPlaylist = { moreFor = null; playlistDialogFor = item }, onEdit = { moreFor = null; editFor = item })
    }
    playlistDialogFor?.let { item ->
        PlaylistPickerDialog(playlists, playlistCounts, item, onDismiss = { playlistDialogFor = null }, onSelect = { playlistId -> scope.launch { val ids = if (selected.isNotEmpty() && item.id in selected) selected else setOf(item.id); ids.forEach { container.structured.addToPlaylist(playlistId, it) }; selected = emptySet(); playlistDialogFor = null } })
    }
    editFor?.let { item ->
        TagEditorSheet(appContext, item, com.airplayr.app.core.metadata.MetadataRepository(appContext), { editFor = null }) {
            editFor = null
            libraryViewModel.refresh()
        }
    }
}

private data class AlbumGroup(val name: String, val songs: List<MediaItem>) { val artist get() = songs.firstOrNull()?.artist.orEmpty() }
private data class CollectionGroup(val name: String, val songs: List<MediaItem>)

@Composable private fun MusicHeader(searchOpen: Boolean, query: String, onQuery: (String) -> Unit, onSearch: () -> Unit, onBack: () -> Unit, grid: Boolean, onGrid: (Boolean) -> Unit) {
    val colors = LocalAirPlayrColors.current
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Home", tint = colors.primary) }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) { Text("AIR PLAYR", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary); Text("Music", style = MaterialTheme.typography.displaySmall, color = colors.textPrimary) }
            IconButton(onClick = { onGrid(!grid) }) { Icon(if (grid) Icons.Default.GridView else Icons.Default.List, "Layout", tint = colors.primary) }
            IconButton(onClick = onSearch) { Icon(if (searchOpen) Icons.Default.Close else Icons.Default.Search, "Search", tint = colors.primary) }
        }
        if (searchOpen) OutlinedTextField(query, onQuery, Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp), placeholder = { Text("Search songs, artists or albums") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
    }
}

@Composable
private fun SelectionBar(count: Int, onClear: () -> Unit, onSelectAll: () -> Unit, onPlay: () -> Unit, onPlayNext: () -> Unit, onPlaylist: () -> Unit, onLike: () -> Unit, onEdit: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHighest)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClear) { Icon(Icons.Default.Close, "Clear", tint = colors.primary) }
            Text("$count selected", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 5.dp))
            IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, "Play selected", tint = colors.primary) }
            IconButton(onClick = onPlayNext) { Icon(AirPlayrIcons.Next, "Play selected next", tint = colors.primary) }
            IconButton(onClick = onPlaylist) { Icon(Icons.Default.QueueMusic, "Add to playlist", tint = colors.primary) }
            IconButton(onClick = onLike) { Icon(Icons.Default.FavoriteBorder, "Like selected", tint = colors.primary) }
            IconButton(onClick = onEdit) { Icon(Icons.Default.MoreVert, "Edit selected", tint = colors.primary) }
            TextButton(onClick = onSelectAll) { Text("Select all") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun NativeMusicRow(item: MediaItem, favorite: Boolean, playing: Boolean, selected: Boolean, onPress: () -> Unit, onLongPress: () -> Unit, onFavorite: () -> Unit, onMore: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Row(Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(20.dp)).background(if(selected) colors.primaryContainer else colors.surfaceContainer).combinedClickable(onClick=onPress,onLongClick=onLongPress).padding(horizontal=10.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(17.dp))){Artwork(item.artworkUri,Modifier.fillMaxSize(),17.dp,item.title);if(selected)Box(Modifier.fillMaxSize().background(colors.primary.copy(alpha=.72f)),contentAlignment=Alignment.Center){Icon(Icons.Default.Check,null,tint=colors.onPrimary)}}
        Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(item.title,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${item.artist ?: "Unknown artist"} · ${time(item.durationMs)}",style=MaterialTheme.typography.bodySmall,color=colors.textSecondary,maxLines=1,overflow=TextOverflow.Ellipsis)}
        IconButton(onClick=onFavorite){Icon(if(favorite)Icons.Default.Favorite else Icons.Default.FavoriteBorder,"Favorite",tint=colors.primary)}
        IconButton(onClick=onMore){Icon(Icons.Default.MoreVert,"More",tint=colors.primary)}
        if(playing)Icon(Icons.Default.PlayArrow,"Playing",Modifier.size(20.dp),tint=colors.primary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NativeMusicTile(item: MediaItem, favorite: Boolean, playing: Boolean, selected: Boolean, onPress: () -> Unit, onLongPress: () -> Unit, onFavorite: () -> Unit, onMore: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Column(
        Modifier.combinedClickable(onClick = onPress, onLongClick = onLongPress)
            .background(if (selected) colors.primaryContainer else colors.surfaceContainer, RoundedCornerShape(20.dp))
            .padding(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp))) {
            Artwork(item.artworkUri, Modifier.fillMaxSize(), 18.dp, item.title)
            if (selected || playing) {
                Box(Modifier.fillMaxSize().background(colors.scrim.copy(alpha = .16f)))
            }
            Row(Modifier.fillMaxSize().padding(4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Top) {
                if (favorite) Icon(Icons.Default.Favorite, "Favorite", tint = colors.primary, modifier = Modifier.size(20.dp).padding(2.dp))
                IconButton(onClick = onMore, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, "More", tint = colors.primary, modifier = Modifier.size(20.dp)) }
            }
            if (playing) Icon(Icons.Default.PlayArrow, "Playing", tint = colors.onPrimary, modifier = Modifier.size(24.dp).align(Alignment.Center).background(colors.primary, CircleShape).padding(3.dp))
            if (selected) Icon(Icons.Default.Check, "Selected", tint = colors.onPrimary, modifier = Modifier.size(26.dp).align(Alignment.BottomEnd).background(colors.primary, CircleShape).padding(4.dp))
        }
        Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
        Text(item.artist ?: "Unknown artist", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.End).size(34.dp)) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", tint = colors.primary) }
    }
}

@Composable private fun CollectionGrid(grid:Boolean, data:List<Any>, onClick:(Any)->Unit, circular:Boolean=false, modifier:Modifier=Modifier) {
    val colors=LocalAirPlayrColors.current
    if(grid) LazyVerticalGrid(GridCells.Fixed(2),modifier, contentPadding=PaddingValues(12.dp), horizontalArrangement=Arrangement.spacedBy(10.dp), verticalArrangement=Arrangement.spacedBy(16.dp)){items(data){ raw -> val (name,songs)=when(raw){is AlbumGroup->raw.name to raw.songs; is CollectionGroup->raw.name to raw.songs; else->"Unknown" to emptyList()}; Column(Modifier.clickableCompat { onClick(raw) }.fillMaxWidth()){Artwork(songs.firstOrNull()?.artworkUri,Modifier.fillMaxWidth().height(170.dp),if(circular)85.dp else 22.dp,name);Text(name,style=MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=6.dp));Text("${songs.size} songs",style=MaterialTheme.typography.labelSmall,color=colors.textSecondary)}}}
    else LazyColumn(modifier,contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(data){raw->val(name,songs)=when(raw){is AlbumGroup->raw.name to raw.songs;is CollectionGroup->raw.name to raw.songs;else->"Unknown" to emptyList()};AirPlayrCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),containerColor=colors.surfaceSolid,elevation=5.dp){Row(Modifier.fillMaxWidth().padding(10.dp).clickableCompat { onClick(raw) },verticalAlignment=Alignment.CenterVertically){Artwork(songs.firstOrNull()?.artworkUri,Modifier.size(66.dp),if(circular)33.dp else 16.dp,name);Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(name,style=MaterialTheme.typography.titleMedium);Text("${songs.size} songs",style=MaterialTheme.typography.bodySmall,color=colors.textSecondary)}}}}}
}

@Composable private fun GenreGrid(data:List<CollectionGroup>, onNavigate:(String)->Unit, modifier:Modifier=Modifier){ val colors=LocalAirPlayrColors.current; LazyVerticalGrid(GridCells.Fixed(2),modifier,contentPadding=PaddingValues(12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(data){g->AirPlayrCard(Modifier.fillMaxWidth().height(148.dp),shape=RoundedCornerShape(24.dp),containerColor=listOf(colors.primaryContainer,colors.secondaryContainer,colors.tertiaryContainer)[(g.name.hashCode() and Int.MAX_VALUE)%3],elevation=5.dp,onClick={onNavigate("genre/${java.net.URLEncoder.encode(g.name,"UTF-8")}")}){Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(colors.surfaceContainer.copy(alpha=.1f),colors.surfaceVariant.copy(alpha=.24f))))){Column(Modifier.padding(16.dp)){Text(g.name,style=MaterialTheme.typography.headlineSmall,maxLines=2);Text("${g.songs.size} songs",style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=4.dp))};Artwork(g.songs.firstOrNull()?.artworkUri,Modifier.size(92.dp).align(Alignment.BottomEnd).padding(5.dp),18.dp,g.name)}}}} }

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
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Your playlists", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Create playlist", tint = colors.primary) }
        }
        if (grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(data, key = { it.id }) { playlist ->
                    PlaylistCard(playlist, counts[playlist.id] ?: 0, playlistItems[playlist.id].orEmpty(), songs, colors, onNavigate)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(data, key = { it.id }) { playlist ->
                    val ids = playlistItems[playlist.id].orEmpty()
                    Row(
                        Modifier.fillMaxWidth().clickableCompat { onNavigate("music-playlist/${playlist.id}") }
                            .background(colors.surfaceContainer, RoundedCornerShape(18.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val art = songs.firstOrNull { it.id in ids && !it.artworkUri.isNullOrBlank() }
                        Artwork(art?.artworkUri, Modifier.size(66.dp), 16.dp, playlist.name)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${counts[playlist.id] ?: 0} songs", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
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
    Column(Modifier.clickableCompat { onNavigate("music-playlist/${playlist.id}") }) {
        val arts = songs.filter { it.id in ids }.filter { !it.artworkUri.isNullOrBlank() }.take(4)
        Card(
            Modifier.fillMaxWidth().height(165.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        ) {
            if (arts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(42.dp), tint = colors.primary)
                }
            } else {
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                    arts.forEachIndexed { index, song ->
                        val cell = Modifier
                            .then(if (index % 2 == 0) Modifier.padding(end = maxWidth / 2) else Modifier.padding(start = maxWidth / 2))
                            .then(if (index / 2 == 0) Modifier.padding(bottom = maxHeight / 2) else Modifier.padding(top = maxHeight / 2))
                        Artwork(song.artworkUri, cell.fillMaxWidth().fillMaxHeight(), 0.dp, song.title)
                    }
                }
            }
        }
        Text(playlist.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
        Text("$count ${if (count == 1) "song" else "songs"}", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

@Composable private fun ActionDialog(item:MediaItem,favorite:Boolean,onDismiss:()->Unit,onPlay:()->Unit,onQueue:()->Unit,onNext:()->Unit,onFavorite:()->Unit,onPlaylist:()->Unit,onEdit:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(item.title)},text={Column(verticalArrangement=Arrangement.spacedBy(6.dp)){DialogAction("Play",onPlay);DialogAction("Play next",onNext);DialogAction("Add to queue",onQueue);DialogAction(if(favorite)"Remove from likes" else "Like",onFavorite);DialogAction("Add to playlist",onPlaylist);DialogAction("Edit song",onEdit)}},confirmButton={})}
@Composable private fun DialogAction(label:String,onClick:()->Unit){TextButton(onClick=onClick,modifier=Modifier.fillMaxWidth()){Text(label,textAlign=TextAlign.Start,modifier=Modifier.fillMaxWidth())}}
@Composable private fun PlaylistPickerDialog(playlists:List<MusicPlaylistEntity>,counts:Map<String,Int>,item:MediaItem,onDismiss:()->Unit,onSelect:(String)->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text("Add to playlist")},text={Column(Modifier.height(320.dp),verticalArrangement=Arrangement.spacedBy(2.dp)){playlists.forEach{p->TextButton(onClick={onSelect(p.id)},modifier=Modifier.fillMaxWidth()){Text("${p.name}  ·  ${counts[p.id]?:0} songs",modifier=Modifier.fillMaxWidth())}};if(playlists.isEmpty())Text("Create a playlist first.")}},confirmButton={TextButton(onClick=onDismiss){Text("Close")}})}
@Composable private fun EmptyMusic(title:String,message:String){val colors=LocalAirPlayrColors.current;Column(Modifier.fillMaxWidth().padding(40.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.LibraryMusic,null,Modifier.size(40.dp),tint=colors.primary);Text(title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=10.dp));Text(message,style=MaterialTheme.typography.bodySmall,color=colors.textSecondary,textAlign=TextAlign.Center)}}
private fun time(ms:Long):String{val s=(ms.coerceAtLeast(0)/1000);return "%02d:%02d".format(s/60,s%60)}
private fun Modifier.clickableCompat(onClick:()->Unit):Modifier=then(androidx.compose.ui.Modifier.clickable(onClick=onClick))
