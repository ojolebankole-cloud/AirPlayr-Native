package com.airplayr.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.core.qa.AirPlayrQaTags
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackUiState
import com.airplayr.app.ui.theme.AirPlayrIcons

@Composable
fun AirPlayrBottomDock(
    currentRoute: String?,
    playbackState: PlaybackUiState,
    playback: PlaybackController,
    onNavigate: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    showMiniPlayer: Boolean = true,
) {
    Box(
        Modifier
            .fillMaxSize()
            .testTag(AirPlayrQaTags.DOCK),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                // The dock is an opaque Material You surface. This prevents the page
                // background from showing through underneath the mini-player/navigation
                // surface while keeping the dock itself floating above the page.
                tint = MaterialTheme.colorScheme.surface,
                border = androidx.compose.ui.graphics.Color.Transparent,
                elevation = 14.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = showMiniPlayer && playbackState.currentId != null && playbackState.currentMediaType == MediaType.AUDIO,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        MiniPlayerSection(playbackState, playback, onOpenPlayer)
                    }
                    if (showMiniPlayer && playbackState.currentId != null && playbackState.currentMediaType == MediaType.AUDIO) {
                        androidx.compose.material3.HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        DockItem(AirPlayrIcons.Home, currentRoute == "home", "Home") { onNavigate("home") }
                        DockItem(AirPlayrIcons.Music, currentRoute == "music", "Music") { onNavigate("music") }
                        Spacer(Modifier.size(1.dp))
                        DockItem(AirPlayrIcons.Video, currentRoute == "videos", "Videos") { onNavigate("videos") }
                        DockItem(AirPlayrIcons.Settings, currentRoute == "settings", "Settings") { onNavigate("settings") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerSection(
    state: PlaybackUiState,
    playback: PlaybackController,
    onOpenPlayer: () -> Unit,
) {
    val fraction = if (state.durationMs > 0) {
        (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(
        Modifier
            .testTag(AirPlayrQaTags.MINI_PLAYER)
            .fillMaxWidth()
            .clickable(onClick = onOpenPlayer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Artwork(state.artworkUri, Modifier.size(62.dp), 19.dp, state.currentTitle)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    state.currentTitle ?: "Song",
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    listOfNotNull(
                        state.currentArtist?.takeIf { it.isNotBlank() },
                        state.currentAlbum?.takeIf { it.isNotBlank() },
                    ).joinToString("  •  "),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = playback::skipPrevious, modifier = Modifier.size(48.dp)) {
                Icon(AirPlayrIcons.Previous, "Previous", Modifier.size(25.dp), tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = playback::togglePlayPause, modifier = Modifier.size(60.dp)) {
                Box(
                    Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.isPlaying) AirPlayrIcons.Pause else AirPlayrIcons.Play,
                        if (state.isPlaying) "Pause" else "Play",
                        Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            IconButton(onClick = playback::skipNext, modifier = Modifier.size(48.dp)) {
                Icon(AirPlayrIcons.Next, "Next", Modifier.size(25.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
        )
    }
}

@Composable
private fun DockItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(58.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
    ) {
        Icon(
            icon,
            label,
            Modifier.size(30.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

const val AIRPLAYR_BOTTOM_DOCK_BASE_HEIGHT = 184
