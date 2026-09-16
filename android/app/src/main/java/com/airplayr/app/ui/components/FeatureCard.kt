package com.airplayr.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun mix(a: Color, b: Color, amount: Float): Color = Color(
    a.red + (b.red - a.red) * amount.coerceIn(0f, 1f),
    a.green + (b.green - a.green) * amount.coerceIn(0f, 1f),
    a.blue + (b.blue - a.blue) * amount.coerceIn(0f, 1f),
    1f,
)

private fun palette(kind: String, dark: Boolean, base: Color, secondary: Color, tertiary: Color, surface: Color): List<Color> {
    if (dark) {
        return when (kind) {
            "rock" -> listOf(mix(base, Color(0xFF2A2020), .55f), mix(secondary, Color(0xFF25212B), .52f), mix(tertiary, Color(0xFF1E2628), .50f))
            "for-you" -> listOf(mix(tertiary, Color(0xFF24202A), .54f), mix(base, Color(0xFF202732), .50f), mix(secondary, Color(0xFF2A2420), .48f))
            else -> listOf(mix(base, Color(0xFF211E24), .58f), mix(secondary, Color(0xFF202329), .56f), mix(tertiary, Color(0xFF20262A), .54f))
        }
    }
    // Keep the cards unmistakably in the active light Material You family. The
    // dynamic scheme provides the hue; blending toward the light surface removes
    // the overly dark tonal jump that was visible in the current home screenshot.
    return when (kind) {
        "rock" -> listOf(mix(surface, base, .34f), mix(surface, secondary, .24f), mix(surface, tertiary, .18f))
        "for-you" -> listOf(mix(surface, tertiary, .30f), mix(surface, base, .22f), mix(surface, secondary, .18f))
        "morning", "afternoon", "night" -> listOf(mix(surface, secondary, .26f), mix(surface, tertiary, .20f), mix(surface, base, .18f))
        "rediscover" -> listOf(mix(surface, tertiary, .26f), mix(surface, base, .20f), mix(surface, secondary, .18f))
        else -> listOf(mix(surface, secondary, .25f), mix(surface, base, .20f), mix(surface, tertiary, .18f))
    }
}

@Composable
fun AirPlayrFeatureCard(
    eyebrow: String,
    title: String,
    imageRes: Int,
    gradient: List<Color>,
    onClick: () -> Unit,
    kind: String = "rediscover",
) {
    val colors = LocalAirPlayrColors.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val actualGradient = palette(kind, dark, colors.primary, colors.secondary, colors.tertiary, colors.surfaceSolid)
    val transition = rememberInfiniteTransition(label = "feature-card-drift")
    val driftX by transition.animateFloat(
        initialValue = -20f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(6200), RepeatMode.Reverse),
        label = "x",
    )
    val driftY by transition.animateFloat(
        initialValue = -16f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(8200), RepeatMode.Reverse),
        label = "y",
    )
    val time = remember { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) }
    val titleFont = when (kind) {
        "rock" -> AirPlayrFonts.DancingScript
        "playlists" -> AirPlayrFonts.ShadowsIntoLight
        "rediscover" -> AirPlayrFonts.Oswald
        else -> AirPlayrFonts.SmoochSans
    }
    val ink = if (dark) Color.White else mix(Color(0xFF263238), actualGradient[0], .14f)

    Box(
        Modifier
            .width(196.dp)
            .height(272.dp)
            .clip(RoundedCornerShape(28.dp))
            .airPlayrShadow(RoundedCornerShape(28.dp), 10.dp)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(actualGradient)))
        Box(
            Modifier
                .size(190.dp)
                .offset(x = (-56).dp + driftX.dp, y = (18).dp + driftY.dp)
                .alpha(.20f)
                .background(actualGradient[1], CircleShape),
        )
        Box(
            Modifier
                .size(170.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-48).dp, y = (-62).dp)
                .alpha(.18f)
                .background(actualGradient[2], CircleShape),
        )
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .10f)))
        Image(
            painter = painterResource(imageRes),
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .height(174.dp)
                .padding(start = 10.dp, top = 10.dp, end = 10.dp)
                .align(Alignment.TopCenter),
        )
        if (kind in setOf("morning", "afternoon", "night")) {
            Text(time, color = ink.copy(alpha = .78f), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 17.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 17.dp, end = 16.dp, bottom = 17.dp)) {
            Text(
                title,
                color = ink,
                fontFamily = titleFont,
                fontSize = 21.sp,
                lineHeight = 24.sp,
                fontWeight = if (kind == "rock" || kind == "playlists") FontWeight.Normal else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                eyebrow,
                color = ink.copy(alpha = .82f),
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}
