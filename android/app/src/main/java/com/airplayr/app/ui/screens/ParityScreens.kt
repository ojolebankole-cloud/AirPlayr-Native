package com.airplayr.app.ui.screens

import com.airplayr.app.ui.theme.LocalAirPlayrColors
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.components.AirPlayrCard
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
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
import com.airplayr.app.ui.components.AIRPLAYR_FLOATING_PLAY_BOTTOM
import com.airplayr.app.ui.components.AIRPLAYR_FLOATING_PLAY_BOTTOM_WITH_MINI
import com.airplayr.app.ui.components.displayArtworkUri
import com.airplayr.app.ui.components.AirPlayrScreenHeader
import com.airplayr.app.ui.components.AirPlayrSectionEmpty
import com.airplayr.app.ui.components.ActionGroup
import com.airplayr.app.ui.components.ActionButton
import com.airplayr.app.ui.theme.AirPlayrColors
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FeatureCollectionScreen(
    title: String,
    subtitle: String,
    loader: suspend () -> List<MediaItem>,
    playback: PlaybackController,
    container: AppContainer? = null,
    onBack: () -> Unit = {},
    onVideoOpen: (String) -> Unit = {},
    onOpenPlayer: () -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var favorite by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    val isTime = title == "Tuned in"
    val profile = remember(now.get(java.util.Calendar.HOUR_OF_DAY)) {
        when (now.get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..8 -> Triple("MORNING", "Start soft. Stay inspired.", "A bright little set for the first stretch of your day.")
            in 9..11 -> Triple("MORNING", "Start soft. Stay inspired.", "A bright little set for the first stretch of your day.")
            in 12..16 -> Triple("AFTERNOON", "Keep the day moving.", "A warm, forward-moving mix for the middle of the day.")
            in 17..21 -> Triple("EVENING", "Ease into the evening.", "A balanced soundtrack for the hours after the rush.")
            else -> Triple("NIGHT", "Settle into the night.", "A softer soundtrack for the quiet hours.")
        }
    }
    LaunchedEffect(title) { loading = true; items = runCatching { loader() }.getOrDefault(emptyList()); loading = false }
    LaunchedEffect(isTime) { if (isTime) while (true) { kotlinx.coroutines.delay(30_000); now = java.util.Calendar.getInstance() } }
    val playbackState by playback.state.collectAsStateWithLifecycle()

    fun playAll(shuffle: Boolean) {
        if (items.isEmpty()) return
        val ordered = if (shuffle) items.shuffled() else items
        val audioItems = ordered.filter { it.mediaType == MediaType.AUDIO }
        val videoItems = ordered.filter { it.mediaType == MediaType.VIDEO }
        if (audioItems.isNotEmpty()) {
            playback.playQueue(audioItems, 0)
        } else if (videoItems.isNotEmpty()) {
            onVideoOpen(if (shuffle) videoItems.random().id else videoItems.first().id)
        }
        menuOpen = false
    }
    val hero = items.firstOrNull()
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary) }
                    Column(Modifier.weight(1f)) {
                        Text(if (isTime) "AIRPLAYR · ${profile.first}" else "AIRPLAYR · PERSONAL", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, letterSpacing = 1.3.sp, color = colors.textPrimary)
                        Text(if (isTime) "Tuned in" else title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 29.sp, color = colors.textPrimary)
                    }
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Options", tint = colors.primary) }
                }
            }
            if (hero != null) item {
                FeatureHero(
                    item = hero, title = when (title) {
                        "Rock your world" -> hero.title
                        "Rediscover" -> hero.title
                        "For you now" -> "A mix that feels like you."
                        "Tuned in" -> profile.second
                        else -> title
                    },
                    subtitle = when (title) {
                        "Rock your world" -> hero.artist ?: "Unknown artist"
                        "Rediscover" -> "A personal rewind from your recent watching."
                        "For you now" -> "Built from your recent plays, favorites and listening patterns — no generic playlist required."
                        "Tuned in" -> profile.third
                        else -> subtitle
                    },
                    badge = when (title) { "Rock your world" -> "HIGH ENERGY · ${items.size} SONGS"; "Rediscover" -> "THE REWIND · ${items.size} VIDEOS"; "Tuned in" -> "${profile.first} MIX · ${items.size} SONGS"; else -> "MADE FOR YOU" },
                    colors = colors,
                    onPlay = { if (hero.mediaType == MediaType.AUDIO) playback.playQueue(items, 0) else onVideoOpen(hero.id) },
                    onShuffle = { playAll(true) }
                )
            } else if (!loading) item {
                AirPlayrSectionEmpty("Your collection is waiting", "Use AirPlayr for a while and this space will build from your local library.")
            }
            if (items.isNotEmpty() && title == "Tuned in") item {
                AirPlayrCard(Modifier.fillMaxWidth().padding(22.dp, 18.dp), shape = RoundedCornerShape(26.dp), containerColor = colors.surfaceContainer, elevation = 5.dp) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(48.dp), shape = androidx.compose.foundation.shape.CircleShape, color = colors.tertiaryContainer) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("✦", color = colors.tertiary, fontSize = 22.sp) } }
                        Column(Modifier.padding(start = 12.dp)) { Text("A different mood, every part of the day.", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 16.sp, color = colors.textPrimary); Text("AirPlayr reshapes this card around your local time without needing a new playlist.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, lineHeight = 18.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp)) }
                    }
                }
            }
            if (items.isNotEmpty()) item {
                Text(if (title == "Rediscover") "Worth the rewind" else if (title == "Rock your world") "The loud side" else if (title == "For you now") "Because you played it" else "The set", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
                Text(if (title == "Rediscover") "Your unfinished and recently watched stories." else if (title == "Rock your world") "Songs ready when you want more energy." else if (title == "For you now") "A few familiar sounds to bring you back in." else "A hand-picked sequence for this moment.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 22.dp))
            }
            items(items, key = { it.id }) { item ->
                FeatureMediaRow(item, colors, onPlay = { if (item.mediaType == MediaType.AUDIO) playback.play(item) else onVideoOpen(item.id) }, onMore = { selected = item; scope.launch { favorite = container?.structured?.isFavorite(item.id) ?: false } })
            }
        }
        if (items.isNotEmpty()) FloatingActionButton(
            onClick = {
                if (items.first().mediaType == MediaType.VIDEO) {
                    onVideoOpen(items.first().id)
                } else {
                    playAll(false)
                    onOpenPlayer()
                }
            },
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = if (playbackState.currentId != null && playbackState.currentMediaType == MediaType.AUDIO) AIRPLAYR_FLOATING_PLAY_BOTTOM_WITH_MINI.dp else AIRPLAYR_FLOATING_PLAY_BOTTOM.dp).size(68.dp),
            shape = RoundedCornerShape(22.dp),
        ) { Icon(Icons.Default.PlayArrow, "Play mix", modifier = Modifier.size(31.dp)) }
        if (menuOpen) {
            ModalBottomSheet(onDismissRequest = { menuOpen = false }, containerColor = colors.surfaceContainerLow) {
                ActionGroup {
                    ActionButton("Shuffle", Icons.Default.Shuffle) { playAll(true) }
                    ActionButton("Add to queue", Icons.Default.QueueMusic) {
                        val audio = items.filter { it.mediaType == MediaType.AUDIO }
                        audio.forEach(playback::addToQueue)
                        menuOpen = false
                        com.airplayr.app.ui.feedback.AppFeedback.showSnackbar(if (audio.isEmpty()) "Videos do not use the music queue." else "Added ${audio.size} songs to the playing queue.")
                    }
                    ActionButton("Play next", Icons.Default.SkipNext) {
                        val audio = items.filter { it.mediaType == MediaType.AUDIO }
                        audio.forEach(playback::playNext)
                        menuOpen = false
                        if (audio.isEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Videos do not use the music queue.")
                    }
                }
            }
        }
        selected?.let { item ->
            ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = colors.surfaceContainerLow) {
                ActionGroup {
                    ActionButton(if (favorite) "Remove from favorites" else "Add to favorites", if (favorite) Icons.Default.Star else Icons.Default.StarBorder) { scope.launch { val active = container?.structured?.toggleFavorite(item) ?: false; favorite = active; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar(if (active) "Added to favorites." else "Removed from favorites.") }; selected = null }
                    ActionButton("Play next", Icons.Default.SkipNext) {
                        if (item.mediaType == MediaType.AUDIO) playback.playNext(item)
                        else com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Videos do not use the music queue.")
                        selected = null
                    }
                    ActionButton("Add to queue", Icons.Default.QueueMusic) {
                        if (item.mediaType == MediaType.AUDIO) {
                            playback.addToQueue(item)
                            com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Added to the playing queue.")
                        } else {
                            com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Videos do not use the music queue.")
                        }
                        selected = null
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHero(item: MediaItem, title: String?, subtitle: String, badge: String, colors: com.airplayr.app.ui.theme.AirPlayrColors, onPlay: () -> Unit, onShuffle: () -> Unit) {
    val art = item.displayArtworkUri()
    Box(Modifier.fillMaxWidth().height(360.dp).padding(horizontal = 22.dp).clip(RoundedCornerShape(30.dp)).airPlayrShadow(RoundedCornerShape(30.dp), 6.dp).clickable { onPlay() }) {
        if (art != null) com.airplayr.app.ui.components.Artwork(art, Modifier.fillMaxSize(), 30.dp, item.title, item.mediaType, if (item.mediaType == MediaType.VIDEO) VideoLibrary else MusicNote)
        else Box(Modifier.fillMaxSize().background(colors.primaryContainer))
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .42f)))
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(shape = RoundedCornerShape(30.dp), color = colors.surfaceContainerHighest) { Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = colors.primary, modifier = Modifier.size(14.dp)); Text(badge, fontFamily = AirPlayrFonts.OpenSans, fontSize = 10.sp, letterSpacing = .8.sp, color = colors.textPrimary, modifier = Modifier.padding(start = 6.dp)) } }
                IconButton(onClick = onShuffle, modifier = Modifier.size(46.dp)) { Surface(Modifier.fillMaxSize(), shape = androidx.compose.foundation.shape.CircleShape, color = colors.primary) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Shuffle, "Shuffle", tint = colors.onPrimary) } } }
            }
            Spacer(Modifier.weight(1f))
            Text(title ?: item.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 34.sp, lineHeight = 40.sp, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontFamily = AirPlayrFonts.OpenSans, fontSize = 14.sp, lineHeight = 20.sp, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("Tap to start the mix", fontSize = 12.sp, color = colors.textPrimary); Surface(Modifier.size(50.dp), shape = androidx.compose.foundation.shape.CircleShape, color = colors.primary) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, "Play", tint = colors.onPrimary, modifier = Modifier.size(22.dp)) } } }
        }
    }
}

@Composable
private fun FeatureMediaRow(item: MediaItem, colors: com.airplayr.app.ui.theme.AirPlayrColors, onPlay: () -> Unit, onMore: () -> Unit) {
    AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 5.dp), shape = RoundedCornerShape(22.dp), containerColor = colors.surfaceContainer, elevation = 5.dp) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            com.airplayr.app.ui.components.Artwork(item.displayArtworkUri(), Modifier.size(82.dp), 18.dp, item.title, item.mediaType, if (item.mediaType == MediaType.VIDEO) VideoLibrary else MusicNote)
            Column(Modifier.weight(1f).padding(horizontal = 13.dp)) { Text(item.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(item.artist ?: if (item.mediaType == MediaType.VIDEO) "Video" else "Unknown artist", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textSecondary, maxLines = 1, modifier = Modifier.padding(top = 2.dp)) }
            IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, "Options", tint = colors.primary) }
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
    onOpenPlayer: () -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
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
    val playbackState by playback.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ListTopBar(value, "${filtered.size} items")
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { item -> MediaListCard(item) { playback.play(item) } }
            }
        }
        if (filtered.any { it.mediaType == MediaType.AUDIO }) {
            FloatingActionButton(
                onClick = {
                    val songs = filtered.filter { it.mediaType == MediaType.AUDIO }
                    if (songs.isNotEmpty()) {
                        playback.playQueue(songs, 0)
                        onOpenPlayer()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = if (playbackState.currentId != null && playbackState.currentMediaType == MediaType.AUDIO) AIRPLAYR_FLOATING_PLAY_BOTTOM_WITH_MINI.dp else AIRPLAYR_FLOATING_PLAY_BOTTOM.dp)
                    .size(68.dp),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp),
            ) {
                Icon(Icons.Default.PlayArrow, "Play", modifier = Modifier.size(31.dp))
            }
        }
    }
}

@Composable
fun RecentScreen(
    type: String,
    viewModel: LibraryViewModel,
    playback: PlaybackController,
    onOpen: (String) -> Unit,
    container: AppContainer? = null,
    onOpenPlayer: () -> Unit = {},
) {
    val audio by viewModel.audio.collectAsStateWithLifecycle()
    val video by viewModel.video.collectAsStateWithLifecycle()
    val history = remember(container) { container?.structured?.history(100) ?: kotlinx.coroutines.flow.flowOf(emptyList()) }
    val historyRows by history.collectAsStateWithLifecycle(emptyList())
    val playbackState by playback.state.collectAsStateWithLifecycle()
    val data = when (type) {
        "songs" -> audio.sortedByDescending { it.dateAdded }.take(100)
        "albums" -> historyRows.filter { it.mediaType == MediaType.AUDIO.value }.mapNotNull { h -> audio.firstOrNull { it.id == h.mediaId } }
            .groupBy { it.album ?: "Unknown album" }.values.mapNotNull { it.firstOrNull() }.take(100)
        "artists" -> historyRows.filter { it.mediaType == MediaType.AUDIO.value }.mapNotNull { h -> audio.firstOrNull { it.id == h.mediaId } }
            .groupBy { it.artist ?: "Unknown artist" }.values.mapNotNull { it.firstOrNull() }.take(100)
        "videos-added" -> video.sortedByDescending { it.dateAdded }.take(100)
        else -> historyRows.filter { it.mediaType == MediaType.VIDEO.value }.mapNotNull { h -> video.firstOrNull { it.id == h.mediaId } }.take(100)
    }
    val audioItems = remember(data, type, audio) {
        when (type) {
            "albums" -> {
                val albumNames = data.map { it.album ?: "Unknown album" }.toSet()
                audio.filter { (it.album ?: "Unknown album") in albumNames }
            }
            "artists" -> {
                val artistNames = data.map { it.artist ?: "Unknown artist" }.toSet()
                audio.filter { (it.artist ?: "Unknown artist") in artistNames }
            }
            else -> data.filter { it.mediaType == MediaType.AUDIO }
        }
    }
    val title = when (type) {
        "songs" -> "Recently added songs"
        "albums" -> "Recently played albums"
        "artists" -> "Recent Artists"
        "videos-added" -> "Recently added videos"
        else -> "Recently played videos"
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ListTopBar(title, "${data.size} local items")
            if (data.isEmpty()) EmptyNativeState("Nothing here yet", "AirPlayr will build this section as your library and listening history grows.")
            else LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(data, key = { it.id }) { item ->
                    MediaListCard(item) {
                        if (item.mediaType == MediaType.AUDIO) playback.play(item) else onOpen(item.id)
                    }
                }
            }
        }
        if (data.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    if (audioItems.isNotEmpty()) {
                        playback.playQueue(audioItems, 0)
                        onOpenPlayer()
                    } else {
                        data.firstOrNull()?.let { onOpen(it.id) }
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = if (playbackState.currentId != null && playbackState.currentMediaType == MediaType.AUDIO) AIRPLAYR_FLOATING_PLAY_BOTTOM_WITH_MINI.dp else AIRPLAYR_FLOATING_PLAY_BOTTOM.dp).size(68.dp),
                containerColor = com.airplayr.app.ui.theme.LocalAirPlayrColors.current.primary,
                contentColor = com.airplayr.app.ui.theme.LocalAirPlayrColors.current.onPrimary,
                shape = RoundedCornerShape(22.dp),
            ) {
                Icon(Icons.Default.PlayArrow, "Play", modifier = Modifier.size(31.dp))
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyPlaylistsScreen(container: AppContainer, playback: PlaybackController, onOpen: (String) -> Unit, onBack: () -> Unit = {}) {
    val colors = LocalAirPlayrColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val playlists by container.structured.playlists().collectAsStateWithLifecycle(emptyList())
    val library by container.mediaLibrary.observe(MediaType.AUDIO).collectAsStateWithLifecycle(initialValue = emptyList())
    var selected by remember { mutableStateOf<com.airplayr.app.core.data.db.MusicPlaylistEntity?>(null) }
    var headerMenu by remember { mutableStateOf(false) }
    var fabOpen by remember { mutableStateOf(false) }
    var dialogMode by remember { mutableStateOf("create") }
    var dialogName by remember { mutableStateOf("") }
    var dialogOpen by remember { mutableStateOf(false) }
    var targetPicker by remember { mutableStateOf(false) }
    var targetSource by remember { mutableStateOf<com.airplayr.app.core.data.db.MusicPlaylistEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<com.airplayr.app.core.data.db.MusicPlaylistEntity?>(null) }
    val artworkLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        val p = selected
        if (uri != null && p != null) scope.launch { container.structured.setPlaylistArtwork(p.id, uri.toString()); selected = null; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist artwork updated.") }
    }
    var importPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }.toList()
                val normalized = { value: String -> value.lowercase().replace("\\", "/") }
                val matched = lines.mapNotNull { line -> library.firstOrNull { song -> val key=normalized(line); val u=normalized(song.uri); val f=normalized(song.filename.ifBlank { song.title }); key==u || key.endsWith("/$f") || key.endsWith(f) } }.distinctBy { it.id }
                if (matched.isEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("No matching songs from this device were found.") else { val p=container.structured.createPlaylist("Imported playlist"); matched.forEach { container.structured.addToPlaylist(p.id,it.id) }; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist imported: ${matched.size} songs.") }
            }.onFailure { com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("The playlist could not be imported.") }
        }
    }
    var exportPlaylist by remember { mutableStateOf<com.airplayr.app.core.data.db.MusicPlaylistEntity?>(null) }
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        val p=exportPlaylist; exportPlaylist=null
        if (uri != null && p != null) scope.launch { runCatching { val content="#EXTM3U\n"+itemsOf(p).joinToString("\n") { "#EXTINF:${it.durationMs/1000},${it.title} - ${it.artist.orEmpty()}\n${it.uri}" }+"\n"; context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist saved.") }.onFailure { com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("The playlist could not be saved.") } }
    }
    val idMap by produceState(initialValue = emptyMap<String, List<MediaItem>>(), playlists, library) {
        value = playlists.associate { p -> p.id to runCatching { container.structured.playlistIds(p.id).mapNotNull { id -> library.firstOrNull { it.id == id } } }.getOrDefault(emptyList()) }
    }
    fun itemsOf(p: com.airplayr.app.core.data.db.MusicPlaylistEntity) = idMap[p.id].orEmpty()
    fun createOrRename() {
        val n = dialogName.trim().ifBlank { "New playlist" }
        scope.launch {
            if (dialogMode == "rename" && selected != null) { container.structured.renamePlaylist(selected!!.id, n); com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist renamed.") }
            else { val created = container.structured.createPlaylist(n); com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist created."); onOpen(created.id) }
            dialogOpen = false; selected = null; dialogName = ""
        }
    }
    fun playPlaylist(p: com.airplayr.app.core.data.db.MusicPlaylistEntity, shuffle: Boolean = false) { val rows = itemsOf(p); if (rows.isEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("This playlist has no songs yet.") else { val ordered = if (shuffle) rows.shuffled() else rows; playback.playQueue(ordered, 0); selected = null } }
    fun queuePlaylist(p: com.airplayr.app.core.data.db.MusicPlaylistEntity, next: Boolean) { val rows = itemsOf(p); if (rows.isEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("This playlist has no songs yet.") else { rows.forEach { if (next) playback.playNext(it) else playback.addToQueue(it) }; selected = null; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("${rows.size} songs ${if (next) "added as next" else "added to the queue"}.") } }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary) }
                    Column(Modifier.weight(1f)) { Text("AIRPLAYR · YOUR ARCHIVE", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 11.sp, letterSpacing = 1.2.sp, color = colors.textPrimary); Text("My playlists", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 30.sp, color = colors.textPrimary) }
                    IconButton(onClick = { headerMenu = true }) { Icon(Icons.Default.MoreVert, "My playlists menu", tint = colors.primary) }
                }
            }
            item {
                AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 22.dp), shape = RoundedCornerShape(30.dp), containerColor = colors.primaryContainer, elevation = 5.dp) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(54.dp), shape = androidx.compose.foundation.shape.CircleShape, color = colors.primary) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.QueueMusic, null, tint = colors.onPrimary) } }; Column(Modifier.padding(start = 12.dp)) { Text("Your archive, your rules", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 21.sp, color = colors.textPrimary); Text("${playlists.size} playlists · ${idMap.values.sumOf { it.size }} songs", fontSize = 12.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp)) } }
                }
            }
            item { Text("Your playlists", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 24.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) }
            if (playlists.isEmpty()) item { AirPlayrSectionEmpty("No playlists yet", "Create a playlist and your archive will appear here.") }
            items(playlists, key = { it.id }) { p ->
                val rows = itemsOf(p)
                AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 22.dp), shape = RoundedCornerShape(24.dp), containerColor = colors.surfaceContainer, elevation = 5.dp) {
                    Column(Modifier.clickable { onOpen(p.id) }.padding(10.dp)) {
                        Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceVariant)) {
                            if (p.artworkUri != null) com.airplayr.app.ui.components.Artwork(p.artworkUri, Modifier.fillMaxSize(), 20.dp, p.name)
                            else if (rows.isNotEmpty()) Row(Modifier.fillMaxSize()) { rows.take(4).forEach { r -> com.airplayr.app.ui.components.Artwork(r.artworkUri, Modifier.weight(1f).fillMaxHeight(), 0.dp, r.title) } }
                            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.QueueMusic, null, tint = colors.primary, modifier = Modifier.size(50.dp)) }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(p.name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${rows.size} ${if (rows.size == 1) "song" else "songs"}", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp)) }; IconButton(onClick = { selected = p }) { Icon(Icons.Default.MoreVert, "Playlist options", tint = colors.primary) } }
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 24.dp), horizontalAlignment = Alignment.End) {
            if (fabOpen) Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
                SmallFabOption("External playlist", Icons.Default.Public) { fabOpen = false; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("External playlist URLs are not connected to AirPlayr yet.") }
                SmallFabOption("Import playlist (.m3u)", Icons.Default.FileDownload) { fabOpen = false; importPickerLauncher.launch(arrayOf("audio/x-mpegurl","application/x-mpegURL","text/plain","*/*")) }
                SmallFabOption("Create playlist", Icons.Default.List) { fabOpen = false; dialogMode = "create"; dialogName = ""; dialogOpen = true }
            }
            Spacer(Modifier.height(10.dp))
            FloatingActionButton(onClick = { fabOpen = !fabOpen }, containerColor = if (fabOpen) colors.surfaceContainerHigh else colors.primary, contentColor = if (fabOpen) colors.primary else colors.onPrimary, modifier = Modifier.size(88.dp), shape = RoundedCornerShape(28.dp)) { Icon(if (fabOpen) Icons.Default.Close else Icons.Default.Add, if (fabOpen) "Close" else "Add", modifier = Modifier.size(34.dp)) }
        }
        if (headerMenu) ModalBottomSheet(onDismissRequest = { headerMenu = false }, containerColor = colors.surfaceContainerLow) { ActionGroup { ActionButton("Shuffle all playlist items", Icons.Default.Shuffle) { val all=playlists.flatMap { itemsOf(it) }.shuffled(); if(all.isEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("There are no songs in your playlists yet.") else playback.playQueue(all,0); headerMenu=false }; ActionButton("Add all items to queue", Icons.Default.QueueMusic) { val all=playlists.flatMap { itemsOf(it) }; all.forEach(playback::addToQueue); headerMenu=false; if(all.isNotEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Added ${all.size} songs to the playing queue.") }; ActionButton("Refresh playlists", Icons.Default.Refresh) { headerMenu = false; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlists refreshed.") } } }
        selected?.let { p -> ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = colors.surfaceContainerLow) { ActionGroup { ActionButton("Play", Icons.Default.PlayArrow) { playPlaylist(p); }; ActionButton("Play next", Icons.Default.SkipNext) { queuePlaylist(p, true) }; ActionButton("Add to queue", Icons.Default.QueueMusic) { queuePlaylist(p, false) }; ActionButton("Add to playlist", Icons.Default.PlaylistAdd) { targetSource=p; targetPicker = true; selected=null }; ActionButton("Choose artwork", Icons.Default.Image) { artworkLauncher.launch("image/*") }; ActionButton("Rename", Icons.Default.Edit) { dialogMode = "rename"; dialogName = p.name; dialogOpen = true }; ActionButton("Save as file", Icons.Default.Save) { exportPlaylist=p; selected=null; exportLauncher.launch("${p.name}.m3u") }; ActionButton("Share playlist", Icons.Default.Share) { val text = "${p.name}\n\n${itemsOf(p).joinToString("\n") { it.title + (it.artist?.let { a -> " · $a" } ?: "") }}"; val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text) }; context.startActivity(android.content.Intent.createChooser(send, "Share playlist").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)); selected = null }; ActionButton("Delete playlist", Icons.Default.Delete, destructive = true) { deleteTarget = p; selected = null } } } }
        if (targetPicker) ModalBottomSheet(onDismissRequest = { targetPicker = false }, containerColor = colors.surfaceContainerLow) { ActionGroup { playlists.filter { it.id != selected?.id }.forEach { target -> ActionButton(target.name, Icons.Default.List) { val ids = targetSource?.let { itemsOf(it).map { m -> m.id } }.orEmpty(); scope.launch { ids.forEach { container.structured.addToPlaylist(target.id, it) }; targetPicker = false; targetSource = null; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Added to ${target.name}.") } } } } }
        if (dialogOpen) AlertDialog(onDismissRequest = { dialogOpen = false }, title = { Text(if (dialogMode == "rename") "Rename playlist" else "Create playlist") }, text = { OutlinedTextField(dialogName, { dialogName = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) }, dismissButton = { TextButton(onClick = { dialogOpen = false }) { Text("Cancel") } }, confirmButton = { TextButton(onClick = { createOrRename() }) { Text(if (dialogMode == "rename") "Save" else "OK") } })
        deleteTarget?.let { p -> AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete playlist?") }, text = { Text("“${p.name}” will be removed. Your media stays on the device.") }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }, confirmButton = { TextButton(onClick = { scope.launch { container.structured.deletePlaylist(p.id); deleteTarget = null; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist deleted.") } }) { Text("Delete", color = colors.error) } }) }
    }
}

@Composable private fun SmallFabOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { val c=LocalAirPlayrColors.current; Row(Modifier.clickable(onClick=onClick), verticalAlignment=Alignment.CenterVertically) { Surface(shape=RoundedCornerShape(12.dp), color=c.surfaceContainerHigh) { Text(label, color=c.textPrimary, fontFamily=AirPlayrFonts.SmoochSans, fontSize=15.sp, modifier=Modifier.padding(horizontal=15.dp,vertical=11.dp)) }; Spacer(Modifier.width(10.dp)); FloatingActionButton(onClick=onClick, modifier=Modifier.size(58.dp), containerColor=c.surfaceContainerHigh, contentColor=c.primary, shape=RoundedCornerShape(18.dp)) { Icon(icon,null,modifier=Modifier.size(22.dp)) } } }

@Composable
fun PlaylistDetailScreen(
    container: AppContainer,
    id: String,
    playback: PlaybackController,
    onOpenPlayer: () -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
    val library by container.mediaLibrary.observe(MediaType.AUDIO).collectAsStateWithLifecycle(initialValue = emptyList())
    var name by remember { mutableStateOf("Playlist") }
    var ids by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(id) {
        val p = container.database.userDataDao().playlist(id)
        name = p?.name ?: "Playlist"
        ids = container.structured.playlistIds(id)
    }
    val items = library.filter { it.id in ids }.sortedBy { ids.indexOf(it.id) }
    val playbackState by playback.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ListTopBar(name, "${items.size} songs")
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Button(onClick = { playback.playQueue(items); if (items.isNotEmpty()) onOpenPlayer() }, enabled = items.isNotEmpty()) { Text("Play playlist") } }
                items(items, key = { it.id }) { item -> MediaListCard(item) { playback.play(item) } }
            }
        }
        if (items.isNotEmpty()) {
            FloatingActionButton(
                onClick = { playback.playQueue(items); onOpenPlayer() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = if (playbackState.currentId != null && playbackState.currentMediaType == MediaType.AUDIO) AIRPLAYR_FLOATING_PLAY_BOTTOM_WITH_MINI.dp else AIRPLAYR_FLOATING_PLAY_BOTTOM.dp)
                    .size(68.dp),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp),
            ) {
                Icon(Icons.Default.PlayArrow, "Play playlist", modifier = Modifier.size(31.dp))
            }
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
