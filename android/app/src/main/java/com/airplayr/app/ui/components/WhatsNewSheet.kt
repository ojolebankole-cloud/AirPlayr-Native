package com.airplayr.app.ui.components

import com.airplayr.app.ui.theme.AirPlayrIcons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class FeatureSection(val title: String, val icon: ImageVector, val items: List<String>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(version: String, onDismiss: () -> Unit) {
    val sections = listOf(
        FeatureSection("MUSIC PLAYER", AirPlayrIcons.Music, listOf(
            "Shuffle, repeat, favorites, queue and playback controls stay within reach.",
            "Adjust playback speed and pitch, start a sleep timer and preserve your preferred playback behavior.",
            "Open lyrics, edit song tags and add music to playlists without leaving the player.",
        )),
        FeatureSection("VIDEO PLAYBACK", AirPlayrIcons.Video, listOf(
            "Browse your video library and keep playback controls close while you watch.",
            "Use subtitles, playback speed and device output controls when supported by the media.",
        )),
        FeatureSection("YOUR LIBRARY", AirPlayrIcons.Music, listOf(
            "AirPlayr indexes the music and videos you allow it to access and keeps the local library refreshed.",
            "Favorites, playlists, recently added media and playback history stay together in one place.",
        )),
        FeatureSection("DISCOVER MORE", Icons.Default.AutoAwesome, listOf(
            "For You, Tuned In, Rediscover and Rock Your World help you return to your music.",
            "Most played and history views turn your local playback into useful personal discovery.",
        )),
        FeatureSection("MAKE IT YOURS", Icons.Default.Settings, listOf(
            "Choose light, dark or system appearance and tune library, playback and accessibility behavior.",
            "Gestures, haptics, reduced motion, contrast and playback defaults can be customized as supported.",
        )),
        FeatureSection("LISTENING & WATCHING", Icons.Default.ShowChart, listOf(
            "AirPlayr keeps local playback activity so you can see your listening and watching patterns as the library grows.",
        )),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.airPlayrShadow(RoundedCornerShape(12.dp), 5.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.padding(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text("What's New?", style = MaterialTheme.typography.headlineMedium)
                    Text("v$version", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("A smoother way to enjoy your music, videos and everything in your library.", style = MaterialTheme.typography.bodyLarge)
            sections.forEach { section ->
                Card(modifier = Modifier.fillMaxWidth().airPlayrShadow(RoundedCornerShape(20.dp), 5.dp), shape = RoundedCornerShape(20.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(section.icon, null, tint = MaterialTheme.colorScheme.primary)
                            Text(section.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        section.items.forEach { item ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("•", color = MaterialTheme.colorScheme.primary)
                                Text(item, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth().airPlayrShadow(RoundedCornerShape(20.dp), 5.dp), shape = RoundedCornerShape(20.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Made to keep your media close and your playback simple.", modifier = Modifier.weight(1f))
                }
            }
            Button(onClick = onDismiss, Modifier.fillMaxWidth().padding(bottom = 10.dp)) { Text("Got it") }
            Spacer(Modifier.height(4.dp))
        }
    }
}
