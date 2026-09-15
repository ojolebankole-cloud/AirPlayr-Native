package com.airplayr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.*
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors

@Composable
fun FavoritesScreen(container: AppContainer, playback: PlaybackController) {
    val favs by container.structured.favorites().collectAsStateWithLifecycle(emptyList())
    val audio by container.mediaLibrary.observe(MediaType.AUDIO).collectAsStateWithLifecycle(emptyList())
    val video by container.mediaLibrary.observe(MediaType.VIDEO).collectAsStateWithLifecycle(emptyList())
    val lookup = remember(audio, video) { (audio + video).associateBy { it.id } }
    val items = favs.sortedByDescending { it.addedAt }.mapNotNull { lookup[it.mediaId] }
    val songs = items.filter { it.mediaType == MediaType.AUDIO }
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxSize().background(colors.background)) {
        AirPlayrScreenHeader("Favorites", "${items.size} saved media items")
        if (items.isEmpty()) AirPlayrSectionEmpty("Nothing saved yet", "Tap the heart on a song or video to add it to Favorites.")
        else LazyColumn(contentPadding = PaddingValues(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            item {
                AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(26.dp), containerColor = colors.surfaceSolid, elevation = 6.dp) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, null, tint = colors.primary, modifier = Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Your saved media", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = colors.textPrimary)
                            Text("${songs.size} saved songs", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textSecondary)
                        }
                        if (songs.isNotEmpty()) Button(onClick = { playback.playQueue(songs) }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play all") }
                    }
                }
            }
            items(items, key = { it.id }) { item -> AirPlayrMediaRow(item, onOpen = { if (item.mediaType == MediaType.AUDIO) playback.play(item) }, compact = false) }
        }
    }
}
