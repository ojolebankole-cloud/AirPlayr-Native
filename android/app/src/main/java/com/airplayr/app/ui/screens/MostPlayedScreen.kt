@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.airplayr.app.ui.screens

import com.airplayr.app.ui.airPlayrFloatingPlayBottomPadding

import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airplayr.app.R
import com.airplayr.app.core.data.PlaybackStatsRepository
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.airPlayrBottomContentPadding
import com.airplayr.app.ui.components.AirPlayrLongListFastScrollHandle
import com.airplayr.app.ui.components.AirPlayrAlbumRailCard
import com.airplayr.app.ui.components.AirPlayrFeatureCollectionActionSheet
import com.airplayr.app.ui.components.AirPlayrMediaRow
import com.airplayr.app.ui.components.AirPlayrSectionEmpty
import com.airplayr.app.ui.components.Artwork
import com.airplayr.app.ui.components.rememberAirPlayrIllustrationPalette
import com.airplayr.app.ui.components.rememberAirPlayrLottieTheme
import com.airplayr.app.ui.components.displayArtworkUri
import com.airplayr.app.ui.feedback.AppFeedback
import com.airplayr.app.ui.theme.AirPlayrArtworkTokens
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MostPlayedScreen(
    container: AppContainer,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    onOpenVideo: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val mostPlayedListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) }
    val music = snapshot.audio
    val videos = snapshot.video
    val allMedia = remember(music, videos) { music + videos }
    var summary by remember { mutableStateOf<PlaybackStatsRepository.Summary?>(null) }
    var daySummary by remember { mutableStateOf<PlaybackStatsRepository.Summary?>(null) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var playlistTarget by remember { mutableStateOf<List<MediaItem>?>(null) }
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var selectedFavorite by remember { mutableStateOf(false) }
    var detailsTarget by remember { mutableStateOf<MediaItem?>(null) }
    var editTarget by remember { mutableStateOf<MediaItem?>(null) }
    var lyricsTarget by remember { mutableStateOf<MediaItem?>(null) }
    var deleteTarget by remember { mutableStateOf<MediaItem?>(null) }
    var slide by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(allMedia) {
        withContext(Dispatchers.IO) {
            summary = container.stats.summary("all", allMedia, 12)
            daySummary = container.stats.summary("day", allMedia, 12)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            slide = 0
            delay(6_000L)
            slide = 1
            delay(6_000L)
            slide = 2
            delay(15_000L)
        }
    }

    val all = summary
    val day = daySummary
    val topTrack = day?.topSongs?.firstOrNull()
    val topVideo = all?.topVideos?.firstOrNull()
    val topTrackItem = remember(topTrack?.id, music) { topTrack?.let { t -> music.firstOrNull { it.id == t.id } } }
    val topVideoItem = remember(topVideo?.id, videos) { topVideo?.let { v -> videos.firstOrNull { it.id == v.id } } }
    val topSongItems = remember(all?.topSongs, music) { all?.topSongs.orEmpty().mapNotNull { top -> music.firstOrNull { it.id == top.id } } }
    val topVideoItems = remember(all?.topVideos, videos) { all?.topVideos.orEmpty().mapNotNull { top -> videos.firstOrNull { it.id == top.id } } }
    val albums = remember(topSongItems) {
        topSongItems
            .filter { !it.album.isNullOrBlank() }
            .groupBy { (it.album ?: "").trim().lowercase() to (it.albumArtist ?: it.artist ?: "").trim().lowercase() }
            .values.mapNotNull { it.firstOrNull() }
            .take(12)
    }

    fun playSongs(list: List<MediaItem>) {
        if (list.isNotEmpty()) { playback.playQueue(list); onOpenPlayer() }
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            state = mostPlayedListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding()),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary, modifier = Modifier.size(29.dp)) }
                    Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                        Text("AIRPLAYR · PERSONAL", fontFamily = AirPlayrFonts.OpenSans, fontSize = 10.sp, letterSpacing = 1.4.sp, color = colors.textSecondary)
                        Text("Most played", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    }
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Most played options", tint = colors.primary, modifier = Modifier.size(25.dp)) }
                }
            }
            item {
                MostPlayedHero(
                    slide = slide,
                    topTrack = topTrackItem,
                    topVideo = topVideoItem,
                    onTrack = { topTrackItem?.let { playSongs(listOf(it)) } },
                    onVideo = { topVideoItem?.let { onOpenVideo(it.id) } },
                )
            }
            if (topSongItems.isNotEmpty()) {
                item {
                    SectionTitle("Most played songs", "Your music leaderboard", colors)
                }
                items(topSongItems, key = { "most-song:${it.id}" }) { item ->
                    AirPlayrMediaRow(
                        item = item,
                        onOpen = { playSongs(listOf(item)) },
                        onMore = {
                            selected = item
                            scope.launch { selectedFavorite = container.structured.isFavorite(item.id) }
                        },
                        playing = playbackState.isPlaying && playbackState.currentId == item.id,
                    )
                }
            }
            if (albums.isNotEmpty()) {
                item { SectionTitle("Albums", "Albums behind your top tracks", colors) }
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        albums.forEach { album ->
                            val albumSongs = topSongItems.filter {
                                (it.album ?: "Unknown album").equals(album.album ?: "Unknown album", true) &&
                                    (it.albumArtist ?: it.artist ?: "").equals(album.albumArtist ?: album.artist ?: "", true)
                            }
                            AirPlayrAlbumRailCard(
                                artworkUri = album.displayArtworkUri(),
                                albumTitle = album.album ?: "Unknown album",
                                artist = album.albumArtist ?: album.artist,
                                onOpen = { onNavigate("album/${android.net.Uri.encode(album.album ?: "Unknown album")}") },
                                onPlay = { playSongs(albumSongs) },
                                playing = playbackState.isPlaying && albumSongs.any { it.id == playbackState.currentId },
                                size = AirPlayrArtworkTokens.albumRail,
                            )
                        }
                    }
                }
            }
            if (topVideoItems.isNotEmpty()) {
                item { SectionTitle("Most played videos", "Your video leaderboard", colors) }
                items(topVideoItems, key = { "most-video:${it.id}" }) { item ->
                    AirPlayrMediaRow(
                        item = item,
                        onOpen = { onOpenVideo(item.id) },
                        onMore = { selected = item; scope.launch { selectedFavorite = container.structured.isFavorite(item.id) } },
                    )
                }
            }
            if (all == null) {
                item { AirPlayrSectionEmpty("Building your most played", "AirPlayr will build this screen from the music and videos you play on this device.") }
            } else if (topSongItems.isEmpty() && topVideoItems.isEmpty()) {
                item { AirPlayrSectionEmpty("Not enough play data yet", "Play some music or videos and your leaderboard will start to fill in.") }
            }
        }
        AirPlayrLongListFastScrollHandle(mostPlayedListState)

        val fabTarget = topTrackItem ?: topVideoItem
        if (fabTarget != null) {
            FloatingActionButton(
                onClick = {
                    if (fabTarget.mediaType == MediaType.AUDIO) {
                        playSongs(listOf(fabTarget))
                    } else {
                        onOpenVideo(fabTarget.id)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = airPlayrFloatingPlayBottomPadding())
                    .size(68.dp),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 10.dp),
            ) {
                Icon(Icons.Default.PlayArrow, "Play most played", modifier = Modifier.size(31.dp))
            }
        }
    }

    if (menuOpen) {
        AirPlayrFeatureCollectionActionSheet(
            title = "Most played",
            onDismiss = { menuOpen = false },
            onShuffle = {
                val audio = topSongItems.shuffled()
                if (audio.isNotEmpty()) { playback.playQueue(audio); onOpenPlayer() }
                topVideoItems.takeIf { it.isNotEmpty() }?.let { com.airplayr.app.core.playback.VideoQueueStore.add(it) }
                menuOpen = false
            },
            onPlayNext = {
                topSongItems.forEach(playback::playNext)
                com.airplayr.app.core.playback.VideoQueueStore.playNext(topVideoItems)
                menuOpen = false
            },
            onAddToQueue = {
                topSongItems.forEach(playback::addToQueue)
                com.airplayr.app.core.playback.VideoQueueStore.add(topVideoItems)
                menuOpen = false
            },
            onAddToPlaylist = {
                if (topSongItems.isNotEmpty()) { playlistTarget = topSongItems; menuOpen = false }
            },
        )
    }
    selected?.let { item ->
        com.airplayr.app.ui.components.HistoryFavoriteActionSheet(
            item = item,
            favorite = selectedFavorite,
            playing = playbackState.isPlaying && playbackState.currentId == item.id,
            container = container,
            onClose = { selected = null },
            onFavorite = { scope.launch { container.structured.toggleFavorite(item); selected = null } },
            onPlayNext = { if (item.mediaType == MediaType.AUDIO) playback.playNext(item) else com.airplayr.app.core.playback.VideoQueueStore.playNext(listOf(item)); selected = null },
            onAddQueue = { if (item.mediaType == MediaType.AUDIO) playback.addToQueue(item) else com.airplayr.app.core.playback.VideoQueueStore.add(listOf(item)); selected = null },
            onPlaylist = { playlistTarget = listOf(item); selected = null },
            onDetails = { detailsTarget = item; selected = null },
            onDelete = { deleteTarget = item; selected = null },
            onBlacklist = null,
            onGoAlbum = { item.album?.let { onNavigate("album/${android.net.Uri.encode(it)}") }; selected = null },
            onGoArtist = { item.artist?.let { onNavigate("artist/${android.net.Uri.encode(it)}") }; selected = null },
            onGoFolder = { item.relativePath?.let { onNavigate("music-folder/${android.net.Uri.encode(it)}") }; selected = null },
            onEdit = { editTarget = item; selected = null },
            onLyrics = { lyricsTarget = item; selected = null },
        )
    }
    playlistTarget?.let { com.airplayr.app.ui.components.HistoryFavoritePlaylistSheet(container, it) { playlistTarget = null } }
    detailsTarget?.let { item -> com.airplayr.app.ui.components.HistoryFavoriteDetailsSheet(item, playing = playbackState.isPlaying && playbackState.currentId == item.id) { detailsTarget = null } }
    editTarget?.let { item -> com.airplayr.app.ui.components.HistoryFavoriteTagEditorSheet(container, item) { editTarget = null } }
    lyricsTarget?.let { item -> com.airplayr.app.ui.components.HistoryFavoriteLyricsSheet(container, item) { lyricsTarget = null } }
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete from device?") },
            text = { Text("Delete ${item.title} from this device. Android may ask you to confirm the deletion.") },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    deleteTarget = null
                    scope.launch(Dispatchers.IO) {
                        val base = if (item.mediaType == MediaType.AUDIO) android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        val ok = runCatching {
                            container.appContext.contentResolver.delete(
                                android.net.Uri.withAppendedPath(base, item.mediaStoreId?.toString() ?: return@runCatching false),
                                null, null,
                            ) > 0
                        }.getOrDefault(false)
                        if (ok) runCatching { container.mediaLibrary.deltaRefresh(item.mediaType) }
                        AppFeedback.showSnackbar(if (ok) "${item.title} deleted from device." else "Unable to delete ${item.title}.")
                    }
                }) { Text("Delete", color = colors.error) }
            },
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String, colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 13.dp)) {
        Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, lineHeight = 32.sp, color = colors.textPrimary)
        Text(subtitle, fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun MostPlayedHero(
    slide: Int,
    topTrack: MediaItem?,
    topVideo: MediaItem?,
    onTrack: () -> Unit,
    onVideo: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val transition = rememberInfiniteTransition(label = "most-played-hero-gradient")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6500, easing = LinearEasing), RepeatMode.Reverse),
        label = "most-played-hero-phase",
    )
    val clockTransition = rememberInfiniteTransition(label = "most-played-clock")
    val clockAlpha by clockTransition.animateFloat(
        initialValue = .72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "most-played-clock-alpha",
    )
    var now by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = java.util.Calendar.getInstance()
            delay(1_000L)
        }
    }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.airplayr_intro))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = slide == 0,
    )
    val illustrationPalette = rememberAirPlayrIllustrationPalette(
        colors.primary, colors.secondary, colors.tertiary,
    )
    val lottieDynamicProperties = rememberAirPlayrLottieTheme(
        colors = illustrationPalette,
        keyPaths = listOf(
            arrayOf("Layer 4 Outlines 3", "**"),
            arrayOf("Layer 4 Outlines 2", "**"),
            arrayOf("Layer 4 Outlines", "**"),
            arrayOf("Layer 8 Outlines 3", "**"),
            arrayOf("Layer 8 Outlines 2", "**"),
            arrayOf("Layer 8 Outlines", "**"),
            arrayOf("Layer 9 Outlines 3", "**"),
            arrayOf("Layer 9 Outlines 2", "**"),
            arrayOf("Layer 9 Outlines", "**"),
            arrayOf("Layer 5 Outlines 3", "**"),
            arrayOf("Layer 5 Outlines 2", "**"),
            arrayOf("Layer 5 Outlines", "**"),
            arrayOf("Layer 11 Outlines", "Group 2", "**"),
            arrayOf("Layer 11 Outlines", "Group 3", "**"),
            arrayOf("Layer 13 Outlines 2", "**"),
            arrayOf("Layer 13 Outlines", "Group 2", "**"),
            arrayOf("Layer 13 Outlines", "Group 3", "**"),
            arrayOf("Layer 13 Outlines", "Group 4", "**"),
            arrayOf("Layer 13 Outlines", "Group 5", "**"),
            arrayOf("Layer 13 Outlines", "Group 6", "**"),
            arrayOf("Layer 13 Outlines", "Group 7", "**"),
            arrayOf("Layer 13 Outlines", "Group 8", "**"),
            arrayOf("Layer 15 Outlines", "Group 1", "**"),
            arrayOf("Layer 15 Outlines", "Group 2", "**"),
            arrayOf("Layer 15 Outlines", "Group 3", "**"),
            arrayOf("Layer 3 Outlines", "**"),
        ),
    )
    val shape = RoundedCornerShape(32.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(350.dp)
            .padding(horizontal = 22.dp)
            .shadow(8.dp, shape, clip = false)
            .clip(shape)
            .clickable(enabled = when (slide) { 1 -> topTrack != null; 2 -> topVideo != null; else -> false }, onClick = { if (slide == 1) onTrack() else if (slide == 2) onVideo() }),
    ) {
        Crossfade(
            targetState = slide,
            animationSpec = tween(700),
            modifier = Modifier.fillMaxSize(),
            label = "most-played-hero-fade",
        ) { page ->
            when (page) {
                1 -> {
                    if (topTrack != null) Artwork(topTrack.displayArtworkUri(), Modifier.fillMaxSize(), 32.dp, topTrack.title, MediaType.AUDIO, AirPlayrIcons.Music, isPlaying = false)
                    else HeroPlaceholder()
                    HeroScrim()
                    HeroCopy(eyebrow = "TOP TRACK · TODAY", title = topTrack?.title ?: "No top track today", subtitle = topTrack?.artist ?: "Play some music today.")
                    if (topTrack != null) {
                        Surface(Modifier.align(Alignment.BottomEnd).padding(22.dp).size(52.dp).clickable(onClick = onTrack), shape = CircleShape, color = colors.primary) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, "Play top track", tint = colors.onPrimary, modifier = Modifier.size(24.dp)) }
                        }
                    }
                }
                2 -> {
                    if (topVideo != null) MostPlayedVideoPreview(topVideo)
                    else HeroPlaceholder()
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .78f)))))
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Bottom) {
                        Surface(shape = RoundedCornerShape(50.dp), color = Color.Black.copy(alpha = .36f)) {
                            Text("TOP VIDEO", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, letterSpacing = 1.4.sp, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                        }
                        Text(topVideo?.title ?: "No top video today", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 34.sp, lineHeight = 39.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(topVideo?.artist.orEmpty().ifBlank { "Your most played video" }, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = Color.White.copy(alpha = .88f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (topVideo != null) Surface(Modifier.size(52.dp).clickable(onClick = onVideo), shape = CircleShape, color = colors.primary) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, "Open top video", tint = colors.onPrimary, modifier = Modifier.size(24.dp)) }
                            }
                        }
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(
                        listOf(colors.surfaceContainerHigh, colors.surfaceContainer, colors.surfaceContainerHighest, colors.surfaceContainerHigh),
                        start = Offset(-sizeForGradient(phase), 0f),
                        end = Offset(sizeForGradient(phase) + 760f, 560f),
                    )))
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .10f)))
                    Text(
                        text = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(now.time),
                        fontFamily = AirPlayrFonts.OpenSans,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = .5.sp,
                        color = Color.White.copy(alpha = clockAlpha),
                        modifier = Modifier.align(Alignment.TopStart).padding(24.dp),
                    )
                    Column(Modifier.fillMaxSize().padding(24.dp)) {
                        Spacer(Modifier.weight(1f))
                        Text("Your sound. Your story.", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 36.sp, lineHeight = 40.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.widthIn(max = 245.dp))
                        Text("The music and videos you keep coming back to.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 14.sp, lineHeight = 20.sp, color = Color.White.copy(alpha = .92f), modifier = Modifier.padding(top = 7.dp).widthIn(max = 245.dp), maxLines = 2)
                    }
                    LottieAnimation(
                        composition = composition,
                        progress = { lottieProgress },
                        dynamicProperties = lottieDynamicProperties,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).width(190.dp).height(190.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MostPlayedVideoPreview(item: MediaItem) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var player by remember(item.id) { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(item.id) {
        val exo = ExoPlayer.Builder(context).build()
        player = exo
        val start = if (item.durationMs > 10_000L) (item.durationMs / 2L - 5_000L).coerceAtLeast(0L) else 0L
        exo.setMediaItem(
            androidx.media3.common.MediaItem.Builder().setMediaId("most-played-preview:${item.id}").setUri(item.uri).build(),
            start,
        )
        exo.volume = 0f
        exo.repeatMode = Player.REPEAT_MODE_ONE
        exo.prepare()
        exo.playWhenReady = true
        onDispose { exo.release(); player = null }
    }
    player?.let { AndroidVideoSurface(it, VideoFitMode.ZOOM, Modifier.fillMaxSize()) }
}

private fun sizeForGradient(phase: Float): Float = 560f + (phase * 420f)

@Composable
private fun HeroScrim() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .06f), Color.Black.copy(alpha = .80f)))
        )
    )
}

@Composable
private fun HeroCopy(eyebrow: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Bottom) {
        Surface(shape = RoundedCornerShape(50.dp), color = Color.Black.copy(alpha = .34f)) {
            Text(eyebrow, fontFamily = AirPlayrFonts.OpenSans, fontSize = 10.sp, letterSpacing = 1.5.sp, color = Color.White, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
        }
        Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 33.sp, lineHeight = 38.sp, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(subtitle, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, lineHeight = 19.sp, color = Color.White.copy(alpha = .88f), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun HeroPlaceholder() {
    val colors = LocalAirPlayrColors.current
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer, colors.tertiaryContainer))))
}
