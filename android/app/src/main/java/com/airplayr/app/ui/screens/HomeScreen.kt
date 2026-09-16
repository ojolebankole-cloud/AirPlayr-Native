package com.airplayr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airplayr.app.R
import com.airplayr.app.core.data.PlaybackStatsRepository
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.*
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackControllerFactory
import com.airplayr.app.ui.theme.AirPlayrColors
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(container: AppContainer, libraryViewModel: LibraryViewModel, onNavigate: (String) -> Unit, onCast: () -> Unit = {}) {
    val colors = LocalAirPlayrColors.current
    val music by libraryViewModel.audio.collectAsStateWithLifecycle()
    val videos by libraryViewModel.video.collectAsStateWithLifecycle()
    val history by container.structured.history(200).collectAsStateWithLifecycle(initialValue = emptyList())
    val videoProgress by container.structured.allVideoProgress().collectAsStateWithLifecycle(initialValue = emptyList())
    val favorites by container.structured.favorites().collectAsStateWithLifecycle(initialValue = emptyList())
    val playback: PlaybackController = viewModel(factory = PlaybackControllerFactory(container.appContext))
    val playbackState by playback.state.collectAsStateWithLifecycle()
    val syncState by container.syncState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf<PlaybackStatsRepository.Summary?>(null) }

    LaunchedEffect(music, videos, history) {
        stats = withContext(Dispatchers.IO) { runCatching { container.stats.summary("month", music + videos, 5) }.getOrNull() }
    }

    val recentSongs = remember(music) { music.sortedWith(compareByDescending<MediaItem> { it.dateAdded }.thenBy { it.title.lowercase() }).take(10) }
    val lookup = remember(music, videos) { (music + videos).associateBy { it.id } }
    val recentPlayedAlbums = remember(history, lookup) {
        history.filter { it.mediaType == MediaType.AUDIO.value }.mapNotNull { h -> lookup[h.mediaId] }.groupBy { (it.album ?: "Unknown album") to (it.albumArtist ?: it.artist ?: "") }
            .map { (key, songs) -> key.first to songs }.take(10)
    }
    val recentArtists = remember(history, lookup) {
        history.filter { it.mediaType == MediaType.AUDIO.value }.mapNotNull { h -> lookup[h.mediaId] }.groupBy { it.artist?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown artist" }
            .map { it.key to it.value }.take(10)
    }
    val currentlyWatching = remember(videoProgress, lookup) {
        videoProgress.filter { !it.completed && it.positionMs > 0 }.sortedByDescending { it.updatedAt }.mapNotNull { lookup[it.mediaId] }.take(10)
    }
    val completedWatching = remember(videoProgress, lookup) {
        videoProgress.filter { it.completed }.sortedByDescending { it.updatedAt }.mapNotNull { lookup[it.mediaId] }.take(10)
    }
    val recentVideos = remember(videos) { videos.sortedByDescending { it.dateAdded }.take(10) }
    val recentlyPlayedVideos = remember(history, lookup) { history.filter { it.mediaType == MediaType.VIDEO.value }.mapNotNull { lookup[it.mediaId] }.distinctBy { it.id }.take(10) }

    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val timeCard = when (hour) {
        in 0..4 -> Triple("Night Jam", "Tuned in this very hour.", R.drawable.dj_boy_cuate)
        in 5..8 -> Triple("Early Morning Jam", "Tuned in this very hour.", R.drawable.recording_pana)
        in 9..11 -> Triple("Late Morning Jam", "Tuned in this very hour.", R.drawable.recording_pana)
        in 12..16 -> Triple("Afternoon Mix", "Live with the culture.", R.drawable.music_cuate)
        in 17..19 -> Triple("Evening Jam", "Beyond sounds.", R.drawable.dj_boy_cuate)
        in 20..22 -> Triple("Night Jam", "Tuned in this very hour.", R.drawable.playing_music_bro)
        in 23..23, in 0..1 -> Triple("Night Extra", "Feel the night.", R.drawable.dj_boy_cuate)
        else -> Triple("Late Night Jam", "For the night.", R.drawable.dj_boy_cuate)
    }
    val featureSeed = remember { java.util.Random(System.nanoTime()).nextLong() }
    val features = remember(timeCard, featureSeed) {
        listOf(
            FeatureSpec("For you now", "Press play & tuned in", "for-you", "for-you", R.drawable.headphone_amico, listOf(Color(0xFFF6D7E8), Color(0xFFE9D8FA), Color(0xFFD7E8F7))),
            FeatureSpec(timeCard.first, timeCard.second, "tuned-in", when (hour) { in 5..11 -> "morning"; in 12..16 -> "afternoon"; in 17..23, in 0..4 -> "night"; else -> "night" }, timeCard.third, listOf(Color(0xFFFFE0B5), Color(0xFFD9F0D8), Color(0xFFD7E9F8))),
            FeatureSpec("Rediscover", "From your previous plays.", "rediscover", "rediscover", R.drawable.media_player_bro, listOf(Color(0xFFCDEDE4), Color(0xFFD8E1F7), Color(0xFFFFE0D1))),
            FeatureSpec("Rock your world", "Turn it up.", "rock-your-world", "rock", R.drawable.playing_music_bro, listOf(Color(0xFFFFD1C5), Color(0xFFD6D8F7), Color(0xFFCBE9F0))),
            FeatureSpec("My playlists", "Your Archive", "my-playlists", "playlists", R.drawable.playlist_pana, listOf(Color(0xFFF3D1E2), Color(0xFFD5DDF6), Color(0xFFD2EBD9))),
        ).sortedBy { it.route.hashCode().toLong() xor featureSeed }
    }

    Box(Modifier.testTag(com.airplayr.app.core.qa.AirPlayrQaTags.HOME).fillMaxSize().background(colors.background)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { HomeHeader({ menuOpen = true }, { onNavigate("search") }, onCast) }
            if (music.isEmpty() && videos.isEmpty() && syncState == com.airplayr.app.di.LibrarySyncState.INITIAL_SCAN) {
                item {
                    Text(
                        "Building your library…",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
            } else if (music.isEmpty() && videos.isEmpty() && syncState == com.airplayr.app.di.LibrarySyncState.FAILED) {
                item {
                    Text(
                        "Your media library is empty or could not be read yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
            }
            item { QuickActions(colors, { onNavigate("history") }, { onNavigate("favorites") }, { onNavigate("most-played") }, { if (music.isNotEmpty()) playback.playQueue(music.shuffled()) }) }
            item { Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { features.forEach { f -> AirPlayrFeatureCard(f.eyebrow, f.title, f.image, f.gradient, { onNavigate(f.route) }, f.kind) } } }
            item { SectionRailHeader("Recently added songs") { onNavigate("recent/songs") } }
            item { HorizontalSongRail(recentSongs, playback) }
            if (recentPlayedAlbums.isNotEmpty()) { item { SectionRailHeader("Recently played albums") { onNavigate("recent/albums") } }; item { HorizontalAlbumRail(recentPlayedAlbums, onNavigate) } }
            if (recentArtists.isNotEmpty()) { item { SectionRailHeader("Recent Artists") { onNavigate("recent/artists") } }; item { HorizontalArtistRail(recentArtists, onNavigate) } }
            if (currentlyWatching.isNotEmpty()) { item { SectionRailHeader("Currently watching") { onNavigate("recent/videos") } }; item { HorizontalVideoRail(currentlyWatching, onNavigate) } }
            if (completedWatching.isNotEmpty()) { item { SectionRailHeader("Complete watching") { onNavigate("recent/videos") } }; item { HorizontalVideoRail(completedWatching, onNavigate) } }
            if (recentVideos.isNotEmpty()) { item { SectionRailHeader("Recently added videos") { onNavigate("recent/videos-added") } }; item { HorizontalVideoRail(recentVideos, onNavigate) } }
            if (recentlyPlayedVideos.isNotEmpty()) { item { SectionRailHeader("Recently played videos") { onNavigate("recent/videos-played") } }; item { HorizontalVideoRail(recentlyPlayedVideos, onNavigate) } }
            if (stats != null && (stats!!.musicPlays > 0 || stats!!.videoPlays > 0)) item { StatsCardPreview(colors, stats, recentSongs.firstOrNull()?.title ?: "—", onNavigate) }
            if (playbackState.currentId != null) item { CurrentPlaybackCard(playbackState.currentTitle.orEmpty(), playbackState.currentArtist.orEmpty(), playbackState.artworkUri, playbackState.positionMs, playbackState.durationMs) }
        }
        MenuDrawer(visible = menuOpen, musicCount = music.size, videoCount = videos.size, folderCount = music.mapNotNull { it.folder?.takeIf(String::isNotBlank) }.distinct().size, container = container, onClose = { menuOpen = false }, onNavigate = onNavigate, onAbout = {}, onSupport = {}, onScanMedia = { libraryViewModel.refresh() })
    }
}

private data class FeatureSpec(val title: String, val eyebrow: String, val route: String, val kind: String, val image: Int, val gradient: List<Color>)

@Composable
private fun HomeHeader(onMenu: () -> Unit, onSearch: () -> Unit, onCast: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    var prompt by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(10_000L); prompt = true }
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(27.dp),
            color = colors.surfaceSolid,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenu, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.Menu, "Open menu", tint = colors.primary, modifier = Modifier.size(28.dp))
                }
                Box(Modifier.weight(1f).height(42.dp).clickable(onClick = onSearch), contentAlignment = Alignment.Center) {
                    Text(
                        if (prompt) "Search your music" else "AIR PLAYR",
                        fontFamily = if (prompt) AirPlayrFonts.SmoochSans else AirPlayrFonts.OpenSansBold,
                        fontSize = if (prompt) 14.sp else 24.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onCast, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.Cast, "Cast", tint = colors.primary, modifier = Modifier.size(24.dp))
                }
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
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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

@Composable private fun StatsCardPreview(colors: AirPlayrColors, stats: PlaybackStatsRepository.Summary?, topTrack: String, onNavigate: (String) -> Unit) {
    AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(30.dp), containerColor = colors.surfaceSolid, elevation = 6.dp, onClick = { onNavigate("listening-watching-stats") }) {
        Column(Modifier.padding(20.dp)) {
            Text("YOUR LIBRARY", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 12.sp, color = colors.textPrimary, letterSpacing = 1.1.sp)
            Text("Listening & watching stats", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 27.sp, lineHeight = 33.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatMini("${stats?.musicPlays ?: 0}", "MUSIC PLAYS", Modifier.weight(1f))
                StatMini("${stats?.videoPlays ?: 0}", "VIDEO PLAYS", Modifier.weight(1f))
                StatMini(formatDuration(stats?.listeningSeconds ?: 0), "LISTENING TIME*", Modifier.weight(1f))
                StatMini("${stats?.completedVideos ?: 0}", "VIDEOS COMPLETED", Modifier.weight(1f))
            }
            Text("* Listening time is measured from actual forward playback tracked by AirPlayr.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(Modifier.weight(1f)) { Text("TOP TRACK", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 11.sp, color = colors.textSecondary); Text(stats?.topSongs?.firstOrNull()?.title ?: topTrack, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Column(Modifier.weight(1f)) { Text("TOP VIDEO", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 11.sp, color = colors.textSecondary); Text(stats?.topVideos?.firstOrNull()?.title ?: "—", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable private fun StatMini(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 23.sp, lineHeight = 27.sp, color = LocalAirPlayrColors.current.textPrimary)
        Text(label, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 9.5.sp, color = LocalAirPlayrColors.current.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun SectionRailHeader(title: String, onMore: () -> Unit) {
    val c = LocalAirPlayrColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 19.sp, color = c.textPrimary, modifier = Modifier.weight(1f))
        IconButton(onClick = onMore, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowForward, "More", tint = c.primary, modifier = Modifier.size(24.dp)) }
    }
}

@Composable private fun HorizontalSongRail(items: List<MediaItem>, playback: PlaybackController) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items.forEach { item -> SongRailCard(item) { playback.play(item) } } }
}

@Composable private fun SongRailCard(item: MediaItem, onClick: () -> Unit) {
    val c = LocalAirPlayrColors.current
    AirPlayrCard(Modifier.width(300.dp).heightIn(min = 72.dp), shape = RoundedCornerShape(18.dp), containerColor = c.surfaceSolid, elevation = 5.dp, onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(item.artworkUri, Modifier.size(58.dp), 14.dp, item.title)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(item.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artist ?: "Unknown artist", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(AirPlayrIcons.More, "More", tint = c.primary)
        }
    }
}

@Composable private fun HorizontalAlbumRail(groups: List<Pair<String,List<MediaItem>>>, onNavigate: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        groups.forEach { (name, songs) ->
            Column(Modifier.width(164.dp).clickable { onNavigate("album/${java.net.URLEncoder.encode(name, "UTF-8")}") }) {
                Artwork(songs.firstOrNull()?.artworkUri, Modifier.size(164.dp), 18.dp, name)
                Text(name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, color = LocalAirPlayrColors.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                Text(songs.firstOrNull()?.artist ?: "Unknown artist", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = LocalAirPlayrColors.current.textPrimary, maxLines = 1)
            }
        }
    }
}

@Composable private fun HorizontalArtistRail(groups: List<Pair<String,List<MediaItem>>>, onNavigate: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        groups.forEach { (name, songs) ->
            Column(Modifier.width(132.dp).clickable { onNavigate("artist/${java.net.URLEncoder.encode(name, "UTF-8")}") }, horizontalAlignment = Alignment.CenterHorizontally) {
                Artwork(songs.firstOrNull()?.artworkUri, Modifier.size(120.dp), 60.dp, name)
                Text(name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 13.sp, color = LocalAirPlayrColors.current.textPrimary, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable private fun HorizontalVideoRail(items: List<MediaItem>, onNavigate: (String) -> Unit) {
    val c = LocalAirPlayrColors.current
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            AirPlayrCard(Modifier.width(196.dp), shape = RoundedCornerShape(18.dp), containerColor = c.surfaceSolid, elevation = 5.dp, onClick = { onNavigate("video/${item.id}") }) {
                Column {
                    Artwork(item.thumbnailUri ?: item.artworkUri, Modifier.fillMaxWidth().height(110.dp), 18.dp, item.title)
                    Text(item.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 15.sp, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable private fun CurrentPlaybackCard(title: String, artist: String, art: String?, position: Long, duration: Long) {
    val c = LocalAirPlayrColors.current
    AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), containerColor = c.surfaceSolid, elevation = 5.dp) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(art, Modifier.size(46.dp), 14.dp)
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
