package com.airplayr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors

@Composable
fun AirPlayrScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = LocalAirPlayrColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = c.primary, modifier = Modifier.size(28.dp))
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = if (onBack == null) 0.dp else 6.dp)) {
            Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 29.sp, lineHeight = 35.sp, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, lineHeight = 18.sp, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

@Composable
fun AirPlayrMediaRow(
    item: MediaItem,
    onOpen: () -> Unit,
    onMore: (() -> Unit)? = null,
    playing: Boolean = false,
    compact: Boolean = false,
) {
    val c = LocalAirPlayrColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 24.dp, vertical = if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(item.artworkUri ?: item.thumbnailUri, Modifier.size(if (compact) 58.dp else 64.dp), if (item.mediaType == MediaType.VIDEO) 13.dp else 16.dp, item.title)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(item.title.ifBlank { item.filename }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = if (compact) 16.sp else 18.sp, lineHeight = if (compact) 21.sp else 23.sp, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(item.artist, item.album).joinToString(" · ").ifBlank { if (item.mediaType == MediaType.VIDEO) "Video" else "Unknown artist" },
                fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, lineHeight = 18.sp, color = c.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (playing) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(24.dp)) {
                listOf(14.dp, 24.dp, 18.dp).forEach { h -> Box(Modifier.width(4.dp).height(h).clip(RoundedCornerShape(3.dp)).background(c.primary)) }
            }
        } else if (onMore != null) {
            IconButton(onClick = onMore, modifier = Modifier.size(42.dp)) { Icon(AirPlayrIcons.More, "More", tint = c.primary, modifier = Modifier.size(22.dp)) }
        } else {
            Icon(Icons.Default.PlayArrow, "Play", tint = c.primary, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun AirPlayrSectionEmpty(title: String, message: String) {
    val c = LocalAirPlayrColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.MusicNote, null, Modifier.size(42.dp), tint = c.primary)
        Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 26.sp, color = c.textPrimary, modifier = Modifier.padding(top = 16.dp))
        Text(message, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, lineHeight = 20.sp, color = c.textPrimary, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
fun AirPlayrSectionHeader(title: String, onMore: (() -> Unit)? = null) {
    val c = LocalAirPlayrColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, lineHeight = 34.sp, color = c.textPrimary, modifier = Modifier.weight(1f))
        if (onMore != null) IconButton(onClick = onMore, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowForward, "More", tint = c.primary, modifier = Modifier.size(24.dp)) }
    }
}
