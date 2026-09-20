package com.airplayr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.ui.components.AirPlayrMediaRow
import com.airplayr.app.ui.components.AirPlayrScreenHeader
import com.airplayr.app.ui.components.AirPlayrSectionEmpty
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.theme.LocalAirPlayrColors

@Composable
fun LibraryScreen(
    title: String,
    type: MediaType,
    viewModel: LibraryViewModel,
    playback: PlaybackController,
    onOpen: (MediaItem) -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    val items = if (type == MediaType.AUDIO) snapshot.audio else snapshot.video
    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().background(colors.background)) {
        AirPlayrScreenHeader(title, "${items.size} cached items")
        if (!snapshot.ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.primary) }
        } else if (items.isEmpty()) {
            AirPlayrSectionEmpty("No media cached yet", "Grant media access and refresh from Home.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    AirPlayrMediaRow(
                        item = item,
                        onOpen = { onOpen(item) },
                        playing = playbackState.isPlaying && playbackState.currentId == item.id,
                        compact = false,
                    )
                }
            }
        }
    }
}
