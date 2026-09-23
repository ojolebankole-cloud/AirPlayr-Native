@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.airplayr.app.ui.screens

import android.content.Intent
import android.net.Uri
import com.airplayr.app.ui.airPlayrBottomContentPadding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
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
import com.airplayr.app.core.lyrics.LyricsTrack
import com.airplayr.app.core.playback.VideoQueueStore
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.AirPlayrSheetScaffold
import com.airplayr.app.ui.components.AirPlayrMediaMenuAction
import com.airplayr.app.ui.components.AirPlayrMoreButton
import com.airplayr.app.ui.components.AirPlayrPremiumMediaActionSheet
import com.airplayr.app.ui.components.AirPlayrSelectionBar
import com.airplayr.app.ui.components.Artwork
import com.airplayr.app.ui.components.displayArtworkUri
import com.airplayr.app.ui.feedback.AppFeedback
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackControllerFactory
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airplayr.app.R
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
    val searchable: String = "",
)

private val searchCombiningMarksRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
private val searchWhitespaceRegex = Regex("\\s+")

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
        .replace(searchCombiningMarksRegex, "")

private fun matches(entry: SearchEntry, query: String): Boolean {
    val tokens = normalize(query).split(searchWhitespaceRegex).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return true
    val searchable = entry.searchable.ifBlank {
        normalize(listOf(entry.title, entry.subtitle, entry.item?.artist, entry.item?.album, entry.item?.genre, entry.item?.filename).filterNotNull().joinToString(" "))
    }
    return tokens.all(searchable::contains)
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
    var selectedIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
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
    val searchSessionSeed = rememberSaveable { java.util.UUID.randomUUID().toString() }
    val searchIconComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.airplayr_animated_icon))
    val searchIconProgress by animateLottieCompositionAsState(
        composition = searchIconComposition,
        iterations = LottieConstants.IterateForever,
    )
    val playColors = remember(searchSessionSeed, colors.primary, colors.secondary, colors.tertiary) {
        val palette = listOf(colors.primary, colors.secondary, colors.tertiary)
        val start = Math.floorMod(searchSessionSeed.hashCode(), palette.size)
        listOf(palette[start], palette[(start + 1) % palette.size], palette[(start + 2) % palette.size])
    }
    val playTransition = rememberInfiniteTransition(label = "search-play-icon")
    val playOffset by playTransition.animateDp(
        initialValue = 0.dp,
        targetValue = (-3).dp,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "search-play-float",
    )
    var playColorIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(searchSessionSeed) {
        while (true) {
            delay(1700L)
            playColorIndex = (playColorIndex + 1) % playColors.size
        }
    }
    val animatedPlayColor by animateColorAsState(playColors[playColorIndex], tween(600), label = "search-play-color")

    LaunchedEffect(query) {
        delay(120)
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
        (songs + albums + artists + pls + genres + videos).map { entry ->
            entry.copy(
                searchable = normalize(
                    listOf(
                        entry.title,
                        entry.subtitle,
                        entry.item?.artist,
                        entry.item?.album,
                        entry.item?.genre,
                        entry.item?.filename,
                    ).filterNotNull().joinToString(" ")
                )
            )
        }
    }

    val recentArtists = remember(snapshot.history, audio) {
        val byArtist = audio.filter { !it.artist.isNullOrBlank() }.groupBy { normalize(it.artist) }
        val seen = mutableSetOf<String>()
        history.asSequence()
            .filter { it.mediaType == MediaType.AUDIO.value }
            .mapNotNull { h ->
                val artistName = h.artist?.trim().takeUnless { it.isNullOrBlank() } ?: return@mapNotNull null
                val key = normalize(artistName)
                if (!seen.add(key)) return@mapNotNull null
                val group = byArtist[key].orEmpty()
                val first = group.firstOrNull() ?: return@mapNotNull null
                SearchEntry("artist:${first.artist}", "artist", first.artist.orEmpty(), "${group.size} songs", first, first.artist)
            }
            .take(8).toList()
    }

    val recentSongs = remember(snapshot.history, lookup) {
        history.asSequence()
            .filter { it.mediaType == MediaType.AUDIO.value }
            .mapNotNull { h ->
                lookup[h.mediaId]?.takeIf { it.mediaType == MediaType.AUDIO }?.let { item ->
                    SearchEntry(item.id, "song", item.title.ifBlank { item.filename }, item.artist ?: "Unknown artist", item)
                }
            }
            .take(8)
            .toList()
    }

    val recentAlbums = remember(snapshot.history, audio) {
        val byAlbum = audio.filter { !it.album.isNullOrBlank() }.groupBy { normalize(it.album) }
        val seen = mutableSetOf<String>()
        history.asSequence()
            .filter { it.mediaType == MediaType.AUDIO.value }
            .mapNotNull { h ->
                val albumName = h.album?.trim().takeUnless { it.isNullOrBlank() } ?: return@mapNotNull null
                val key = normalize(albumName)
                if (!seen.add(key)) return@mapNotNull null
                val first = byAlbum[key]?.firstOrNull() ?: return@mapNotNull null
                SearchEntry("album:${first.album}", "album", first.album.orEmpty(), first.artist ?: "Unknown artist", first, first.album)
            }
            .take(8).toList()
    }

    val recentVideos = remember(snapshot.history, lookup) {
        history.asSequence()
            .filter { it.mediaType == MediaType.VIDEO.value }
            .mapNotNull { h ->
                lookup[h.mediaId]?.takeIf { it.mediaType == MediaType.VIDEO }?.let { item ->
                    SearchEntry(item.id, "video", item.title.ifBlank { item.filename }, item.artist ?: "Video", item)
                }
            }
            .take(8)
            .toList()
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

    fun playSearchAudio(item: MediaItem, sequence: List<MediaItem>) {
        val ordered = sequence.filter { it.mediaType == MediaType.AUDIO }.distinctBy { it.id }
        val index = ordered.indexOfFirst { it.id == item.id }
        if (index >= 0) playback.playLoopingQueue(ordered, index)
        onOpen(item)
    }

    fun playSearchVideo(item: MediaItem, sequence: List<MediaItem>) {
        val ordered = sequence.filter { it.mediaType == MediaType.VIDEO }.distinctBy { it.id }
        val index = ordered.indexOfFirst { it.id == item.id }
        if (index >= 0) VideoQueueStore.setLoopingQueue(ordered, item.id)
        onOpen(item)
    }

    fun openEntry(entry: SearchEntry) {
        when (entry.type) {
            "song" -> entry.item?.let { item -> playSearchAudio(item, filtered.mapNotNull { it.item }) }
            "video" -> entry.item?.let { item -> playSearchVideo(item, filtered.mapNotNull { it.item }) }
            "album" -> entry.routeKey?.let { onNavigate("album/${Uri.encode(it)}") }
            "artist" -> entry.routeKey?.let { onNavigate("artist/${Uri.encode(it)}") }
            "genre" -> entry.routeKey?.let { onNavigate("genre/${Uri.encode(it)}") }
            "playlist" -> entry.routeKey?.let { onNavigate("music-playlist/$it") }
        }
    }

    val recentSearches by container.preferences.recentSearches.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ChevronLeft, "Back", tint = colors.primary)
            }
            Surface(
                Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceContainerHigh,
                tonalElevation = 0.dp,
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, "Search", tint = colors.primary, modifier = Modifier.size(21.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp).focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = colors.textPrimary, fontSize = 17.sp, fontFamily = AirPlayrFonts.OpenSans),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            val value = debounced.trim()
                            if (value.isNotBlank()) scope.launch { container.preferences.recordRecentSearch(value) }
                            focus.clearFocus()
                        }),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    LottieAnimation(
                                        composition = searchIconComposition,
                                        progress = { searchIconProgress },
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            "Search your plays",
                                            color = colors.textSecondary,
                                            fontSize = 16.sp,
                                            fontFamily = AirPlayrFonts.SmoochSans,
                                            lineHeight = 18.sp,
                                            maxLines = 1,
                                        )
                                        Text(
                                            "AIR PLAYR",
                                            color = colors.textPrimary,
                                            fontSize = 9.sp,
                                            lineHeight = 10.sp,
                                            fontFamily = AirPlayrFonts.OpenSansBold,
                                            letterSpacing = .7.sp,
                                        )
                                    }
                                    Surface(
                                        modifier = Modifier.offset(y = playOffset).size(30.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = animatedPlayColor.copy(alpha = .18f),
                                        tonalElevation = 0.dp,
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PlayArrow, "Play", tint = animatedPlayColor, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            inner()
                        },
                    )
                    if (query.isNotEmpty()) IconButton(onClick = { query = ""; focus.clearFocus() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Clear", tint = colors.primary)
                    }
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

        if (selectedIds.isNotEmpty()) {
            val chosen = (audio + video).filter { it.id in selectedIds }
            AirPlayrSelectionBar(
                count = selectedIds.size,
                onClear = { selectedIds = emptySet() },
                onSelectAll = { selectedIds = (audio + video).map { it.id }.toSet() },
                onPlay = chosen.firstOrNull { it.mediaType == MediaType.AUDIO }?.let { song -> ({ playSearchAudio(song, chosen.filter { it.mediaType == MediaType.AUDIO }); selectedIds = emptySet() }) },
                onPlayNext = chosen.filter { it.mediaType == MediaType.AUDIO }.takeIf { it.isNotEmpty() }?.let { songs -> ({ songs.forEach(playback::playNext); selectedIds = emptySet() }) },
                onPlaylist = chosen.firstOrNull { it.mediaType == MediaType.AUDIO }?.let { song -> ({ playlistTarget = song }) },
                onLike = { scope.launch { chosen.filter { it.mediaType == MediaType.AUDIO }.forEach { if (it.id !in favoriteIdSet) container.structured.toggleFavorite(it) }; selectedIds = emptySet() } },
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },
            contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (debounced.isNotBlank()) {
                item { SearchSectionHeader(if (filter == "all") "RESULTS" else FILTERS.first { it.first == filter }.second.uppercase(Locale.ROOT), colors) }
                items(filtered, key = { "result:${it.type}:${it.id}" }) { entry ->
                    val playing = entry.item?.let { it.mediaType == MediaType.AUDIO && playbackState.isPlaying && playbackState.currentId == it.id } == true
                    val id = entry.item?.id
                    SearchContentRow(entry, colors, playing, selected = id in selectedIds, onClick = { if (selectedIds.isNotEmpty() && id != null) selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id else openEntry(entry) }, onLongPress = { if (id != null) selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id }, onMore = { entry.item?.let { selected = it } })
                }
                if (snapshot.ready && filtered.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, null, Modifier.size(42.dp), tint = colors.primary)
                            Text("No results found", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 22.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
                            Text("Nothing matched “$debounced”.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 5.dp))
                        }
                    }
                }
            } else {
                item { SearchSectionHeader("RECENT SEARCHES", colors) }
                if (recentSearches.isEmpty()) {
                    item { Text("Your recent searches will appear here.", modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = colors.textSecondary) }
                } else {
                    items(recentSearches, key = { "search:$it" }) { value ->
                        RecentSearchRow(value, colors, onClick = { query = value; scope.launch { container.preferences.recordRecentSearch(value) } }, onRemove = { scope.launch { container.preferences.removeRecentSearch(value) } })
                    }
                }

                if (recentArtists.isNotEmpty()) {
                    item { SearchSectionHeader("RECENTLY PLAYED ARTISTS", colors) }
                    items(recentArtists, key = { "recent-artist:${it.id}" }) { entry ->
                        SearchArtistRow(
                            entry, colors, selected = entry.item?.id in selectedIds,
                            onClick = { val id = entry.item?.id; if (selectedIds.isNotEmpty() && id != null) selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id else openEntry(entry.copy(type = "artist")) },
                            onLongPress = { entry.item?.id?.let { selectedIds = if (it in selectedIds) selectedIds - it else selectedIds + it } },
                            onMore = { entry.item?.let { selected = it } },
                        )
                    }
                }
                if (recentSongs.isNotEmpty()) {
                    item { SearchSectionHeader("RECENTLY PLAYED SONGS", colors) }
                    items(recentSongs, key = { "recent-song:${it.id}" }) { entry ->
                        val playing = entry.item?.let { playbackState.isPlaying && playbackState.currentId == it.id } == true
                        SearchSongRow(
                            entry, colors, playing, selected = entry.item?.id in selectedIds,
                            onClick = { val id = entry.item?.id; if (selectedIds.isNotEmpty() && id != null) selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id else entry.item?.let { playSearchAudio(it, recentSongs.mapNotNull { e -> e.item }) } },
                            onLongPress = { entry.item?.id?.let { selectedIds = if (it in selectedIds) selectedIds - it else selectedIds + it } },
                            onMore = { entry.item?.let { selected = it } },
                        )
                    }
                }
                if (recentAlbums.isNotEmpty()) {
                    item { SearchSectionHeader("RECENTLY PLAYED ALBUMS", colors) }
                    items(recentAlbums, key = { "recent-album:${it.id}" }) { entry ->
                        SearchAlbumRow(
                            entry, colors, selected = entry.item?.id in selectedIds,
                            onClick = { val id = entry.item?.id; if (selectedIds.isNotEmpty() && id != null) selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id else openEntry(entry.copy(type = "album")) },
                            onLongPress = { entry.item?.id?.let { selectedIds = if (it in selectedIds) selectedIds - it else selectedIds + it } },
                            onMore = { entry.item?.let { selected = it } },
                        )
                    }
                }
                if (recentVideos.isNotEmpty()) {
                    item { SearchSectionHeader("RECENTLY PLAYED VIDEOS", colors) }
                    items(recentVideos, key = { "recent-video:${it.id}" }) { entry ->
                        SearchVideoRow(
                            entry, colors, selected = entry.item?.id in selectedIds,
                            onClick = { val id = entry.item?.id; if (selectedIds.isNotEmpty() && id != null) selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id else entry.item?.let { playSearchVideo(it, recentVideos.mapNotNull { e -> e.item }) } },
                            onLongPress = { entry.item?.id?.let { selectedIds = if (it in selectedIds) selectedIds - it else selectedIds + it } },
                            onMore = { entry.item?.let { selected = it } },
                        )
                    }
                }
                if (snapshot.ready && recentSearches.isEmpty() && recentArtists.isEmpty() && recentSongs.isEmpty() && recentAlbums.isEmpty() && recentVideos.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, null, Modifier.size(42.dp), tint = colors.primary)
                            Text("Your library is empty", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 22.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
                            Text("Your recently played and available media will appear here.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 5.dp))
                        }
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
                    if (item.mediaType == MediaType.VIDEO) {
                        com.airplayr.app.core.playback.VideoQueueStore.playNext(listOf(item))
                        AppFeedback.showSnackbar("Added to video queue as next.")
                    } else {
                        playback.playNext(item)
                        AppFeedback.showSnackbar("Added to playing queue as next.")
                    }
                }
            },
            onAddQueue = {
                selected?.let { item ->
                    selected = null
                    if (item.mediaType == MediaType.VIDEO) {
                        com.airplayr.app.core.playback.VideoQueueStore.add(listOf(item))
                        AppFeedback.showSnackbar("Added to video queue.")
                    } else {
                        playback.addToQueue(item)
                        AppFeedback.showSnackbar("Added to the playing queue.")
                    }
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
                if (item.mediaType == MediaType.VIDEO) Modifier.width(124.dp).height(70.dp) else Modifier.size(92.dp),
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
private fun SearchSectionHeader(title: String, colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 12.sp, letterSpacing = 1.1.sp, color = colors.textPrimary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp))
}

@Composable
private fun RecentSearchRow(value: String, colors: com.airplayr.app.ui.theme.AirPlayrColors, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.History, "Recent search", tint = colors.textSecondary, modifier = Modifier.size(24.dp))
        Text(value, color = colors.textPrimary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 14.dp))
        IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.Close, "Remove recent search", tint = colors.textPrimary) }
    }
}

@Composable
private fun SearchGenericRow(
    entry: SearchEntry,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    onClick: () -> Unit,
) {
    val icon = when (entry.type) {
        "artist" -> AirPlayrIcons.Artist
        "album" -> AirPlayrIcons.AlbumDisc
        "playlist" -> Icons.Default.QueueMusic
        "genre" -> AirPlayrIcons.Music
        "video" -> AirPlayrIcons.Video
        else -> AirPlayrIcons.Music
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(42.dp),
        )

        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                entry.title,
                color = colors.textPrimary,
                fontFamily = AirPlayrFonts.SmoochSans,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                entry.subtitle,
                color = colors.textSecondary,
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchContentRow(entry: SearchEntry, colors: com.airplayr.app.ui.theme.AirPlayrColors, playing: Boolean, selected: Boolean = false, onClick: () -> Unit, onLongPress: () -> Unit, onMore: () -> Unit) {
    if (entry.item == null) {
        SearchGenericRow(entry, colors, onClick)
        return
    }
    val item = entry.item
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongPress).background(if (selected) colors.primaryContainer.copy(alpha=.72f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(
            item.displayArtworkUri(),
            if (item.mediaType == MediaType.VIDEO) Modifier.width(126.dp).height(71.dp) else Modifier.size(64.dp),
            16.dp, entry.title, item.mediaType,
            when (entry.type) {
                "artist" -> AirPlayrIcons.Artist
                "album" -> AirPlayrIcons.AlbumDisc
                "video" -> AirPlayrIcons.Video
                else -> AirPlayrIcons.Music
            }, isPlaying = playing,
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(entry.title, color = colors.textPrimary, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.subtitle, color = colors.textSecondary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AirPlayrMoreButton(onMore)
    }
}

@Composable
private fun SearchSongRow(entry: SearchEntry, colors: com.airplayr.app.ui.theme.AirPlayrColors, playing: Boolean, selected: Boolean = false, onClick: () -> Unit, onLongPress: () -> Unit, onMore: () -> Unit) {
    val item = entry.item ?: return
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongPress).background(if (selected) colors.primaryContainer.copy(alpha=.72f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(item.displayArtworkUri(), Modifier.size(64.dp), 16.dp, entry.title, MediaType.AUDIO, AirPlayrIcons.Music, isPlaying = false)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(entry.title, color = colors.textPrimary, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.subtitle + if (item.durationMs > 0) " · ${formatDuration(item.durationMs)}" else "", color = colors.textSecondary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (playing) Icon(Icons.Default.PlayArrow, "Playing", tint = colors.primary, modifier = Modifier.size(20.dp).padding(end = 2.dp))
        AirPlayrMoreButton(onMore)
    }
}

@Composable
private fun SearchArtistRow(entry: SearchEntry, colors: com.airplayr.app.ui.theme.AirPlayrColors, selected: Boolean = false, onClick: () -> Unit, onLongPress: () -> Unit, onMore: () -> Unit) {
    val item = entry.item ?: return
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongPress).background(if (selected) colors.primaryContainer.copy(alpha=.72f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(item.displayArtworkUri(), Modifier.size(64.dp), 32.dp, entry.title, MediaType.AUDIO, AirPlayrIcons.Artist, isPlaying = false)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(entry.title, color = colors.textPrimary, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.subtitle, color = colors.textSecondary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AirPlayrMoreButton(onMore)
    }
}

@Composable
private fun SearchAlbumRow(entry: SearchEntry, colors: com.airplayr.app.ui.theme.AirPlayrColors, selected: Boolean = false, onClick: () -> Unit, onLongPress: () -> Unit, onMore: () -> Unit) {
    val item = entry.item ?: return
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongPress).background(if (selected) colors.primaryContainer.copy(alpha=.72f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(item.displayArtworkUri(), Modifier.size(104.dp), 18.dp, entry.title, MediaType.AUDIO, AirPlayrIcons.AlbumDisc, isPlaying = false)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(entry.title, color = colors.textPrimary, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(entry.subtitle, color = colors.textSecondary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        AirPlayrMoreButton(onMore)
    }
}

@Composable
private fun SearchVideoRow(entry: SearchEntry, colors: com.airplayr.app.ui.theme.AirPlayrColors, selected: Boolean = false, onClick: () -> Unit, onLongPress: () -> Unit, onMore: () -> Unit) {
    val item = entry.item ?: return
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongPress).background(if (selected) colors.primaryContainer.copy(alpha=.72f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(item.thumbnailUri ?: item.artworkUri, Modifier.width(158.dp).height(89.dp), 16.dp, entry.title, MediaType.VIDEO, AirPlayrIcons.Video, isPlaying = false)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(entry.title, color = colors.textPrimary, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(entry.subtitle + if (item.durationMs > 0) " · ${formatDuration(item.durationMs)}" else "", color = colors.textSecondary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        AirPlayrMoreButton(onClick = onMore)
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
    val isVideo = item.mediaType == MediaType.VIDEO
    val common = buildList {
        add(AirPlayrMediaMenuAction("Details", Icons.Default.Info, onDetails))
        add(AirPlayrMediaMenuAction("Share", Icons.Default.Share, {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType ?: if (isVideo) "video/*" else "audio/*"
                putExtra(Intent.EXTRA_TEXT, item.uri)
                putExtra(Intent.EXTRA_TITLE, item.title)
            }
            runCatching { container.appContext.startActivity(Intent.createChooser(send, "Share ${item.title}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            onClose()
        }))
        add(AirPlayrMediaMenuAction("Delete from device", Icons.Default.Delete, onDelete, destructive = true))
    }
    val groups = if (isVideo) {
        listOf(
            listOf(
                AirPlayrMediaMenuAction("Play next", Icons.Default.PlaylistPlay, onPlayNext),
                AirPlayrMediaMenuAction("Add to video queue", Icons.Default.QueueMusic, onAddQueue),
            ),
            common,
        )
    } else {
        listOf(
            listOf(
                AirPlayrMediaMenuAction("Play next", Icons.Default.PlaylistPlay, onPlayNext),
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
                AirPlayrMediaMenuAction("Tag editor", Icons.Default.Edit, onEditMetadata),
                AirPlayrMediaMenuAction("Edit lyrics", AirPlayrIcons.Lyrics, onEditLyrics),
            ),
            common,
        )
    }
    AirPlayrPremiumMediaActionSheet(
        item = item,
        favorite = favorite,
        playing = playing,
        onFavorite = onFavorite,
        groups = groups,
        onDismiss = onClose,
    )
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
    val service = container.lyricsRepository
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
                            service.fetch(item.id, LyricsTrack(item.title, item.artist.orEmpty(), item.album.orEmpty(), item.durationMs / 1000.0), true)
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
                        val syncedText = if (Regex("\\[\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?\\]").containsMatchIn(lyrics)) lyrics else null
                        val result = LyricsService.Result(
                            provider = "AirPlayr",
                            trackName = item.title,
                            artistName = item.artist.orEmpty(),
                            albumName = item.album.orEmpty(),
                            duration = item.durationMs / 1000.0,
                            plainLyrics = lyrics.ifBlank { null },
                            syncedLyrics = if (syncedText != null) syncedText else null,
                            cues = if (syncedText != null) service.parseLrc(syncedText) else emptyList(),
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
