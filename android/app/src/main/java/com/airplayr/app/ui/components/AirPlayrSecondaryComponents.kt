package com.airplayr.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.ui.theme.AirPlayrArtworkTokens
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors

/** Shared screen header. The content owns the status-bar inset while the application shell stays edge-to-edge. */
@Composable
fun AirPlayrScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = LocalAirPlayrColors.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = c.primary, modifier = Modifier.size(28.dp))
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = if (onBack == null) 0.dp else 6.dp)) {
            Text(
                title,
                fontFamily = AirPlayrFonts.SmoochSans,
                fontSize = 29.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) Text(
                subtitle,
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}

/** AirPlayr wordmark with Material You colors and a single accent variation for "Air". */
@Composable
fun AirPlayrWordmark(modifier: Modifier = Modifier) {
    val c = LocalAirPlayrColors.current
    val airColor = androidx.compose.runtime.remember(c.primary, c.secondary, c.tertiary) {
        listOf(c.primary, c.secondary, c.tertiary).random()
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Air",
            fontFamily = AirPlayrFonts.SmoochSans,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = airColor,
        )
        Text(
            "Playr",
            fontFamily = AirPlayrFonts.SmoochSans,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = c.onSurface,
        )
    }
}

/**
 * The standard AirPlayr artwork treatment. It keeps menu/play controls and the animated playing
 * indicator in one place so every screen uses the same interaction language.
 */
@Composable
fun AirPlayrMediaArtwork(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
    title: String? = null,
    mediaType: MediaType = MediaType.AUDIO,
    fallbackIcon: ImageVector = when (mediaType) {
        MediaType.AUDIO -> AirPlayrIcons.Music
        MediaType.VIDEO -> AirPlayrIcons.Video
    },
    playing: Boolean = false,
    fallbackIconSize: Dp = 22.dp,
    onPlay: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
) {
    val c = LocalAirPlayrColors.current
    Box(modifier) {
        Artwork(
            uri = artworkUri,
            modifier = Modifier.fillMaxSize(),
            radius = radius,
            contentDescription = title,
            mediaType = mediaType,
            fallbackIcon = fallbackIcon,
            fallbackIconSize = fallbackIconSize,
            isPlaying = playing,
        )

        if (onMore != null) {
            Surface(
                onClick = onMore,
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(36.dp),
                shape = CircleShape,
                color = c.scrim.copy(alpha = 0.46f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MoreVert, "More options", tint = Color.White, modifier = Modifier.size(21.dp))
                }
            }
        }

        if (onPlay != null) {
            Surface(
                onClick = onPlay,
                modifier = Modifier.align(Alignment.BottomStart).padding(9.dp).size(40.dp),
                shape = CircleShape,
                color = c.surfaceContainerHighest.copy(alpha = 0.96f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AirPlayrIcons.Play, "Play", tint = c.primary, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

/** Reference-style album card: square art, upper-right overflow, lower-left play and metadata. */
@Composable
fun AirPlayrAlbumRailCard(
    artworkUri: String?,
    albumTitle: String,
    artist: String?,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onMore: (() -> Unit)? = null,
    playing: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = AirPlayrArtworkTokens.albumRail,
) {
    Column(modifier.width(size).clickable(onClick = onOpen)) {
        AirPlayrMediaArtwork(
            artworkUri = artworkUri,
            modifier = Modifier.size(size).clip(RoundedCornerShape(AirPlayrArtworkTokens.albumCorner)),
            radius = AirPlayrArtworkTokens.albumCorner,
            title = albumTitle,
            mediaType = MediaType.AUDIO,
            fallbackIcon = AirPlayrIcons.AlbumDisc,
            playing = playing,
            fallbackIconSize = AirPlayrArtworkTokens.albumFallbackIcon,
            onPlay = onPlay,
            onMore = onMore,
        )
        Column(
            Modifier.fillMaxWidth().padding(top = 7.dp),
        ) {
            Text(
                albumTitle,
                fontFamily = AirPlayrFonts.SmoochSans,
                fontSize = 17.sp,
                color = LocalAirPlayrColors.current.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                artist?.takeUnless { it.isBlank() } ?: "Unknown artist",
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 12.sp,
                color = LocalAirPlayrColors.current.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
        AirPlayrMediaArtwork(
            artworkUri = item.displayArtworkUri(),
            modifier = Modifier.size(if (compact) AirPlayrArtworkTokens.compactRow else AirPlayrArtworkTokens.row)
                .clip(RoundedCornerShape(if (compact) 14.dp else 16.dp)),
            radius = if (compact) 14.dp else 16.dp,
            title = item.title,
            mediaType = item.mediaType,
            fallbackIcon = if (item.mediaType == MediaType.VIDEO) AirPlayrIcons.Video else AirPlayrIcons.Music,
            playing = playing,
            onPlay = onOpen,
            onMore = onMore,
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                item.title.ifBlank { item.filename },
                fontFamily = AirPlayrFonts.SmoochSans,
                fontSize = if (compact) 16.sp else 18.sp,
                lineHeight = if (compact) 21.sp else 23.sp,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(item.artist, item.album).joinToString(" · ").ifBlank { if (item.mediaType == MediaType.VIDEO) "Video" else "Unknown artist" },
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
fun AirPlayrSectionEmpty(title: String, message: String) {
    val c = LocalAirPlayrColors.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(AirPlayrIcons.Music, null, Modifier.size(42.dp), tint = c.primary)
        Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 26.sp, color = c.textPrimary, modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.SemiBold)
        Text(message, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, lineHeight = 20.sp, color = c.textSecondary, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
fun AirPlayrSectionHeader(title: String, onMore: (() -> Unit)? = null) {
    val c = LocalAirPlayrColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, lineHeight = 34.sp, color = c.textPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (onMore != null) IconButton(onClick = onMore, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.ArrowForward, "More", tint = c.primary, modifier = Modifier.size(24.dp)) }
    }
}
