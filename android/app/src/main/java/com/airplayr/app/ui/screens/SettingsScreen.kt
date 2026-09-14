package com.airplayr.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.di.AppContainer
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, onDiagnostics: () -> Unit = {}, onMetadata: () -> Unit = {}) {
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val current = settings ?: return
    var folderStatus by remember { mutableStateOf("") }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            val count = runCatching { container.safMediaScanner.scanTree(uri, container.mediaLibrary) }.getOrDefault(0)
            container.preferences.addSafRoot(uri.toString())
            folderStatus = "Indexed $count media files from selected folder"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }
        item {
            Card {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleLarge)
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system", "light", "dark").forEach { value ->
                            FilterChip(
                                selected = current.theme == value,
                                onClick = { scope.launch { container.preferences.setTheme(value) } },
                                label = { Text(value.replaceFirstChar(Char::uppercase)) },
                                modifier = Modifier.semantics { role = Role.RadioButton },
                            )
                        }
                    }
                    SettingSwitch("High contrast", current.highContrast) { scope.launch { container.preferences.setHighContrast(it) } }
                    SettingSwitch("Reduce motion", current.reduceMotion) { scope.launch { container.preferences.setReduceMotion(it) } }
                    SettingSwitch("Haptic feedback", current.hapticFeedback) { scope.launch { container.preferences.setHapticFeedback(it) } }
                    SettingSwitch("Touch feedback", current.touchFeedback) { scope.launch { container.preferences.setTouchFeedback(it) } }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Library", style = MaterialTheme.typography.titleLarge)
                    SettingSwitch("Auto refresh library", current.autoRefreshLibrary) { scope.launch { container.preferences.setAutoRefreshLibrary(it) } }
                    Button(onClick = { folderLauncher.launch(null) }) { Text("Add media folder") }
                    if (folderStatus.isNotBlank()) Text(folderStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Sort: ${current.librarySort} (${current.librarySortOrder})", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Layout: ${current.libraryLayout}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Playback", style = MaterialTheme.typography.titleLarge)
                    Text("Default speed: ${"%.2f".format(current.defaultSpeed)}×")
                    Slider(
                        value = current.defaultSpeed.toFloat(),
                        onValueChange = { value -> scope.launch { container.preferences.setDefaultSpeed(value.toDouble()) } },
                        valueRange = 0.25f..4f,
                        steps = 14,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Crossfade: ${current.crossfadeSeconds.toInt()}s")
                    Slider(
                        value = current.crossfadeSeconds.toFloat(),
                        onValueChange = { value -> scope.launch { container.preferences.setCrossfadeSeconds(value.toDouble()) } },
                        valueRange = 0f..12f,
                        steps = 11,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SettingSwitch("Gapless playback", current.gaplessPlayback) { scope.launch { container.preferences.setGaplessPlayback(it) } }
                    SettingSwitch("Background playback", current.backgroundPlayback) { scope.launch { container.preferences.setBackgroundPlayback(it) } }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Advanced", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onMetadata, modifier = Modifier.fillMaxWidth()) { Text("Online metadata & artwork") }
                    Button(onClick = onDiagnostics, modifier = Modifier.fillMaxWidth()) { Text("Diagnostics & recovery") }
                    HorizontalDivider()
                    Text("Cast is available from the player using Android Media3 Output Switcher.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.semantics { role = Role.Switch })
    }
}
