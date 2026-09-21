package com.airplayr.app.ui.screens

import com.airplayr.app.ui.components.AirPlayrCard
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.airplayr.app.ui.airPlayrBottomContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.preferences.AirPlayrSettings
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.AirPlayrSheetScaffold
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(container: AppContainer, onDiagnostics: () -> Unit = {}, onMetadata: () -> Unit = {}, onNavigate: (String) -> Unit = {}) {
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val current = settings ?: return
    val statusBarTop = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val colors = LocalAirPlayrColors.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selector by remember { mutableStateOf<SettingSelector?>(null) }
    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var cacheSize by remember { mutableStateOf<String?>(null) }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            val count = runCatching { container.safMediaScanner.scanTree(uri, container.mediaLibrary) }.getOrDefault(0)
            container.preferences.addSafRoot(uri.toString())
            status = "Indexed $count media files"
        }
    }
    fun set(field: String, value: Any) = scope.launch {
        when (field) {
            "theme" -> container.preferences.setTheme(value as String)
            "resume" -> container.preferences.setResumeMode(value as String)
            "speed" -> container.preferences.setDefaultSpeed(value as Double)
            "crossfade" -> container.preferences.setCrossfadeSeconds(value as Double)
            "replay" -> container.preferences.setReplayGainMode(value as String)
            "preamp" -> container.preferences.setReplayGainPreampDb(value as Double)
            "aspect" -> container.preferences.setDefaultAspect(value as String)
            "autohide" -> container.preferences.setAutoHideMs(value as Int)
            "subSize" -> container.preferences.setSubtitleSize(value as Double)
            "subColor" -> container.preferences.setSubtitleColor(value as String)
            "subPos" -> container.preferences.setSubtitlePosition(value as String)
            "subDelay" -> container.preferences.setSubtitleDelayMs(value as Int)
            "volume" -> container.preferences.setDefaultVolume(value as Double)
            "seek" -> container.preferences.setSeekStepSeconds(value as Int)
            "layout" -> container.preferences.setLibraryLayout(value as String)
            "columns" -> container.preferences.setGridColumns(value as Int)
            "sort" -> container.preferences.setLibrarySort(value as String)
            "sortOrder" -> container.preferences.setLibrarySortOrder(value as String)
        }
    }
    fun toggle(field: String, value: Boolean) = scope.launch {
        when (field) {
            "highContrast" -> container.preferences.setHighContrast(value)
            "reduceMotion" -> container.preferences.setReduceMotion(value)
            "haptic" -> container.preferences.setHapticFeedback(value)
            "touch" -> container.preferences.setTouchFeedback(value)
            "gapless" -> container.preferences.setGaplessPlayback(value)
            "replayClip" -> container.preferences.setReplayGainPreventClipping(value)
            "awake" -> container.preferences.setKeepAwake(value)
            "subDefault" -> container.preferences.setSubtitlesEnabledDefault(value)
            "subBg" -> container.preferences.setSubtitleBg(value)
            "rememberSub" -> container.preferences.setRememberSubtitleTrack(value)
            "externalSub" -> container.preferences.setPreferExternalSubtitles(value)
            "rememberAudio" -> container.preferences.setRememberAudioTrack(value)
            "muted" -> container.preferences.setMuteOnStart(value)
            "doubleTap" -> container.preferences.setGestureDoubleTapSeek(value)
            "gestureVolume" -> container.preferences.setGestureVolume(value)
            "gestureBrightness" -> container.preferences.setGestureBrightness(value)
            "longPress" -> container.preferences.setGestureLongPress2x(value)
            "pinch" -> container.preferences.setGesturePinchZoom(value)
            "horizontal" -> container.preferences.setGestureHorizontalSeek(value)
            "autoRefresh" -> container.preferences.setAutoRefreshLibrary(value)
            "notification" -> container.preferences.setShowNowPlayingNotification(value)
            "pip" -> container.preferences.setAllowsPictureInPicture(value)
            "autoPip" -> container.preferences.setAutoEnterPiPOnBackground(value)
            "background" -> container.preferences.setBackgroundPlayback(value)
            "history" -> container.preferences.setSavePlaybackHistory(value)
            "continue" -> container.preferences.setShowContinueWatching(value)
            "recent" -> container.preferences.setShowRecentlyPlayed(value)
        }
    }
    fun confirmAction(title: String, message: String, action: suspend () -> Unit) { confirm = ConfirmAction(title, message, action) }

    LazyColumn(
        state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusBarTop + 22.dp, bottom = airPlayrBottomContentPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge); Text("AirPlayr controls and Android integration", color = colors.textSecondary) }

        item {
        settingSection("Appearance") {
            SettingRow("Theme", themeLabel(current.theme), onClick = { selector = SettingSelector("Theme", current.theme, listOf("system" to "System", "dark" to "Dark", "light" to "Light")) { set("theme", it) } })
            SettingSwitch("High contrast", "Strengthens semantic text and controls", current.highContrast) { toggle("highContrast", it) }
            SettingSwitch("Reduce motion", "Reduces non-essential transitions", current.reduceMotion) { toggle("reduceMotion", it) }
            SettingSwitch("Haptic feedback", "Use haptics for meaningful control actions", current.hapticFeedback) { toggle("haptic", it) }
            SettingSwitch("Touch feedback", "Keep pressed-state feedback visible", current.touchFeedback) { toggle("touch", it) }
        }
        }

        item {
        settingSection("Player") {
            SettingRow("Resume behavior", resumeLabel(current.resumeMode), onClick = { selector = SettingSelector("Resume behavior", current.resumeMode, listOf("auto" to "Resume automatically", "ask" to "Ask before resuming", "start" to "Always start from beginning")) { set("resume", it) } })
            SettingRow("Default speed", "${trimNumber(current.defaultSpeed)}×", onClick = { selector = SettingSelector("Default speed", current.defaultSpeed.toString(), listOf("0.25" to "0.25×", "0.5" to "0.50×", "0.75" to "0.75×", "1.0" to "1×", "1.25" to "1.25×", "1.5" to "1.5×", "1.75" to "1.75×", "2.0" to "2×", "2.5" to "2.5×", "3.0" to "3×", "3.5" to "3.5×", "4.0" to "4×")) { set("speed", it.toDouble()) } })
            SettingRow("Default aspect ratio", aspectLabel(current.defaultAspect), onClick = { selector = SettingSelector("Aspect ratio", current.defaultAspect, listOf("contain" to "Fit", "cover" to "Crop", "fill" to "Fill")) { set("aspect", it) } })
            SettingRow("Auto-hide controls", autoHideLabel(current.autoHideMs), onClick = { selector = SettingSelector("Auto-hide controls", current.autoHideMs.toString(), listOf("2000" to "2 seconds", "4000" to "4 seconds", "6000" to "6 seconds", "10000" to "10 seconds", "0" to "Never auto-hide")) { set("autohide", it.toInt()) } })
            SettingSwitch("Keep screen awake", "Prevent sleep while a video is playing", current.keepAwake) { toggle("awake", it) }
        }
        }

        item {
        settingSection("Premium playback") {
            SettingRow("Crossfade", if (current.crossfadeSeconds > 0) "${trimNumber(current.crossfadeSeconds)} seconds" else "Off", onClick = { selector = SettingSelector("Crossfade", current.crossfadeSeconds.toString(), listOf("0.0" to "Off", "2.0" to "2 seconds", "3.0" to "3 seconds", "4.0" to "4 seconds", "5.0" to "5 seconds", "8.0" to "8 seconds", "12.0" to "12 seconds")) { set("crossfade", it.toDouble()) } })
            SettingSwitch("Gapless playback", "Preload the next queue item", current.gaplessPlayback) { toggle("gapless", it) }
            SettingRow("ReplayGain", replayGainLabel(current.replayGainMode), onClick = { selector = SettingSelector("ReplayGain", current.replayGainMode, listOf("off" to "Off", "track" to "Track gain", "album" to "Album gain", "track_preferred_album" to "Track / album fallback")) { set("replay", it) } })
            SettingRow(
                "ReplayGain preamp",
                "${trimNumber(current.replayGainPreampDb)} dB",
                onClick = {
                    selector = SettingSelector(
                        "ReplayGain preamp",
                        current.replayGainPreampDb.toString(),
                        (-12..12).map { it.toString() to "$it dB" },
                    ) { set("preamp", it.toDouble()) }
                },
            )
            SettingSwitch("ReplayGain clipping protection", "Prevent excessive peaks", current.replayGainPreventClipping) { toggle("replayClip", it) }
        }
        }

        item {
        settingSection("Subtitles") {
            SettingSwitch("Subtitles enabled by default", null, current.subtitlesEnabledDefault) { toggle("subDefault", it) }
            SettingRow("Text size", subtitleSizeLabel(current.subtitleSize), onClick = { selector = SettingSelector("Subtitle size", current.subtitleSize.toString(), listOf("0.8" to "Small", "1.0" to "Medium", "1.2" to "Large", "1.5" to "Extra large")) { set("subSize", it.toDouble()) } })
            SettingRow("Text color", subtitleColorLabel(current.subtitleColor), onClick = { selector = SettingSelector("Subtitle color", current.subtitleColor, listOf("#FFFFFF" to "White", "#FFEB3B" to "Yellow", "#00E5FF" to "Cyan", "#FF80AB" to "Pink", "#000000" to "Black")) { set("subColor", it) } })
            SettingRow("Position", if (current.subtitlePosition == "top") "Top" else "Bottom", onClick = { selector = SettingSelector("Subtitle position", current.subtitlePosition, listOf("bottom" to "Bottom", "top" to "Top")) { set("subPos", it) } })
            SettingRow(
                "Delay",
                "${current.subtitleDelayMs} ms",
                onClick = {
                    selector = SettingSelector(
                        "Subtitle delay",
                        current.subtitleDelayMs.toString(),
                        (-4000..4000 step 500).map { it.toString() to "$it ms" },
                    ) { set("subDelay", it.toInt()) }
                },
            )
            SettingSwitch("Background behind text", null, current.subtitleBg) { toggle("subBg", it) }
            SettingSwitch("Remember subtitle track", null, current.rememberSubtitleTrack) { toggle("rememberSub", it) }
            SettingSwitch("Prefer external subtitles", null, current.preferExternalSubtitles) { toggle("externalSub", it) }
        }
        }

        item {
        settingSection("Audio") {
            SettingSwitch("Start muted", "New playback sessions begin at volume 0", current.muteOnStart) { toggle("muted", it) }
            SettingRow("Default volume", "${(current.defaultVolume * 100).roundToInt()}%", onClick = { selector = SettingSelector("Default volume", current.defaultVolume.toString(), listOf("0.0" to "0%", "0.25" to "25%", "0.5" to "50%", "0.75" to "75%", "1.0" to "100%")) { set("volume", it.toDouble()) } })
            SettingSwitch("Remember audio track", null, current.rememberAudioTrack) { toggle("rememberAudio", it) }
        }
        }

        item {
        settingSection("Gestures") {
            SettingSwitch("Double-tap seek", null, current.gestureDoubleTapSeek) { toggle("doubleTap", it) }
            SettingSwitch("Volume gesture", "Vertical swipe on the right side", current.gestureVolume) { toggle("gestureVolume", it) }
            SettingSwitch("Brightness gesture", "Vertical swipe on the left side", current.gestureBrightness) { toggle("gestureBrightness", it) }
            SettingSwitch("Long-press 2× speed", null, current.gestureLongPress2x) { toggle("longPress", it) }
            SettingSwitch("Pinch to zoom", null, current.gesturePinchZoom) { toggle("pinch", it) }
            SettingSwitch("Horizontal seek swipe", null, current.gestureHorizontalSeek) { toggle("horizontal", it) }
            SettingRow(
                "Seek interval",
                "${current.seekStepSeconds}s",
                onClick = {
                    selector = SettingSelector(
                        "Seek interval",
                        current.seekStepSeconds.toString(),
                        listOf("5" to "5 seconds", "10" to "10 seconds", "15" to "15 seconds", "30" to "30 seconds"),
                    ) { set("seek", it.toInt()) }
                },
            )
        }
        }

        item {
        settingSection("Discovery") {
            SettingRow("Smart collections", "Favorites · recent · most played · albums · artists", onClick = { onNavigate("smart-collections") })
            SettingRow("Video collections", "Continue · favorites · watched · bookmarks", onClick = { onNavigate("video-collections") })
        }
        }

        item {
        settingSection("Library") {
            SettingRow("Layout", if (current.libraryLayout == "list") "List" else "Grid", onClick = { selector = SettingSelector("Library layout", current.libraryLayout, listOf("grid" to "Grid", "list" to "List")) { set("layout", it) } })
            SettingRow(
                "Grid columns",
                if (current.gridColumns == 0) "Auto" else "${current.gridColumns}",
                onClick = {
                    selector = SettingSelector(
                        "Grid columns",
                        current.gridColumns.toString(),
                        listOf("0" to "Auto (by screen width)", "2" to "2 columns", "3" to "3 columns", "4" to "4 columns"),
                    ) { set("columns", it.toInt()) }
                },
            )
            SettingRow("Sort by", sortLabel(current.librarySort), onClick = { selector = SettingSelector("Sort library", current.librarySort, listOf("recent" to "Recently added", "name" to "Name", "duration" to "Duration", "modified" to "Date modified")) { set("sort", it) } })
            SettingRow("Sort order", if (current.librarySortOrder == "asc") "Ascending" else "Descending", onClick = { selector = SettingSelector("Sort order", current.librarySortOrder, listOf("desc" to "Descending", "asc" to "Ascending")) { set("sortOrder", it) } })
            SettingSwitch("Auto refresh library", null, current.autoRefreshLibrary) { toggle("autoRefresh", it) }
            SettingRow("Add media folder", "Android Storage Access Framework", icon = Icons.Default.FolderOpen, onClick = { folderLauncher.launch(null) })
            if (status != null) Text(status!!, color = colors.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        }

        item {
        settingSection("Diagnostics & recovery") {
            SettingRow("Online metadata & artwork", "Catalog lookup and artwork caching", icon = Icons.Default.Info, onClick = onMetadata)
            SettingRow("Diagnostics & recovery", "Playback · EQ · Cast · Library · Theme", icon = Icons.Default.SettingsIcon, onClick = onDiagnostics)
        }
        }

        item {
        settingSection("Android") {
            SettingSwitch("Lock screen & notification controls", "Media3 MediaSession controls", current.showNowPlayingNotification) { toggle("notification", it) }
            SettingSwitch("Picture-in-Picture", "Allow PiP from the player", current.allowsPictureInPicture) { toggle("pip", it) }
            SettingSwitch("Auto-enter PiP when leaving app", "Let Android shrink the activity into PiP", current.autoEnterPiPOnBackground) { toggle("autoPip", it) }
            SettingSwitch("Background playback", "Keep music playback alive outside the app", current.backgroundPlayback) { toggle("background", it) }
        }
        }

        item {
        settingSection("History & privacy") {
            SettingSwitch("Save playback history", "Recently Played and playback analytics", current.savePlaybackHistory) { toggle("history", it) }
            SettingSwitch("Continue Watching", null, current.showContinueWatching) { toggle("continue", it) }
            SettingSwitch("Recently Played", null, current.showRecentlyPlayed) { toggle("recent", it) }
            SettingRow("Clear all history", "Remove Recent/Continue Watching records", destructive = true, onClick = { confirmAction("Clear all history?", "This removes playback history and video progress but does not delete media.") { container.structured.clearHistory() } })
            SettingRow("Clear app data", "Reset preferences and local library state", destructive = true, icon = Icons.Default.DeleteSweep, onClick = { confirmAction("Clear app data?", "This removes local preferences, history, favorites, playlists, saved queue, lyrics and DSP state. Media files remain on the device.") {
                container.database.clearAllTables()
                container.preferences.resetToDefaults()
                com.airplayr.app.core.playback.queue.QueueStore(container.appContext).clear()
                com.airplayr.app.core.lyrics.LyricsService(container.appContext).clearAll()
                container.metadata.clearOverrides()
                container.dsp.reset()
                status = "App data reset"
            } })
        }
        }

        item {
        settingSection("Storage") {
            SettingRow("App cache", cacheSize ?: "Tap to calculate", onClick = { scope.launch { cacheSize = withContext(Dispatchers.IO) { formatBytes(calculateCache(context.cacheDir)) } } })
            SettingRow("Clear cache", "Temporary files only", destructive = true, onClick = { confirmAction("Clear cache?", "Only AirPlayr temporary cache files will be removed.") { withContext(Dispatchers.IO) { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }; cacheSize = "Cleared" } })
        }
        }

        item {
        settingSection("About AirPlayr") {
            Text("AirPlayr", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            Text("Music-first local media player for Android. Media, history and settings stay on this device.", color = colors.textSecondary, modifier = Modifier.padding(horizontal = 16.dp))
            Text("Version ${com.airplayr.app.BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE}", color = colors.textSecondary, modifier = Modifier.padding(16.dp))
            SettingRow("Reset all settings", "History and playlists are kept", destructive = true, onClick = { confirmAction("Reset settings?", "Restore all AirPlayr settings to their defaults.") { container.preferences.resetToDefaults(); status = "Settings restored" } })
        }
        }
    }

    selector?.let { sel -> SelectionSheet(title = sel.title, options = sel.options, selected = sel.selected, onSelect = { key -> sel.onSelect(key); selector = null }, onDismiss = { selector = null }) }
    confirm?.let { action -> ConfirmSheet(action.title, action.message, onCancel = { confirm = null }, onConfirm = { scope.launch { action.action(); confirm = null } }) }
}

private data class SettingSelector(val title: String, val selected: String, val options: List<Pair<String, String>>, val onSelect: (String) -> Unit)

private data class ConfirmAction(val title: String, val message: String, val action: suspend () -> Unit)

@Composable private fun settingSection(title: String, content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = LocalAirPlayrColors.current.textSecondary, modifier = Modifier.padding(start = 10.dp, bottom = 4.dp, top = 8.dp)); AirPlayrCard(shape = RoundedCornerShape(24.dp), containerColor = LocalAirPlayrColors.current.surfaceContainer, elevation = 5.dp) { Column(Modifier.fillMaxWidth(), content = content) } } }

@Composable private fun SettingRow(label: String, value: String? = null, destructive: Boolean = false, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) { val c = LocalAirPlayrColors.current; Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) { if (icon != null) Icon(icon, null, tint = if (destructive) c.error else c.primary, modifier = Modifier.size(21.dp).padding(end = 2.dp)); Column(Modifier.weight(1f).padding(horizontal = if (icon == null) 0.dp else 10.dp)) { Text(label, style = MaterialTheme.typography.bodyLarge, color = if (destructive) c.error else c.textPrimary); if (value != null) Text(value, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 2) }; Text(if (destructive) "" else "›", color = if (destructive) c.error else c.primary, style = MaterialTheme.typography.titleLarge) } }

@Composable private fun SettingSwitch(label: String, description: String?, checked: Boolean, onChange: (Boolean) -> Unit) { val c = LocalAirPlayrColors.current; Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary); if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = c.textSecondary) }; Switch(checked, onChange) } }

@Composable private fun SelectionSheet(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) { AirPlayrSheetScaffold(title, "SETTINGS", onDismiss) { options.forEach { (key, label) -> Row(Modifier.fillMaxWidth().clickable { onSelect(key) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(key == selected, { onSelect(key) }); Text(label, style = MaterialTheme.typography.bodyLarge) } } } }
@Composable private fun ConfirmSheet(title: String, message: String, onCancel: () -> Unit, onConfirm: () -> Unit) { AirPlayrSheetScaffold(title, "CONFIRM", onCancel) { Text(message, style = MaterialTheme.typography.bodyLarge); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onCancel) { Text("Cancel") }; Button(onClick = onConfirm) { Text("Confirm") } } } }

private fun themeLabel(v: String) = when (v) { "light" -> "Light"; "dark" -> "Dark"; else -> "System" }
private fun resumeLabel(v: String) = when (v) { "ask" -> "Ask before resuming"; "start" -> "Always start from beginning"; else -> "Resume automatically" }
private fun aspectLabel(v: String) = when (v) { "cover" -> "Crop"; "fill" -> "Fill"; else -> "Fit" }
private fun autoHideLabel(v: Int) = when (v) { 0 -> "Never auto-hide"; 2000 -> "2 seconds"; 6000 -> "6 seconds"; 10000 -> "10 seconds"; else -> "4 seconds" }
private fun replayGainLabel(v: String) = when (v) { "track" -> "Track gain"; "album" -> "Album gain"; "track_preferred_album" -> "Track / album fallback"; else -> "Off" }
private fun subtitleSizeLabel(v: Double) = when (v) { 0.8 -> "Small"; 1.2 -> "Large"; 1.5 -> "Extra large"; else -> "Medium" }
private fun subtitleColorLabel(v: String) = mapOf("#FFFFFF" to "White", "#FFEB3B" to "Yellow", "#00E5FF" to "Cyan", "#FF80AB" to "Pink", "#000000" to "Black")[v] ?: v
private fun sortLabel(v: String) = when (v) { "name" -> "Name"; "duration" -> "Duration"; "modified" -> "Date modified"; else -> "Recently added" }
private fun trimNumber(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else "%.2f".format(v)
private fun calculateCache(dir: File): Long = dir.listFiles()?.sumOf { if (it.isDirectory) calculateCache(it) else it.length() } ?: 0L
private fun formatBytes(n: Long) = when { n < 1024 -> "$n B"; n < 1024 * 1024 -> "%.1f KB".format(n / 1024.0); n < 1024L * 1024 * 1024 -> "%.1f MB".format(n / (1024.0 * 1024.0)); else -> "%.1f GB".format(n / (1024.0 * 1024.0 * 1024.0)) }
