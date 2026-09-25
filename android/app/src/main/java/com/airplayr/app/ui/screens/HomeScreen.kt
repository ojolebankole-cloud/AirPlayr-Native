package com.airplayr.app.ui.screens

import com.airplayr.app.ui.airPlayrBottomContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.R
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.di.LibrarySyncProgress
import com.airplayr.app.di.SafScanUiState
import com.airplayr.app.di.SafScanPhase
import com.airplayr.app.ui.components.*
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.navigation.HomeViewModel
import com.airplayr.app.ui.navigation.HomeViewModelFactory
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.theme.AirPlayrColors
import com.airplayr.app.ui.theme.AirPlayrArtworkTokens
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    container: AppContainer,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    onNavigate: (String) -> Unit,
    onMenu: () -> Unit = {},
    onCast: () -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
    val homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = HomeViewModelFactory(container.homeRepository),
    )
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()
    val syncState by container.syncState.collectAsStateWithLifecycle()
    val stats by container.homeStats.summary.collectAsStateWithLifecycle()
    val syncProgress by container.syncProgress.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val history = homeState.history
    val statsHistory = history.take(100)
    LaunchedEffect(history) {
        container.homeStats.refreshIfStale()
    }

    val recentSongs = homeState.recentSongs
    val onRepeat = homeState.onRepeat
    val recentPlayedAlbums = homeState.recentPlayedAlbums
    val recentArtists = homeState.recentArtists
    val currentlyWatching = homeState.currentlyWatching
    val completedWatching = homeState.completedWatching
    val recentVideos = homeState.recentVideos
    val recentlyPlayedVideos = homeState.recentlyPlayedVideos
    val audioHistory = statsHistory.filter { it.mediaType == MediaType.AUDIO.value }
    val videoHistory = statsHistory.filter { it.mediaType == MediaType.VIDEO.value }
    // The stats card is backed by the durable playback-event aggregates rather than the bounded
    // history rail. This keeps video/listening counts correct even when history contains only the
    // most recent 100 media rows.
    val musicPlays = stats?.musicPlays ?: audioHistory.sumOf { it.playCount }
    val videoPlays = stats?.videoPlays ?: videoHistory.sumOf { it.playCount }

    // Match Expo exactly: refresh the time-of-day card every 30 seconds while
    // Home is mounted, so crossing a time boundary updates without reopening Home.
    var currentHour by rememberSaveable { mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        }
    }
    val hour = currentHour
    val timeCard = when (hour) {
        in 5..8 -> Triple("Early Morning Jam", "Tuned in this very hour.", "feature-cards/Headphone-cuate.svg")
        in 9..11 -> Triple("Late Morning Jam", "Tuned in this very hour.", "feature-cards/Headphone-cuate.svg")
        in 12..16 -> Triple("Afternoon Mix", "Live with the culture.", "feature-cards/Music-cuate.svg")
        in 17..19 -> Triple("Evening Jam", "Beyond sounds.", "feature-cards/DJ boy-cuate.svg")
        in 20..22 -> Triple("Night Jam", "Tuned in this very hour.", "feature-cards/Recording-pana.svg")
        in 23..23, in 0..1 -> Triple("Night Extra", "Feel the night.", "feature-cards/DJ boy-cuate.svg")
        else -> Triple("Late Night Jam", "For the night.", "feature-cards/DJ boy-cuate.svg")
    }
    val featureSessionSeed = rememberSaveable { java.util.UUID.randomUUID().toString() }
    val features = remember(timeCard, featureSessionSeed) {
        val list = listOf(
            FeatureSpec("For you now", "Press play & tuned in", "for-you", "for-you", "feature-cards/Headphone-amico.svg"),
            FeatureSpec(timeCard.first, timeCard.second, "tuned-in", when (hour) { in 5..11 -> "morning"; in 12..16 -> "afternoon"; else -> "night" }, timeCard.third),
            FeatureSpec("Rediscover", "From your previous plays.", "rediscover", "rediscover", "feature-cards/Media player-bro.svg"),
            FeatureSpec("Rock your world", "Turn it up.", "rock-your-world", "rock", "feature-cards/Playing Music-bro.svg"),
            FeatureSpec("My playlists", "Your Archive", "my-playlists", "playlists", "feature-cards/Playlist-pana.svg"),
        )
        list.shuffled(java.util.Random(featureSessionSeed.hashCode().toLong()))
    }

    Box(Modifier.testTag(com.airplayr.app.core.qa.AirPlayrQaTags.HOME).fillMaxSize().background(colors.background)) {
        val homeListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) { androidx.compose.foundation.lazy.LazyListState(0) }
        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = airPlayrBottomContentPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeHeader(
                        onMenu = onMenu,
                        onSearch = { onNavigate("search") },
                        onCast = onCast,
                        scanProgress = syncProgress,
                    )
                    if (homeState.audioCount == 0 && homeState.videoCount == 0 && syncState == com.airplayr.app.di.LibrarySyncState.INITIAL_SCAN) {
                        Text(
                            "Building your library…",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        )
                    } else if (homeState.audioCount == 0 && homeState.videoCount == 0 && syncState == com.airplayr.app.di.LibrarySyncState.FAILED) {
                        Text(
                            "Your media library is empty or could not be read yet.",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        )
                    }
                    QuickActions(
                        colors,
                        { onNavigate("history") },
                        { onNavigate("favorites") },
                        { onNavigate("most-played") },
                        { scope.launch { val all = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { container.mediaLibrary.all(MediaType.AUDIO) }; if (all.isNotEmpty()) playback.playQueue(all.shuffled()) } },
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) })
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        features.forEach { f ->
                            AirPlayrFeatureCard(f.eyebrow, f.title, f.image, { onNavigate(f.route) }, f.kind, hour, featureSessionSeed)
                        }
                    }
                }
            }
            if (onRepeat.isNotEmpty()) {
                item {
                    HomeSection("On repeat", { onNavigate("recent/on-repeat") }) {
                        OnRepeatRail(onRepeat, playback, onNavigate)
                    }
                }
            }
            if (recentSongs.isNotEmpty()) {
                item {
                    HomeSection("Recently added songs", { onNavigate("recent/songs") }) {
                        TwoRowSongRail(recentSongs, playback)
                    }
                }
            }
            if (recentPlayedAlbums.isNotEmpty()) {
                item {
                    HomeSection("Recently played albums", { onNavigate("recent/albums") }) {
                        HorizontalAlbumRail(recentPlayedAlbums, playback, onNavigate)
                    }
                }
            }
            if (recentArtists.isNotEmpty()) {
                item {
                    HomeSection("Recent Artists", { onNavigate("recent/artists") }) {
                        HorizontalArtistRail(recentArtists, onNavigate)
                    }
                }
            }
            if (currentlyWatching.isNotEmpty()) {
                item {
                    HomeSection("Currently watching", { onNavigate("recent/videos") }) {
                        HorizontalVideoRail(currentlyWatching, onNavigate)
                    }
                }
            }
            if (completedWatching.isNotEmpty()) {
                item {
                    HomeSection("Complete watching", { onNavigate("recent/videos") }) {
                        HorizontalVideoRail(completedWatching, onNavigate)
                    }
                }
            }
            if (recentVideos.isNotEmpty()) {
                item {
                    HomeSection("Recently added videos", { onNavigate("recent/videos-added") }) {
                        HorizontalVideoRail(recentVideos, onNavigate)
                    }
                }
            }
            if (recentlyPlayedVideos.isNotEmpty()) {
                item {
                    HomeSection("Recently played videos", { onNavigate("recent/videos-played") }) {
                        HorizontalVideoRail(recentlyPlayedVideos, onNavigate)
                    }
                }
            }
            if (musicPlays > 0 || videoPlays > 0) {
                item {
                    ListeningWatchingStatsCard(
                        listeningSeconds = stats?.listeningSeconds ?: 0L,
                        watchingSeconds = stats?.watchingSeconds ?: 0L,
                        musicPlays = musicPlays,
                        videoPlays = videoPlays,
                        completedVideos = stats?.completedVideos ?: videoHistory.count { it.completed },
                        topTrack = stats?.topTrack ?: audioHistory.maxWithOrNull(compareBy<com.airplayr.app.core.data.db.PlaybackHistoryEntity> { it.playCount }.thenBy { it.playedAt })?.title ?: "—",
                        topVideo = stats?.topVideo ?: videoHistory.maxWithOrNull(compareBy<com.airplayr.app.core.data.db.PlaybackHistoryEntity> { it.playCount }.thenBy { it.playedAt })?.title ?: "—",
                        onNavigate = onNavigate,
                    )
                }
            }
            item {
                CurrentPlaybackHost(playback)
            }
        }

    }
}

@Composable
fun SafScanOverlay(state: SafScanUiState) {
    val colors = LocalAirPlayrColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim.copy(alpha = 0.58f))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .heightIn(min = 156.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Text(
                    text = when (state.phase) {
                        SafScanPhase.SCANNING -> "Scanning files"
                        SafScanPhase.COMPLETE -> when {
                            state.addedAudio + state.addedVideo > 0 && state.updatedAudio + state.updatedVideo == 0 -> "Media added"
                            state.updatedAudio + state.updatedVideo > 0 -> "Library updated"
                            else -> "Scan complete"
                        }
                        SafScanPhase.ERROR -> "Scan failed"
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
                    color = colors.textPrimary,
                )

                when (state.phase) {
                    SafScanPhase.SCANNING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier.weight(1f).height(6.dp),
                                color = colors.primary,
                                trackColor = colors.primaryContainer,
                            )
                            Spacer(Modifier.width(12.dp))
                            Box(Modifier.width(12.dp).height(6.dp).clip(RoundedCornerShape(4.dp)).background(colors.primary))
                        }
                        Text(
                            text = if (state.found == 0) "Looking for music and videos…" else "Found ${state.found} media files…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                    SafScanPhase.COMPLETE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            val added = listOf(
                                state.addedAudio.takeIf { it > 0 }?.let { "$it ${if (it == 1) "song" else "songs"}" },
                                state.addedVideo.takeIf { it > 0 }?.let { "$it ${if (it == 1) "video" else "videos"}" },
                            ).filterNotNull().joinToString(" • ")
                            val updated = listOf(
                                state.updatedAudio.takeIf { it > 0 }?.let { "$it ${if (it == 1) "song" else "songs"}" },
                                state.updatedVideo.takeIf { it > 0 }?.let { "$it ${if (it == 1) "video" else "videos"}" },
                            ).filterNotNull().joinToString(" • ")
                            if (added.isNotBlank()) Text("Added $added to your library", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                            if (updated.isNotBlank()) Text("Updated $updated in your library", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                            if (added.isBlank() && updated.isBlank()) Text("No new media files were found", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                        }
                    }
                    SafScanPhase.ERROR -> {
                        Text(
                            text = state.error ?: "The selected folder could not be scanned.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

private data class FeatureSpec(val title: String, val eyebrow: String, val route: String, val kind: String, val image: String)

@Composable
private fun HomeHeader(
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onCast: () -> Unit,
    scanProgress: LibrarySyncProgress,
) {
    val colors = LocalAirPlayrColors.current
    var showSearchPrompt by remember { mutableStateOf(false) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.airplayr_animated_icon))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = showSearchPrompt,
    )
    var lottiePaletteCycle by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(showSearchPrompt, colors.primary, colors.secondary, colors.tertiary) {
        if (!showSearchPrompt) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(2_800L)
            lottiePaletteCycle = (lottiePaletteCycle + 1) % 3
        }
    }
    val baseLottieColors = remember(colors.primary, colors.secondary, colors.tertiary) {
        listOf(colors.primary, colors.secondary, colors.tertiary)
    }
    val lottieThemeColors = remember(baseLottieColors, lottiePaletteCycle) {
        listOf(
            baseLottieColors[lottiePaletteCycle % 3],
            baseLottieColors[(lottiePaletteCycle + 1) % 3],
            baseLottieColors[(lottiePaletteCycle + 2) % 3],
        )
    }
    val lottieDynamicProperties = rememberAirPlayrLottieTheme(
        colors = lottieThemeColors,
        keyPaths = listOf(
            arrayOf("Pre-comp 1", "Layer 4", "**"),
            arrayOf("Pre-comp 1", "Layer 1", "**"),
            arrayOf("Pre-comp 1", "Layer 4", "Group 1", "Path 1"),
        ),
    )
    val playTransition = rememberInfiniteTransition(label = "home-search-play")
    val playOffset by playTransition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "home-search-play-float",
    )
    var playColorIndex by rememberSaveable { mutableIntStateOf(0) }
    val playColors = listOf(colors.primary, colors.secondary, colors.tertiary)
    LaunchedEffect(Unit) {
        showSearchPrompt = false
        kotlinx.coroutines.delay(10_000L)
        showSearchPrompt = true
    }
    LaunchedEffect(showSearchPrompt, playColors) {
        if (!showSearchPrompt) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(1700L)
            playColorIndex = (playColorIndex + 1) % playColors.size
        }
    }
    val animatedPlayColor by animateColorAsState(playColors[playColorIndex], tween(600), label = "home-search-play-color")
    val searchEnabled = !scanProgress.visible
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, start = 24.dp, end = 24.dp, bottom = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(colors.surfaceContainer).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenu, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.Menu, "Open menu", tint = colors.primary, modifier = Modifier.size(28.dp))
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable(enabled = searchEnabled, onClick = onSearch),
                contentAlignment = Alignment.Center,
            ) {
                if (scanProgress.visible) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!scanProgress.complete) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.primary)
                        Text(
                            if (scanProgress.complete) {
                                "Updated ${scanProgress.count} ${scanProgress.label}"
                            } else if (scanProgress.total > 0) {
                                "Updating ${scanProgress.count.coerceAtLeast(1)} of ${scanProgress.total} ${scanProgress.label}…"
                            } else {
                                "Updating ${scanProgress.count.coerceAtLeast(1)} ${scanProgress.label}…"
                            },
                            fontFamily = AirPlayrFonts.SmoochSans,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    if (showSearchPrompt) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            LottieAnimation(
                                composition = composition,
                                progress = { lottieProgress },
                                dynamicProperties = lottieDynamicProperties,
                                modifier = Modifier.size(37.dp),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "AIR",
                                    fontFamily = AirPlayrFonts.SmoochSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp,
                                    lineHeight = 24.sp,
                                    letterSpacing = .7.sp,
                                    color = colors.brandAccent,
                                )
                                val wordmarkColors = remember(colors.primary, colors.secondary, colors.tertiary) {
                                    listOf(colors.primary, colors.secondary, colors.tertiary, colors.primary)
                                }
                                Text(
                                    "PLAYR",
                                    fontFamily = AirPlayrFonts.SmoochSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp,
                                    lineHeight = 24.sp,
                                    letterSpacing = .7.sp,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        brush = Brush.linearGradient(
                                            colors = wordmarkColors,
                                            start = androidx.compose.ui.geometry.Offset(-120f, 0f),
                                            end = androidx.compose.ui.geometry.Offset(180f, 0f),
                                        ),
                                        fontFamily = AirPlayrFonts.SmoochSans,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = .7.sp,
                                    ),
                                    maxLines = 1,
                                )
                            }
                            Box(
                                modifier = Modifier.offset(y = playOffset.dp).size(34.dp).padding(start = 3.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.PlayArrow, "Play", tint = animatedPlayColor, modifier = Modifier.size(19.dp))
                            }
                        }
                    } else {
                        Text(
                            "Search your plays",
                            fontFamily = AirPlayrFonts.SmoochSans,
                            fontSize = 20.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            IconButton(onClick = onCast, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.Cast, "Cast", tint = colors.primary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun QuickActions(colors: AirPlayrColors, onHistory: () -> Unit, onFavorites: () -> Unit, onMostPlayed: () -> Unit, onShuffle: () -> Unit) {
    val actions = listOf(
        Triple("History", Icons.Default.History, onHistory),
        Triple("Favorites", AirPlayrIcons.FavoriteBorder, onFavorites),
        Triple("Most played", Icons.Default.TrendingUp, onMostPlayed),
        Triple("Shuffle", AirPlayrIcons.Shuffle, onShuffle),
    )
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
        actions.forEachIndexed { index, action ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp).clickable(onClick = action.third)) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(listOf(colors.primaryContainer, colors.secondaryContainer, colors.tertiaryContainer, colors.primaryContainer)[index]), contentAlignment = Alignment.Center) {
                    Icon(action.second, action.first, tint = colors.primary, modifier = Modifier.size(23.dp))
                }
                Text(action.first, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 11.sp, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun ListeningWatchingStatsCard(
    listeningSeconds: Long,
    watchingSeconds: Long,
    musicPlays: Int,
    videoPlays: Int,
    completedVideos: Int,
    topTrack: String,
    topVideo: String,
    onNavigate: (String) -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val totalPlays = (musicPlays + videoPlays).coerceAtLeast(1)
    val musicSweep = 360f * musicPlays / totalPlays.toFloat()
    Card(
        onClick = { onNavigate("listening-watching-stats") },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Listening & watching", style = MaterialTheme.typography.titleLarge.copy(fontFamily = AirPlayrFonts.SmoochSans, fontWeight = FontWeight.SemiBold))
                    Text("Your activity at a glance", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        drawArc(colors.primary, -90f, musicSweep, useCenter = true, style = Fill)
                        drawArc(colors.secondaryContainer, -90f + musicSweep, 360f - musicSweep, useCenter = true, style = Fill)
                        drawCircle(colors.surfaceContainer, radius = size.minDimension * .30f)
                    }
                    Icon(Icons.Default.BarChart, "Statistics", tint = colors.primary, modifier = Modifier.size(22.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumStatTile(Icons.Default.Headphones, formatExpoListeningTime(listeningSeconds), "LISTENING", Modifier.weight(1f))
                PremiumStatTile(Icons.Default.Visibility, formatExpoListeningTime(watchingSeconds), "WATCHING", Modifier.weight(1f))
                PremiumStatTile(Icons.Default.MusicNote, "$musicPlays", "MUSIC PLAYS", Modifier.weight(1f))
                PremiumStatTile(Icons.Default.VideoLibrary, "$videoPlays", "VIDEO PLAYS", Modifier.weight(1f))
            }
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = .55f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("TOP TRACK", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Text(topTrack, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(Modifier.weight(1f)) {
                    Text("TOP VIDEO", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Text(topVideo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, "Completed videos", tint = colors.primary, modifier = Modifier.size(18.dp))
                Text(" $completedVideos videos completed", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun PremiumStatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier) {
    val colors = LocalAirPlayrColors.current
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainerHigh).padding(12.dp)) {
        Icon(icon, label, tint = colors.primary, modifier = Modifier.size(19.dp))
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, maxLines = 1)
    }
}

@Composable
private fun StatMini(value: String, label: String, modifier: Modifier) {
    val c = LocalAirPlayrColors.current
    Column(modifier.padding(bottom = 18.dp)) {
        Text(
            value,
            fontFamily = FontFamily.Default,
            fontSize = 27.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
            color = c.textPrimary,
            maxLines = 1,
        )
        Text(
            label,
            fontFamily = FontFamily.Default,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            color = c.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeSection(
    title: String,
    onMore: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionRailHeader(title, onMore)
        content()
    }
}

private fun formatExpoListeningTime(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0L)
    if (seconds < 60L) return "${seconds}s"
    val minutes = kotlin.math.round(seconds / 60.0).toInt()
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) "${hours}h" else "${hours}h ${remainder}m"
}

@Composable private fun SectionRailHeader(title: String, onMore: () -> Unit) {
    val c = LocalAirPlayrColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 19.sp, color = c.textPrimary, modifier = Modifier.weight(1f))
        IconButton(onClick = onMore, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowForward, "More", tint = c.primary, modifier = Modifier.size(24.dp)) }
    }
}

@Composable private fun TwoRowSongRail(items: List<MediaItem>, playback: PlaybackController) {
    val columns = items.chunked(2)
    val railState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    Row(
        Modifier.horizontalScroll(railState).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        columns.forEach { pair ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(300.dp)) {
                pair.forEach { item ->
                    SongRailCard(
                        item,
                        Modifier.fillMaxWidth(),
                        playing = playbackState.isPlaying && playbackState.currentId == item.id,
                    ) {
                        val index = items.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                        playback.playCollection(items, index)
                    }
                }
            }
        }
    }
}

@Composable private fun SongRailCard(item: MediaItem, modifier: Modifier = Modifier, playing: Boolean = false, onClick: () -> Unit) {
    val c = LocalAirPlayrColors.current
    Card(
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = c.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(item.artworkUri, Modifier.size(58.dp), 14.dp, item.title, MediaType.AUDIO, AirPlayrIcons.Music, isPlaying = playing)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(item.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artist ?: "Unknown artist", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun HorizontalAlbumRail(
    groups: List<Pair<String, List<MediaItem>>>,
    playback: PlaybackController,
    onNavigate: (String) -> Unit,
) {
    val railState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    Row(
        Modifier.horizontalScroll(railState).padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        groups.forEach { (name, songs) ->
            var menuOpen by rememberSaveable(name) { mutableStateOf(false) }
            val albumRoute = "album/${java.net.URLEncoder.encode(name, "UTF-8")}"
            val playing = playbackState.isPlaying && songs.any { it.id == playbackState.currentId }
            AirPlayrAlbumRailCard(
                artworkUri = songs.firstOrNull()?.artworkUri,
                albumTitle = name,
                artist = songs.firstOrNull()?.artist,
                onOpen = { onNavigate(albumRoute) },
                onPlay = { songs.firstOrNull()?.let(playback::play) },
                onMore = { menuOpen = true },
                playing = playing,
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Play album") },
                    leadingIcon = { Icon(AirPlayrIcons.Play, null) },
                    onClick = { menuOpen = false; if (songs.isNotEmpty()) playback.playQueue(songs) },
                )
                DropdownMenuItem(
                    text = { Text("Open album") },
                    leadingIcon = { Icon(Icons.Default.Album, null) },
                    onClick = { menuOpen = false; onNavigate(albumRoute) },
                )
            }
        }
    }
}

@Composable
private fun OnRepeatRail(
    items: List<MediaItem>,
    playback: PlaybackController,
    onNavigate: (String) -> Unit,
) {
    val railState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    Row(
        Modifier.horizontalScroll(railState).padding(horizontal = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.chunked(2).forEach { pair ->
            Column(Modifier.width(272.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { item ->
                    RepeatRailCard(
                        item = item,
                        modifier = Modifier.fillMaxWidth(),
                        playing = playbackState.isPlaying && playbackState.currentId == item.id,
                        onClick = {
                            if (item.mediaType == MediaType.AUDIO) {
                                val index = items.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                playback.playCollection(items, index)
                            } else {
                                onNavigate("video/${item.id}")
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RepeatRailCard(item: MediaItem, modifier: Modifier = Modifier, playing: Boolean = false, onClick: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Card(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxSize().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(
                if (item.mediaType == MediaType.VIDEO) item.thumbnailUri ?: item.artworkUri ?: item.uri else item.artworkUri,
                Modifier.size(58.dp),
                14.dp,
                item.title,
                item.mediaType,
                if (item.mediaType == MediaType.VIDEO) AirPlayrIcons.Video else AirPlayrIcons.Music,
                isPlaying = playing,
            )
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(item.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artist ?: "Unknown artist", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable private fun HorizontalArtistRail(groups: List<Pair<String,List<MediaItem>>>, onNavigate: (String) -> Unit) {
    val railState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    Row(Modifier.horizontalScroll(railState).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        groups.forEach { (name, songs) ->
            Column(
                Modifier
                    .width(AirPlayrArtworkTokens.artistRail)
                    .clickable { onNavigate("artist/${java.net.URLEncoder.encode(name, "UTF-8")}") },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Artwork(songs.firstOrNull()?.artworkUri, Modifier.size(AirPlayrArtworkTokens.artistRail), 82.dp, name, MediaType.AUDIO, AirPlayrIcons.Artist, fallbackIconSize = AirPlayrArtworkTokens.artistFallbackIcon, fallbackIconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = com.airplayr.app.ui.theme.AirPlayrArtworkTokens.fallbackIconAlpha))
                Text(name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 13.sp, color = LocalAirPlayrColors.current.textPrimary, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable private fun HorizontalVideoRail(items: List<MediaItem>, onNavigate: (String) -> Unit) {
    val c = LocalAirPlayrColors.current
    val railState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    Row(Modifier.horizontalScroll(railState).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            Column(Modifier.width(196.dp).clickable { onNavigate("video/${item.id}") }) {
                Box {
                    Artwork(item.thumbnailUri ?: item.uri, Modifier.fillMaxWidth().height(110.dp), 18.dp, item.title, MediaType.VIDEO, AirPlayrIcons.Video)
                    Box(Modifier.align(Alignment.BottomStart).padding(8.dp).size(36.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                        Icon(AirPlayrIcons.Play, "Play video", tint = c.onPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Text(item.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 15.sp, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
            }
        }
    }
}

@Composable
private fun CurrentPlaybackHost(playback: PlaybackController) {
    val playbackState by playback.state.collectAsStateWithLifecycle()
    if (playbackState.currentId != null && playbackState.currentMediaType == MediaType.AUDIO) {
        CurrentPlaybackCard(
            playbackState.currentTitle.orEmpty(),
            playbackState.currentArtist.orEmpty(),
            playbackState.artworkUri,
            playbackState.positionMs,
            playbackState.durationMs,
            playbackState.isPlaying,
        )
    }
}

@Composable private fun CurrentPlaybackCard(title: String, artist: String, art: String?, position: Long, duration: Long, isPlaying: Boolean) {
    val c = LocalAirPlayrColors.current
    AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), containerColor = c.surfaceSolid, elevation = 5.dp) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(art, Modifier.size(46.dp), 14.dp, mediaType = MediaType.AUDIO, isPlaying = isPlaying)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(title.ifBlank { "Playing" }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist.ifBlank { "Unknown artist" }, fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = c.textSecondary, maxLines = 1)
                }
            }
            val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(progress = { fraction }, Modifier.fillMaxWidth().padding(top = 8.dp), color = c.primary, trackColor = c.outlineVariant.copy(alpha = .25f))
        }
    }
}

private fun formatDuration(seconds: Long): String = when { seconds < 60 -> "${seconds}s"; seconds < 3600 -> "${seconds / 60}m"; else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m" }
