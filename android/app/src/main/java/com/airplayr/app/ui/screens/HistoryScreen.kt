package com.airplayr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
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
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(container: AppContainer, playback: PlaybackController, onOpenVideo: (String) -> Unit) {
    val history by container.structured.history(100).collectAsStateWithLifecycle(emptyList())
    val audio by container.mediaLibrary.observe(MediaType.AUDIO).collectAsStateWithLifecycle(emptyList())
    val lookup = remember(audio) { audio.associateBy { it.id } }
    val scope = rememberCoroutineScope()
    val songs = history.mapNotNull { lookup[it.mediaId] }.distinctBy { it.id }
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxSize().background(colors.background)) {
        AirPlayrScreenHeader("History", "Your recent listening activity")
        if (history.isEmpty()) {
            AirPlayrSectionEmpty("Nothing here yet", "Songs and videos you play will build your history automatically.")
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    Text("RECENTLY PLAYED SONGS", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 12.sp, letterSpacing = 1.1.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                }
                items(songs, key = { it.id }) { item ->
                    AirPlayrMediaRow(item, onOpen = { playback.play(item) }, compact = false)
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, tint = colors.primary, modifier = Modifier.size(26.dp))
                        Text("${history.sumOf { it.playCount }} total plays", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = colors.textPrimary, modifier = Modifier.padding(start = 12.dp))
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { scope.launch { songs.firstOrNull()?.let { playback.play(it) } } }, enabled = songs.isNotEmpty()) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play") }
                    }
                }
                item {
                    Text("Albums from your history", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        history.mapNotNull { lookup[it.mediaId] }.groupBy { it.album ?: "Unknown album" }.toList().take(10).forEach { (album, group) ->
                            Column(Modifier.width(164.dp).clickable { group.firstOrNull()?.let(playback::play) }) {
                                Artwork(group.firstOrNull()?.artworkUri, Modifier.size(164.dp), 18.dp, album)
                                Text(album, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, color = colors.textPrimary, maxLines = 1, modifier = Modifier.padding(top = 7.dp))
                                Text(group.firstOrNull()?.artist ?: "Unknown artist", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textPrimary, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
