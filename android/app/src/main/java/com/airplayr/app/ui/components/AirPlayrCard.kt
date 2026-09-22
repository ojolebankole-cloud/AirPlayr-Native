package com.airplayr.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airplayr.app.ui.theme.AirPlayrRadii
import com.airplayr.app.ui.theme.LocalAirPlayrColors

/** Shared AirPlayr card primitive. Only non-video UI should use this for new cards. */
object AirPlayrCardDefaults {
    val radius: Dp = AirPlayrRadii.lg
    val largeRadius: Dp = AirPlayrRadii.xxl
    val elevation: Dp = 0.dp
    val largeElevation: Dp = 0.dp
}

@Composable
fun AirPlayrCard(
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(AirPlayrCardDefaults.radius),
    containerColor: Color = LocalAirPlayrColors.current.surfaceSolid,
    elevation: Dp = AirPlayrCardDefaults.elevation,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val clickModifier = onClick?.let { modifier.clickable(onClick = it) } ?: modifier
    Card(
        modifier = clickModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        // AirPlayr cards are deliberately flat. Keep the parameter for source compatibility,
        // but do not render a card shadow anywhere.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { content() }
}

fun Modifier.airPlayrShadow(shape: Shape, elevation: Dp): Modifier = this
