package com.airplayr.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Divider
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.playback.dsp.DspState
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.airPlayrBottomContentPadding
import com.airplayr.app.ui.components.airPlayrShadow
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val freqs = listOf("32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
private val presets = listOf(
    "custom", "flat", "acoustic", "bass_booster", "bass_reducer", "classical", "deep",
    "electronic", "hip_hop", "modern_hip_hop", "90s", "rnb", "jazz", "latin", "stereo",
    "loudness", "pop", "piano", "lounge", "rock", "gospel", "choir", "small_speakers",
    "treble_booster", "treble_reducer",
)
private const val BAND_COLUMN_WIDTH = 78
private const val GRAPH_PLOT_HEIGHT = 330

@Composable
fun EqualizerScreen(container: AppContainer, onBack: () -> Unit) {
    val state by container.dsp.state.collectAsStateWithLifecycle(initialValue = DspState())
    val scope = rememberCoroutineScope()
    val colors = LocalAirPlayrColors.current
    var bandValues by remember(state.bands) {
        mutableStateOf(freqs.indices.map { i -> ((state.bands[i] ?: 0) / 100f).coerceIn(-12f, 12f) })
    }
    var bass by remember(state.bassMb) { mutableFloatStateOf((state.bassMb / 1500f * 100f).coerceIn(0f, 100f)) }
    var surround by remember(state.virtualizerStrength) { mutableFloatStateOf((state.virtualizerStrength / 1000f * 100f).coerceIn(0f, 100f)) }
    var volume by remember(state.loudnessMb) { mutableFloatStateOf((state.loudnessMb / 1500f * 100f).coerceIn(0f, 100f)) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            EqualizerHeader(
                enabled = state.eqEnabled,
                onBack = onBack,
                onEnabledChange = { enabled ->
                    scope.launch {
                        container.dsp.setEqualizer(enabled, state.eqPreset, state.bassMb, state.loudnessMb, state.virtualizerStrength)
                    }
                },
            )
        }

        item {
            EqualizerGraphCard(
                bands = bandValues,
                onBandChange = { index, value ->
                    val next = bandValues.toMutableList().also { it[index] = value.coerceIn(-12f, 12f) }
                    bandValues = next
                    scope.launch { container.dsp.setBand(index, (value * 100f).roundToInt()) }
                },
            )
        }

        item {
            PresetRow(
                selected = state.eqPreset,
                onPreset = { preset -> scope.launch { applyPreset(container, preset) } },
            )
        }

        item {
            EffectsCard(
                bass = bass,
                surround = surround,
                volume = volume,
                onBassChange = { bass = it },
                onBassChangeFinished = {
                    scope.launch { container.dsp.setEqualizer(state.eqEnabled, "manual", (bass / 100f * 1500f).roundToInt(), state.loudnessMb, state.virtualizerStrength) }
                },
                onSurroundChange = { surround = it },
                onSurroundChangeFinished = {
                    scope.launch { container.dsp.setEqualizer(state.eqEnabled, "manual", state.bassMb, state.loudnessMb, (surround / 100f * 1000f).roundToInt()) }
                },
                onVolumeChange = { volume = it },
                onVolumeChangeFinished = {
                    scope.launch { container.dsp.setEqualizer(state.eqEnabled, "manual", state.bassMb, (volume / 100f * 1500f).roundToInt(), state.virtualizerStrength) }
                },
            )
        }
    }
}

@Composable
private fun EqualizerHeader(enabled: Boolean, onBack: () -> Unit, onEnabledChange: (Boolean) -> Unit) {
    val colors = LocalAirPlayrColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 10.dp, bottom = 30.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = colors.textPrimary, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.GraphicEq, "Equalizer enabled", tint = colors.textMuted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        Column(Modifier.padding(horizontal = 34.dp)) {
            Text(
                "10 BANDS · ±12 DB",
                fontFamily = AirPlayrFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 1.7.sp,
                color = colors.primary,
                modifier = Modifier.padding(top = 22.dp),
            )
            Text(
                "Equalizer",
                fontFamily = AirPlayrFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                lineHeight = 48.sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun EqualizerGraphCard(
    bands: List<Float>,
    onBandChange: (Int, Float) -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val scroll = rememberSaveable(saver = androidx.compose.foundation.ScrollState.Saver) { androidx.compose.foundation.ScrollState(0) }
    val contentWidth = (BAND_COLUMN_WIDTH * bands.size).dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .airPlayrShadow(RoundedCornerShape(30.dp), 5.dp),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(430.dp)
                .horizontalScroll(scroll),
        ) {
            Box(Modifier.width(contentWidth).height(430.dp)) {
                GraphGrid(bands, colors)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 0.dp),
                ) {
                    bands.forEachIndexed { index, value ->
                        BandColumn(
                            index = index,
                            value = value,
                            colors = colors,
                            onChange = { onBandChange(index, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphGrid(bands: List<Float>, colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    val grid = colors.outline.copy(alpha = 0.34f)
    val zero = colors.primary.copy(alpha = 0.95f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(7f, 7f), 0f)
    Canvas(Modifier.width((BAND_COLUMN_WIDTH * bands.size).dp).height(GRAPH_PLOT_HEIGHT.dp).padding(top = 28.dp, start = 0.dp)) {
        val top = 0f
        val bottom = size.height
        val center = bottom / 2f
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEachIndexed { i, fraction ->
            val y = top + bottom * fraction
            drawLine(
                color = if (i == 2) zero else grid,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = if (i == 2) 2.5f else 2f,
                pathEffect = if (i == 2) null else dash,
            )
        }
        val step = size.width / bands.size
        repeat(bands.size) { index ->
            val x = step * index + step / 2f
            drawLine(grid, Offset(x, top + 24f), Offset(x, bottom - 24f), strokeWidth = 2f)
        }
        drawLine(grid, Offset(0f, center), Offset(size.width, center), strokeWidth = 2.5f)
    }
}

@Composable
private fun BandColumn(
    index: Int,
    value: Float,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    onChange: (Float) -> Unit,
) {
    val plotHeight = GRAPH_PLOT_HEIGHT.dp
    val thumbHeight = 42.dp
    val density = LocalDensity.current
    val normalized = ((12f - value) / 24f).coerceIn(0f, 1f)
    val y = normalized * (GRAPH_PLOT_HEIGHT - 42).toFloat()

    Column(
        Modifier.width(BAND_COLUMN_WIDTH.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(BAND_COLUMN_WIDTH.dp)
                .height(plotHeight)
                .pointerInput(index) {
                    detectTapGestures { offset ->
                        val next = 12f - ((offset.y / size.height).coerceIn(0f, 1f) * 24f)
                        onChange(next)
                    }
                },
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp)
                    .width(2.dp)
                    .height((GRAPH_PLOT_HEIGHT - 56).dp)
                    .background(colors.outline.copy(alpha = 0.30f)),
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = y.dp)
                    .size(width = 40.dp, height = thumbHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primary)
                    .border(2.dp, colors.surface.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
                    .pointerInput(index) {
                        var dragValue = value
                        detectDragGestures(
                            onDragStart = { dragValue = value },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val currentY = (12f - dragValue) / 24f * (GRAPH_PLOT_HEIGHT - 42)
                                val dragDp = with(density) { dragAmount.y.toDp().value }
                                val nextY = (currentY + dragDp).coerceIn(0f, (GRAPH_PLOT_HEIGHT - 42).toFloat())
                                dragValue = 12f - (nextY / (GRAPH_PLOT_HEIGHT - 42).toFloat()) * 24f
                                onChange(dragValue)
                            },
                        )
                    },
            )
        }
        Column(
            Modifier.height(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(freqs[index], fontSize = 11.sp, color = colors.textSecondary)
            Text(value.roundToInt().toString(), fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun PresetRow(selected: String, onPreset: (String) -> Unit) {
    val colors = LocalAirPlayrColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 0.dp, top = 26.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        presets.forEach { preset ->
            val label = presetLabel(preset)
            FilterChip(
                selected = selected == preset || (preset == "bass_booster" && selected == "bass"),
                onClick = { onPreset(preset) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Clip) },
                modifier = Modifier.height(46.dp),
                shape = RoundedCornerShape(29.dp),
            )
        }
    }
}

@Composable
private fun EffectsCard(
    bass: Float,
    surround: Float,
    volume: Float,
    onBassChange: (Float) -> Unit,
    onBassChangeFinished: () -> Unit,
    onSurroundChange: (Float) -> Unit,
    onSurroundChangeFinished: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .airPlayrShadow(RoundedCornerShape(30.dp), 4.dp),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        EffectRow(
            title = "Bass boost",
            description = "Lifts the bottom two octaves without touching the rest of the curve.",
            value = bass,
            onChange = onBassChange,
            onChangeFinished = onBassChangeFinished,
        )
        Divider(color = colors.outlineVariant.copy(alpha = 0.45f))
        EffectRow(
            title = "Surround sound",
            description = "Widens the stereo image to simulate spatial depth.",
            value = surround,
            onChange = onSurroundChange,
            onChangeFinished = onSurroundChangeFinished,
        )
        Divider(color = colors.outlineVariant.copy(alpha = 0.45f))
        EffectRow(
            title = "Volume boost",
            description = "Amplifies playback past unity. A look-ahead limiter prevents hard clipping; very high values may still distort.",
            value = volume,
            onChange = onVolumeChange,
            onChangeFinished = onVolumeChangeFinished,
        )
    }
}

@Composable
private fun EffectRow(
    title: String,
    description: String,
    value: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 26.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                color = colors.textPrimary,
            )
            Text(
                if (value <= 0.5f) "off" else "${value.roundToInt()}%",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.primary,
            )
        }
        Text(
            description,
            modifier = Modifier.padding(top = 8.dp),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = colors.textSecondary,
        )
        PercentageSlider(value = value, onChange = onChange, onChangeFinished = onChangeFinished, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun PercentageSlider(
    value: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAirPlayrColors.current
    androidx.compose.material3.Slider(
        value = value.coerceIn(0f, 100f),
        onValueChange = { onChange(it) },
        onValueChangeFinished = onChangeFinished,
        valueRange = 0f..100f,
        modifier = modifier.fillMaxWidth().height(32.dp),
        colors = androidx.compose.material3.SliderDefaults.colors(
            thumbColor = colors.primary,
            activeTrackColor = colors.primary,
            inactiveTrackColor = colors.outlineVariant.copy(alpha = 0.55f),
        ),
    )
}

private fun presetLabel(preset: String): String = when (preset) {
    "flat" -> "Flat"
    "custom" -> "Custom"
    "acoustic" -> "Acoustic"
    "bass_booster" -> "Bass booster"
    "bass_reducer" -> "Bass reducer"
    "classical" -> "Classical"
    "deep" -> "Deep"
    "electronic" -> "Electronic"
    "hip_hop" -> "Hip hop"
    "modern_hip_hop" -> "Modern hip hop"
    "90s" -> "90s"
    "rnb" -> "RnB"
    "jazz" -> "Jazz"
    "latin" -> "Latin"
    "stereo" -> "Stereo"
    "loudness" -> "Loudness"
    "pop" -> "Pop"
    "piano" -> "Piano"
    "lounge" -> "Lounge"
    "rock" -> "Rock"
    "gospel" -> "Gospel"
    "choir" -> "Choir"
    "small_speakers" -> "Small speakers"
    "treble_booster" -> "Treble booster"
    "treble_reducer" -> "Treble reducer"
    else -> preset
}

private suspend fun applyPreset(container: AppContainer, preset: String) {
    val curves = mapOf(
        "flat" to listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        "custom" to listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        "acoustic" to listOf(3, 2, 1, 2, 3, 2, 1, 2, 2, 1),
        "bass_booster" to listOf(9, 8, 7, 5, 3, 1, 0, 0, 0, 0),
        "bass_reducer" to listOf(-9, -8, -7, -5, -3, -1, 0, 0, 0, 0),
        "classical" to listOf(0, 0, 0, 1, 2, 2, 2, 3, 4, 4),
        "deep" to listOf(7, 5, 3, 1, 0, -1, -1, 0, 1, 2),
        "electronic" to listOf(5, 4, 1, 0, -2, 1, 3, 5, 4, 3),
        "hip_hop" to listOf(6, 5, 2, 1, -1, 0, 2, 2, 1, 4),
        "modern_hip_hop" to listOf(7, 6, 3, 0, -1, 1, 3, 4, 2, 5),
        "90s" to listOf(5, 4, 2, 0, -1, 0, 2, 3, 3, 2),
        "rnb" to listOf(5, 4, 2, 0, -1, 1, 3, 4, 3, 2),
        "jazz" to listOf(3, 2, 1, 2, 2, 1, 2, 2, 2, 2),
        "latin" to listOf(5, 4, 2, 1, 0, 1, 3, 3, 2, 2),
        "stereo" to listOf(2, 1, 1, 0, 0, 0, 1, 2, 3, 3),
        "loudness" to listOf(5, 4, 3, 1, 0, 0, 1, 3, 4, 5),
        "pop" to listOf(-1, 1, 3, 4, 2, 0, 2, 3, 2, 1),
        "piano" to listOf(2, 1, 1, 2, 3, 3, 2, 2, 1, 0),
        "lounge" to listOf(2, 2, 1, 1, 0, 1, 2, 2, 1, 0),
        "rock" to listOf(6, 4, 2, -1, -2, 1, 3, 4, 5, 5),
        "gospel" to listOf(3, 3, 2, 1, 1, 2, 3, 3, 2, 2),
        "choir" to listOf(-1, 0, 2, 4, 4, 3, 2, 1, 0, -1),
        "small_speakers" to listOf(-3, 2, 4, 3, 1, 0, 2, 3, 4, 5),
        "treble_booster" to listOf(0, 0, 0, 1, 2, 3, 5, 7, 8, 9),
        "treble_reducer" to listOf(0, 0, 0, -1, -2, -3, -5, -7, -8, -9),
    )
    val currentState = container.dsp.state.first()
    val bands = if (preset == "custom") {
        (0..9).associateWith { currentState.bands[it] ?: 0 }
    } else {
        val curve = curves[preset] ?: curves.getValue("custom")
        curve.mapIndexed { index, value -> index to value * 100 }.toMap()
    }
    container.dsp.setPreset(true, preset, bands)
}
