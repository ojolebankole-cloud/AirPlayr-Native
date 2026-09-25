package com.airplayr.app.ui.components

import android.graphics.Picture
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import com.caverock.androidsvg.SVG
import kotlin.math.roundToInt

/**
 * Renders the supplied AirPlayr SVG directly from assets as vector artwork.
 *
 * AndroidSVG records the SVG into a Picture at the actual Compose viewport size and
 * the recorded vector drawing is then replayed into the Compose canvas. This avoids
 * the previous full-canvas SrcIn compositing pass, which could turn the illustration
 * viewport into an opaque rectangle on hardware renderers.
 *
 * The source SVG is never converted to PNG/bitmap. Parsing and Picture creation are
 * cached and only repeated when the asset or measured viewport changes.
 */
@Composable
fun AirPlayrSvgImage(
    assetPath: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val svg = remember(assetPath, context) {
        runCatching {
            SVG.getFromAsset(context.assets, assetPath)
        }.getOrNull()
    }

    if (svg == null) return

    androidx.compose.foundation.Canvas(
        modifier = modifier.drawWithCache {
            val widthPx = size.width.roundToInt().coerceAtLeast(1)
            val heightPx = size.height.roundToInt().coerceAtLeast(1)
            val picture: Picture? = runCatching {
                svg.renderToPicture(widthPx, heightPx)
            }.getOrNull()

            onDrawWithContent {
                drawContent()
                picture?.let { recorded ->
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawPicture(
                            recorded,
                            RectF(0f, 0f, size.width, size.height),
                        )
                    }
                }
            }
        },
    ) {}
}
