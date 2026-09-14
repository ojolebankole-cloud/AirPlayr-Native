package com.airplayr.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.R
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController

private data class FeatureCardData(
    val key: String,
    val title: String,
    val eyebrow: String,
    val route: String,
    val image: Int,
    val colors: List<androidx.compose.ui.graphics.Color>,
)

@Composable
fun HomeScreen(
    container: AppContainer,
    libraryViewModel: LibraryViewModel,
    onNavigate: (String) -> Unit,
) {
    val music by libraryViewModel.audio.collectAsStateWithLifecycle()
    val videos by libraryViewModel.video.collectAsStateWithLifecycle()
    val playback = androidx.lifecycle.viewmodel.compose.viewModel<com.airplayr.app.ui.playback.PlaybackController>(
        factory = com.airplayr.app.ui.playback.PlaybackControllerFactory(container.appContext)
    )
    val playbackState by playback.state.collectAsStateWithLifecycle()
    var recent by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    LaunchedEffect(music) {
        recent = music.sortedByDescending { it.dateAdded }.take(12)
    }

    val timeFeature = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour in 5..8 -> FeatureCardData("early-morning", "Early Morning Jam", "Tuned in this very hour.", "tuned-in", R.drawable.recording_pana, listOf(
                androidx.compose.ui.graphics.Color(0xFFFFE0B5), androidx.compose.ui.graphics.Color(0xFFD9F0D8), androidx.compose.ui.graphics.Color(0xFFD7E9F8)
            ))
            hour in 9..11 -> FeatureCardData("late-morning", "Late Morning Jam", "Tuned in this very hour.", "tuned-in", R.drawable.recording_pana, listOf(
                androidx.compose.ui.graphics.Color(0xFFFFD9C7), androidx.compose.ui.graphics.Color(0xFFE3F0D2), androidx.compose.ui.graphics.Color(0xFFD7E5FA)
            ))
            hour in 12..16 -> FeatureCardData("afternoon", "Afternoon Mix", "Live with the culture.", "tuned-in", R.drawable.music_cuate, listOf(
                androidx.compose.ui.graphics.Color(0xFFFFD0C1), androidx.compose.ui.graphics.Color(0xFFD5E8F7), androidx.compose.ui.graphics.Color(0xFFE8D7F5)
            ))
            hour in 17..19 -> FeatureCardData("evening", "Evening Jam", "Beyond sounds.", "tuned-in", R.drawable.dj_boy_cuate, listOf(
                androidx.compose.ui.graphics.Color(0xFFD9D4F7), androidx.compose.ui.graphics.Color(0xFFE6CEE8), androidx.compose.ui.graphics.Color(0xFFCFE3F1)
            ))
            hour in 20..22 -> FeatureCardData("night", "Night Jam", "Tuned in this very hour.", "tuned-in", R.drawable.dj_boy_cuate, listOf(
                androidx.compose.ui.graphics.Color(0xFFD5DDF6), androidx.compose.ui.graphics.Color(0xFFE5D0EA), androidx.compose.ui.graphics.Color(0xFFD4E8E5)
            ))
            hour == 23 || hour < 2 -> FeatureCardData("night-extra", "Night Extra", "Feel the night.", "tuned-in", R.drawable.dj_boy_cuate, listOf(
                androidx.compose.ui.graphics.Color(0xFFD9D4F7), androidx.compose.ui.graphics.Color(0xFFE6CEE8), androidx.compose.ui.graphics.Color(0xFFCFE3F1)
            ))
            else -> FeatureCardData("late-night", "Late Night Jam", "For the night.", "tuned-in", R.drawable.dj_boy_cuate, listOf(
                androidx.compose.ui.graphics.Color(0xFFD5DDF6), androidx.compose.ui.graphics.Color(0xFFE5D0EA), androidx.compose.ui.graphics.Color(0xFFD4E8E5)
            ))
        }
    }
    val features = listOf(
        FeatureCardData("for-you", "For you now", "Press play & tuned in", "for-you", R.drawable.headphone_amico, listOf(
            androidx.compose.ui.graphics.Color(0xFFF6D7E8), androidx.compose.ui.graphics.Color(0xFFE9D8FA), androidx.compose.ui.graphics.Color(0xFFD7E8F7)
        )),
        timeFeature,
        FeatureCardData("rediscover", "Rediscover", "From your previous plays", "rediscover", R.drawable.media_player_bro, listOf(
            androidx.compose.ui.graphics.Color(0xFFCDEDE4), androidx.compose.ui.graphics.Color(0xFFD8E1F7), androidx.compose.ui.graphics.Color(0xFFFFE0D1)
        )),
        FeatureCardData("rock", "Rock your world", "Turn it up", "rock-your-world", R.drawable.playing_music_bro, listOf(
            androidx.compose.ui.graphics.Color(0xFFFFD1C5), androidx.compose.ui.graphics.Color(0xFFD6D8F7), androidx.compose.ui.graphics.Color(0xFFCBE9F0)
        )),
        FeatureCardData("playlists", "My playlists", "Your Archive", "my-playlists", R.drawable.playlist_pana, listOf(
            androidx.compose.ui.graphics.Color(0xFFF3D1E2), androidx.compose.ui.graphics.Color(0xFFD5DDF6), androidx.compose.ui.graphics.Color(0xFFD2EBD9)
        )),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HomeHeader(
                musicCount = music.size,
                videoCount = videos.size,
                onSearch = { onNavigate("search") },
                onQueue = { onNavigate("queue") },
                onStats = { onNavigate("listening-watching-stats") },
            )
        }

        item {
            QuickActions(
                onHistory = { onNavigate("history") },
                onFavorites = { onNavigate("favorites") },
                onMostPlayed = { onNavigate("most-played") },
                onShuffle = {
                    if (music.isNotEmpty()) playback.playQueue(music.shuffled())
                },
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                features.forEach { feature ->
                    FeatureCard(feature) { onNavigate(feature.route) }
                }
            }
        }

        item {
            SectionHeader("Recently added songs", "recent/songs") { onNavigate("recent/songs") }
        }

        items(recent, key = { it.id }) { item ->
            HomeMediaRow(item = item, onPlay = { playback.play(item) }, onMore = { onNavigate("player") })
        }

        if (videos.isNotEmpty()) {
            item { SectionHeader("Recently added videos", "recent/videos") { onNavigate("recent/videos") } }
            items(videos.sortedByDescending { it.dateAdded }.take(8), key = { it.id }) { item ->
                HomeMediaRow(item = item, onPlay = { onNavigate("video/${item.id}") }, onMore = { onNavigate("video/${item.id}") })
            }
        }

        if (playbackState.currentId != null) {
            item { MiniNowPlaying(playbackState, playback) }
        }
    }
}

@Composable
private fun HomeHeader(
    musicCount: Int,
    videoCount: Int,
    onSearch: () -> Unit,
    onQueue: () -> Unit,
    onStats: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.airplayr_logo),
                contentDescription = "AirPlayr",
                modifier = Modifier.size(52.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("AIRPLAYR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Your music. Your videos. Your world.", style = MaterialTheme.typography.titleMedium)
                Text("$musicCount songs · $videoCount videos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSearch) { Icon(Icons.Default.LibraryMusic, "Search library") }
            IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Queue") }
            IconButton(onClick = onStats) { Icon(Icons.Default.TrendingUp, "Listening and watching stats") }
        }
    }
}

@Composable
private fun QuickActions(
    onHistory: () -> Unit,
    onFavorites: () -> Unit,
    onMostPlayed: () -> Unit,
    onShuffle: () -> Unit,
) {
    val actions = listOf(
        Triple("History", Icons.Default.History, onHistory),
        Triple("Favorites", Icons.Default.FavoriteBorder, onFavorites),
        Triple("Most played", Icons.Default.TrendingUp, onMostPlayed),
        Triple("Shuffle", Icons.Default.Shuffle, onShuffle),
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        actions.forEach { (label, icon, action) ->
            Column(
                Modifier.width(76.dp).clickable(onClick = action),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(icon, contentDescription = label, modifier = Modifier.padding(13.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 2)
            }
        }
    }
}

@Composable
private fun FeatureCard(data: FeatureCardData, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(300.dp).height(188.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = data.colors.first()),
    ) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(data.colors))) {
            Column(Modifier.padding(18.dp).align(Alignment.TopStart).fillMaxWidth(0.65f)) {
                Text(data.eyebrow, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(data.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Image(
                painter = painterResource(data.image),
                contentDescription = data.title,
                modifier = Modifier.size(145.dp).align(Alignment.BottomEnd).padding(4.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, moreRoute: String, onMore: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Text("More", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onMore))
    }
}

@Composable
private fun HomeMediaRow(item: MediaItem, onPlay: () -> Unit, onMore: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onPlay)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) { Icon(Icons.Default.LibraryMusic, null, Modifier.padding(14.dp)) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.title, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(item.artist, item.album).joinToString(" · ").ifBlank { item.filename }, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, "Play") }
            IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, "More") }
        }
    }
}

@Composable
private fun MiniNowPlaying(state: com.airplayr.app.ui.playback.PlaybackUiState, controller: PlaybackController) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.currentTitle ?: "Playing", maxLines = 1, style = MaterialTheme.typography.titleMedium)
                    Text(state.currentArtist.orEmpty(), maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = controller::togglePlayPause) {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause")
                }
            }
            val fraction = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator({ fraction }, Modifier.fillMaxWidth())
        }
    }
}
