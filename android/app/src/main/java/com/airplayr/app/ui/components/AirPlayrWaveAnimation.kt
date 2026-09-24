package com.airplayr.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Reusable renderer for the supplied AirPlayr illustrated wave animation.
 * The original Lottie JSON is loaded once from assets and played natively.
 */
@Composable
fun AirPlayrWaveAnimation(
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("animations/illustrated_wave.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        restartOnPlay = false,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
    )
}
