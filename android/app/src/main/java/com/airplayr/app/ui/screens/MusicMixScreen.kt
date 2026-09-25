package com.airplayr.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import kotlin.math.ceil
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.intelligence.MusicMixConfig
import com.airplayr.app.core.intelligence.MusicMixGenerator
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.airPlayrBottomContentPadding
import com.airplayr.app.ui.airPlayrFloatingPlayBottomPadding
import com.airplayr.app.ui.components.AirPlayrWaveAnimation
import com.airplayr.app.ui.components.AirPlayrFeatureCollectionActionSheet
import com.airplayr.app.ui.components.AirPlayrPremiumMediaActionSheet
import com.airplayr.app.ui.components.AirPlayrMediaMenuAction
import com.airplayr.app.ui.components.HistoryFavoritePlaylistSheet
import com.airplayr.app.ui.components.AirPlayrMediaArtwork
import com.airplayr.app.ui.components.Artwork
import com.airplayr.app.ui.components.displayArtworkUri
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val FOR_YOU_HERO_TITLE = "Mix made for you now"
private const val FOR_YOU_HERO_DESCRIPTION = "Build from your recent plays, favourite and listening pattern"

@Composable
fun MusicMixScreen(
    mode: MusicMixMode,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    container: AppContainer,
    onBack: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    val today by rememberCurrentDay()
    val hour by rememberCurrentHour()
    var cycle by remember(mode, today, selectedGenre, hour) { mutableStateOf(0) }
    var mixPlaybackActive by remember(mode, today, selectedGenre, hour) { mutableStateOf(false) }
    var headerMenu by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<MediaItem?>(null) }
    var selectedSongFavorite by remember { mutableStateOf(false) }
    var playlistTarget by remember { mutableStateOf<List<MediaItem>?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val config = remember(mode, hour) {
        when (mode) {
            MusicMixMode.FOR_YOU -> MusicMixConfig(MusicMixGenerator.FOR_YOU_ID, FOR_YOU_HERO_TITLE)
            MusicMixMode.TIME_OF_DAY -> MusicMixGenerator.configForHour(hour)
            MusicMixMode.ROCK_YOUR_WORLD -> MusicMixConfig(MusicMixGenerator.ROCK_YOUR_WORLD_ID, "Rock Your World")
        }
    }
    val genres = remember(snapshot.audio) { MusicMixGenerator.availableGenres(snapshot.audio) }

    var mix by remember(config.id, today) {
        mutableStateOf<List<MediaItem>>(emptyList())
    }
    var mixLoading by remember(config.id, today, selectedGenre) { mutableStateOf(true) }
    LaunchedEffect(config.id, selectedGenre, today, snapshot.audio, snapshot.favorites) {
        if (!snapshot.ready) return@LaunchedEffect
        mixLoading = true
        val generated = withContext(Dispatchers.Default) {
            if (mode == MusicMixMode.ROCK_YOUR_WORLD) {
                MusicMixGenerator.rockYourWorldMix(
                    library = snapshot.audio,
                    history = snapshot.history,
                    favorites = snapshot.favorites,
                    selectedGenre = selectedGenre,
                    date = today,
                    cycle = cycle,
                )
            } else {
                MusicMixGenerator.dailyMix(
                    config = config,
                    library = snapshot.audio,
                    history = snapshot.history,
                    favorites = snapshot.favorites,
                    selectedGenre = selectedGenre,
                    date = today,
                    cycle = cycle,
                )
            }
        }
        // Publish the newly generated filter result only after the whole computation is complete.
        // The previous mix stays visible while this runs, so changing filters never flashes an
        // empty state or makes the user wait for the screen to reconstruct itself.
        mix = generated
        mixLoading = false
    }

    LaunchedEffect(config.id, selectedGenre, today, mixPlaybackActive) {
        if (!mixPlaybackActive || mix.isEmpty()) return@LaunchedEffect
        var activeMix = mix
        var activeCycle = cycle
        while (isActive) {
            val state = playback.stableState.value
            val atNaturalEnd = !state.isPlaying &&
                state.queueSize == activeMix.size &&
                state.currentQueueIndex >= activeMix.lastIndex &&
                state.durationMs > 0L &&
                state.positionMs >= (state.durationMs - 750L).coerceAtLeast(0L)
            if (atNaturalEnd) {
                val previousLastId = activeMix.lastOrNull()?.id
                activeCycle += 1
                val nextMix = withContext(Dispatchers.Default) {
                    (if (mode == MusicMixMode.ROCK_YOUR_WORLD) {
                        MusicMixGenerator.rockYourWorldMix(
                            library = snapshot.audio,
                            history = snapshot.history,
                            favorites = snapshot.favorites,
                            selectedGenre = selectedGenre,
                            date = today,
                            cycle = activeCycle,
                        )
                    } else {
                        MusicMixGenerator.dailyMix(
                            config = config,
                            library = snapshot.audio,
                            history = snapshot.history,
                            favorites = snapshot.favorites,
                            selectedGenre = selectedGenre,
                            date = today,
                            cycle = activeCycle,
                        )
                    }).toMutableList().also { next ->
                        if (next.size > 1 && previousLastId != null && next.first().id == previousLastId) {
                            val swapIndex = next.indexOfFirst { it.id != previousLastId }
                            if (swapIndex > 0) {
                                val first = next[0]
                                next[0] = next[swapIndex]
                                next[swapIndex] = first
                            }
                        }
                    }
                }
                if (nextMix.isEmpty()) break
                activeMix = nextMix
                mix = nextMix
                cycle = activeCycle
                playback.playLoopingQueue(nextMix, repeatAll = false)
            }
            delay(250L)
        }
    }

    val heroItem = remember(snapshot.audio, snapshot.history, playbackState.currentId, mix) {
        playbackState.currentId
            ?.takeIf { playbackState.currentMediaType == MediaType.AUDIO }
            ?.let { id -> snapshot.audio.firstOrNull { it.id == id } }
            ?: snapshot.history.firstOrNull { it.mediaType == MediaType.AUDIO.value }
                ?.let { history -> snapshot.audio.firstOrNull { it.id == history.mediaId } }
            ?: mix.firstOrNull()
    }
    val playingId = playbackState.currentId

    val gridState = rememberLazyGridState()

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = airPlayrBottomContentPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 2.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary)
                    }
                    Text(
                        text = config.title,
                        fontFamily = AirPlayrFonts.SmoochSans,
                        fontSize = 27.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { headerMenu = true }) {
                        Icon(Icons.Default.MoreVert, "${config.title} options", tint = colors.primary, modifier = Modifier.size(25.dp))
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                if (mode == MusicMixMode.ROCK_YOUR_WORLD) {
                    RockYourWorldHero(
                        hour = hour,
                        colors = colors,
                        onPlay = { if (mix.isNotEmpty()) { mixPlaybackActive = true; playback.playLoopingQueue(mix, repeatAll = false) } },
                        onShuffle = { if (mix.isNotEmpty()) { mixPlaybackActive = true; playback.playLoopingQueue(mix, repeatAll = false) } },
                    )
                } else {
                    MixHero(
                        item = heroItem,
                        config = config,
                        colors = colors,
                        playing = heroItem?.id == playingId,
                        onPlay = { if (mix.isNotEmpty()) { mixPlaybackActive = true; playback.playLoopingQueue(mix, repeatAll = false) } },
                        onShuffle = { if (mix.isNotEmpty()) { mixPlaybackActive = true; playback.playLoopingQueue(mix, repeatAll = false) } },
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = if (mode == MusicMixMode.FOR_YOU) "Your mix" else "${config.title} · Daily mix",
                    fontFamily = AirPlayrFonts.SmoochSans,
                    fontSize = 25.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                if (genres.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedGenre == null,
                            onClick = { selectedGenre = null },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer,
                            ),
                        )
                        genres.forEach { genre ->
                            FilterChip(
                                selected = selectedGenre == genre,
                                onClick = { selectedGenre = if (selectedGenre == genre) null else genre },
                                label = { Text(genre) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primaryContainer,
                                    selectedLabelColor = colors.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }
            }

            if (mixLoading && mix.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        color = colors.primary,
                        trackColor = colors.surfaceContainerHigh,
                    )
                }
            }
            if (!snapshot.ready || (mix.isEmpty() && mixLoading)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Loading your music…",
                        color = colors.textSecondary,
                        fontFamily = AirPlayrFonts.OpenSans,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                }
            } else if (mix.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No music is available for this mix yet.",
                        color = colors.textSecondary,
                        fontFamily = AirPlayrFonts.OpenSans,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                }
            } else {
                items(mix, key = { it.id }) { item ->
                    MusicMixGridCell(
                        item = item,
                        playing = item.id == playingId,
                        onClick = {
                            mixPlaybackActive = true
                            val index = mix.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                            playback.playLoopingQueue(mix, index, repeatAll = false)
                        },
                        onMore = {
                            selectedSong = item
                            scope.launch { selectedSongFavorite = container.structured.isFavorite(item.id) }
                        },
                    )
                }
            }
        }

        if (mix.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    mixPlaybackActive = true
                    playback.playLoopingQueue(mix, repeatAll = false)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = airPlayrFloatingPlayBottomPadding())
                    .size(68.dp),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp),
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 10.dp),
            ) {
                Icon(Icons.Default.PlayArrow, "Play mix", modifier = Modifier.size(31.dp))
            }
        }

        if (headerMenu) {
            AirPlayrFeatureCollectionActionSheet(
                title = config.title,
                onDismiss = { headerMenu = false },
                onShuffle = {
                    if (mix.isNotEmpty()) {
                        mixPlaybackActive = true
                        playback.playLoopingQueue(mix, repeatAll = false)
                    }
                    headerMenu = false
                },
                onPlayNext = {
                    mix.forEach(playback::playNext)
                    headerMenu = false
                },
                onAddToQueue = {
                    mix.forEach(playback::addToQueue)
                    headerMenu = false
                },
                onAddToPlaylist = {
                    if (mix.isNotEmpty()) playlistTarget = mix
                    headerMenu = false
                },
            )
        }
    }

    selectedSong?.let { item ->
        AirPlayrPremiumMediaActionSheet(
            item = item,
            playing = playbackState.isPlaying && playbackState.currentId == item.id,
            favorite = selectedSongFavorite,
            groups = listOf(
                listOf(
                    AirPlayrMediaMenuAction("Play now", Icons.Default.PlayArrow, {
                        mixPlaybackActive = true
                        val index = mix.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                        playback.playLoopingQueue(mix, index, repeatAll = false)
                        selectedSong = null
                    }),
                    AirPlayrMediaMenuAction("Play next", Icons.Default.SkipNext, { playback.playNext(item); selectedSong = null }),
                    AirPlayrMediaMenuAction("Add to queue", Icons.Default.QueueMusic, { playback.addToQueue(item); selectedSong = null }),
                ),
            ),
            onFavorite = { scope.launch { selectedSongFavorite = container.structured.toggleFavorite(item) } },
            onDismiss = { selectedSong = null },
        )
    }

    playlistTarget?.let { items ->
        HistoryFavoritePlaylistSheet(container = container, items = items, onDismiss = { playlistTarget = null })
    }
}

enum class MusicMixMode { FOR_YOU, TIME_OF_DAY, ROCK_YOUR_WORLD }

private data class RockWorldSlide(val key: String, val asset: String)

private fun rockWorldSlideForHour(hour: Int): RockWorldSlide = when (hour.coerceIn(0, 23)) {
    in 5..8 -> RockWorldSlide("morning", "rock-your-world/morning.jpg")
    in 9..11 -> RockWorldSlide("late-morning", "rock-your-world/late-morning.jpg")
    in 12..16 -> RockWorldSlide("afternoon", "rock-your-world/afternoon.jpg")
    in 17..19 -> RockWorldSlide("evening", "rock-your-world/evening.jpg")
    in 20..22 -> RockWorldSlide("late-evening", "rock-your-world/late-evening.jpg")
    in 23..23, in 0..1 -> RockWorldSlide("night", "rock-your-world/night.jpg")
    else -> RockWorldSlide("late-night", "rock-your-world/late-night.jpg")
}

@Composable
private fun RockYourWorldHero(
    hour: Int,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    val slide = rockWorldSlideForHour(hour)
    Box(
        Modifier
            .fillMaxWidth()
            .height(390.dp)
            .shadow(12.dp, shape)
            .clip(shape)
    ) {
        AnimatedContent(
            targetState = slide.key,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            label = "rock-world-time-slide",
            modifier = Modifier.fillMaxSize(),
        ) { key ->
            RockWorldPhoto(
                assetPath = rockWorldSlideForKey(key).asset,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .12f)))))
        IconButton(
            onClick = onShuffle,
            modifier = Modifier.align(Alignment.TopEnd).padding(18.dp).size(50.dp),
        ) {
            androidx.compose.material3.Surface(
                Modifier.fillMaxSize(),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = colors.primary,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Shuffle, "Shuffle mix", tint = colors.onPrimary, modifier = Modifier.size(23.dp))
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(18.dp).size(58.dp)
                .clip(androidx.compose.foundation.shape.CircleShape).background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onPlay, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.PlayArrow, "Play mix", tint = colors.onPrimary, modifier = Modifier.size(28.dp))
            }
        }
    }
}

private fun rockWorldSlideForKey(key: String): RockWorldSlide = when (key) {
    "morning" -> RockWorldSlide("morning", "rock-your-world/morning.jpg")
    "late-morning" -> RockWorldSlide("late-morning", "rock-your-world/late-morning.jpg")
    "afternoon" -> RockWorldSlide("afternoon", "rock-your-world/afternoon.jpg")
    "evening" -> RockWorldSlide("evening", "rock-your-world/evening.jpg")
    "late-evening" -> RockWorldSlide("late-evening", "rock-your-world/late-evening.jpg")
    "night" -> RockWorldSlide("night", "rock-your-world/night.jpg")
    else -> RockWorldSlide("late-night", "rock-your-world/late-night.jpg")
}

@Composable
private fun RockWorldPhoto(assetPath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(null, assetPath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(assetPath).use { input ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(input, null, bounds)
                    val sample = ceil(maxOf(bounds.outWidth / 1200f, bounds.outHeight / 900f)).toInt().coerceAtLeast(1)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                        inScaled = false
                    }
                    context.assets.open(assetPath).use { data -> BitmapFactory.decodeStream(data, null, options) }
                }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun MixHero(
    item: MediaItem?,
    config: MusicMixConfig,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    playing: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(390.dp)
            .shadow(12.dp, shape)
            .clip(shape)
            .clickable(enabled = item != null && config.title.isNotBlank(), onClick = onPlay),
    ) {
        if (item != null && item.displayArtworkUri() != null) {
            Artwork(
                uri = item.displayArtworkUri(),
                modifier = Modifier.fillMaxSize(),
                radius = 30.dp,
                contentDescription = item.title,
                mediaType = MediaType.AUDIO,
                fallbackIcon = AirPlayrIcons.Music,
                isPlaying = playing,
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .86f)))))
        } else {
            Box(Modifier.fillMaxSize().background(colors.primaryContainer))
        }

        IconButton(
            onClick = onShuffle,
            modifier = Modifier.align(Alignment.TopEnd).padding(18.dp).size(50.dp),
            enabled = item != null,
        ) {
            androidx.compose.material3.Surface(
                Modifier.fillMaxSize(),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = colors.primary,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Shuffle, "Shuffle mix", tint = colors.onPrimary, modifier = Modifier.size(23.dp))
                }
            }
        }

        Column(
            Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                if (config.id == MusicMixGenerator.FOR_YOU_ID) FOR_YOU_HERO_TITLE else config.title,
                fontFamily = AirPlayrFonts.SmoochSans,
                fontSize = 34.sp,
                lineHeight = 39.sp,
                color = if (item != null) Color.White else colors.onPrimaryContainer,
                maxLines = 2,
            )
            Text(
                if (config.id == MusicMixGenerator.FOR_YOU_ID) FOR_YOU_HERO_DESCRIPTION else "A daily music mix shaped by your library and today's listening mood.",
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = if (item != null) Color.White.copy(alpha = .9f) else colors.onPrimaryContainer.copy(alpha = .9f),
                modifier = Modifier.padding(top = 5.dp),
            )
            Spacer(Modifier.height(4.dp))
            AirPlayrWaveAnimation(
                modifier = Modifier.fillMaxWidth().height(124.dp),
                isPlaying = playing,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    item?.title ?: "Your music library",
                    fontFamily = AirPlayrFonts.OpenSans,
                    fontSize = 12.sp,
                    color = if (item != null) Color.White.copy(alpha = .82f) else colors.onPrimaryContainer.copy(alpha = .82f),
                    maxLines = 1,
                )
                Box(
                    Modifier.size(50.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PlayArrow, "Play mix", tint = colors.onPrimary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun MusicMixGridCell(
    item: MediaItem,
    playing: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Column(
        Modifier.fillMaxWidth(),
    ) {
        AirPlayrMediaArtwork(
            artworkUri = item.displayArtworkUri(),
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(18.dp)),
            radius = 18.dp,
            title = item.title,
            mediaType = MediaType.AUDIO,
            fallbackIcon = AirPlayrIcons.Music,
            playing = playing,
            onPlay = onClick,
            onMore = onMore,
        )
        Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
            Text(
                item.title.ifBlank { item.filename },
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 13.sp,
                color = colors.textPrimary,
                maxLines = 1,
                modifier = Modifier.padding(top = 7.dp),
            )
            Text(
                item.artist.orEmpty().ifBlank { "Unknown artist" },
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun rememberCurrentDay(): androidx.compose.runtime.State<LocalDate> = produceState(LocalDate.now()) {
    while (true) {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val next = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)
        val waitMs = Duration.between(now, next).toMillis().coerceAtLeast(1_000L)
        delay(waitMs)
        value = LocalDate.now()
    }
}

@Composable
private fun rememberCurrentHour(): androidx.compose.runtime.State<Int> = produceState(LocalDateTime.now().hour) {
    while (true) {
        delay(30_000L)
        value = LocalDateTime.now().hour
    }
}
