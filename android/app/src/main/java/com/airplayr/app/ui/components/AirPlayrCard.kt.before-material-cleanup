package com.airplayr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    val elevation: Dp = 5.dp
    val largeElevation: Dp = 6.dp
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
    Box(
        clickModifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.18f),
                clip = false,
            )
            .clip(shape)
            .background(containerColor)
    ) { content() }
}

fun Modifier.airPlayrShadow(shape: Shape, elevation: Dp): Modifier =
    if (elevation.value <= 0f) this else shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.14f),
        spotColor = Color.Black.copy(alpha = 0.18f),
        clip = false,
    )
