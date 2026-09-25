package com.airplayr.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

/**
 * Applies a small, stable set of colour filters to exact Lottie key paths.
 * The animation composition, geometry and timing are never rebuilt.
 */
@Composable
fun rememberAirPlayrLottieTheme(
    colors: List<Color>,
    keyPaths: List<Array<String>>,
): LottieDynamicProperties {
    require(colors.isNotEmpty()) { "AirPlayr Lottie themes require at least one colour." }
    require(keyPaths.isNotEmpty()) { "AirPlayr Lottie themes require at least one key path." }

    val stableColors = remember(colors) { colors.toList() }
    val properties = keyPaths.mapIndexed { index, keyPath ->
        val color = stableColors[index % stableColors.size]
        val keyPathId = remember(keyPath.contentToString()) { keyPath.contentToString() }
        val filter = remember(color, keyPathId) {
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                color.toArgb(),
                BlendModeCompat.SRC_ATOP,
            )
        }
        rememberLottieDynamicProperty(
            LottieProperty.COLOR_FILTER,
            filter,
            *keyPath,
        )
    }

    return rememberLottieDynamicProperties(*properties.toTypedArray())
}


/**
 * Illustration-only palette. It deliberately shifts the wallpaper/app accent into a separate hue
 * family so a Lottie illustration does not disappear into the hero background using the same RGB
 * values. The input accents still influence the result, so Dynamic Color remains respected.
 */
@Composable
fun rememberAirPlayrIllustrationPalette(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    intervalMs: Long = 15_000L,
): List<Color> {
    var cycle by remember(primary, secondary, tertiary) { mutableIntStateOf(0) }
    LaunchedEffect(primary, secondary, tertiary) {
        while (true) {
            kotlinx.coroutines.delay(intervalMs)
            cycle = (cycle + 1) % 3
        }
    }
    val base = remember(primary, secondary, tertiary) { listOf(primary, secondary, tertiary) }
    fun shifted(color: Color, degrees: Float, lightness: Float): Color {
        val hsl = FloatArray(3)
        ColorUtils.RGBToHSL(
            (color.red * 255f).toInt().coerceIn(0, 255),
            (color.green * 255f).toInt().coerceIn(0, 255),
            (color.blue * 255f).toInt().coerceIn(0, 255),
            hsl,
        )
        hsl[0] = (hsl[0] + degrees + 360f) % 360f
        hsl[1] = (hsl[1] * 0.90f + 0.10f).coerceIn(0.28f, 0.88f)
        hsl[2] = (hsl[2] + lightness).coerceIn(0.30f, 0.72f)
        return Color(ColorUtils.HSLToColor(hsl))
    }
    val offsets = listOf(128f, 188f, 248f)
    return remember(base, cycle) {
        List(base.size) { index -> shifted(base[(index + cycle) % base.size], offsets[index], if (index == 1) 0.04f else 0f) }
    }
}
