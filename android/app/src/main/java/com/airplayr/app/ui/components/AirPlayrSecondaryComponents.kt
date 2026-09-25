package com.airplayr.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
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

/** AirPlayr wordmark: AIR stays on the app accent; PLAYR carries the animated Material You mix. */
@Composable
fun AirPlayrWordmark(modifier: Modifier = Modifier) {
    val c = LocalAirPlayrColors.current
    val transition = rememberInfiniteTransition(label = "airplayr-wordmark-gradient")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "airplayr-wordmark-phase",
    )
    val playrBrush = Brush.linearGradient(
        colors = listOf(c.primary, c.secondary, c.tertiary, c.primary),
        start = androidx.compose.ui.geometry.Offset(
            -180f + (phase * 180f),
            0f,
        ),
        end = androidx.compose.ui.geometry.Offset(
            220f + (phase * 180f),
            0f,
        ),
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "AIR",
            fontFamily = AirPlayrFonts.SmoochSans,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = c.brandAccent,
        )
        Spacer(Modifier.width(7.dp))
        Text(
            "PLAYR",
            fontFamily = AirPlayrFonts.SmoochSans,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            style = MaterialTheme.typography.titleLarge.copy(brush = playrBrush, fontFamily = AirPlayrFonts.SmoochSans, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
        )
        Spacer(Modifier.width(5.dp))
        Icon(
            AirPlayrIcons.Play,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(18.dp),
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
    fallbackIconTint: Color? = null,
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
            fallbackIconTint = fallbackIconTint ?: c.onSurfaceVariant.copy(alpha = com.airplayr.app.ui.theme.AirPlayrArtworkTokens.fallbackIconAlpha),
            isPlaying = playing,
        )

        if (onMore != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(36.dp)
                    .graphicsLayer {
                        // Keep the three-dot hit target completely transparent.
                        shape = CircleShape
                        clip = false
                    }
                    .clickable(onClick = onMore),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    "More options",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
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
fun AirPlayrMoreButton(
    onClick: () -> Unit,
    contentDescription: String = "More options",
    modifier: Modifier = Modifier,
) {
    val c = LocalAirPlayrColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.size(42.dp),
        shape = CircleShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MoreVert, contentDescription, tint = c.textPrimary, modifier = Modifier.size(22.dp))
        }
    }
}

/** Shared Home/History/Favorites-style horizontal video rail with 16:9 thumbnails and play overlay. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AirPlayrVideoRail(
    items: List<MediaItem>,
    onOpen: (MediaItem) -> Unit,
    onMore: ((MediaItem) -> Unit)? = null,
    selectedIds: Set<String> = emptySet(),
    onLongPress: ((MediaItem) -> Unit)? = null,
) {
    val c = LocalAirPlayrColors.current
    val railState = androidx.compose.runtime.saveable.rememberSaveable(saver = androidx.compose.foundation.ScrollState.Saver) { androidx.compose.foundation.ScrollState(0) }
    Row(
        Modifier.horizontalScroll(railState).padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEach { item ->
            val selected = item.id in selectedIds
            Column(
                Modifier
                    .width(196.dp)
                    .combinedClickable(
                        onClick = { onOpen(item) },
                        onLongClick = onLongPress?.let { { it(item) } },
                    ),
            ) {
                Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(18.dp))) {
                    Artwork(
                        item.thumbnailUri ?: item.artworkUri ?: item.uri,
                        Modifier.fillMaxSize(),
                        18.dp,
                        item.title,
                        MediaType.VIDEO,
                        AirPlayrIcons.Video,
                    )
                    if (selected) {
                        Box(Modifier.fillMaxSize().background(c.primary.copy(alpha = .24f)))
                    }
                    Box(
                        Modifier.align(Alignment.BottomStart).padding(8.dp).size(36.dp).clip(CircleShape).background(c.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(AirPlayrIcons.Play, "Play video", tint = c.onPrimary, modifier = Modifier.size(18.dp))
                    }
                    onMore?.let {
                        AirPlayrMoreButton(
                            onClick = { it(item) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                        )
                    }
                }
                Text(
                    item.title.ifBlank { item.filename },
                    fontFamily = AirPlayrFonts.SmoochSans,
                    fontSize = 15.sp,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
        }
    }
}

/**
 * Clean media artwork + metadata row. Play and overflow controls intentionally live outside the
 * artwork so music and video rows share one premium interaction language across the app.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AirPlayrMediaRow(
    item: MediaItem,
    onOpen: () -> Unit,
    onMore: (() -> Unit)? = null,
    playing: Boolean = false,
    compact: Boolean = false,
    showPlayOverlay: Boolean = false,
    selected: Boolean = false,
    onLongPress: (() -> Unit)? = null,
) {
    val c = LocalAirPlayrColors.current
    val shape = RoundedCornerShape(if (compact) 18.dp else 22.dp)
    val rowModifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = onOpen,
            onLongClick = onLongPress,
        )
        .background(if (selected) c.primaryContainer.copy(alpha = .72f) else Color.Transparent, shape)
        .padding(horizontal = 18.dp, vertical = if (compact) 6.dp else 8.dp)
    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
        val artworkModifier = if (item.mediaType == MediaType.VIDEO) {
            Modifier
                .width(if (compact) AirPlayrArtworkTokens.videoCompactWidth else AirPlayrArtworkTokens.videoRowWidth)
                .height(if (compact) AirPlayrArtworkTokens.videoCompactHeight else AirPlayrArtworkTokens.videoRowHeight)
                .clip(RoundedCornerShape(if (compact) 14.dp else 17.dp))
        } else {
            Modifier
                .size(if (compact) AirPlayrArtworkTokens.compactRow else AirPlayrArtworkTokens.row)
                .clip(RoundedCornerShape(if (compact) 14.dp else 17.dp))
        }
        Box(artworkModifier) {
            Artwork(
                uri = item.displayArtworkUri(),
                modifier = Modifier.fillMaxSize(),
                radius = if (compact) 14.dp else 17.dp,
                contentDescription = item.title,
                mediaType = item.mediaType,
                fallbackIcon = if (item.mediaType == MediaType.VIDEO) AirPlayrIcons.Video else AirPlayrIcons.Music,
                isPlaying = playing,
            )
            if (selected) {
                Surface(
                    Modifier.align(Alignment.BottomEnd).padding(5.dp).size(28.dp),
                    shape = CircleShape,
                    color = c.primary,
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, "Selected", tint = c.onPrimary, modifier = Modifier.size(18.dp)) } }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
            Text(
                item.title.ifBlank { item.filename },
                fontFamily = AirPlayrFonts.SmoochSans,
                fontSize = if (compact) 16.sp else 18.sp,
                lineHeight = if (compact) 21.sp else 23.sp,
                color = c.textPrimary,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(item.artist, item.album).joinToString(" · ").ifBlank { if (item.mediaType == MediaType.VIDEO) "Video" else "Unknown artist" },
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (onMore != null) {
            AirPlayrMoreButton(onMore, modifier = Modifier.padding(start = 2.dp))
        }
    }
}

/** Shared premium item menu. Callers supply actions, while the surface, header and action rows stay identical. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirPlayrPremiumMediaActionSheet(
    item: MediaItem,
    playing: Boolean = false,
    favorite: Boolean = false,
    groups: List<List<AirPlayrMediaMenuAction>>,
    onFavorite: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val c = LocalAirPlayrColors.current
    val isVideo = item.mediaType == MediaType.VIDEO
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = c.surfaceContainer),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(
                        item.displayArtworkUri(),
                        if (isVideo) Modifier.width(112.dp).height(63.dp) else Modifier.size(68.dp),
                        18.dp,
                        item.title,
                        item.mediaType,
                        if (isVideo) AirPlayrIcons.Video else AirPlayrIcons.Music,
                        isPlaying = playing,
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                        Text(item.title.ifBlank { if (isVideo) "Video" else "Song" }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 20.sp, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (isVideo) listOfNotNull(item.artist, if (item.durationMs > 0) formatDurationLabel(item.durationMs) else null).joinToString(" · ").ifBlank { "Video" }
                            else listOfNotNull(item.artist, item.album).joinToString(" · ").ifBlank { "Unknown artist" },
                            fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                        Surface(shape = RoundedCornerShape(50), color = c.primaryContainer.copy(alpha = .72f), modifier = Modifier.padding(top = 7.dp)) {
                            Text(if (isVideo) "VIDEO" else "MUSIC", fontFamily = AirPlayrFonts.OpenSans, fontSize = 9.sp, letterSpacing = 1.1.sp, color = c.primary, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                        }
                    }
                    if (onFavorite != null) {
                        IconButton(onClick = onFavorite) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (favorite) "Remove from favorites" else "Add to favorites", tint = c.primary) }
                    }
                }
            }
            groups.filter { it.isNotEmpty() }.forEach { group ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Column {
                        group.forEachIndexed { index, action ->
                            val content = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 18.dp)
                            Row(
                                modifier = content.then(if (action.enabled) Modifier.clickable(onClick = action.onClick) else Modifier),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(Modifier.size(38.dp), shape = RoundedCornerShape(13.dp), color = if (action.destructive) c.errorContainer else c.primaryContainer.copy(alpha = .62f)) {
                                    Box(contentAlignment = Alignment.Center) { Icon(action.icon, null, tint = if (action.destructive) c.error else c.primary, modifier = Modifier.size(20.dp)) }
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Text(action.label, fontFamily = AirPlayrFonts.OpenSans, fontSize = 14.sp, color = if (action.destructive) c.error else c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    action.subtitle?.let { Text(it, fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, color = c.textSecondary, modifier = Modifier.padding(top = 2.dp)) }
                                }
                                if (action.enabled) Icon(Icons.Default.ArrowForwardIos, null, tint = c.textSecondary, modifier = Modifier.size(14.dp))
                            }
                            if (index != group.lastIndex) Spacer(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(1.dp).background(c.outlineVariant.copy(alpha = .38f)))
                        }
                    }
                }
            }
        }
    }
}

data class AirPlayrMediaMenuAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val subtitle: String? = null,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
)

private fun formatDurationLabel(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    return if (total >= 3600L) "%d:%02d:%02d".format(total / 3600L, (total % 3600L) / 60L, total % 60L) else "%d:%02d".format(total / 60L, total % 60L)
}

@Composable
fun AirPlayrSelectionBar(
    modifier: Modifier = Modifier,
    count: Int,
    onClear: () -> Unit,
    onSelectAll: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onPlaylist: (() -> Unit)? = null,
    onLike: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
) {
    val c = LocalAirPlayrColors.current
    Card(
        modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClear) { Icon(Icons.Default.Close, "Clear selection", tint = c.primary) }
            Text("$count selected", fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = c.textPrimary, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
            onPlay?.let { IconButton(onClick = it) { Icon(AirPlayrIcons.Play, "Play selected", tint = c.primary) } }
            onPlayNext?.let { IconButton(onClick = it) { Icon(AirPlayrIcons.Next, "Play selected next", tint = c.primary) } }
            onPlaylist?.let { IconButton(onClick = it) { Icon(Icons.Default.PlaylistAdd, "Add selected to playlist", tint = c.primary) } }
            onLike?.let { IconButton(onClick = it) { Icon(Icons.Default.FavoriteBorder, "Favorite selected", tint = c.primary) } }
            onMore?.let { IconButton(onClick = it) { Icon(Icons.Default.MoreVert, "More selection actions", tint = c.primary) } }
            onSelectAll?.let { TextButton(onClick = it) { Text("All", color = c.primary) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirPlayrBulkSelectionSheet(
    count: Int,
    canFavorite: Boolean,
    onPlay: () -> Unit,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onFavorite: (() -> Unit)? = null,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalAirPlayrColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text("Selected media", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 26.sp, color = c.textPrimary)
                Text("$count ${if (count == 1) "item" else "items"} selected", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = c.textSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = c.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp)) {
                Column {
                    ActionButton("Play now", Icons.Default.PlayArrow, onPlay)
                    onPlayNext?.let { ActionButton("Play next", Icons.Default.SkipNext, it) }
                    ActionButton("Add to queue", Icons.Default.QueueMusic, onAddToQueue)
                    ActionButton("Add to playlist", Icons.Default.PlaylistAdd, onAddToPlaylist)
                    if (canFavorite && onFavorite != null) ActionButton("Add to favorites", Icons.Default.FavoriteBorder, onFavorite)
                    ActionButton("Share", Icons.Default.Share, onShare)
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = c.errorContainer.copy(alpha = .72f)), elevation = CardDefaults.cardElevation(0.dp)) {
                ActionButton("Delete from device", Icons.Default.Delete, onDelete, destructive = true)
            }
        }
    }
}

@Composable
fun ActionGroup(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val c = LocalAirPlayrColors.current

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = c.surfaceContainerHigh,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (destructive) c.error else c.primary,
                modifier = Modifier.size(22.dp),
            )

            Text(
                label,
                color = if (destructive) c.error else c.textPrimary,
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
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
