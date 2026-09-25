@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.airplayr.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.playback.VideoQueueStore
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.airPlayrBottomContentPadding
import com.airplayr.app.ui.airPlayrFloatingPlayBottomPadding
import com.airplayr.app.ui.components.AirPlayrCard
import com.airplayr.app.ui.components.AirPlayrMoreButton
import com.airplayr.app.ui.components.AirPlayrMediaRow
import com.airplayr.app.ui.components.AirPlayrPremiumMediaActionSheet
import com.airplayr.app.ui.components.Artwork
import com.airplayr.app.ui.components.AirPlayrMediaMenuAction
import com.airplayr.app.ui.components.HistoryFavoriteDetailsSheet
import com.airplayr.app.ui.feedback.AppFeedback
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.launch

private const val VIDEO_MODE_FOLDERS = "folders"
private const val VIDEO_MODE_ALL = "all"

private val VIDEO_SORTS = listOf(
    "recent" to "Recently added",
    "name" to "Name",
    "artist" to "Artist",
    "album" to "Album",
    "genre" to "Genre",
    "duration" to "Duration",
    "modified" to "Date modified",
)

private data class VideoFolderGroup(
    val name: String,
    val path: String,
    val items: List<MediaItem>,
) {
    val latestAdded: Long get() = items.maxOfOrNull { it.dateAdded } ?: 0L
    val latestModified: Long get() = items.maxOfOrNull { it.dateModified } ?: 0L
}

@Composable
fun VideoLibraryScreen(
    container: AppContainer,
    libraryViewModel: LibraryViewModel,
    onOpenVideo: (String) -> Unit,
    onSearch: () -> Unit,
    onCast: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val videoModePreference by container.preferences.videoLibraryMode.collectAsStateWithLifecycle(initialValue = VIDEO_MODE_FOLDERS)
    val videoSortPreference by container.preferences.videoLibrarySort.collectAsStateWithLifecycle(initialValue = "recent")
    val videoSortOrderPreference by container.preferences.videoLibrarySortOrder.collectAsStateWithLifecycle(initialValue = "desc")
    val currentVideoId by VideoQueueStore.currentId.collectAsStateWithLifecycle()

    var viewMode by rememberSaveable { mutableStateOf(VIDEO_MODE_FOLDERS) }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var sortKey by rememberSaveable { mutableStateOf("recent") }
    var sortAscending by rememberSaveable { mutableStateOf(false) }
    var filterOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var menuItem by remember { mutableStateOf<MediaItem?>(null) }
    var menuFavorite by remember { mutableStateOf(false) }
    var detailsTarget by remember { mutableStateOf<MediaItem?>(null) }
    var folderMenu by remember { mutableStateOf<VideoFolderGroup?>(null) }
    val videoListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) { androidx.compose.foundation.lazy.LazyListState(0) }

    LaunchedEffect(selectedFolder) {
        videoListState.scrollToItem(0)
    }

    LaunchedEffect(videoModePreference) {
        viewMode = if (videoModePreference == VIDEO_MODE_ALL) VIDEO_MODE_ALL else VIDEO_MODE_FOLDERS
        if (viewMode == VIDEO_MODE_ALL) selectedFolder = null
    }
    LaunchedEffect(menuItem?.id) {
        val item = menuItem
        menuFavorite = item?.let { runCatching { container.structured.isFavorite(it.id) }.getOrDefault(false) } ?: false
    }
    LaunchedEffect(videoSortPreference, videoSortOrderPreference) {
        sortKey = videoSortPreference
        sortAscending = videoSortOrderPreference == "asc"
    }

    val videos = remember(snapshot.video) { snapshot.video }
    val folders = remember(videos) {
        videos.groupBy { folderPathForVideo(it) }
            .map { (path, items) ->
                VideoFolderGroup(
                    name = path.substringAfterLast('/').ifBlank { path.substringAfterLast('\\').ifBlank { "Videos" } },
                    path = path,
                    items = items,
                )
            }
    }
    val selectedFolderGroup = remember(folders, selectedFolder) { folders.firstOrNull { it.path == selectedFolder } }
    val activeVideos = remember(videos, selectedFolder) {
        selectedFolder?.let { path -> videos.filter { folderPathForVideo(it) == path } } ?: videos
    }
    val orderedVideos = remember(activeVideos, sortKey, sortAscending) { sortVideoItems(activeVideos, sortKey, sortAscending) }
    val orderedFolders = remember(folders, sortKey, sortAscending) { sortVideoFolders(folders, sortKey, sortAscending) }

    val currentVideo = remember(videos, currentVideoId) { videos.firstOrNull { it.id == currentVideoId } }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            VideoHeader(
                title = if (selectedFolderGroup == null) "Videos" else selectedFolderGroup.name,
                subtitle = when {
                    selectedFolderGroup != null -> "${selectedFolderGroup.items.size} videos"
                    viewMode == VIDEO_MODE_FOLDERS -> "${videos.size} videos · ${folders.size} folders"
                    else -> "${videos.size} videos"
                },
                onBack = if (selectedFolderGroup != null) ({ selectedFolder = null }) else null,
                onSearch = onSearch,
                onCast = onCast,
            )

            VideoFeatureRail(onNavigate = onNavigate)

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VideoLibraryFilter(
                    mode = viewMode,
                    expanded = filterOpen,
                    onExpand = { filterOpen = true },
                    onDismiss = { filterOpen = false },
                    onModeSelected = { mode ->
                        filterOpen = false
                        selectedFolder = null
                        viewMode = mode
                        scope.launch { container.preferences.setVideoLibraryMode(mode) }
                    },
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { sortOpen = true }) {
                    Icon(Icons.Default.Sort, "Sort videos", tint = colors.primary, modifier = Modifier.size(25.dp))
                }
            }

            if (selectedFolderGroup == null && viewMode == VIDEO_MODE_FOLDERS) {
                if (!snapshot.ready) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = colors.primary) }
                } else if (orderedFolders.isEmpty()) {
                    VideoEmptyState("No video folders yet", "Videos on this device will appear here after the library is scanned.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding() + 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(orderedFolders, key = { it.path }) { folder ->
                            VideoFolderRow(folder = folder, onOpen = { selectedFolder = folder.path }, onMore = { folderMenu = folder })
                        }
                    }
                }
            } else {
                if (!snapshot.ready) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = colors.primary) }
                } else if (orderedVideos.isEmpty()) {
                    VideoEmptyState(
                        if (selectedFolderGroup != null) "This folder is empty" else "No videos found",
                        if (selectedFolderGroup != null) "The folder is indexed, but no playable videos are currently available." else "Videos on this device will appear here after the library is scanned.",
                    )
                } else {
                    LazyColumn(
                        state = videoListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding() + 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(orderedVideos, key = { it.id }) { item ->
                            AirPlayrMediaRow(
                                item = item,
                                onOpen = { onOpenVideo(item.id) },
                                onMore = { menuItem = item },
                                playing = false,
                                compact = false,
                            )
                        }
                    }
                }
            }
        }

        currentVideo?.let { item ->
            FloatingCurrentVideo(
                item = item,
                onClick = { onOpenVideo(item.id) },
                collapsed = videoListState.firstVisibleItemIndex > 0 || videoListState.firstVisibleItemScrollOffset > 72,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = airPlayrFloatingPlayBottomPadding()),
            )
        }
    }

    folderMenu?.let { folder ->
        ModalBottomSheet(onDismissRequest = { folderMenu = null }, containerColor = colors.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(folder.name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 25.sp, color = colors.textPrimary)
                Text("${folder.items.size} ${if (folder.items.size == 1) "video" else "videos"}", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textSecondary)
                Surface(onClick = {
                    folderMenu = null
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "AirPlayr video folder: ${folder.name}\n${folder.path}")
                    }
                    context.startActivity(android.content.Intent.createChooser(send, "Share folder"))
                }, shape = RoundedCornerShape(18.dp), color = colors.surfaceContainer) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                        Text("Share folder", color = colors.textPrimary, modifier = Modifier.padding(start = 12.dp))
                    }
                }
                Surface(onClick = { folderMenu = null; selectedFolder = folder.path }, shape = RoundedCornerShape(18.dp), color = colors.surfaceContainer) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                        Text("Open folder", color = colors.textPrimary, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
    }

    if (sortOpen) {
        VideoSortSheet(
            selectedKey = sortKey,
            ascending = sortAscending,
            onSelectKey = { key ->
                sortKey = key
                sortOpen = false
                scope.launch { container.preferences.setVideoLibrarySort(key) }
            },
            onAscending = {
                sortAscending = true
                scope.launch { container.preferences.setVideoLibrarySortOrder("asc") }
            },
            onDescending = {
                sortAscending = false
                scope.launch { container.preferences.setVideoLibrarySortOrder("desc") }
            },
            onDismiss = { sortOpen = false },
        )
    }

    menuItem?.let { item ->
        AirPlayrPremiumMediaActionSheet(
            item = item,
            playing = currentVideoId == item.id,
            favorite = menuFavorite,
            onFavorite = { scope.launch { menuFavorite = container.structured.toggleFavorite(item) } },
            groups = listOf(
                listOf(
                    AirPlayrMediaMenuAction("Open", AirPlayrIcons.Video, { menuItem = null; onOpenVideo(item.id) }),
                    AirPlayrMediaMenuAction("Details", Icons.Default.Info, { menuItem = null; detailsTarget = item }),
                ),
                listOf(
                    AirPlayrMediaMenuAction("Play next", Icons.Default.SkipNext, { menuItem = null; VideoQueueStore.playNext(listOf(item)) }),
                    AirPlayrMediaMenuAction("Add to video queue", Icons.Default.QueueMusic, { menuItem = null; VideoQueueStore.add(listOf(item)) }),
                    AirPlayrMediaMenuAction("Share", Icons.Default.Share, {
                        val uri = android.net.Uri.parse(item.uri)
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = item.mimeType ?: "video/*"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            clipData = android.content.ClipData.newRawUri(item.title, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching { container.appContext.startActivity(android.content.Intent.createChooser(send, "Share ${item.title}").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        menuItem = null
                    }),
                ),
            ),
            onDismiss = { menuItem = null },
        )
    }

    detailsTarget?.let { item ->
        HistoryFavoriteDetailsSheet(
            item = item,
            playing = currentVideoId == item.id,
            onDismiss = { detailsTarget = null },
        )
    }
}

@Composable
private fun VideoHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    onSearch: () -> Unit,
    onCast: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary, modifier = Modifier.size(25.dp))
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = if (onBack != null) 4.dp else 0.dp)) {
            Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onSearch, modifier = Modifier.size(46.dp)) {
            Icon(Icons.Default.Search, "Search videos", tint = colors.primary, modifier = Modifier.size(25.dp))
        }
        IconButton(onClick = onCast, modifier = Modifier.size(46.dp)) {
            Icon(Icons.Default.Cast, "Cast", tint = colors.primary, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
private fun VideoFeatureRail(onNavigate: (String) -> Unit) {
    val colors = LocalAirPlayrColors.current
    val cards = listOf(
        VideoFeatureCard("recent", "Recently added") { onNavigate("recent/videos-added") },
        VideoFeatureCard("played", "Recently played") { onNavigate("recent/videos-played") },
        VideoFeatureCard("playlists", "Playlists") { onNavigate("video-playlists") },
        VideoFeatureCard("continue", "Continue watching") { onNavigate("video-collections") },
        VideoFeatureCard("vault", "Vault") { onNavigate("vault") },
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(cards, key = { it.key }) { card ->
            Surface(
                onClick = card.onClick,
                shape = RoundedCornerShape(26.dp),
                color = colors.surfaceContainerHigh,
                tonalElevation = 0.dp,
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(card.icon, null, tint = when (card.key) { "recent" -> colors.secondary; "played" -> colors.tertiary; "playlists" -> colors.primary; "continue" -> colors.secondary; else -> colors.textSecondary }, modifier = Modifier.size(21.dp))
                    Text(card.label, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 16.sp, color = colors.textPrimary, modifier = Modifier.padding(start = 8.dp), maxLines = 1)
                }
            }
        }
    }
}

private data class VideoFeatureCard(
    val key: String,
    val label: String,
    val onClick: () -> Unit,
) {
    val icon: androidx.compose.ui.graphics.vector.ImageVector
        get() = when (key) {
            "recent" -> Icons.Default.VideoLibrary
            "played" -> Icons.Default.History
            "playlists" -> Icons.Default.PlaylistPlay
            "continue" -> Icons.Default.WatchLater
            else -> Icons.Default.Lock
        }
}

@Composable
private fun VideoLibraryFilter(
    mode: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onModeSelected: (String) -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Box {
        Surface(onClick = onExpand, shape = RoundedCornerShape(20.dp), color = colors.surfaceContainerHigh) {
            Row(Modifier.padding(horizontal = 15.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (mode == VIDEO_MODE_FOLDERS) Icons.Default.Folder else Icons.Default.VideoLibrary, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Text(if (mode == VIDEO_MODE_FOLDERS) "All folders" else "All videos", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 16.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 7.dp))
            }
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(225.dp),
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.surfaceContainerLow,
            tonalElevation = 8.dp,
        ) {
            VideoFilterMenuItem("All folders", Icons.Default.Folder, mode == VIDEO_MODE_FOLDERS) { onModeSelected(VIDEO_MODE_FOLDERS) }
            VideoFilterMenuItem("All videos", Icons.Default.VideoLibrary, mode == VIDEO_MODE_ALL) { onModeSelected(VIDEO_MODE_ALL) }
        }
    }
}

@Composable
private fun VideoFilterMenuItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Surface(onClick = onClick, color = if (selected) colors.primaryContainer else androidx.compose.ui.graphics.Color.Transparent, shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) colors.primary else colors.textSecondary, modifier = Modifier.size(24.dp))
            Text(label, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 16.sp, color = colors.textPrimary, modifier = Modifier.weight(1f).padding(start = 12.dp))
            if (selected) Icon(Icons.Default.Check, null, tint = colors.primary, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun VideoFolderRow(folder: VideoFolderGroup, onOpen: () -> Unit, onMore: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    AirPlayrCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surfaceSolid,
        elevation = 2.dp,
        onClick = onOpen,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(58.dp), shape = RoundedCornerShape(17.dp), color = colors.primaryContainer) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Folder, null, tint = colors.primary, modifier = Modifier.size(31.dp))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(folder.name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${folder.items.size} ${if (folder.items.size == 1) "video" else "videos"}", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 3.dp))
            }
            AirPlayrMoreButton(onMore)
        }
    }
}

@Composable
private fun VideoLibraryRow(item: MediaItem, playing: Boolean, onOpen: () -> Unit, onMore: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(item.thumbnailUri ?: item.artworkUri, Modifier.size(78.dp).clip(RoundedCornerShape(15.dp)), 15.dp, item.title, MediaType.VIDEO, AirPlayrIcons.Video, isPlaying = playing)
        Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
            Text(item.title.ifBlank { item.filename }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(videoSecondaryLabel(item), fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AirPlayrMoreButton(onMore)
    }
}

@Composable
private fun FloatingCurrentVideo(item: MediaItem, onClick: () -> Unit, collapsed: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalAirPlayrColors.current
    Surface(
        onClick = onClick,
        modifier = modifier.animateContentSize(animationSpec = androidx.compose.animation.core.tween(220)),
        shape = RoundedCornerShape(28.dp),
        color = colors.primary,
        shadowElevation = 10.dp,
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(50.dp), shape = CircleShape, color = colors.onPrimary.copy(alpha = .15f)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, "Resume video", tint = colors.onPrimary, modifier = Modifier.size(30.dp))
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = !collapsed,
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) + androidx.compose.animation.slideInHorizontally(initialOffsetX = { it / 3 }),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(140)) + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it / 3 }),
            ) {
                Box(Modifier.padding(start = 10.dp).widthIn(max = 220.dp).clip(RoundedCornerShape(12.dp))) {
                    Text(item.title.ifBlank { item.filename }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, color = colors.onPrimary, maxLines = 1, modifier = Modifier.basicMarquee())
                }
            }
        }
    }
}

@Composable
private fun VideoEmptyState(title: String, subtitle: String) {
    val colors = LocalAirPlayrColors.current
    Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.VideoLibrary, null, tint = colors.primary, modifier = Modifier.size(46.dp))
            Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 23.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
            Text(subtitle, fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 5.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun VideoSortSheet(
    selectedKey: String,
    ascending: Boolean,
    onSelectKey: (String) -> Unit,
    onAscending: () -> Unit,
    onDescending: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SORT YOUR LIBRARY", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, letterSpacing = 1.2.sp, color = colors.textSecondary)
            Text("Choose how Videos are arranged", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            VIDEO_SORTS.forEach { (key, label) ->
                Surface(onClick = { onSelectKey(key) }, shape = RoundedCornerShape(18.dp), color = if (key == selectedKey) colors.primaryContainer else colors.surfaceContainer) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (key == selectedKey) Icons.Default.Check else Icons.Default.Sort, null, tint = if (key == selectedKey) colors.primary else colors.textSecondary, modifier = Modifier.size(20.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 12.dp), color = colors.textPrimary)
                        if (key == selectedKey) Text(if (ascending) "ASC" else "DESC", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(onClick = onAscending, shape = RoundedCornerShape(15.dp), color = if (ascending) colors.primary else colors.surfaceContainerHigh, modifier = Modifier.weight(1f).height(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("Ascending", color = if (ascending) colors.onPrimary else colors.textPrimary, fontWeight = FontWeight.SemiBold) }
                }
                Surface(onClick = onDescending, shape = RoundedCornerShape(15.dp), color = if (!ascending) colors.primary else colors.surfaceContainerHigh, modifier = Modifier.weight(1f).height(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("Descending", color = if (!ascending) colors.onPrimary else colors.textPrimary, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

private fun folderPathForVideo(item: MediaItem): String =
    item.folder?.trim()?.takeUnless { it.isNullOrBlank() }
        ?: item.relativePath?.trim()?.substringBeforeLast('/', "").takeUnless { it.isNullOrBlank() }
        ?: item.path?.trim()?.substringBeforeLast('/', "").takeUnless { it.isNullOrBlank() }
        ?: "Videos"

private fun sortVideoItems(items: List<MediaItem>, key: String, ascending: Boolean): List<MediaItem> {
    val sorted = when (key) {
        "artist" -> items.sortedWith(compareBy<MediaItem> { it.artist.orEmpty().lowercase() }.thenBy { it.title.lowercase() })
        "album" -> items.sortedWith(compareBy<MediaItem> { it.album.orEmpty().lowercase() }.thenBy { it.title.lowercase() })
        "genre" -> items.sortedWith(compareBy<MediaItem> { it.genre.orEmpty().lowercase() }.thenBy { it.title.lowercase() })
        "duration" -> items.sortedWith(compareBy<MediaItem> { it.durationMs }.thenBy { it.title.lowercase() })
        "modified" -> items.sortedWith(compareBy<MediaItem> { it.dateModified }.thenBy { it.title.lowercase() })
        "recent" -> items.sortedWith(compareBy<MediaItem> { it.dateAdded }.thenBy { it.title.lowercase() })
        else -> items.sortedBy { it.title.lowercase() }
    }
    return if (ascending) sorted else sorted.asReversed()
}

private fun sortVideoFolders(items: List<VideoFolderGroup>, key: String, ascending: Boolean): List<VideoFolderGroup> {
    val sorted = when (key) {
        "recent" -> items.sortedWith(compareBy<VideoFolderGroup> { it.latestAdded }.thenBy { it.name.lowercase() })
        "modified" -> items.sortedWith(compareBy<VideoFolderGroup> { it.latestModified }.thenBy { it.name.lowercase() })
        "duration" -> items.sortedWith(compareBy<VideoFolderGroup> { it.items.sumOf { video -> video.durationMs } }.thenBy { it.name.lowercase() })
        else -> items.sortedBy { it.name.lowercase() }
    }
    return if (ascending) sorted else sorted.asReversed()
}

private fun videoSecondaryLabel(item: MediaItem): String {
    val duration = formatVideoDuration(item.durationMs)
    val folder = folderPathForVideo(item).substringAfterLast('/').ifBlank { "Videos" }
    return "$duration · $folder"
}

private fun formatVideoDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
}
