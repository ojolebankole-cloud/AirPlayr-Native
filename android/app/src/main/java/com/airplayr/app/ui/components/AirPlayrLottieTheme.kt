package com.airplayr.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

/**
 * Shared, low-frequency Lottie colour treatment.
 *
 * The existing animations have different layer structures, so callers provide
 * three exact key paths. Animation timing and geometry are untouched.
 */
@Composable
fun rememberAirPlayrLottieTheme(
    colors: List<Color>,
    keyPaths: List<Array<String>>,
): LottieDynamicProperties {
    require(keyPaths.size == 3) { "AirPlayr Lottie themes require three key paths." }

    val stableColors = remember(colors) { colors.toList() }
    val color1 = stableColors[0]
    val color2 = stableColors[1 % stableColors.size]
    val color3 = stableColors[2 % stableColors.size]

    val filter1 = remember(color1) {
        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color1.toArgb(), BlendModeCompat.SRC_ATOP)
    }
    val filter2 = remember(color2) {
        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color2.toArgb(), BlendModeCompat.SRC_ATOP)
    }
    val filter3 = remember(color3) {
        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color3.toArgb(), BlendModeCompat.SRC_ATOP)
    }

    val property1 = rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, filter1, keyPaths[0])
    val property2 = rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, filter2, keyPaths[1])
    val property3 = rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, filter3, keyPaths[2])

    return rememberLottieDynamicProperties(property1, property2, property3)
}
