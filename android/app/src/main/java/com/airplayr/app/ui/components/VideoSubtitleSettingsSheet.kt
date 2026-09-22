package com.airplayr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.di.AppContainer
import kotlinx.coroutines.launch

@Composable
fun VideoSubtitleSettingsSheet(container: AppContainer, onDismiss: () -> Unit) {
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val current = settings ?: return
    val scope = rememberCoroutineScope()
    val sizes = listOf(0.8 to "Small", 1.0 to "Medium", 1.2 to "Large", 1.5 to "Extra large")
    val colors = listOf("#FFFFFF" to "White", "#FFEB3B" to "Yellow", "#00E5FF" to "Cyan", "#FF80AB" to "Pink", "#000000" to "Black")

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ClosedCaption, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Subtitle appearance", style = MaterialTheme.typography.headlineSmall)
                    Text("Tune subtitles without changing the video itself.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
            }

            Text("TEXT SIZE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sizes) { (value, label) ->
                    FilterChip(selected = current.subtitleSize == value, onClick = { scope.launch { container.preferences.setSubtitleSize(value) } }, label = { Text(label) })
                }
            }

            Text("TEXT COLOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(colors) { (value, label) ->
                    FilterChip(
                        selected = current.subtitleColor.equals(value, true),
                        onClick = { scope.launch { container.preferences.setSubtitleColor(value) } },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.foundation.layout.Box(Modifier.size(18.dp).padding(end = 4.dp).then(Modifier), contentAlignment = Alignment.Center) {
                                    androidx.compose.foundation.background(parseColor(value), CircleShape).size(14.dp)
                                }
                                Text(label)
                            }
                        },
                    )
                }
            }

            Text("POSITION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = current.subtitlePosition == "bottom", onClick = { scope.launch { container.preferences.setSubtitlePosition("bottom") } }, label = { Text("Bottom") })
                FilterChip(selected = current.subtitlePosition == "top", onClick = { scope.launch { container.preferences.setSubtitlePosition("top") } }, label = { Text("Top") })
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Background behind text", fontWeight = FontWeight.Medium)
                    Text("Improves readability over bright scenes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = current.subtitleBg, onCheckedChange = { scope.launch { container.preferences.setSubtitleBg(it) } })
            }
        }
    }
}

private fun parseColor(value: String): Color = runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(Color.White)
