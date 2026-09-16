package com.airplayr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.ui.components.AirPlayrMediaRow
import com.airplayr.app.ui.components.AirPlayrScreenHeader
import com.airplayr.app.ui.components.AirPlayrSectionEmpty
import com.airplayr.app.ui.navigation.LibraryViewModel

@Composable
fun LibraryScreen(title: String, type: MediaType, viewModel: LibraryViewModel, onOpen: (MediaItem) -> Unit = {}) {
    val items = when (type) {
        MediaType.AUDIO -> viewModel.audio.collectAsStateWithLifecycle().value
        MediaType.VIDEO -> viewModel.video.collectAsStateWithLifecycle().value
    }
    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().background(com.airplayr.app.ui.theme.LocalAirPlayrColors.current.background)) {
        AirPlayrScreenHeader(title, "${items.size} cached items")
        if (items.isEmpty()) {
            AirPlayrSectionEmpty("No media cached yet", "Grant media access and refresh from Home.")
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(items, key = { it.id }) { item ->
                    AirPlayrMediaRow(item, onOpen = { onOpen(item) }, compact = false)
                }
            }
        }
    }
}
