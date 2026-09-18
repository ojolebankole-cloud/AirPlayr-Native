package com.airplayr.app.ui.components

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrFeatureCardTokens
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private val LIGHT_GRADIENTS = mapOf(
    "for-you" to listOf(listOf("#F6D7E8", "#E9D8FA", "#D7E8F7"), listOf("#F8D9E1", "#E3D9F8", "#D6EAF2")),
    "morning" to listOf(listOf("#FFE0B5", "#D9F0D8", "#D7E9F8"), listOf("#FFD9C7", "#E3F0D2", "#D7E5FA")),
    "afternoon" to listOf(listOf("#FFD0C1", "#D5E8F7", "#E8D7F5"), listOf("#FFDDB2", "#D8EEE8", "#DAD9F7")),
    "night" to listOf(listOf("#D9D4F7", "#E6CEE8", "#CFE3F1"), listOf("#D5DDF6", "#E5D0EA", "#D4E8E5")),
    "rediscover" to listOf(listOf("#CDEDE4", "#D8E1F7", "#FFE0D1"), listOf("#D2EBDD", "#DAD5F3", "#FFDCCF")),
    "rock" to listOf(listOf("#FFD1C5", "#D6D8F7", "#CBE9F0"), listOf("#FFD8B8", "#D4E5F4", "#DCD2F4")),
    "playlists" to listOf(listOf("#F3D1E2", "#D5DDF6", "#D2EBD9"), listOf("#F5D6C9", "#D8D5F3", "#D0E9E0")),
)
private val DARK_GRADIENTS = mapOf(
    "for-you" to listOf(listOf("#241B3A", "#30244A", "#182B3D"), listOf("#2A1D35", "#29264A", "#18313B")),
    "morning" to listOf(listOf("#3A2920", "#203A35", "#202C45"), listOf("#3A2628", "#24382D", "#202B48")),
    "afternoon" to listOf(listOf("#17343D", "#3A2A20", "#2C2343"), listOf("#19323A", "#343020", "#292448")),
    "night" to listOf(listOf("#23244B", "#34213E", "#182D42"), listOf("#202A48", "#30223D", "#19383A")),
    "rediscover" to listOf(listOf("#163936", "#28264A", "#3A2824"), listOf("#193A32", "#302545", "#392820")),
    "rock" to listOf(listOf("#3B2422", "#29264A", "#173641"), listOf("#38251F", "#26384A", "#302340")),
    "playlists" to listOf(listOf("#3A2030", "#25294A", "#193A35"), listOf("#38252A", "#282544", "#1D3933")),
)
private val ACCENTS = mapOf(
    "for-you" to listOf("#7C5CFC", "#FF6B9E", "#29A6A6"),
    "morning" to listOf("#FF7B54", "#7C5CFC", "#27A36A"),
    "afternoon" to listOf("#1687A7", "#FF8A34", "#7E5AE6"),
    "night" to listOf("#6555D9", "#F05C8D", "#2D7BD4"),
    "rediscover" to listOf("#1E8C87", "#7A5AF8", "#EA6B4A"),
    "rock" to listOf("#E35A40", "#6A5CF6", "#1887A6"),
    "playlists" to listOf("#D94C85", "#5B69E8", "#18A77B"),
)

private data class CardPalette(val gradient: List<Color>, val alternate: List<Color>, val accent: Color, val accent2: Color, val accentLight: Color, val ink: Color)

private fun hex(value: String): Color = Color(android.graphics.Color.parseColor(value))
private fun mix(a: Color, b: Color, amount: Float): Color { val t = amount.coerceIn(0f, 1f); return Color(a.red + (b.red-a.red)*t, a.green+(b.green-a.green)*t, a.blue+(b.blue-a.blue)*t, 1f) }
private fun hashString(value: String): Int { var h = 2166136261u; value.forEach { h = (h xor it.code.toUInt()) * 16777619u }; return h.toInt() }
private fun hsl(color: Color): FloatArray { val out = FloatArray(3); ColorUtils.RGBToHSL((color.red*255).roundToInt(), (color.green*255).roundToInt(), (color.blue*255).roundToInt(), out); return out }
private fun shifted(color: Color, hue: Float, light: Float, sat: Float): Color { val h = hsl(color); h[0] = (h[0]+hue+360f)%360f; h[1] = (h[1]*sat).coerceIn(0f,1f); h[2] = (h[2]+light).coerceIn(0f,1f); return Color(ColorUtils.HSLToColor(h)) }

private fun palette(kind: String, phase: String, seed: String, dark: Boolean, base: Color, secondary: Color, tertiary: Color, surface: Color, surfaceLow: Color, surfaceHigh: Color, surfaceHighest: Color): CardPalette {
    val fallbacks = (if (dark) DARK_GRADIENTS else LIGHT_GRADIENTS)[kind] ?: LIGHT_GRADIENTS.getValue("for-you")
    val dynamicBase = base
    val dynamicSecondary = secondary
    val dynamicTertiary = tertiary
    val shifts = floatArrayOf(-42f,-18f,16f,38f,64f,86f,112f,138f,164f,196f,224f,252f)
    val hue = shifts[Math.floorMod(hashString("$seed:$kind:$phase"), 12)]
    val accent = shifted(dynamicBase, hue, if (dark) .04f else .06f, 1.02f)
    val accent2 = shifted(dynamicSecondary, -hue*.55f, if (dark) .02f else .04f, 1.04f)
    val accent3 = shifted(dynamicTertiary, hue*.42f, if (dark) .01f else .03f, 1f)
    val fallback = fallbacks[Math.floorMod(hashString("$seed:$kind:fallback"), fallbacks.size)]
    fun fb(index: Int) = hex(fallback[index])
    val baseSurface = if (dark) surfaceLow else surface
    val a = mix(baseSurface, accent, if (dark) .28f else .44f)
    val b = mix(if (dark) surfaceHigh else surfaceHigh, accent2, if (dark) .30f else .46f)
    val c = mix(if (dark) surfaceHighest else surfaceHighest, accent3, if (dark) .26f else .44f)
    val alt = listOf(mix(a, accent3, if (dark) .30f else .38f), mix(c, accent, if (dark) .34f else .44f), mix(b, accent2, if (dark) .28f else .40f))
    return CardPalette(listOf(a,b,c), alt, accent, accent2, mix(accent, Color.White, .48f), if (dark) Color.White else mix(hex("#263238"), accent, .14f))
}

private fun phase(hour: Int) = when (hour) { in 5..11 -> "morning"; in 12..16 -> "afternoon"; in 17..21 -> "evening"; else -> "night" }
private fun digitalTime(calendar: Calendar): String { val h = calendar.get(Calendar.HOUR); val m = calendar.get(Calendar.MINUTE).toString().padStart(2,'0'); return "${if (h == 0) 12 else h}:$m" }

private val SOURCE_ACCENTS = mapOf(
    "headphone_amico" to listOf("#BA68C8"),
    "recording_pana" to listOf("#FF725E", "#EB996E", "#FFBE9D"),
    "music_cuate" to listOf("#F7A9A0", "#FFC727", "#FF9BBC"),
    "dj_boy_cuate" to listOf("#FFC727", "#F291B8"),
    "media_player_bro" to listOf("#92E3A9"),
    "playing_music_bro" to listOf("#92E3A9"),
    "playlist_pana" to listOf("#FF725E", "#EB996E", "#FFBE9D"),
)

@Composable
private fun rememberRecoloredArtwork(imageRes: Int, artworkKey: String, palette: CardPalette): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    return produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, imageRes, artworkKey, palette.accent, palette.accent2, palette.accentLight) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val source = BitmapFactory.decodeResource(context.resources, imageRes) ?: return@runCatching null
                val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
                val sources = SOURCE_ACCENTS[artworkKey].orEmpty().map { android.graphics.Color.parseColor(it) }
                val targets = intArrayOf(palette.accent.toArgb(), palette.accent2.toArgb(), palette.accentLight.toArgb())
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                for (i in pixels.indices) {
                    val pixel = pixels[i]
                    val alpha = android.graphics.Color.alpha(pixel)
                    if (alpha == 0) continue
                    var best = -1
                    var bestDistance = Int.MAX_VALUE
                    val pr = android.graphics.Color.red(pixel); val pg = android.graphics.Color.green(pixel); val pb = android.graphics.Color.blue(pixel)
                    sources.forEachIndexed { index, sourceColor ->
                        val sr = android.graphics.Color.red(sourceColor); val sg = android.graphics.Color.green(sourceColor); val sb = android.graphics.Color.blue(sourceColor)
                        val d = (pr-sr)*(pr-sr) + (pg-sg)*(pg-sg) + (pb-sb)*(pb-sb)
                        if (d < bestDistance) { bestDistance = d; best = index }
                    }
                    if (best >= 0 && bestDistance <= 22*22) {
                        val target = targets[best % targets.size]
                        pixels[i] = android.graphics.Color.argb(alpha, android.graphics.Color.red(target), android.graphics.Color.green(target), android.graphics.Color.blue(target))
                    }
                }
                bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                bitmap.asImageBitmap()
            }.getOrNull()
        }
    }.value
}
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
fun AirPlayrFeatureCard(eyebrow: String, title: String, imageRes: Int, gradient: List<Color>, onClick: () -> Unit, kind: String = "rediscover") {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val colors = LocalAirPlayrColors.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val lifecycleOwner = LocalLifecycleOwner.current
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    val sessionSeed = remember { "${System.currentTimeMillis()}-${kotlin.random.Random.nextInt()}" }
    var wallpaper by remember { mutableStateOf(wallpaperSeed(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { wallpaper = wallpaperSeed(context); now = Calendar.getInstance() } }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000L); now = Calendar.getInstance() } }
    val currentPhase = phase(now.get(Calendar.HOUR_OF_DAY))
    val seed = "$wallpaper:$sessionSeed"
    val p = remember(kind, currentPhase, seed, dark, colors.primary, colors.secondary, colors.tertiary, colors.surface, colors.surfaceContainerLow, colors.surfaceContainerHigh, colors.surfaceContainerHighest) { palette(kind, currentPhase, seed, dark, colors.primary, colors.secondary, colors.tertiary, colors.surface, colors.surfaceContainerLow, colors.surfaceContainerHigh, colors.surfaceContainerHighest) }
    val width = (configuration.screenWidthDp * .49f).coerceIn(178f, 196f).dp
    val height = (width.value * 1.39f).dp
    val artworkKey = when (imageRes) { com.airplayr.app.R.drawable.headphone_amico -> "headphone_amico"; com.airplayr.app.R.drawable.recording_pana -> "recording_pana"; com.airplayr.app.R.drawable.music_cuate -> "music_cuate"; com.airplayr.app.R.drawable.dj_boy_cuate -> "dj_boy_cuate"; com.airplayr.app.R.drawable.media_player_bro -> "media_player_bro"; com.airplayr.app.R.drawable.playing_music_bro -> "playing_music_bro"; com.airplayr.app.R.drawable.playlist_pana -> "playlist_pana"; else -> "" }
    val artwork = rememberRecoloredArtwork(imageRes, artworkKey, p)
    val titleFont = when (kind) { "rock" -> AirPlayrFonts.DancingScript; "playlists" -> AirPlayrFonts.ShadowsIntoLight; "rediscover" -> AirPlayrFonts.Oswald; else -> AirPlayrFonts.SmoochSans }
    Box(Modifier.width(width).height(height).clip(RoundedCornerShape(27.dp)).airPlayrShadow(RoundedCornerShape(27.dp), AirPlayrFeatureCardTokens.shadowElevationDp.dp).clickable(onClick = onClick)) {
        // One smooth Material-compatible gradient with no stacked overlays or transition seams.
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(p.gradient, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(1000f, 1000f))))
        if (artwork != null) Image(artwork, title, Modifier.fillMaxWidth().height(height*.64f).padding(10.dp).align(Alignment.TopCenter)) else Image(painterResource(imageRes), title, Modifier.fillMaxWidth().height(height*.64f).padding(10.dp).align(Alignment.TopCenter))
        if (kind in setOf("morning","afternoon","night")) Text(digitalTime(now), color=p.ink.copy(alpha=.78f), fontSize=12.sp, lineHeight=16.sp, fontWeight=FontWeight.Medium, modifier=Modifier.align(Alignment.TopEnd).padding(top=16.dp,end=17.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(start=17.dp,end=16.dp,bottom=17.dp)) {
            Text(title,color=p.ink,fontFamily=titleFont,fontSize=21.sp,lineHeight=24.sp,fontWeight=if(kind=="rock"||kind=="playlists") FontWeight.Normal else FontWeight.Normal,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(eyebrow,color=p.ink.copy(alpha=.82f),fontFamily=AirPlayrFonts.OpenSans,fontSize=11.sp,lineHeight=14.sp,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=5.dp))
        }
    }
}
