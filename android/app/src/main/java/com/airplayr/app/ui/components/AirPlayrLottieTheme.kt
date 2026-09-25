package com.airplayr.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
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
 * Stable, low-frequency palette rotation for non-header AirPlayr illustrations.
 * It changes the colour mapping occasionally without recreating the Lottie composition.
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
    return remember(base, cycle) {
        List(base.size) { index -> base[(index + cycle) % base.size] }
    }
}
