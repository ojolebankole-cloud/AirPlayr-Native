package com.airplayr.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airplayr.app.ui.theme.LocalAirPlayrColors

/**
 * Reusable renderer for the supplied AirPlayr illustrated wave animation.
 * The original Lottie JSON remains untouched; only its line colours are themed.
 */
@Composable
fun AirPlayrWaveAnimation(
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    isPlaying: Boolean = true,
) {
    val colors = LocalAirPlayrColors.current
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("animations/illustrated_wave.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = isPlaying,
        restartOnPlay = false,
    )
    val palette = rememberAirPlayrIllustrationPalette(
        colors.primary,
        colors.secondary,
        colors.tertiary,
    )
    val dynamicProperties = rememberAirPlayrLottieTheme(
        colors = palette,
        keyPaths = listOf(
            arrayOf("**", "line1", "**"),
            arrayOf("**", "line2", "**"),
            arrayOf("**", "line3", "**"),
            arrayOf("**", "line4", "**"),
            arrayOf("**", "line5", "**"),
            arrayOf("**", "line6", "**"),
            arrayOf("**", "line7", "**"),
            arrayOf("**", "line8", "**"),
            arrayOf("**", "line9", "**"),
            arrayOf("**", "line10", "**"),
            arrayOf("**", "line11", "**"),
            arrayOf("**", "line12", "**"),
        ),
    )
    AnimatedVisibility(
        visible = isPlaying,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            dynamicProperties = dynamicProperties,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
