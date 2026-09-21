package com.airplayr.app.ui.screens
import com.airplayr.app.ui.airPlayrBottomContentPadding

import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.airplayr.app.ui.components.airPlayrShadow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.di.AppContainer
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.roundToInt

private val freqs = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")
private val presets = listOf("flat", "acoustic", "bass", "vocal", "treble", "rock")

@Composable
fun EqualizerScreen(container: AppContainer) {
    val state by container.dsp.state.collectAsStateWithLifecycle(initialValue = com.airplayr.app.core.playback.dsp.DspState())
    val scope = rememberCoroutineScope()
    val bandValues = remember(state.bands) { freqs.indices.map { i -> ((state.bands[i] ?: 0) / 100f).coerceIn(-12f, 12f) } }
    var bass by remember(state.bassMb) { mutableFloatStateOf((state.bassMb / 100f).coerceIn(-15f, 15f)) }
    var loudness by remember(state.loudnessMb) { mutableFloatStateOf((state.loudnessMb / 100f).coerceIn(0f, 15f)) }
    var virtualizer by remember(state.virtualizerStrength) { mutableFloatStateOf((state.virtualizerStrength / 10f).coerceIn(0f, 100f)) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("10 BANDS · ±12 DB", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Equalizer", style = MaterialTheme.typography.displaySmall)
                Text("Shape the sound of the current AirPlayr playback session.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.eqEnabled, onCheckedChange = { value -> scope.launch { container.dsp.setEqualizer(value, state.eqPreset, state.bassMb, state.loudnessMb, state.virtualizerStrength) } })
        }
        LazyVerticalGrid(state = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() },
            columns = GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = airPlayrBottomContentPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(modifier = Modifier.airPlayrShadow(RoundedCornerShape(28.dp), 6.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        EqCurve(bands = bandValues, modifier = Modifier.fillMaxWidth().height(250.dp))
                        Text("Drag controls below to tune each band", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { preset ->
                        FilterChip(selected = state.eqPreset == preset, onClick = { scope.launch { applyPreset(container, preset) } }, label = { Text(preset.replaceFirstChar(Char::uppercase)) })
                    }
                }
            }
            items(freqs.indices.toList(), span = { GridItemSpan(1) }) { index ->
                BandCard(freqs[index], bandValues[index], onChange = { next -> scope.launch { container.dsp.setBand(index, (next * 100).roundToInt()) } })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Sound shaping", style = MaterialTheme.typography.headlineSmall)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                ShapeControl("Bass boost", "Lifts the bottom end without changing the rest of the curve.", bass, -15f..15f) { bass = it; scope.launch { container.dsp.setEqualizer(state.eqEnabled, "manual", (it * 100).roundToInt(), state.loudnessMb, state.virtualizerStrength) } }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                ShapeControl("Loudness", "Adds controlled gain for quieter playback levels.", loudness, 0f..15f) { loudness = it; scope.launch { container.dsp.setEqualizer(state.eqEnabled, "manual", state.bassMb, (it * 100).roundToInt(), state.virtualizerStrength) } }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                ShapeControl("Virtualizer", "Widens the stereo field when supported by the device.", virtualizer, 0f..100f) { virtualizer = it; scope.launch { container.dsp.setEqualizer(state.eqEnabled, "manual", state.bassMb, state.loudnessMb, (it * 10).roundToInt()) } }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(modifier = Modifier.airPlayrShadow(RoundedCornerShape(24.dp), 5.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Audio effects connected", style = MaterialTheme.typography.titleMedium); Text("Band changes and sound shaping are persisted and applied to the Android audio session when supported.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { scope.launch { container.dsp.setEqualizer(true, "flat", 0, 0, 0); container.dsp.clearBands() } }) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reset EQ") }
                }
            }
        }
    }
}

private suspend fun applyPreset(container: AppContainer, preset: String) {
    val curves = mapOf(
        "flat" to listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        "acoustic" to listOf(2, 1, 1, 2, 3, 2, 1, 2, 2, 1),
        "bass" to listOf(8, 7, 6, 4, 2, 1, 0, 0, 0, 0),
        "vocal" to listOf(-2, -1, 1, 3, 4, 4, 3, 2, 0, -1),
        "treble" to listOf(0, 0, 0, 1, 2, 3, 5, 7, 8, 9),
        "rock" to listOf(6, 4, 2, -1, -2, 1, 3, 4, 5, 5),
    )
    container.dsp.setEqualizer(true, preset, 0, 0, 0)
    curves[preset].orEmpty().forEachIndexed { index, value -> container.dsp.setBand(index, value * 100) }
}

@Composable
private fun BandCard(label: String, value: Float, onChange: (Float) -> Unit) {
    Card(modifier = Modifier.airPlayrShadow(RoundedCornerShape(20.dp), 4.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(58.dp)) { Text(label, style = MaterialTheme.typography.labelLarge); Text("${value.roundToInt()} dB", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Slider(value = value, onValueChange = onChange, valueRange = -12f..12f, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ShapeControl(title: String, description: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Card(modifier = Modifier.airPlayrShadow(RoundedCornerShape(24.dp), 5.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Text("${value.roundToInt()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = value, onValueChange = onChange, valueRange = range)
        }
    }
}

@Composable
private fun EqCurve(bands: List<Float>, modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val mid = size.height / 2f
        val xStep = size.width / (bands.size - 1).coerceAtLeast(1)
        val path = Path()
        bands.forEachIndexed { i, v ->
            val x = i * xStep
            val y = mid - (v / 12f) * (size.height * .36f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(primary, 7f, Offset(x, y))
        }
        drawLine(outlineVariant, Offset(0f, mid), Offset(size.width, mid), strokeWidth = 2f)
        drawPath(path, primary, style = Stroke(width = 5f, cap = StrokeCap.Round))
    }
}
