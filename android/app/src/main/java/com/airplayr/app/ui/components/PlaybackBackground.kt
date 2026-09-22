package com.airplayr.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.unit.dp
import com.airplayr.app.core.data.preferences.NowPlayingTheme
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background-only visual system for the Now Playing experience.
 *
 * It deliberately owns no playback state and draws no controls. The same component can therefore
 * be placed behind the player, Lyrics sheet and Queue sheet without changing their behavior.
 */
private val SkyBlue = Color(0xFFF5FBFF)

private data class ArtworkPalette(
    val colors: List<Color>,
    val average: Color,
)

@Composable
fun PlaybackBackground(
    theme: String,
    artworkUri: String?,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    val colors = LocalAirPlayrColors.current
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val bitmap by produceState<Bitmap?>(null, artworkUri, inspection) {
        value = if (inspection) null else withContext(Dispatchers.IO) {
            artworkUri?.takeIf { it.isNotBlank() }?.let { loadBackdropBitmap(context, it) }
        }
    }
    val palette by produceState(
        initialValue = ArtworkPalette(listOf(SkyBlue), SkyBlue),
        key1 = bitmap,
    ) {
        value = bitmap?.let { extractPalette(it) } ?: ArtworkPalette(listOf(SkyBlue), SkyBlue)
    }

    val effectiveTheme = theme.takeIf { it in NowPlayingTheme.values } ?: NowPlayingTheme.DEFAULT
    val isDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val needsCpuBlur = bitmap != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
        effectiveTheme in setOf(NowPlayingTheme.BLUR, NowPlayingTheme.GRADIENT_BLUR, NowPlayingTheme.GLOW, NowPlayingTheme.SHIMMER)
    val blurredBitmap by produceState<Bitmap?>(null, bitmap, needsCpuBlur) {
        value = if (needsCpuBlur) withContext(Dispatchers.Default) { bitmap?.let { blurBitmap(it) } } else null
    }
    val displayBitmap = (blurredBitmap ?: bitmap)?.asImageBitmap()

    Box(modifier) {
        when (effectiveTheme) {
            NowPlayingTheme.DEFAULT -> {
                val materialYou = if (isDynamic) {
                    if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else null
                Box(Modifier.matchParentSize().background(materialYou?.background ?: SkyBlue))
            }

            NowPlayingTheme.COLOURED -> {
                Box(Modifier.matchParentSize().background(palette.colors.firstOrNull() ?: SkyBlue))
            }

            NowPlayingTheme.GRADIENT -> {
                Box(
                    Modifier.matchParentSize().background(
                        Brush.linearGradient(
                            colors = gradientColors(palette),
                        ),
                    ),
                )
            }

            NowPlayingTheme.BLUR -> {
                ArtworkLayer(displayBitmap, Modifier.matchParentSize(), blur = if (isDynamic) 34.dp else 0.dp, alpha = 0.78f)
                Box(Modifier.matchParentSize().background(palette.average.copy(alpha = 0.20f)))
            }

            NowPlayingTheme.GRADIENT_BLUR -> {
                ArtworkLayer(displayBitmap, Modifier.matchParentSize(), blur = if (isDynamic) 42.dp else 0.dp, alpha = 0.68f)
                Box(
                    Modifier.matchParentSize().background(
                        Brush.linearGradient(
                            colors = gradientColors(palette).map { it.copy(alpha = 0.78f) },
                        ),
                    ),
                )
                Box(Modifier.matchParentSize().background(palette.average.copy(alpha = 0.10f)))
            }

            NowPlayingTheme.GLOW -> {
                val glowBase = if (bitmap == null) SkyBlue else colors.background
                Box(
                    Modifier.matchParentSize().background(
                        Brush.radialGradient(
                            colors = listOf(
                                palette.colors.getOrElse(0) { SkyBlue }.copy(alpha = 0.92f),
                                palette.colors.getOrElse(1) { SkyBlue }.copy(alpha = 0.62f),
                                glowBase.copy(alpha = 0.96f),
                            ),
                        ),
                    ),
                )
                if (bitmap != null) ArtworkLayer(displayBitmap, Modifier.matchParentSize(), blur = if (isDynamic) 64.dp else 0.dp, alpha = 0.16f, scale = 1.24f)
                Box(
                    Modifier.matchParentSize().background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.08f),
                            ),
                        ),
                    ),
                )
            }

            NowPlayingTheme.SHIMMER -> {
                val transition = rememberInfiniteTransition(label = "now-playing-shimmer")
                val phase by transition.animateFloat(
                    initialValue = -0.8f,
                    targetValue = 1.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(5200),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "shimmer-phase",
                )
                val base = gradientColors(palette)
                Box(
                    Modifier.matchParentSize().background(
                        Brush.linearGradient(
                            colors = listOf(
                                base[0],
                                base[1],
                                Color.White.copy(alpha = 0.16f),
                                base[2],
                                base[0],
                            ),
                            start = androidx.compose.ui.geometry.Offset(phase * 1800f, 0f),
                            end = androidx.compose.ui.geometry.Offset(phase * 1800f + 1100f, 1500f),
                        ),
                    ),
                )
                if (bitmap != null) {
                    ArtworkLayer(
                        displayBitmap,
                        Modifier.matchParentSize(),
                        blur = if (isDynamic) 48.dp else 0.dp,
                        alpha = 0.13f,
                        scale = 1.16f,
                    )
                }
                if (reduceMotion) {
                    // Keep the selected Shimmer appearance static when the user's global
                    // Reduce motion preference is enabled.
                    Box(
                        Modifier.matchParentSize().background(
                            Brush.linearGradient(
                                colors = base.map { it.copy(alpha = 0.72f) },
                            ),
                        ),
                    )
                }
            }
        }

        // A very light readability veil keeps the existing controls legible without becoming a
        // second visual theme. It is intentionally independent of playback and artwork state.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    if (colors.background.luminance() < 0.5f) {
                        Color.Black.copy(alpha = 0.06f)
                    } else {
                        Color.White.copy(alpha = 0.04f)
                    },
                ),
        )
    }
}

@Composable
private fun ArtworkLayer(
    bitmap: ImageBitmap?,
    modifier: Modifier,
    blur: androidx.compose.ui.unit.Dp,
    alpha: Float,
    scale: Float = 1.10f,
) {
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier)
                .alpha(alpha),
            contentScale = ContentScale.Crop,
        )
    } ?: Box(modifier.background(SkyBlue))
}

private fun gradientColors(palette: ArtworkPalette): List<Color> {
    val p = palette.colors
    return listOf(
        p.getOrElse(0) { SkyBlue },
        p.getOrElse(1) { p.getOrElse(0) { SkyBlue } },
        p.getOrElse(2) { p.getOrElse(0) { SkyBlue } },
    )
}

private suspend fun loadBackdropBitmap(context: Context, raw: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val uri = Uri.parse(raw)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        val sample = calculateSample(bounds.outWidth, bounds.outHeight, 512, 512)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
}

private fun calculateSample(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
    var sample = 1
    while (width / (sample * 2) >= targetWidth && height / (sample * 2) >= targetHeight) {
        sample *= 2
    }
    return sample
}

private fun blurBitmap(source: Bitmap): Bitmap {
    val target = Bitmap.createScaledBitmap(source, 160, 160, true)
    val width = target.width
    val height = target.height
    val input = IntArray(width * height)
    val horizontal = IntArray(width * height)
    val output = IntArray(width * height)
    target.getPixels(input, 0, width, 0, 0, width, height)
    val radius = 6
    val diameter = radius * 2 + 1

    for (y in 0 until height) {
        var r = 0
        var g = 0
        var b = 0
        var a = 0
        for (x in -radius..radius) {
            val px = input[y * width + x.coerceIn(0, width - 1)]
            r += android.graphics.Color.red(px)
            g += android.graphics.Color.green(px)
            b += android.graphics.Color.blue(px)
            a += android.graphics.Color.alpha(px)
        }
        for (x in 0 until width) {
            horizontal[y * width + x] = android.graphics.Color.argb(a / diameter, r / diameter, g / diameter, b / diameter)
            val removeX = (x - radius).coerceIn(0, width - 1)
            val addX = (x + radius + 1).coerceIn(0, width - 1)
            val removePx = input[y * width + removeX]
            val addPx = input[y * width + addX]
            r += android.graphics.Color.red(addPx) - android.graphics.Color.red(removePx)
            g += android.graphics.Color.green(addPx) - android.graphics.Color.green(removePx)
            b += android.graphics.Color.blue(addPx) - android.graphics.Color.blue(removePx)
            a += android.graphics.Color.alpha(addPx) - android.graphics.Color.alpha(removePx)
        }
    }

    for (x in 0 until width) {
        var r = 0
        var g = 0
        var b = 0
        var a = 0
        for (y in -radius..radius) {
            val px = horizontal[y.coerceIn(0, height - 1) * width + x]
            r += android.graphics.Color.red(px)
            g += android.graphics.Color.green(px)
            b += android.graphics.Color.blue(px)
            a += android.graphics.Color.alpha(px)
        }
        for (y in 0 until height) {
            output[y * width + x] = android.graphics.Color.argb(a / diameter, r / diameter, g / diameter, b / diameter)
            val removeY = (y - radius).coerceIn(0, height - 1)
            val addY = (y + radius + 1).coerceIn(0, height - 1)
            val removePx = horizontal[removeY * width + x]
            val addPx = horizontal[addY * width + x]
            r += android.graphics.Color.red(addPx) - android.graphics.Color.red(removePx)
            g += android.graphics.Color.green(addPx) - android.graphics.Color.green(removePx)
            b += android.graphics.Color.blue(addPx) - android.graphics.Color.blue(removePx)
            a += android.graphics.Color.alpha(addPx) - android.graphics.Color.alpha(removePx)
        }
    }

    return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
}

private fun extractPalette(bitmap: Bitmap): ArtworkPalette {
    val sampleW = minOf(bitmap.width, 96)
    val sampleH = minOf(bitmap.height, 96)
    val scaled = if (bitmap.width != sampleW || bitmap.height != sampleH) {
        Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
    } else bitmap

    val buckets = mutableMapOf<Int, Int>()
    var totalR = 0L
    var totalG = 0L
    var totalB = 0L
    var count = 0

    val step = 4
    for (y in 0 until scaled.height step step) {
        for (x in 0 until scaled.width step step) {
            val pixel = scaled.getPixel(x, y)
            val alpha = android.graphics.Color.alpha(pixel)
            if (alpha < 170) continue
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            totalR += r
            totalG += g
            totalB += b
            count++

            // 32-level quantization makes the palette stable while keeping the implementation
            // lightweight enough for every track change.
            val qr = r / 32
            val qg = g / 32
            val qb = b / 32
            val key = (qr shl 8) or (qg shl 4) or qb
            buckets[key] = (buckets[key] ?: 0) + 1
        }
    }

    if (count == 0) return ArtworkPalette(listOf(SkyBlue), SkyBlue)

    val average = Color(
        red = totalR.toFloat() / count / 255f,
        green = totalG.toFloat() / count / 255f,
        blue = totalB.toFloat() / count / 255f,
    )

    val candidates = buckets.entries
        .sortedByDescending { it.value }
        .take(12)
        .map { entry ->
            val r = ((entry.key shr 8) and 0xF) * 32 + 16
            val g = ((entry.key shr 4) and 0xF) * 32 + 16
            val b = (entry.key and 0xF) * 32 + 16
            Color(
                red = (r.coerceAtMost(255) / 255f),
                green = (g.coerceAtMost(255) / 255f),
                blue = (b.coerceAtMost(255) / 255f),
            )
        }

    val selected = buildList {
        for (candidate in candidates) {
            if (all { colorDistance(it, candidate) > 0.12f }) add(candidate)
            if (size == 3) break
        }
    }.ifEmpty { listOf(average) }

    return ArtworkPalette(selected, average)
}

private fun colorDistance(a: Color, b: Color): Float {
    val dr = a.red - b.red
    val dg = a.green - b.green
    val db = a.blue - b.blue
    return kotlin.math.sqrt(dr * dr + dg * dg + db * db)
}
