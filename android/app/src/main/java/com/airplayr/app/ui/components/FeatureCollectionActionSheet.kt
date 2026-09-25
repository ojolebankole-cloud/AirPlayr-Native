@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.airplayr.app.ui.components

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors

/**
 * One shared four-action sheet used by History, Favorites, Most Played and feature-collection
 * headers. This is intentionally separate from per-song/album/video item menus.
 */
@Composable
fun AirPlayrFeatureCollectionActionSheet(
    title: String,
    onDismiss: () -> Unit,
    onShuffle: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                fontFamily = AirPlayrFonts.SmoochSans,
                fontSize = 24.sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            ActionButton("Shuffle", Icons.Default.Shuffle, onClick = onShuffle)
            ActionButton("Play next", Icons.Default.SkipNext, onClick = onPlayNext)
            ActionButton("Add to queue", Icons.Default.QueueMusic, onClick = onAddToQueue)
            ActionButton("Add to playlist", Icons.Default.PlaylistAdd, onClick = onAddToPlaylist, destructive = false)
            // Disabled actions still render in the same position so the shared architecture stays
            // visually consistent; callers can present a snackbar instead when a section is empty.
        }
    }
}
