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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val freqs = listOf("32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
private val presets = listOf("flat", "acoustic", "bass_booster", "bass_reducer", "vocal", "treble", "rock")
private const val BAND_COLUMN_WIDTH = 114
private const val GRAPH_PLOT_HEIGHT = 575

@Composable
fun EqualizerScreen(container: AppContainer, onBack: () -> Unit = {}) {
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
                onBassChange = {
                    bass = it
                    scope.launch { container.dsp.setEqualizer(state.eqEnabled, "manual", (it / 100f * 1500f).roundToInt(), state.loudnessMb, state.virtualizerStrength) }
                },
                onSurroundChange = {
                    surround = it
                    scope.launch { container.dsp.setEqualizer(state.eqEnabled, "manual", state.bassMb, state.loudnessMb, (it / 100f * 1000f).roundToInt()) }
                },
                onVolumeChange = {
                    volume = it
                    scope.launch { container.dsp.setEqualizer(state.eqEnabled, "manual", state.bassMb, (it / 100f * 1500f).roundToInt(), state.virtualizerStrength) }
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
                fontSize = 48.sp,
                lineHeight = 54.sp,
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
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(710.dp)
                .horizontalScroll(scroll),
        ) {
            Box(Modifier.width(contentWidth).height(710.dp)) {
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
    val thumbHeight = 54.dp
    val density = LocalDensity.current
    val normalized = ((12f - value) / 24f).coerceIn(0f, 1f)
    val y = normalized * (GRAPH_PLOT_HEIGHT - 54).toFloat()

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
                    .size(width = 48.dp, height = thumbHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primary)
                    .border(2.dp, colors.surface.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
                    .pointerInput(index) {
                        var dragValue = value
                        detectDragGestures(
                            onDragStart = { dragValue = value },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val currentY = (12f - dragValue) / 24f * (GRAPH_PLOT_HEIGHT - 54)
                                val dragDp = with(density) { dragAmount.y.toDp().value }
                                val nextY = (currentY + dragDp).coerceIn(0f, (GRAPH_PLOT_HEIGHT - 54).toFloat())
                                dragValue = 12f - (nextY / (GRAPH_PLOT_HEIGHT - 54).toFloat()) * 24f
                                onChange(dragValue)
                            },
                        )
                    },
            )
        }
        Column(
            Modifier.height(92.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(freqs[index], fontSize = 12.sp, color = colors.textSecondary)
            Text(value.roundToInt().toString(), fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 5.dp))
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
            val label = when (preset) {
                "bass_booster" -> "Bass Booster"
                "bass_reducer" -> "Bass Reducer"
                else -> preset.replaceFirstChar(Char::uppercase)
            }
            FilterChip(
                selected = selected == preset || (preset == "bass_booster" && selected == "bass"),
                onClick = { onPreset(preset) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Clip) },
                modifier = Modifier.height(58.dp),
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
    onSurroundChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .airPlayrShadow(RoundedCornerShape(30.dp), 4.dp),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        EffectRow(
            title = "Bass boost",
            description = "Lifts the bottom two octaves without touching the rest of the curve.",
            value = bass,
            onChange = onBassChange,
        )
        Divider(color = colors.outlineVariant.copy(alpha = 0.45f))
        EffectRow(
            title = "Surround sound",
            description = "Widens the stereo image to simulate spatial depth.",
            value = surround,
            onChange = onSurroundChange,
        )
        Divider(color = colors.outlineVariant.copy(alpha = 0.45f))
        EffectRow(
            title = "Volume boost",
            description = "Amplifies playback past unity. A look-ahead limiter prevents hard clipping; very high values may still distort.",
            value = volume,
            onChange = onVolumeChange,
        )
    }
}

@Composable
private fun EffectRow(
    title: String,
    description: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 26.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                fontFamily = AirPlayrFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
                lineHeight = 29.sp,
                color = colors.textPrimary,
            )
            Text(
                if (value <= 0.5f) "off" else "${value.roundToInt()}%",
                fontFamily = AirPlayrFonts.OpenSans,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = colors.primary,
            )
        }
        Text(
            description,
            modifier = Modifier.padding(top = 8.dp),
            fontFamily = AirPlayrFonts.OpenSans,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = colors.textSecondary,
        )
        PercentageSlider(value = value, onChange = onChange, modifier = Modifier.padding(top = 20.dp))
    }
}

@Composable
private fun PercentageSlider(value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAirPlayrColors.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(42.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onChange((offset.x / size.width * 100f).coerceIn(0f, 100f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onChange((change.position.x / size.width * 100f).coerceIn(0f, 100f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.outlineVariant.copy(alpha = 0.55f)),
        )
        Box(
            Modifier
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.primary),
        )
        Box(
            Modifier
                .offset(x = ((value / 100f) * (maxWidth.value - 6f)).dp)
                .width(6.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.primary),
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(7.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(colors.textPrimary.copy(alpha = 0.75f)),
        )
    }
}

private suspend fun applyPreset(container: AppContainer, preset: String) {
    val curves = mapOf(
        "flat" to listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        "acoustic" to listOf(2, 1, 1, 2, 3, 2, 1, 2, 2, 1),
        "bass_booster" to listOf(8, 7, 6, 4, 2, 1, 0, 0, 0, 0),
        "bass_reducer" to listOf(-8, -7, -6, -4, -2, -1, 0, 0, 0, 0),
        "vocal" to listOf(-2, -1, 1, 3, 4, 4, 3, 2, 0, -1),
        "treble" to listOf(0, 0, 0, 1, 2, 3, 5, 7, 8, 9),
        "rock" to listOf(6, 4, 2, -1, -2, 1, 3, 4, 5, 5),
    )
    container.dsp.setEqualizer(true, preset, 0, 0, 0)
    curves[preset].orEmpty().forEachIndexed { index, value -> container.dsp.setBand(index, value * 100) }
}
