package com.airplayr.app.ui.components

import android.app.WallpaperManager
import android.os.Build
import androidx.core.graphics.ColorUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private data class CardPalette(val gradient: List<Color>, val alternate: List<Color>, val accent: Color, val accent2: Color, val accentLight: Color, val ink: Color)

private fun mix(a: Color, b: Color, amount: Float): Color { val t = amount.coerceIn(0f, 1f); return Color(a.red + (b.red-a.red)*t, a.green+(b.green-a.green)*t, a.blue+(b.blue-a.blue)*t, 1f) }
private fun hashString(value: String): Int { var h = 2166136261u; value.forEach { h = (h xor it.code.toUInt()) * 16777619u }; return h.toInt() }
private fun hsl(color: Color): FloatArray { val out = FloatArray(3); ColorUtils.RGBToHSL((color.red*255).roundToInt(), (color.green*255).roundToInt(), (color.blue*255).roundToInt(), out); return out }
private fun shifted(color: Color, hue: Float, light: Float, sat: Float): Color { val h = hsl(color); h[0] = (h[0]+hue+360f)%360f; h[1] = (h[1]*sat).coerceIn(0f,1f); h[2] = (h[2]+light).coerceIn(0f,1f); return Color(ColorUtils.HSLToColor(h)) }

private fun palette(kind: String, phase: String, seed: String, dark: Boolean, base: Color, secondary: Color, tertiary: Color, surface: Color, surfaceLow: Color, surfaceHigh: Color, surfaceHighest: Color): CardPalette {
    val shifts = floatArrayOf(-42f, -18f, 16f, 38f, 64f, 86f, 112f, 138f, 164f, 196f, 224f, 252f)
    val hue = shifts[Math.floorMod(hashString("$seed:$kind:$phase"), shifts.size)]
    val accent = shifted(base, hue, if (dark) .03f else .02f, 1.02f)
    val accent2 = shifted(secondary, -hue * .45f, if (dark) .02f else .015f, 1.02f)
    val accent3 = shifted(tertiary, hue * .34f, if (dark) .01f else .01f, 1f)
    val baseSurface = if (dark) surfaceLow else surface
    val a = mix(baseSurface, accent, if (dark) .34f else .22f)
    val b = mix(surfaceHigh, accent2, if (dark) .30f else .18f)
    val c = mix(surfaceHighest, accent3, if (dark) .24f else .14f)
    val alt = listOf(
        mix(a, accent3, if (dark) .24f else .16f),
        mix(c, accent, if (dark) .26f else .18f),
        mix(b, accent2, if (dark) .22f else .14f),
    )
    val ink = if (dark) Color.White else base.copy(red = (base.red * .74f).coerceIn(0f, 1f), green = (base.green * .74f).coerceIn(0f, 1f), blue = (base.blue * .74f).coerceIn(0f, 1f), alpha = 1f)
    return CardPalette(listOf(a, b, c), alt, accent, accent2, mix(accent, surfaceHighest, .45f), ink)
}

private fun phase(hour: Int) = when (hour) { in 5..11 -> "morning"; in 12..16 -> "afternoon"; in 17..21 -> "evening"; else -> "night" }
private fun digitalTime(calendar: Calendar): String { val h = calendar.get(Calendar.HOUR); val m = calendar.get(Calendar.MINUTE).toString().padStart(2,'0'); return "${if (h == 0) 12 else h}:$m" }

private fun wallpaperSeed(context: android.content.Context): String {
    if (Build.VERSION.SDK_INT < 27) return "session"
    return runCatching {
        val c = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        val p = c?.primaryColor?.toArgb()?.toUInt()?.toString(16) ?: "none"
        val s = c?.secondaryColor?.toArgb()?.toUInt()?.toString(16) ?: "none"
        val t = c?.tertiaryColor?.toArgb()?.toUInt()?.toString(16) ?: "none"
        "$p:$s:$t"
    }.getOrDefault("session")
}

@Composable
fun AirPlayrFeatureCard(
    eyebrow: String,
    title: String,
    imageRes: Int,
    onClick: () -> Unit,
    kind: String = "rediscover",
    clockHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    stableSessionSeed: String? = null,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val colors = LocalAirPlayrColors.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val now = remember(clockHour) {
        Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, clockHour) }
    }
    // Home provides a saveable session seed so the artwork palette/key remains stable when
    // the Home destination is recreated. Generating a new random seed here caused recolouring
    // cache misses and repeated CPU-heavy bitmap work on every return to Home.
    val sessionSeed = stableSessionSeed ?: remember { "${System.currentTimeMillis()}-${kotlin.random.Random.nextInt()}" }
    val wallpaper = remember { wallpaperSeed(context) }
    val currentPhase = phase(clockHour)
    val seed = "$wallpaper:$sessionSeed"
    val p = remember(kind, currentPhase, seed, dark, colors.primary, colors.secondary, colors.tertiary, colors.surface, colors.surfaceContainerLow, colors.surfaceContainerHigh, colors.surfaceContainerHighest) { palette(kind, currentPhase, seed, dark, colors.primary, colors.secondary, colors.tertiary, colors.surface, colors.surfaceContainerLow, colors.surfaceContainerHigh, colors.surfaceContainerHighest) }
    val width = (configuration.screenWidthDp * .49f).coerceIn(178f, 196f).dp
    val height = (width.value * 1.39f).dp
    val titleFont = when (kind) { "rock" -> AirPlayrFonts.DancingScript; "playlists" -> AirPlayrFonts.ShadowsIntoLight; "rediscover" -> AirPlayrFonts.Oswald; else -> AirPlayrFonts.SmoochSans }
    Box(Modifier.width(width).height(height).clip(RoundedCornerShape(27.dp)).clickable(onClick = onClick)) {
        // One smooth Material-compatible gradient with no stacked overlays or transition seams.
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(p.gradient, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(1000f, 1000f))))
        Image(painterResource(imageRes), title, Modifier.fillMaxWidth().height(height*.64f).padding(10.dp).align(Alignment.TopCenter))
        if (kind in setOf("morning","afternoon","night")) Text(digitalTime(now), color=p.ink.copy(alpha=.78f), fontSize=12.sp, lineHeight=16.sp, fontWeight=FontWeight.Medium, modifier=Modifier.align(Alignment.TopEnd).padding(top=16.dp,end=17.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(start=17.dp,end=16.dp,bottom=17.dp)) {
            Text(title,color=p.ink,fontFamily=titleFont,fontSize=21.sp,lineHeight=24.sp,fontWeight=if(kind=="rock"||kind=="playlists") FontWeight.Normal else FontWeight.Normal,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(eyebrow,color=p.ink.copy(alpha=.82f),fontFamily=AirPlayrFonts.OpenSans,fontSize=11.sp,lineHeight=14.sp,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=5.dp))
        }
    }
}
