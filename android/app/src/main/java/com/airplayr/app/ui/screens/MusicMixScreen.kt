package com.airplayr.app.ui.screens

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
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilterChip
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
import androidx.compose.foundation.Image
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.intelligence.MusicMixConfig
import com.airplayr.app.core.intelligence.MusicMixGenerator
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.airPlayrBottomContentPadding
import com.airplayr.app.ui.components.AirPlayrWaveAnimation
import com.airplayr.app.ui.components.Artwork
import com.airplayr.app.ui.components.displayArtworkUri
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
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

    val config = remember(mode, hour) {
        when (mode) {
            MusicMixMode.FOR_YOU -> MusicMixConfig(MusicMixGenerator.FOR_YOU_ID, FOR_YOU_HERO_TITLE)
            MusicMixMode.TIME_OF_DAY -> MusicMixGenerator.configForHour(hour)
        }
    }
    val genres = remember(snapshot.audio) { MusicMixGenerator.availableGenres(snapshot.audio) }

    var mix by remember(config.id, selectedGenre, today, snapshot.audio, snapshot.history, snapshot.favorites) {
        mutableStateOf<List<MediaItem>>(emptyList())
    }
    LaunchedEffect(config.id, selectedGenre, today, snapshot.audio, snapshot.history, snapshot.favorites) {
        mix = withContext(Dispatchers.Default) {
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

    LaunchedEffect(config.id, selectedGenre, today, mixPlaybackActive) {
        if (!mixPlaybackActive || mix.isEmpty()) return@LaunchedEffect
        while (isActive) {
            val state = playback.stableState.value
            val atNaturalEnd = !state.isPlaying &&
                state.queueSize == mix.size &&
                state.currentQueueIndex >= mix.lastIndex &&
                state.durationMs > 0L &&
                state.positionMs >= (state.durationMs - 750L).coerceAtLeast(0L)
            if (atNaturalEnd) {
                val previousLastId = mix.lastOrNull()?.id
                cycle += 1
                val nextMix = withContext(Dispatchers.Default) {
                    MusicMixGenerator.dailyMix(
                        config = config,
                        library = snapshot.audio,
                        history = snapshot.history,
                        favorites = snapshot.favorites,
                        selectedGenre = selectedGenre,
                        date = today,
                        cycle = cycle,
                    ).toMutableList().also { next ->
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
                mix = nextMix
                if (nextMix.isNotEmpty()) playback.playLoopingQueue(nextMix, repeatAll = false)
                break
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

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = rememberLazyGridState(),
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
                    Modifier.fillMaxWidth().padding(horizontal = 2.dp),
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
                    IconButton(
                        onClick = { if (mix.isNotEmpty()) { mixPlaybackActive = true; playback.playLoopingQueue(mix, repeatAll = false) } },
                        enabled = mix.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.Shuffle, "Shuffle mix", tint = colors.primary)
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                MixHero(
                    item = heroItem,
                    config = config,
                    colors = colors,
                    playing = heroItem?.id == playingId,
                    onPlay = { if (mix.isNotEmpty()) { mixPlaybackActive = true; playback.playLoopingQueue(mix, repeatAll = false) } },
                )
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

            if (mix.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        if (snapshot.ready) "No music is available for this mix yet." else "Loading your music…",
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
                        onClick = { mixPlaybackActive = true; playback.playLoopingQueue(mix, mix.indexOfFirst { it.id == item.id }.coerceAtLeast(0), repeatAll = false) },
                    )
                }
            }
        }
    }
}

enum class MusicMixMode { FOR_YOU, TIME_OF_DAY }

@Composable
private fun MixHero(
    item: MediaItem?,
    config: MusicMixConfig,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    playing: Boolean,
    onPlay: () -> Unit,
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
                modifier = Modifier.fillMaxWidth().height(72.dp),
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
private fun MusicMixGridCell(item: MediaItem, playing: Boolean, onClick: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Artwork(
            uri = item.displayArtworkUri(),
            modifier = Modifier.fillMaxWidth().height(170.dp),
            radius = 18.dp,
            contentDescription = item.title,
            mediaType = MediaType.AUDIO,
            fallbackIcon = AirPlayrIcons.Music,
            isPlaying = playing,
        )
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
