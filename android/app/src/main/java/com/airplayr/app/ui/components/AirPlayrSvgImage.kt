package com.airplayr.app.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.caverock.androidsvg.SVG

/**
 * Renders the supplied AirPlayr SVG directly from assets as vector artwork.
 *
 * The SVG is parsed once per asset key; it is never converted to a bitmap.
 */
@Composable
fun AirPlayrSvgImage(
    assetPath: String,
    modifier: Modifier = Modifier,
    tint: Brush? = null,
    tintAlpha: Float = 0.16f,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val svg = remember(assetPath, context) {
        runCatching {
            SVG.getFromAsset(context.assets, assetPath)
        }.getOrNull()
    }

    Canvas(
        modifier = modifier.drawWithContent {
            drawContent()
            if (tint != null) {
                drawRect(
                    brush = tint,
                    alpha = tintAlpha.coerceIn(0f, 1f),
                    blendMode = androidx.compose.ui.graphics.BlendMode.SrcIn,
                )
            }
        },
    ) {
        svg ?: return@Canvas
        drawIntoCanvas { canvas ->
            svg.renderToCanvas(
                canvas.nativeCanvas,
                RectF(0f, 0f, size.width, size.height),
            )
        }
    }
}
