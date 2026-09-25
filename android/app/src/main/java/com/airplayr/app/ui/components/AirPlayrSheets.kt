package com.airplayr.app.ui.components

import com.airplayr.app.ui.theme.AirPlayrIcons
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import android.content.Context
import android.content.ClipDescription
import android.content.ClipboardManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.lyrics.LyricsRepository
import com.airplayr.app.core.lyrics.LyricsService
import com.airplayr.app.core.lyrics.LyricsTrack
import com.airplayr.app.core.metadata.MetadataMatch
import com.airplayr.app.core.metadata.MetadataRepository
import com.airplayr.app.core.playback.dsp.DspRepository
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirPlayrSheetScaffold(
    title: String,
    eyebrow: String? = null,
    onDismiss: () -> Unit,
    sheetBackgroundTheme: String? = null,
    sheetBackgroundArtworkUri: String? = null,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (sheetBackgroundTheme == null) MaterialTheme.colorScheme.surfaceContainerLow else androidx.compose.ui.graphics.Color.Transparent,
        tonalElevation = if (sheetBackgroundTheme == null) 8.dp else 0.dp,
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (sheetBackgroundTheme != null) {
                PlaybackBackground(
                    theme = sheetBackgroundTheme,
                    artworkUri = sheetBackgroundArtworkUri,
                    modifier = Modifier.matchParentSize(),
                    reduceMotion = reduceMotion,
                )
            }
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 22.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        if (!eyebrow.isNullOrBlank()) Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(title, style = MaterialTheme.typography.headlineMedium)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }
                content()
            }
        }
    }
}

@Composable
fun SleepTimerSheet(
    currentMinutes: Int,
    remainingSeconds: Long,
    finishCurrent: Boolean,
    fadeOut: Boolean,
    onDismiss: () -> Unit,
    onApply: (minutes: Int, finishCurrent: Boolean, fadeOut: Boolean) -> Unit,
    onCancelTimer: () -> Unit,
) {
    var minutes by remember(currentMinutes) { mutableIntStateOf(currentMinutes.takeIf { it > 0 } ?: 30) }
    var finishCurrentState by remember(finishCurrent) { mutableStateOf(finishCurrent) }
    var fadeOutState by remember(fadeOut) { mutableStateOf(fadeOut) }
    val maxMinutes = 120
    val minMinutes = 5
    val progress = ((minutes - minMinutes).toFloat() / (maxMinutes - minMinutes)).coerceIn(0f, 1f)
    val presets = listOf(5, 10, 15, 30, 45, 60, 90)
    val teal = MaterialTheme.colorScheme.primary
    val ringTrack = MaterialTheme.colorScheme.surfaceContainerHigh
    val text = MaterialTheme.colorScheme.onSurface

    fun updateFromRing(position: Offset, size: Size) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val dx = position.x - cx
        val dy = position.y - cy
        if (dx * dx + dy * dy < (size.minDimension * .22f) * (size.minDimension * .22f)) return
        val degrees = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0 + 360.0) % 360.0)
        val raw = minMinutes + ((degrees / 360.0) * (maxMinutes - minMinutes))
        minutes = ((raw / 5.0).roundToInt() * 5).coerceIn(minMinutes, maxMinutes)
    }

    AirPlayrSheetScaffold("Sleep timer", "PLAYBACK", onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sleep timer", style = MaterialTheme.typography.headlineLarge, color = text)
            }
            Icon(Icons.Default.Timer, "Sleep timer", tint = teal, modifier = Modifier.size(28.dp))
        }

        Box(
            Modifier.fillMaxWidth().height(340.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                Modifier
                    .size(300.dp)
                    .pointerInput(maxMinutes) {
                        detectDragGestures(
                            onDragStart = { position ->
                                updateFromRing(position, Size(size.width.toFloat(), size.height.toFloat()))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                updateFromRing(change.position, Size(size.width.toFloat(), size.height.toFloat()))
                            },
                        )
                    },
            ) {
                val stroke = 22.dp.toPx()
                val r = (size.minDimension - stroke) / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                drawArc(ringTrack, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(teal, -90f, progress * 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                // Reference-style minute ticks and labels.
                for (i in 0 until 24) {
                    val a = (-90f + i * 15f) * (PI / 180f)
                    val outer = r + 14.dp.toPx()
                    val inner = if (i % 3 == 0) outer - 12.dp.toPx() else outer - 7.dp.toPx()
                    val p1 = Offset(c.x + cos(a).toFloat() * inner, c.y + sin(a).toFloat() * inner)
                    val p2 = Offset(c.x + cos(a).toFloat() * outer, c.y + sin(a).toFloat() * outer)
                    drawLine(MaterialTheme.colorScheme.outlineVariant, p1, p2, strokeWidth = if (i % 3 == 0) 4.dp.toPx() else 2.dp.toPx(), cap = StrokeCap.Round)
                }
                val handleAngle = (-90f + progress * 360f) * (PI / 180f)
                val handle = Offset(c.x + cos(handleAngle).toFloat() * r, c.y + sin(handleAngle).toFloat() * r)
                drawCircle(Color.White, 25.dp.toPx(), handle)
                drawCircle(teal, 18.dp.toPx(), handle)
                drawCircle(Color.White, 7.dp.toPx(), handle)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(minutes.toString(), style = MaterialTheme.typography.displayLarge, color = text)
                Text(if (minutes >= 60 && minutes % 60 != 0) "h : mm" else "min", color = teal, style = MaterialTheme.typography.titleMedium)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("STOPS AT  ", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(System.currentTimeMillis() + minutes * 60_000L))}", style = MaterialTheme.typography.titleSmall, color = teal)
        }

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { value ->
                FilterChip(selected = minutes == value, onClick = { minutes = value }, label = { Text(if (value >= 60) "${value / 60}h${if (value % 60 == 0) "" else " ${value % 60}m"}" else "${value}m") })
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Finish current song", style = MaterialTheme.typography.titleMedium); Text("Don't cut off mid-track", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(finishCurrentState, { finishCurrentState = it })
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Fade out", style = MaterialTheme.typography.titleMedium); Text("Last 10 sec", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(fadeOutState, { fadeOutState = it })
                }
            }
        }
        if (remainingSeconds > 0) TextButton(onClick = onCancelTimer, modifier = Modifier.fillMaxWidth()) { Text("Cancel active timer", color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onApply(minutes, finishCurrentState, fadeOutState); onDismiss() }) { Text(if (remainingSeconds > 0) "Update" else "Start") }
        }
    }
}

@Composable
fun SpeedPitchSheet(dsp: com.airplayr.app.core.playback.dsp.DspState, controller: PlaybackController, onDismiss: () -> Unit) {
    val speedPresets = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f)
    var speed by remember(dsp.speed) { mutableFloatStateOf(dsp.speed.toFloat()) }
    var pitch by remember(dsp.pitchSemitones) { mutableIntStateOf(dsp.pitchSemitones) }
    var preserve by remember(dsp.preservePitch) { mutableStateOf(dsp.preservePitch) }
    var lock by remember(dsp.lockPitch) { mutableStateOf(dsp.lockPitch) }
    AirPlayrSheetScaffold("Speed & Pitch", "PLAYBACK", onDismiss) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SummaryTile("SPEED", "%.2f×".format(speed))
            SummaryTile("PITCH", if (pitch >= 0) "+${pitch} st" else "${pitch} st")
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text("Speed", style = MaterialTheme.typography.titleMedium) }
                Slider(value = speed, onValueChange = { speed = it }, valueRange = .25f..4f, steps = 14, onValueChangeFinished = { controller.setSpeed(speed) })
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }), horizontalArrangement = Arrangement.spacedBy(8.dp)) { speedPresets.forEach { s -> FilterChip(selected = kotlin.math.abs(speed - s) < .02f, onClick = { speed = s; controller.setSpeed(s) }, label = { Text("%.2f×".format(s)) }) } }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pitch", style = MaterialTheme.typography.titleMedium)
                Slider(value = pitch.toFloat(), onValueChange = { pitch = it.toInt() }, valueRange = -12f..12f, steps = 23, enabled = !lock, onValueChangeFinished = { controller.setPitchSemitones(pitch) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("−12 st"); Text("0 st"); Text("+12 st") }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Preserve pitch", style = MaterialTheme.typography.titleMedium); Text("Time-stretch playback without changing pitch", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(preserve, { preserve = it; if (it) lock = false; controller.setPreservePitch(it) }) }
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Lock pitch to speed", style = MaterialTheme.typography.titleMedium); Text("Vinyl-style speed/pitch linkage", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(lock, { lock = it; controller.setLockPitch(it); if (it) { preserve = false; pitch = (12 * kotlin.math.log2(speed.toDouble())).toInt().coerceIn(-12, 12); controller.setPitchSemitones(pitch) } }) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { speed = 1f; pitch = 0; preserve = true; lock = false; controller.setSpeed(1f); controller.setPitchSemitones(0); controller.setPreservePitch(true); controller.setLockPitch(false) }) { Text("Reset") }; Spacer(Modifier.width(8.dp)); Button(onClick = onDismiss) { Text("Done") } }
    }
}

@Composable
private fun SummaryTile(label: String, value: String) { Card(Modifier.width(150.dp), shape = RoundedCornerShape(24.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) { Column(Modifier.padding(18.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(value, style = MaterialTheme.typography.displaySmall) } } }

@Composable
fun LyricsSheet(
    context: Context,
    repository: LyricsRepository,
    state: PlaybackUiState,
    onClose: () -> Unit,
    onSeek: (Double) -> Unit,
    onEdit: () -> Unit,
) {
    val service = repository
    var result by remember(state.currentId) { mutableStateOf<LyricsService.Result?>(null) }
    var lyricsState by remember(state.currentId) { mutableStateOf("idle") }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadSaved() = scope.launch {
        lyricsState = "loading"
        val id = state.currentId.orEmpty()
        val track = LyricsTrack(
            state.currentTitle.orEmpty(),
            state.currentArtist.orEmpty(),
            state.currentAlbum.orEmpty(),
            state.durationMs / 1000.0,
        )
        val loaded = withContext(Dispatchers.IO) {
            service.getCached(id) ?: service.fetchShared(id, track, force = false).await()
        }
        result = loaded
        lyricsState = if (loaded != null) "ready" else "error"
    }
    fun refresh() = scope.launch {
        lyricsState = "loading"
        refreshing = true
        val fetched = withContext(Dispatchers.IO) {
            service.fetchShared(
                state.currentId.orEmpty(),
                LyricsTrack(
                    state.currentTitle.orEmpty(),
                    state.currentArtist.orEmpty(),
                    state.currentAlbum.orEmpty(),
                    state.durationMs / 1000.0,
                ),
                true,
            ).await()
        }
        result = fetched
        lyricsState = when {
            fetched != null -> "ready"
            else -> "error"
        }
        refreshing = false
    }

    LaunchedEffect(state.currentId) { loadSaved() }

    val cues = result?.cues.orEmpty()
    val position = state.positionMs / 1000.0
    val activeIndex = cues.indexOfLast { position >= it.start && position < it.end }
    val hasSynced = cues.isNotEmpty()
    val hasPlain = !result?.plainLyrics.isNullOrBlank() || (!hasSynced && !result?.syncedLyrics.isNullOrBlank())
    val lyricsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) }
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(activeIndex, cues.size) {
        if (activeIndex >= 0 && activeIndex < cues.size) {
            runCatching { lyricsListState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0)) }
        }
    }

    AirPlayrSheetScaffold(
        title = "Lyrics",
        eyebrow = "PLAYBACK",
        onDismiss = onClose,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.currentTitle ?: "Unknown song", style = MaterialTheme.typography.titleLarge)
                    Text(state.currentArtist ?: "Unknown artist", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    result?.let { lyricResult ->
                        Text(
                            text = "${lyricResult.provider} · ${when (lyricResult.syncLevel) { 3 -> "Rich sync"; 2 -> "Synced"; else -> "Lyrics" }}",
                            color = colors.primary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                IconButton(onClick = onEdit) { Icon(AirPlayrIcons.Lyrics, "Edit lyrics", tint = colors.primary) }
                IconButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { service.clear(state.currentId.orEmpty()) }
                        result = null
                        lyricsState = "idle"
                    }
                }) { Icon(Icons.Default.Delete, "Delete saved lyrics", tint = colors.primary) }
            }

            when {
                lyricsState == "idle" && !hasSynced && !hasPlain -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        LyricsActionCard("Fetch Lyrics", "Search online for matching lyrics", Icons.Default.CloudDownload) { refresh() }
                        LyricsActionCard("Edit Lyrics", "Paste, write, or open a lyrics file", Icons.Default.Edit) { onEdit() }
                        LyricsActionCard("Saved Lyrics", "Open lyrics saved on this device", Icons.Default.BookmarkBorder) { loadSaved() }
                    }
                }
                lyricsState == "loading" && result == null -> {
                    Column(Modifier.fillMaxWidth().padding(vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
            }

            if (lyricsState == "error" && !hasSynced && !hasPlain) {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudOff, null, Modifier.size(34.dp), tint = colors.primary)
                    Text("Lyrics couldn't be loaded", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                    TextButton(onClick = { refresh() }) { Text("Try again") }
                }
            }

            if (hasSynced) {
                LazyColumn(
                    state = lyricsListState,
                    modifier = Modifier.fillMaxWidth().height(500.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    items(cues.size) { i ->
                        val cue = cues[i]
                        val active = i == activeIndex
                        androidx.compose.foundation.layout.Box(
                            Modifier.fillMaxWidth().clickable { onSeek(cue.start) }.padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                cue.text,
                                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                style = if (active) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                                color = if (active) colors.primary else colors.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else if (hasPlain) {
                Column(Modifier.fillMaxWidth().height(500.dp).verticalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) })) {
                    Text((result?.plainLyrics ?: result?.syncedLyrics).orEmpty(), Modifier.fillMaxWidth().padding(8.dp), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            }

            if (lyricsState == "ready" && !refreshing) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { refresh() }) { Text("Refresh") }
                }
            }
        }
    }
}

@Composable
private fun LyricsActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun LyricsEditorSheet(context: Context, repository: LyricsRepository, item: MediaItem, onClose: () -> Unit) {
    val service = repository
    var existing by remember(item.id) { mutableStateOf<LyricsService.Result?>(null) }
    var text by remember(item.id) { mutableStateOf("") }
    var provider by remember(item.id) { mutableStateOf("Manual") }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty() }.getOrDefault("")
            provider = "File import"; status = "Imported lyrics file"
        }
    }
    LaunchedEffect(item.id) {
        val saved = withContext(Dispatchers.IO) { service.getCached(item.id) }
        existing = saved
        text = saved?.syncedLyrics ?: saved?.plainLyrics.orEmpty()
        provider = saved?.provider ?: "Manual"
    }

    fun paste() {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = cb?.primaryClip
        if (clip != null && clip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) { text = clip.getItemAt(0).coerceToText(context).toString(); provider = "Clipboard"; status = "Pasted from clipboard" }
    }
    AirPlayrSheetScaffold("Lyrics editor", "EDIT LYRICS", onClose) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge); Text(item.artist.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { status = "Searching online…"; val r = withContext(Dispatchers.IO) { service.fetch(item.id, LyricsTrack(item.title, item.artist.orEmpty(), item.album.orEmpty(), item.durationMs / 1000.0), true) }; if (r != null) { text = r.syncedLyrics ?: r.plainLyrics.orEmpty(); provider = r.provider; status = "Found ${r.provider}" } else status = "No confident match found" } }) { Icon(Icons.Default.CloudDownload, null); Spacer(Modifier.width(6.dp)); Text("Search online") }
                    OutlinedButton(onClick = { picker.launch(arrayOf("text/plain", "text/x-lrc", "text/*", "application/octet-stream")) }) { Text("Open file") }
                    IconButton(onClick = ::paste) { Icon(Icons.Default.ContentPaste, "Paste") }
                }
                if (status != null) Text(status!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().height(320.dp), label = { Text("Lyrics / LRC") })
                Text("Timestamped [mm:ss.xx] lines are parsed as synced lyrics; plain text stays scrollable.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { text = "" }) { Text("Clear") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { scope.launch { val syncedText = if (Regex("\\[\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?\\]").containsMatchIn(text)) text else null; val plain = if (syncedText == null) text else null; val savedProvider = if (provider.equals("Manual", true) || provider.equals("File import", true) || provider.equals("Clipboard", true)) provider else "AirPlayr"; val result = LyricsService.Result(savedProvider, item.title, item.artist.orEmpty(), item.album.orEmpty(), item.durationMs / 1000.0, plain, syncedText, service.parseLrc(syncedText.orEmpty()), 100); withContext(Dispatchers.IO) { service.save(item.id, result) }; onClose() } }) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("Save lyrics") }
                }
            }
        }
    }
}

@Composable
fun TagEditorSheet(context: Context, item: MediaItem, metadata: MetadataRepository, onClose: () -> Unit, onSaved: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val fields = remember(item.id) { mutableStateOf(mapOf("Title" to item.title, "Artist" to item.artist.orEmpty(), "Album" to item.album.orEmpty(), "Album artist" to item.albumArtist.orEmpty(), "Genre" to item.genre.orEmpty(), "Year" to "", "Track number" to "", "Track total" to "", "Disc number" to "", "Disc total" to "", "Composer" to "")) }
    var busy by remember { mutableStateOf(false) }
    var matches by remember { mutableStateOf<List<MetadataMatch>>(emptyList()) }
    fun set(key: String, value: String) { fields.value = fields.value + (key to value) }
    AirPlayrSheetScaffold("Tag editor", "MUSIC METADATA", onClose) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(enabled = !busy, onClick = { scope.launch { busy = true; matches = metadata.search(item.copy(title = fields.value["Title"].orEmpty(), artist = fields.value["Artist"].orEmpty(), album = fields.value["Album"].orEmpty()), force = true); busy = false } }) { Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text(if (busy) "Searching…" else "Lookup") }; TextButton(onClick = { fields.value = fields.value + ("Title" to item.title) }) { Text("Restore title") } }
            fields.value.forEach { (label, value) -> OutlinedTextField(value, { set(label, it) }, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = label !in setOf("Composer")) }
            if (matches.isNotEmpty()) {
                Text("Matches", style = MaterialTheme.typography.titleMedium)
                matches.forEach { match -> Card(Modifier.fillMaxWidth().clickable { fields.value = fields.value + mapOf("Title" to match.title, "Artist" to match.artist, "Album" to match.album, "Album artist" to match.albumArtist, "Genre" to match.genre, "Year" to match.year, "Track number" to match.trackNumber.toString(), "Track total" to match.trackTotal.toString(), "Disc number" to match.discNumber.toString(), "Disc total" to match.discTotal.toString()) }, elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) { Column(Modifier.padding(14.dp)) { Text(match.title, style = MaterialTheme.typography.titleMedium); Text("${match.artist} • ${match.album}", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Match score ${match.score}") } } }
            }
            Card(shape = RoundedCornerShape(24.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) { Column(Modifier.padding(16.dp)) { Text("Android media-store editing", style = MaterialTheme.typography.titleMedium); Text("AirPlayr writes the fields Android exposes for this media type. Unsupported embedded fields are left unchanged.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClose) { Text("Cancel") }; Spacer(Modifier.width(8.dp)); Button(enabled = !busy, onClick = { scope.launch { busy = true; val patch = com.airplayr.app.core.metadata.MetadataMatch("manual", item.id, fields.value["Title"].orEmpty(), fields.value["Artist"].orEmpty(), fields.value["Album"].orEmpty(), fields.value["Album artist"].orEmpty(), fields.value["Genre"].orEmpty(), fields.value["Year"].orEmpty(), fields.value["Track number"].orEmpty().toIntOrNull() ?: 0, fields.value["Track total"].orEmpty().toIntOrNull() ?: 0, fields.value["Disc number"].orEmpty().toIntOrNull() ?: 0, fields.value["Disc total"].orEmpty().toIntOrNull() ?: 0, item.durationMs, null, null, 100); val saved = metadata.applyToMediaStore(item, patch); busy = false; if (saved) onSaved(); onClose() } }) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text("Save tags") } }
        }
    }
}
