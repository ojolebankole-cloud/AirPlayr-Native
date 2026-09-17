package com.airplayr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airplayr.app.ui.theme.AirPlayrElevation
import com.airplayr.app.ui.theme.AirPlayrGlass
import com.airplayr.app.ui.theme.AirPlayrRadii

/**
 * Native AirPlayr glass foundation.
 *
 * The reference app uses translucent/blurred surfaces. Compose's dependency-
 * free implementation keeps the same tonal layers, opacity, rim and elevation
 * values while using a blurred backdrop on the app canvas.
 */
@Composable
fun LiquidGlassBackground(content: @Composable BoxScope.() -> Unit) {
    // The reference contract retains AirPlayrGlass.backgroundBlurRadiusDp/
    // backgroundSecondaryBlurRadiusDp for surface-level treatments. The reference
    // root does not blur the entire app canvas, so we intentionally do not
    // apply those radii here. Glass is applied only to explicit surfaces.
    // only to the interactive surfaces that explicitly use BlurView. The root
    // container itself is transparent so no separate top background layer is added.
    Box(Modifier.fillMaxSize(), content = content)
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(AirPlayrRadii.xl),
    tint: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = AirPlayrGlass.surfaceAlpha),
    border: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AirPlayrGlass.borderAlpha),
    elevation: androidx.compose.ui.unit.Dp = AirPlayrElevation.soft,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0xFF171426).copy(alpha = 0.28f),
                spotColor = Color(0xFF171426).copy(alpha = 0.28f),
            )
            .background(tint, shape)
            .border(1.dp, border, shape),
        content = content,
    )
}
