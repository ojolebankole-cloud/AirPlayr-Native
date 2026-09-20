package com.airplayr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.*
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import com.airplayr.app.ui.theme.AirPlayrIcons
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun CollectionsScreen(container: AppContainer, libraryViewModel: LibraryViewModel, onOpen: (String) -> Unit, playback: PlaybackController) {
    val scope = rememberCoroutineScope()
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    val c = LocalAirPlayrColors.current
    val definitions = listOf(
        Def("For you now", "favorites + fresh + recent", Icons.Default.AutoAwesome, { container.collections.forYou(20) }, "for-you"),
        Def("Recently played", "your latest listening", Icons.Default.History, { container.collections.recentlyPlayedItems(20) }, "history"),
        Def("Recently added", "newest songs first", Icons.Default.PlaylistPlay, { container.collections.recentlyAdded(20) }, "recent/songs"),
        Def("Most played", "your top repeats", Icons.Default.TrendingUp, { container.collections.mostPlayed(20) }, "most-played"),
        Def("Favorites", "songs you saved", Icons.Default.Favorite, { container.collections.favorites(20) }, "favorites"),
        Def("Rediscover", "previous plays worth revisiting", Icons.Default.History, { container.collections.rediscover(20) }, "rediscover"),
        Def("Rock your world", "turn it up", Icons.Default.LocalFireDepartment, { container.collections.rockYourWorld(20) }, "rock-your-world"),
    )
    val cached by libraryViewModel.cachedFeatures.collectAsStateWithLifecycle()
    val collectionKeys = remember(definitions) { definitions.map { "collection:${it.title}" } }
    val allCollectionsCached = remember(cached, collectionKeys) { collectionKeys.all(cached::containsKey) }
    val hasCollectionItems = remember(cached, collectionKeys) { collectionKeys.any { !cached[it].isNullOrEmpty() } }
    var loading by remember { mutableStateOf(!allCollectionsCached) }
    suspend fun reload(force: Boolean = false) {
        loading = true
        coroutineScope {
            definitions.map { def ->
                async { libraryViewModel.loadFeature("collection:${def.title}", def.loader, force = force) }
            }.forEach { it.await() }
        }
        loading = false
    }
    LaunchedEffect(allCollectionsCached) { if (!allCollectionsCached) reload() }
    Column(Modifier.fillMaxSize().background(c.background)) {
        AirPlayrScreenHeader("Smart collections", "Built from your local music library", trailing = { IconButton(onClick = { scope.launch { reload(force = true) } }) { Icon(Icons.Default.Refresh, "Refresh", tint = c.primary) } })
        if (loading && !allCollectionsCached) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.primary) }
        } else if (!hasCollectionItems) {
            AirPlayrSectionEmpty("No collections yet", "Listen to music and AirPlayr will start building these collections.")
        else LazyColumn(state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(definitions, key = { it.title }) { def ->
                val rows = cached["collection:${def.title}"].orEmpty()
                if (rows.isNotEmpty()) AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(28.dp), containerColor = c.surfaceContainerLow, elevation = 5.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(48.dp), shape = RoundedCornerShape(16.dp), color = c.primaryContainer) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(def.icon, null, tint = c.primary, modifier = Modifier.size(22.dp)) } }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(def.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 19.sp, color = c.textPrimary); Text(def.subtitle, fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = c.textSecondary) }
                            Text("${rows.size}", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 16.sp, color = c.primary)
                        }
                        Row(Modifier.padding(top = 14.dp).horizontalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rows.take(8).forEach { item -> Column(Modifier.width(112.dp).clickable { if (item.mediaType.name == "AUDIO") playback.play(item) }) { Artwork(item.artworkUri, Modifier.size(112.dp), 16.dp, item.title, MediaType.AUDIO, AirPlayrIcons.Music, isPlaying = playbackState.isPlaying && playbackState.currentId == item.id); Text(item.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 14.sp, color = c.textPrimary, maxLines = 1, modifier = Modifier.padding(top = 5.dp)); Text(item.artist ?: "Unknown artist", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, color = c.textSecondary, maxLines = 1) } }
                        }
                        TextButton(onClick = { onOpen(def.route) }, modifier = Modifier.align(Alignment.End)) { Text("View all", fontFamily = AirPlayrFonts.SmoochSans) }
                    }
                }
            }
        }
    }
}
private data class Def(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val loader: suspend () -> List<MediaItem>, val route: String)
