package com.airplayr.app.ui.screens

import com.airplayr.app.ui.airPlayrBottomContentPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.*
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    title: String,
    type: MediaType,
    viewModel: LibraryViewModel,
    playback: PlaybackController,
    onOpen: (MediaItem) -> Unit = {},
    container: AppContainer,
) {
    val colors = LocalAirPlayrColors.current
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    val items = if (type == MediaType.AUDIO) snapshot.audio else snapshot.video
    var selectedIds by rememberSaveable(type) { mutableStateOf<Set<String>>(emptySet()) }
    var menuItem by remember { mutableStateOf<MediaItem?>(null) }
    var menuFavorite by remember { mutableStateOf(false) }
    var detailsTarget by remember { mutableStateOf<MediaItem?>(null) }
    val scope = rememberCoroutineScope()
    val libraryListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) }

    LaunchedEffect(menuItem?.id) {
        val item = menuItem
        menuFavorite = item?.let { runCatching { container.structured.isFavorite(it.id) }.getOrDefault(false) } ?: false
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            AirPlayrScreenHeader(title, "${items.size} cached items")
            if (selectedIds.isNotEmpty()) {
                val chosen = items.filter { it.id in selectedIds }
                AirPlayrSelectionBar(
                    count = selectedIds.size,
                    onClear = { selectedIds = emptySet() },
                    onSelectAll = { selectedIds = items.map { it.id }.toSet() },
                    onPlay = if (type == MediaType.AUDIO) ({ if (chosen.isNotEmpty()) { onOpen(chosen.first()); selectedIds = emptySet() } }) else null,
                    onPlayNext = if (type == MediaType.AUDIO) ({ chosen.forEach(playback::playNext); selectedIds = emptySet() }) else null,
                    onLike = if (type == MediaType.AUDIO) ({ scope.launch { chosen.forEach { container.structured.toggleFavorite(it) }; selectedIds = emptySet() } }) else null,
                )
            }
            // The library snapshot is populated from Room/MediaStore flows. Do not block navigation
            // behind a spinner: the screen chrome enters immediately and the rows simply arrive.
            if (items.isEmpty() && snapshot.ready) {
                AirPlayrSectionEmpty("No media cached yet", "Grant media access and refresh from Home.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = libraryListState,
                    contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        val playing = playbackState.isPlaying && playbackState.currentId == item.id
                        AirPlayrMediaRow(
                            item = item,
                            onOpen = { if (selectedIds.isNotEmpty()) selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id else onOpen(item) },
                            onMore = { menuItem = item },
                            playing = playing,
                            compact = false,
                            selected = item.id in selectedIds,
                            onLongPress = { selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id },
                        )
                    }
                }
            }
        }
        AirPlayrLongListFastScrollHandle(libraryListState)
    }

    menuItem?.let { item ->
        val isVideo = item.mediaType == MediaType.VIDEO
        AirPlayrPremiumMediaActionSheet(
            item = item,
            playing = playbackState.isPlaying && playbackState.currentId == item.id,
            favorite = menuFavorite,
            onFavorite = { scope.launch { menuFavorite = container.structured.toggleFavorite(item) } },
            groups = buildList {
                add(listOf(
                    AirPlayrMediaMenuAction("Open", if (isVideo) AirPlayrIcons.Video else AirPlayrIcons.Music, { menuItem = null; onOpen(item) }),
                    AirPlayrMediaMenuAction("Details", Icons.Default.Info, { menuItem = null; detailsTarget = item }),
                ))
                if (isVideo) add(listOf(
                    AirPlayrMediaMenuAction("Play next", Icons.Default.SkipNext, { menuItem = null; com.airplayr.app.core.playback.VideoQueueStore.playNext(listOf(item)) }),
                    AirPlayrMediaMenuAction("Add to video queue", Icons.Default.QueueMusic, { menuItem = null; com.airplayr.app.core.playback.VideoQueueStore.add(listOf(item)) }),
                )) else add(listOf(
                    AirPlayrMediaMenuAction("Play next", Icons.Default.SkipNext, { menuItem = null; playback.playNext(item) }),
                    AirPlayrMediaMenuAction("Add to queue", Icons.Default.QueueMusic, { menuItem = null; playback.addToQueue(item) }),
                ))
                add(listOf(
                    AirPlayrMediaMenuAction("Share", Icons.Default.Share, {
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            this.type = item.mimeType ?: if (isVideo) "video/*" else "audio/*"
                            putExtra(android.content.Intent.EXTRA_TEXT, item.uri)
                            putExtra(android.content.Intent.EXTRA_TITLE, item.title)
                        }
                        runCatching { container.appContext.startActivity(android.content.Intent.createChooser(send, "Share ${item.title}").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        menuItem = null
                    }),
                ))
            },
            onDismiss = { menuItem = null },
        )
    }

    detailsTarget?.let { item ->
        MediaDetailsSheet(item = item, playing = playbackState.isPlaying && playbackState.currentId == item.id, onDismiss = { detailsTarget = null })
    }
}
