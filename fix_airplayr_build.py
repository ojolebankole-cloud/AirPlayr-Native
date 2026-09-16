#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path.cwd()

def find_one(suffix: str) -> Path:
    matches = [p for p in ROOT.rglob(suffix) if p.is_file()]
    if not matches:
        raise SystemExit(f"ERROR: cannot find {suffix} under {ROOT}")
    if len(matches) > 1:
        # Prefer files under android/ or native/ app source trees.
        preferred = [p for p in matches if any(x in p.parts for x in ("android", "native"))]
        if len(preferred) == 1:
            return preferred[0]
        raise SystemExit("ERROR: multiple matches for %s:\n%s" % (suffix, "\n".join(map(str, matches))))
    return matches[0]

def add_import(path: Path, imp: str):
    s = path.read_text()
    if imp in s:
        return
    lines = s.splitlines()
    i = 0
    while i < len(lines) and not lines[i].startswith("import "):
        i += 1
    lines.insert(i, imp)
    path.write_text("\n".join(lines) + "\n")

def replace_once(path: Path, old: str, new: str, label: str):
    s = path.read_text()
    if new in s:
        return
    if old not in s:
        raise SystemExit(f"ERROR: expected source pattern not found for {label}: {path}")
    path.write_text(s.replace(old, new, 1))

def replace_all(path: Path, old: str, new: str):
    s = path.read_text()
    path.write_text(s.replace(old, new))

# Locate project files.
app_gradle = find_one("app/build.gradle.kts")
core_cast = find_one("core/cast/CastController.kt")
ui_cast = find_one("ui/cast/CastController.kt")
air_icons = find_one("ui/theme/AirPlayrIcons.kt")
air_app = find_one("ui/AirPlayrApp.kt")
playback_service = find_one("core/playback/service/AirPlayrPlaybackService.kt")
replay_gain = find_one("core/playback/service/ReplayGainReader.kt")
startup_perm = find_one("ui/permissions/StartupPermissionFlow.kt")
playback_controller = find_one("ui/playback/PlaybackController.kt")
air_sheets = find_one("ui/components/AirPlayrSheets.kt")
menu_drawer = find_one("ui/components/MenuDrawer.kt")
whats_new = find_one("ui/components/WhatsNewSheet.kt")
diagnostics = find_one("ui/diagnostics/DiagnosticsScreen.kt")
equalizer = find_one("ui/screens/EqualizerScreen.kt")
favorites = find_one("ui/screens/FavoritesScreen.kt")
home = find_one("ui/screens/HomeScreen.kt")
music = find_one("ui/screens/MusicScreen.kt")
parity = find_one("ui/screens/ParityScreens.kt")
player = find_one("ui/screens/PlayerScreen.kt")
settings = find_one("ui/screens/SettingsScreen.kt")
stats = find_one("ui/screens/StatsScreen.kt")
video = find_one("ui/screens/VideoScreen.kt")
color_tokens = find_one("ui/theme/AirPlayrColorTokens.kt")

# 1. CastController collision: keep the lifecycle-aware implementation and make it canonical.
rich = ui_cast.read_text().replace("package com.airplayr.app.core.cast", "package com.airplayr.app.core.cast")
core_cast.write_text(rich)
ui_cast.unlink()

# 2. Lifecycle dependency; keep Compose BOM as the Material3 version authority.
s = app_gradle.read_text()
needle = 'implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")'
if 'implementation("androidx.lifecycle:lifecycle-process:2.11.0")' not in s:
    if needle not in s:
        raise SystemExit("ERROR: lifecycle-viewmodel-compose dependency not found")
    s = s.replace(needle, needle + '\n    implementation("androidx.lifecycle:lifecycle-process:2.11.0")')
app_gradle.write_text(s)

# 3. AirPlayrIcons: correct Compose vector APIs without changing geometry.
s = air_icons.read_text()
s = s.replace('import androidx.compose.ui.graphics.vector.PathFillType', 'import androidx.compose.ui.graphics.PathFillType\nimport androidx.compose.ui.graphics.SolidColor')
s = s.replace('cubicTo(', 'curveTo(')
s = re.sub(r'fill = Color\.([A-Za-z0-9_]+)', r'fill = SolidColor(Color.\1)', s)
air_icons.write_text(s)

# 4. AirPlayrApp mechanical fixes.
add_import(air_app, 'import androidx.compose.runtime.setValue')
add_import(air_app, 'import androidx.compose.ui.Modifier')
add_import(air_app, 'import androidx.compose.material3.MaterialTheme')
add_import(air_app, 'import androidx.compose.material3.ExperimentalMaterial3Api')
s = air_app.read_text()
if '@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun AirPlayrApp(' not in s:
    s = s.replace('@Composable\nfun AirPlayrApp(', '@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun AirPlayrApp(', 1)
s = s.replace('HomeScreen(container = container, libraryViewModel = libraryViewModel, onCast = { castOpen = true }) { route -> navController.navigate(route) }',
              'HomeScreen(container = container, libraryViewModel = libraryViewModel, onNavigate = { route -> navController.navigate(route) }, onCast = { castOpen = true })')
air_app.write_text(s)

# 5. Playback service: non-null notification provider; only ExoPlayer receives DSP.
replace_all(playback_service,
    'setMediaNotificationProvider(if (state.showNowPlayingNotification) mediaNotificationProvider else null)',
    '// Media3 requires a non-null notification provider once one is installed.\n                    // Notification visibility must therefore be handled separately from provider registration.\n                    setMediaNotificationProvider(mediaNotificationProvider)')
replace_all(playback_service, 'applyDspTo(target, dsp.state.first())', 'if (target is ExoPlayer) applyDspTo(target, dsp.state.first())')

# 6. Jaudiotagger package.
replace_all(replay_gain, 'net.jthink.jaudiotagger.audio.AudioFileIO', 'org.jaudiotagger.audio.AudioFileIO')

# 7. AirPlayrSheets imports + composable context fix in sleep timer Canvas.
for imp in [
    'import androidx.compose.material3.ExperimentalMaterial3Api',
    'import androidx.compose.material3.OutlinedButton',
    'import androidx.compose.material3.CircularProgressIndicator',
    'import androidx.compose.material3.CardDefaults',
    'import androidx.compose.foundation.layout.PaddingValues',
    'import com.airplayr.app.ui.theme.AirPlayrIcons',
]: add_import(air_sheets, imp)
s = air_sheets.read_text()
if '@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun AirPlayrSheetScaffold' not in s:
    s = s.replace('@Composable\nfun AirPlayrSheetScaffold(', '@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun AirPlayrSheetScaffold(', 1)
s = s.replace('''                Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {\n                    Canvas(Modifier.fillMaxSize()) {\n                        val stroke = 16.dp.toPx()\n                        drawArc(MaterialTheme.colorScheme.surfaceContainerHigh,''',
'''                Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {\n                    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh\n                    val progressColor = MaterialTheme.colorScheme.primary\n                    Canvas(Modifier.fillMaxSize()) {\n                        val stroke = 16.dp.toPx()\n                        drawArc(trackColor,''')
s = s.replace('drawArc(canvasPrimary,', 'drawArc(progressColor,')
s = s.replace('drawArc(MaterialTheme.colorScheme.primary,', 'drawArc(progressColor,')
s = s.replace('drawCircle(MaterialTheme.colorScheme.primary, 13.dp.toPx(),', 'drawCircle(progressColor, 13.dp.toPx(),')
air_sheets.write_text(s)

# 8. MenuDrawer imports.
for imp in [
    'import androidx.compose.runtime.mutableStateOf',
    'import androidx.compose.foundation.layout.weight',
    'import androidx.compose.foundation.layout.height',
    'import androidx.compose.ui.draw.clip',
    'import androidx.compose.foundation.shape.RoundedCornerShape',
]: add_import(menu_drawer, imp)

# 9. Experimental What's New.
add_import(whats_new, 'import androidx.compose.material3.ExperimentalMaterial3Api')
s = whats_new.read_text()
if '@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun WhatsNewSheet(' not in s:
    s = s.replace('@Composable\nfun WhatsNewSheet(', '@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun WhatsNewSheet(', 1)
whats_new.write_text(s)

# 10. Permissions request API uses Array<String>, not Map<String, Boolean>.
s = startup_perm.read_text()
s = s.replace('launcher.launch(mapOf(Manifest.permission.NEARBY_WIFI_DEVICES to true))','launcher.launch(arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES))')
s = s.replace('launcher.launch(mapOf(Manifest.permission.POST_NOTIFICATIONS to true))','launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))')
s = s.replace('mapOf(Manifest.permission.READ_MEDIA_AUDIO to true)','arrayOf(Manifest.permission.READ_MEDIA_AUDIO)')
s = s.replace('mapOf(Manifest.permission.READ_EXTERNAL_STORAGE to true)','arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)')
s = re.sub(r'mapOf\(\s*Manifest\.permission\.READ_MEDIA_VIDEO to true,\s*Manifest\.permission\.READ_MEDIA_VISUAL_USER_SELECTED to true,?\s*\)', 'arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)', s)
s = re.sub(r'mapOf\(\s*Manifest\.permission\.READ_MEDIA_VIDEO to true,?\s*\)', 'arrayOf(Manifest.permission.READ_MEDIA_VIDEO)', s)
s = re.sub(r'mapOf\(\s*Manifest\.permission\.READ_EXTERNAL_STORAGE to true,?\s*\)', 'arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)', s)
startup_perm.write_text(s)

# 11. Playback controller MIME + local nullable configuration.
s = playback_controller.read_text().replace('MimeTypes.APPLICATION_OCTET_STREAM', '"application/octet-stream"')
old = '''            val m = c.getMediaItemAt(index)\n            val uri = m.localConfiguration?.uri?.toString() ?: return@mapNotNull null\n            MediaItem(\n                id = m.mediaId.ifBlank { "queue:$index:$uri" }, mediaStoreId = null, uri = uri,'''
new = '''            val m = c.getMediaItemAt(index)\n            val localConfig = m.localConfiguration ?: return@mapNotNull null\n            val uri = localConfig.uri.toString()\n            MediaItem(\n                id = m.mediaId.ifBlank { "queue:$index:$uri" }, mediaStoreId = null, uri = uri,'''
s = s.replace(old, new).replace('if (m.localConfiguration.mimeType?.startsWith("video/") == true)', 'if (localConfig.mimeType?.startsWith("video/") == true)').replace('mimeType = m.localConfiguration.mimeType,', 'mimeType = localConfig.mimeType,')
playback_controller.write_text(s)

# 12. Equalizer Canvas reads theme colors outside DrawScope.
s = equalizer.read_text()
if 'val primary = MaterialTheme.colorScheme.primary\n    val outlineVariant = MaterialTheme.colorScheme.outlineVariant\n    Canvas(modifier)' not in s:
    s = s.replace('''    Canvas(modifier) {\n        val mid = size.height / 2f''', '''    val primary = MaterialTheme.colorScheme.primary\n    val outlineVariant = MaterialTheme.colorScheme.outlineVariant\n    Canvas(modifier) {\n        val mid = size.height / 2f''', 1)
s = s.replace('drawCircle(MaterialTheme.colorScheme.primary, 7f, Offset(x, y))', 'drawCircle(primary, 7f, Offset(x, y))')
s = s.replace('drawLine(MaterialTheme.colorScheme.outlineVariant, Offset(0f, mid), Offset(size.width, mid), strokeWidth = 2f)', 'drawLine(outlineVariant, Offset(0f, mid), Offset(size.width, mid), strokeWidth = 2f)')
s = s.replace('drawPath(path, MaterialTheme.colorScheme.primary, style = Stroke(width = 5f, cap = StrokeCap.Round))', 'drawPath(path, primary, style = Stroke(width = 5f, cap = StrokeCap.Round))')
equalizer.write_text(s)

# 13. Simple missing imports.
add_import(favorites, 'import androidx.compose.foundation.shape.RoundedCornerShape')
add_import(stats, 'import androidx.compose.runtime.saveable.rememberSaveable')
for imp in [
    'import androidx.compose.foundation.clickable',
    'import androidx.compose.foundation.layout.weight',
    'import androidx.compose.foundation.layout.fillMaxHeight',
    'import androidx.compose.material.icons.filled.QueueMusic',
    'import androidx.compose.material.icons.filled.MusicNote',
    'import androidx.compose.material.icons.filled.LibraryMusic',
]: add_import(music, imp)
for imp in [
    'import androidx.compose.foundation.horizontalScroll',
    'import androidx.compose.ui.draw.clip',
    'import androidx.compose.ui.unit.sp',
    'import androidx.compose.material.icons.filled.ArrowForward',
    'import com.airplayr.app.ui.components.AirPlayrCard',
    'import com.airplayr.app.ui.theme.AirPlayrFonts',
    'import com.airplayr.app.ui.theme.LocalAirPlayrColors',
]: add_import(parity, imp)
add_import(settings, 'import com.airplayr.app.ui.components.AirPlayrCard')

# 14. HomeScreen source-level fixes.
s = home.read_text()
s = s.replace('it.artist?.trim().takeUnless(String::isNullOrBlank)', 'it.artist?.trim().takeUnless { it.isNullOrBlank() }')
s = s.replace('in 23..23, 0..1 ->', 'in 23..23, in 0..1 ->')
s = s.replace('in 17..23, 0..4 ->', 'in 17..23, in 0..4 ->')
s = s.replace('onNavigate = onNavigate, onScanMedia = { libraryViewModel.refresh() })', 'onNavigate = onNavigate, onAbout = {}, onSupport = {}, onScanMedia = { libraryViewModel.refresh() })')
home.write_text(s)

# 15. PlayerScreen: missing repo import, duplicate modifier, and playlist sheet receiver.
add_import(player, 'import com.airplayr.app.core.data.preferences.PreferencesRepository')
s = player.read_text()
s = s.replace('''                    onValueChangeFinished = { seeking = false; controller.seekTo((slider * progressDuration).toLong().coerceIn(0L, progressDuration)) },\n                    modifier = Modifier.fillMaxWidth().height(34.dp),''',
'''                    onValueChangeFinished = { seeking = false; controller.seekTo((slider * progressDuration).toLong().coerceIn(0L, progressDuration)) },''')
# Target only the PlayerPlaylistSheet block.
pos = s.find('private fun PlayerPlaylistSheet')
if pos >= 0:
    before, after = s[:pos], s[pos:]
    after = after.replace('val preferences = container.preferences', 'val preferences = container.preferences', 1)
    after = re.sub(r'    val settings by preferences\.settings\.collectAsStateWithLifecycle\(initialValue = com\.airplayr\.app\.core\.data\.preferences\.AirPlayrSettings\((?:.|\n)*?\n    \)\)\n',
                   '    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)\n    val currentSettings = settings ?: return\n', after, count=1)
    after = after.replace('settings.sleepDeadlineMs', 'currentSettings.sleepDeadlineMs')
    after = after.replace('    val preferences = remember(appContext) { PreferencesRepository(appContext) }\n', '', 1)
    s = before + after
player.write_text(s)

# 16. Settings: each settingSection must be inside LazyColumn.item; fix selector lambda call.
s = settings.read_text()
start = s.find('    LazyColumn(')
end = s.find('\n    selector?.let', start)
if start < 0 or end < 0:
    raise SystemExit('ERROR: SettingsScreen LazyColumn region not found')
region = s[start:end]
starts = [m.start() for m in re.finditer(r'(?m)^        settingSection\(', region)]
insertions=[]
for st in starts:
    openpos = region.find('{', st)
    depth = 0
    closepos = None
    for i in range(openpos, len(region)):
        if region[i] == '{': depth += 1
        elif region[i] == '}':
            depth -= 1
            if depth == 0:
                closepos = i + 1
                break
    if closepos is None:
        raise SystemExit('ERROR: could not match a settingSection block')
    line_end = region.find('\n', closepos)
    if line_end < 0: line_end = len(region)
    insertions.append((st, '        item {\n'))
    insertions.append((line_end, '\n        }'))
for pos, ins in sorted(insertions, reverse=True):
    region = region[:pos] + ins + region[pos:]
s = s[:start] + region + s[end:]
s = s.replace('selector?.let { sel -> SelectionSheet(sel.title, sel.options, sel.selected) { key -> sel.onSelect(key); selector = null } { selector = null } }',
              'selector?.let { sel -> SelectionSheet(title = sel.title, options = sel.options, selected = sel.selected, onSelect = { key -> sel.onSelect(key); selector = null }, onDismiss = { selector = null }) }')
settings.write_text(s)

# 17. VideoScreen: do not construct a 50+ field settings object; wait for the real preference state. Use ComponentActivity for lifecycle access.
add_import(video, 'import androidx.activity.ComponentActivity')
s = video.read_text()
s = s.replace('val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = AirPlayrSettings())\n    val video by vm.video.collectAsStateWithLifecycle();',
              'val settingsState by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)\n    val settings = settingsState ?: return\n    val video by vm.video.collectAsStateWithLifecycle();')
s = s.replace('val activity = context as? Activity\n            val observer = androidx.lifecycle.LifecycleEventObserver', 'val activity = context as? ComponentActivity\n            val observer = androidx.lifecycle.LifecycleEventObserver')
video.write_text(s)

# 18. AirPlayrColorTokens: ensure base is always AirPlayrColors; dynamic roles are overlaid below.
s = color_tokens.read_text().replace('val base = if (dynamic) scheme else if (darkTheme) AirPlayrDarkBase else AirPlayrLightBase', 'val base = if (darkTheme) AirPlayrDarkBase else AirPlayrLightBase')
color_tokens.write_text(s)

# 19. Diagnostics repository API is resetToDefaults(), not resetPlaybackDefaults().
replace_all(diagnostics, 'container.preferences.resetPlaybackDefaults()', 'container.preferences.resetToDefaults()')

print('Applied AirPlayr build fixes.')
