package com.airplayr.app.ui.screens

import com.airplayr.app.ui.theme.AirPlayrIcons

import com.airplayr.app.ui.theme.LocalAirPlayrColors
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.components.AirPlayrCard
import com.airplayr.app.ui.components.airPlayrShadow
import com.airplayr.app.ui.airPlayrBottomContentPadding
import com.airplayr.app.ui.airPlayrFloatingPlayBottomPadding

import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.components.AirPlayrMediaRow
import com.airplayr.app.ui.components.AirPlayrMediaArtwork
import com.airplayr.app.ui.components.AIRPLAYR_FLOATING_PLAY_BOTTOM
import com.airplayr.app.ui.components.AIRPLAYR_FLOATING_PLAY_BOTTOM_WITH_MINI
import com.airplayr.app.ui.components.displayArtworkUri
import com.airplayr.app.ui.components.AirPlayrScreenHeader
import com.airplayr.app.ui.components.AirPlayrSectionEmpty
import com.airplayr.app.ui.components.ActionGroup
import com.airplayr.app.ui.components.ActionButton
import com.airplayr.app.ui.feedback.AppFeedback
import com.airplayr.app.ui.components.HistoryFavoritePlaylistSheet
import com.airplayr.app.ui.components.TagEditorSheet
import com.airplayr.app.core.playback.VideoQueueStore
import com.airplayr.app.ui.theme.AirPlayrColors
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FeatureCollectionScreen(
    title: String,
    subtitle: String,
    loader: suspend () -> List<MediaItem>,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    container: AppContainer? = null,
    onBack: () -> Unit = {},
    onVideoOpen: (String) -> Unit = {},
    onOpenPlayer: () -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val cachedFeatures by libraryViewModel.cachedFeatures.collectAsStateWithLifecycle()
    val cacheKey = "feature:$title"
    var items by remember(cachedFeatures, cacheKey) { mutableStateOf(cachedFeatures[cacheKey].orEmpty()) }
    var loading by remember(cachedFeatures, cacheKey) { mutableStateOf(cachedFeatures[cacheKey] == null) }
    var menuOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var favorite by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    val isTime = title == "Tuned in"
    val profile = remember(now.get(java.util.Calendar.HOUR_OF_DAY)) {
        when (now.get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..8 -> Triple("MORNING", "Start soft. Stay inspired.", "A bright little set for the first stretch of your day.")
            in 9..11 -> Triple("MORNING", "Start soft. Stay inspired.", "A bright little set for the first stretch of your day.")
            in 12..16 -> Triple("AFTERNOON", "Keep the day moving.", "A warm, forward-moving mix for the middle of the day.")
            in 17..21 -> Triple("EVENING", "Ease into the evening.", "A balanced soundtrack for the hours after the rush.")
            else -> Triple("NIGHT", "Settle into the night.", "A softer soundtrack for the quiet hours.")
        }
    }
    LaunchedEffect(title) { libraryViewModel.loadFeature(cacheKey, loader) }
    LaunchedEffect(isTime) { if (isTime) while (true) { kotlinx.coroutines.delay(30_000); now = java.util.Calendar.getInstance() } }
    LaunchedEffect(cachedFeatures, cacheKey) { cachedFeatures[cacheKey]?.let { items = it; loading = false } }
    val playbackState by playback.stableState.collectAsStateWithLifecycle()

    fun playAll(shuffle: Boolean) {
        if (items.isEmpty()) return
        val ordered = if (shuffle) items.shuffled() else items
        val audioItems = ordered.filter { it.mediaType == MediaType.AUDIO }
        val videoItems = ordered.filter { it.mediaType == MediaType.VIDEO }
        if (audioItems.isNotEmpty()) {
            playback.playQueue(audioItems, 0)
        } else if (videoItems.isNotEmpty()) {
            onVideoOpen(if (shuffle) videoItems.random().id else videoItems.first().id)
        }
        menuOpen = false
    }
    val hero = items.firstOrNull()
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding()), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary) }
                    Column(Modifier.weight(1f)) {
                        Text(if (isTime) "AIRPLAYR · ${profile.first}" else "AIRPLAYR · PERSONAL", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, letterSpacing = 1.3.sp, color = colors.textPrimary)
                        Text(if (isTime) "Tuned in" else title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 29.sp, color = colors.textPrimary)
                    }
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Options", tint = colors.primary) }
                }
            }
            if (hero != null) item {
                FeatureHero(
                    item = hero, title = when (title) {
                        "Rock your world" -> hero.title
                        "Rediscover" -> hero.title
                        "For you now" -> "A mix that feels like you."
                        "Tuned in" -> profile.second
                        else -> title
                    },
                    subtitle = when (title) {
                        "Rock your world" -> hero.artist ?: "Unknown artist"
                        "Rediscover" -> "A personal rewind from your recent watching."
                        "For you now" -> "Built from your recent plays, favorites and listening patterns — no generic playlist required."
                        "Tuned in" -> profile.third
                        else -> subtitle
                    },
                    badge = when (title) { "Rock your world" -> "HIGH ENERGY · ${items.size} SONGS"; "Rediscover" -> "THE REWIND · ${items.size} VIDEOS"; "Tuned in" -> "${profile.first} MIX · ${items.size} SONGS"; else -> "MADE FOR YOU" },
                    colors = colors,
                    playing = playbackState.isPlaying && playbackState.currentId == hero.id,
                    onPlay = { if (hero.mediaType == MediaType.AUDIO) playback.playQueue(items, 0) else onVideoOpen(hero.id) },
                    onShuffle = { playAll(true) }
                )
            } else if (loading) item {
                Box(Modifier.fillMaxWidth().padding(vertical = 56.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.primary) }
            } else item {
                AirPlayrSectionEmpty("Your collection is waiting", "Use AirPlayr for a while and this space will build from your local library.")
            }
            if (items.isNotEmpty() && title == "Tuned in") item {
                AirPlayrCard(Modifier.fillMaxWidth().padding(22.dp, 18.dp), shape = RoundedCornerShape(26.dp), containerColor = colors.surfaceContainer, elevation = 5.dp) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(48.dp), shape = androidx.compose.foundation.shape.CircleShape, color = colors.tertiaryContainer) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("✦", color = colors.tertiary, fontSize = 22.sp) } }
                        Column(Modifier.padding(start = 12.dp)) { Text("A different mood, every part of the day.", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 16.sp, color = colors.textPrimary); Text("AirPlayr reshapes this card around your local time without needing a new playlist.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, lineHeight = 18.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp)) }
                    }
                }
            }
            if (items.isNotEmpty()) item {
                Text(if (title == "Rediscover") "Worth the rewind" else if (title == "Rock your world") "The loud side" else if (title == "For you now") "Because you played it" else "The set", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 28.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
                Text(if (title == "Rediscover") "Your unfinished and recently watched stories." else if (title == "Rock your world") "Songs ready when you want more energy." else if (title == "For you now") "A few familiar sounds to bring you back in." else "A hand-picked sequence for this moment.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 22.dp))
            }
            items(items, key = { it.id }) { item ->
                FeatureMediaRow(item, colors, playing = playbackState.isPlaying && playbackState.currentId == item.id, onPlay = { if (item.mediaType == MediaType.AUDIO) playback.play(item) else onVideoOpen(item.id) }, onMore = { selected = item; scope.launch { favorite = container?.structured?.isFavorite(item.id) ?: false } })
            }
        }
        if (items.isNotEmpty()) FloatingActionButton(
            onClick = {
                if (items.first().mediaType == MediaType.VIDEO) {
                    onVideoOpen(items.first().id)
                } else {
                    playAll(false)
                    onOpenPlayer()
                }
            },
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = airPlayrFloatingPlayBottomPadding()).size(68.dp),
            shape = RoundedCornerShape(22.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 10.dp),
        ) { Icon(Icons.Default.PlayArrow, "Play mix", modifier = Modifier.size(31.dp)) }
        if (menuOpen) {
            ModalBottomSheet(onDismissRequest = { menuOpen = false }, containerColor = colors.surfaceContainerLow) {
                ActionGroup {
                    ActionButton("Shuffle", Icons.Default.Shuffle, onClick = { playAll(true) })
                    ActionButton("Add to queue", Icons.Default.QueueMusic, onClick = {
                        val audio = items.filter { it.mediaType == MediaType.AUDIO }
                        audio.forEach(playback::addToQueue)
                        menuOpen = false
                        com.airplayr.app.ui.feedback.AppFeedback.showSnackbar(if (audio.isEmpty()) "Videos do not use the music queue." else "Added ${audio.size} songs to the playing queue.")
                    })
                    ActionButton("Play next", Icons.Default.SkipNext, onClick = {
                        val audio = items.filter { it.mediaType == MediaType.AUDIO }
                        audio.forEach(playback::playNext)
                        menuOpen = false
                        if (audio.isEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Videos do not use the music queue.")
                    })
                }
            }
        }
        selected?.let { item ->
            ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = colors.surfaceContainerLow) {
                ActionGroup {
                    ActionButton(if (favorite) "Remove from favorites" else "Add to favorites", if (favorite) Icons.Default.Star else Icons.Default.StarBorder, onClick = { scope.launch { val active = container?.structured?.toggleFavorite(item) ?: false; favorite = active; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar(if (active) "Added to favorites." else "Removed from favorites.") }; selected = null })
                    ActionButton("Play next", Icons.Default.SkipNext, onClick = {
                        if (item.mediaType == MediaType.AUDIO) playback.playNext(item)
                        else com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Videos do not use the music queue.")
                        selected = null
                    })
                    ActionButton("Add to queue", Icons.Default.QueueMusic, onClick = {
                        if (item.mediaType == MediaType.AUDIO) {
                            playback.addToQueue(item)
                            com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Added to the playing queue.")
                        } else {
                            com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Videos do not use the music queue.")
                        }
                        selected = null
                    })
                }
            }
        }
    }
}

@Composable
private fun FeatureHero(item: MediaItem, title: String?, subtitle: String, badge: String, colors: com.airplayr.app.ui.theme.AirPlayrColors, playing: Boolean, onPlay: () -> Unit, onShuffle: () -> Unit) {
    val art = item.displayArtworkUri()
    Box(Modifier.fillMaxWidth().height(360.dp).padding(horizontal = 22.dp).clip(RoundedCornerShape(30.dp)).airPlayrShadow(RoundedCornerShape(30.dp), 6.dp).clickable { onPlay() }) {
        if (art != null) com.airplayr.app.ui.components.Artwork(art, Modifier.fillMaxSize(), 30.dp, item.title, item.mediaType, if (item.mediaType == MediaType.VIDEO) AirPlayrIcons.Video else AirPlayrIcons.Music, isPlaying = playing)
        else Box(Modifier.fillMaxSize().background(colors.primaryContainer))
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .42f)))
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(shape = RoundedCornerShape(30.dp), color = colors.surfaceContainerHighest) { Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = colors.primary, modifier = Modifier.size(14.dp)); Text(badge, fontFamily = AirPlayrFonts.OpenSans, fontSize = 10.sp, letterSpacing = .8.sp, color = colors.textPrimary, modifier = Modifier.padding(start = 6.dp)) } }
                IconButton(onClick = onShuffle, modifier = Modifier.size(46.dp)) { Surface(Modifier.fillMaxSize(), shape = androidx.compose.foundation.shape.CircleShape, color = colors.primary) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Shuffle, "Shuffle", tint = colors.onPrimary) } } }
            }
            Spacer(Modifier.weight(1f))
            Text(title ?: item.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 34.sp, lineHeight = 40.sp, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontFamily = AirPlayrFonts.OpenSans, fontSize = 14.sp, lineHeight = 20.sp, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("Tap to start the mix", fontSize = 12.sp, color = colors.textPrimary); Surface(Modifier.size(50.dp), shape = androidx.compose.foundation.shape.CircleShape, color = colors.primary) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, "Play", tint = colors.onPrimary, modifier = Modifier.size(22.dp)) } } }
        }
    }
}

@Composable
private fun FeatureMediaRow(item: MediaItem, colors: com.airplayr.app.ui.theme.AirPlayrColors, playing: Boolean, onPlay: () -> Unit, onMore: () -> Unit) {
    AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 5.dp), shape = RoundedCornerShape(22.dp), containerColor = colors.surfaceContainer, elevation = 5.dp) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            com.airplayr.app.ui.components.AirPlayrMediaArtwork(
                artworkUri = item.displayArtworkUri(),
                modifier = Modifier.size(82.dp),
                radius = 18.dp,
                title = item.title,
                mediaType = item.mediaType,
                fallbackIcon = if (item.mediaType == MediaType.VIDEO) AirPlayrIcons.Video else AirPlayrIcons.Music,
                playing = playing,
                onPlay = onPlay,
                onMore = onMore,
            )
            Column(Modifier.weight(1f).padding(horizontal = 13.dp).clickable(onClick = onPlay)) {
                Text(item.title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artist ?: if (item.mediaType == MediaType.VIDEO) "Video" else "Unknown artist", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textSecondary, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}


private enum class FolderTab { ALL, MUSIC, VIDEO }

private data class FolderEntry(
    val path: String,
    val name: String,
    val audio: List<MediaItem>,
    val video: List<MediaItem>,
    val dateAdded: Long,
    val dateModified: Long,
)

private fun mediaFolderPath(item: MediaItem): String =
    (item.folder ?: item.relativePath ?: item.path)
        ?.trim()
        ?.trim('/')
        ?.takeIf { it.isNotBlank() }
        ?: "Root"

private fun mediaFolderName(path: String): String =
    path.trim('/').substringAfterLast('/').ifBlank { "Root" }

private fun buildFolderEntries(audio: List<MediaItem>, video: List<MediaItem>): List<FolderEntry> =
    (audio + video)
        .groupBy(::mediaFolderPath)
        .map { (path, items) ->
            FolderEntry(
                path = path,
                name = mediaFolderName(path),
                audio = items.filter { it.mediaType == MediaType.AUDIO },
                video = items.filter { it.mediaType == MediaType.VIDEO },
                dateAdded = items.maxOfOrNull { it.dateAdded } ?: 0L,
                dateModified = items.maxOfOrNull { it.dateModified } ?: 0L,
            )
        }
        .sortedBy { it.name.lowercase() }

@Composable
private fun FolderAccent(audioCount: Int, videoCount: Int): Triple<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    val colors = LocalAirPlayrColors.current
    return when {
        audioCount > 0 && videoCount > 0 -> Triple(colors.secondaryContainer, colors.onSecondaryContainer, colors.secondary)
        audioCount > 0 -> Triple(colors.primaryContainer, colors.onPrimaryContainer, colors.primary)
        else -> Triple(colors.tertiaryContainer, colors.onTertiaryContainer, colors.tertiary)
    }
}

@Composable
private fun FolderSummaryChip(
    folder: FolderEntry,
) {
    val colors = LocalAirPlayrColors.current
    val (container, content, _) = FolderAccent(folder.audio.size, folder.video.size)
    val label = when {
        folder.audio.isNotEmpty() && folder.video.isNotEmpty() -> "MIXED"
        folder.audio.isNotEmpty() -> "MUSIC"
        else -> "VIDEO"
    }
    val countText = when {
        folder.audio.isNotEmpty() && folder.video.isNotEmpty() -> "${folder.audio.size} songs · ${folder.video.size} videos"
        folder.audio.isNotEmpty() -> "${folder.audio.size} ${if (folder.audio.size == 1) "song" else "songs"}"
        else -> "${folder.video.size} ${if (folder.video.size == 1) "video" else "videos"}"
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                when {
                    folder.audio.isNotEmpty() && folder.video.isNotEmpty() -> Icons.Default.PlayArrow
                    folder.audio.isNotEmpty() -> Icons.Default.MusicNote
                    else -> Icons.Default.VideoLibrary
                },
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp),
            )
            Text(
                label,
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 1.sp,
                color = content,
            )
            Text(
                countText,
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 12.sp,
                color = content,
            )
        }
    }
}

@Composable
private fun FolderCard(
    folder: FolderEntry,
    selectionMode: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val (iconContainer, iconContent, accent) = FolderAccent(folder.audio.size, folder.video.size)
    val shape = RoundedCornerShape(22.dp)
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(if (selected) Modifier.border(2.dp, colors.primary, shape) else Modifier)

    AirPlayrCard(
        modifier = cardModifier,
        shape = shape,
        containerColor = if (selected) colors.primaryContainer.copy(alpha = 0.34f) else colors.surfaceSolid,
        elevation = 0.dp,
        onClick = if (selectionMode) onToggleSelection else onOpen,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(78.dp).clip(RoundedCornerShape(20.dp)).background(iconContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = iconContent,
                        modifier = Modifier.size(40.dp),
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                        shape = RoundedCornerShape(9.dp),
                        color = accent,
                        shadowElevation = 0.dp,
                    ) {
                        Icon(
                            when {
                                folder.audio.isNotEmpty() && folder.video.isNotEmpty() -> Icons.Default.PlayArrow
                                folder.audio.isNotEmpty() -> Icons.Default.MusicNote
                                else -> Icons.Default.VideoLibrary
                            },
                            contentDescription = null,
                            tint = when {
                                folder.audio.isNotEmpty() && folder.video.isNotEmpty() -> colors.onSecondary
                                folder.audio.isNotEmpty() -> colors.onPrimary
                                else -> colors.onTertiary
                            },
                            modifier = Modifier.padding(5.dp).size(16.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = 13.dp, end = if (selectionMode) 38.dp else 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        folder.name,
                        fontFamily = AirPlayrFonts.SmoochSans,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        folder.path,
                        fontFamily = AirPlayrFonts.OpenSans,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FolderSummaryChip(folder)
                }
            }
            if (selectionMode) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp).size(30.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (selected) colors.primary else colors.surfaceContainerHighest,
                    shadowElevation = 0.dp,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (selected) "Selected" else "Not selected",
                            tint = if (selected) colors.onPrimary else colors.textSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    container: AppContainer,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    onOpen: (String) -> Unit,
    onBack: () -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(FolderTab.ALL) }
    var sortKey by rememberSaveable { mutableStateOf("name") }
    var ascending by rememberSaveable { mutableStateOf(true) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var menuOpen by remember { mutableStateOf(false) }
    var sortSheetOpen by remember { mutableStateOf(false) }
    var playlistSheetOpen by remember { mutableStateOf(false) }

    val allFolders = remember(snapshot.audio, snapshot.video) { buildFolderEntries(snapshot.audio, snapshot.video) }
    val musicFolders = remember(allFolders) { allFolders.filter { it.audio.isNotEmpty() } }
    val videoFolders = remember(allFolders) { allFolders.filter { it.video.isNotEmpty() } }
    val baseFolders = when (selectedTab) {
        FolderTab.ALL -> allFolders
        FolderTab.MUSIC -> musicFolders
        FolderTab.VIDEO -> videoFolders
    }
    val visibleFolders = remember(baseFolders, sortKey, ascending) {
        val sorted = when (sortKey) {
            "count" -> baseFolders.sortedBy { folder ->
                when (selectedTab) {
                    FolderTab.ALL -> folder.audio.size + folder.video.size
                    FolderTab.MUSIC -> folder.audio.size
                    FolderTab.VIDEO -> folder.video.size
                }
            }
            "added" -> baseFolders.sortedBy { it.dateAdded }
            "modified" -> baseFolders.sortedBy { it.dateModified }
            else -> baseFolders.sortedBy { it.name.lowercase() }
        }
        if (ascending) sorted else sorted.asReversed()
    }
    val visiblePaths = remember(visibleFolders) { visibleFolders.map { it.path }.toSet() }
    val selectedFolders = remember(allFolders, selectedPaths) { allFolders.filter { it.path in selectedPaths } }
    val selectedItems = remember(selectedFolders) {
        selectedFolders.flatMap { it.audio + it.video }.distinctBy { it.id }
    }
    val hasSelection = selectedPaths.isNotEmpty()

    fun clearSelection() {
        selectedPaths = emptySet()
        selectionMode = false
    }

    fun enterSelectionMode() {
        selectionMode = true
        selectedPaths = emptySet()
    }

    fun toggleFolderSelection(folder: FolderEntry) {
        selectedPaths = if (folder.path in selectedPaths) selectedPaths - folder.path else selectedPaths + folder.path
    }

    fun selectAllVisible() {
        selectedPaths = visiblePaths
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        AirPlayrScreenHeader(
            title = "Folders",
            subtitle = "${allFolders.size} folders · media only",
            onBack = if (selectionMode) ({ clearSelection() }) else onBack,
            trailing = {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, "Folder options", tint = colors.primary, modifier = Modifier.size(27.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (!selectionMode) {
                            DropdownMenuItem(text = { Text("Select") }, leadingIcon = { Icon(Icons.Default.CheckCircle, null) }, onClick = { menuOpen = false; enterSelectionMode() })
                            DropdownMenuItem(text = { Text("Refresh library") }, leadingIcon = { Icon(Icons.Default.Refresh, null) }, onClick = { menuOpen = false; libraryViewModel.refresh() })
                            DropdownMenuItem(text = { Text("Sort by") }, leadingIcon = { Icon(Icons.Default.Sort, null) }, onClick = { menuOpen = false; sortSheetOpen = true })
                            DropdownMenuItem(text = { Text("Blacklist") }, leadingIcon = { Icon(Icons.Default.Block, null) }, onClick = { menuOpen = false; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Blacklist is not available yet.") })
                        } else if (!hasSelection) {
                            DropdownMenuItem(text = { Text("Select all") }, leadingIcon = { Icon(Icons.Default.CheckCircle, null) }, onClick = { menuOpen = false; selectAllVisible() })
                            DropdownMenuItem(text = { Text("Refresh library") }, leadingIcon = { Icon(Icons.Default.Refresh, null) }, onClick = { menuOpen = false; libraryViewModel.refresh() })
                            DropdownMenuItem(text = { Text("Sort by") }, leadingIcon = { Icon(Icons.Default.Sort, null) }, onClick = { menuOpen = false; sortSheetOpen = true })
                            DropdownMenuItem(text = { Text("Blacklist") }, leadingIcon = { Icon(Icons.Default.Block, null) }, onClick = { menuOpen = false; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Blacklist is not available yet.") })
                        } else {
                            DropdownMenuItem(text = { Text("Deselect") }, leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, null) }, onClick = { menuOpen = false; clearSelection() })
                            DropdownMenuItem(text = { Text("Play next") }, leadingIcon = { Icon(Icons.Default.SkipNext, null) }, onClick = {
                                menuOpen = false
                                val audio = selectedItems.filter { it.mediaType == MediaType.AUDIO }
                                val video = selectedItems.filter { it.mediaType == MediaType.VIDEO }
                                audio.asReversed().forEach { playback.playNext(it) }
                                if (video.isNotEmpty()) VideoQueueStore.playNext(video)
                                com.airplayr.app.ui.feedback.AppFeedback.showSnackbar(folderQueueMessage(audio.size, video.size, true))
                                clearSelection()
                            })
                            DropdownMenuItem(text = { Text("Add to queue") }, leadingIcon = { Icon(Icons.Default.QueueMusic, null) }, onClick = {
                                menuOpen = false
                                val audio = selectedItems.filter { it.mediaType == MediaType.AUDIO }
                                val video = selectedItems.filter { it.mediaType == MediaType.VIDEO }
                                audio.forEach { playback.addToQueue(it) }
                                if (video.isNotEmpty()) VideoQueueStore.add(video)
                                com.airplayr.app.ui.feedback.AppFeedback.showSnackbar(folderQueueMessage(audio.size, video.size, false))
                                clearSelection()
                            })
                            DropdownMenuItem(text = { Text("Add to playlist") }, leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) }, onClick = { menuOpen = false; playlistSheetOpen = true })
                        }
                    }
                }
            },
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FolderTabButton("All", allFolders.size, selectedTab == FolderTab.ALL) { selectedTab = FolderTab.ALL; if (selectionMode) selectedPaths = emptySet() }
            FolderTabButton("Music", musicFolders.size, selectedTab == FolderTab.MUSIC) { selectedTab = FolderTab.MUSIC; if (selectionMode) selectedPaths = emptySet() }
            FolderTabButton("Videos", videoFolders.size, selectedTab == FolderTab.VIDEO) { selectedTab = FolderTab.VIDEO; if (selectionMode) selectedPaths = emptySet() }
        }

        if (selectionMode) {
            Text(
                if (hasSelection) "${selectedPaths.intersect(visiblePaths).size} selected" else "Select folders",
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 12.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 2.dp),
            )
        }

        if (!snapshot.ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.primary) }
        } else if (visibleFolders.isEmpty()) {
            EmptyNativeState(
                when (selectedTab) { FolderTab.ALL -> "No folders yet"; FolderTab.MUSIC -> "No music folders yet"; FolderTab.VIDEO -> "No video folders yet" },
                "Grant media access or add a media folder in Settings.",
            )
        } else {
            LazyColumn(
                state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = airPlayrBottomContentPadding()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visibleFolders, key = { "${selectedTab.name}:${it.path}" }) { folder ->
                    FolderCard(
                        folder = folder,
                        selectionMode = selectionMode,
                        selected = folder.path in selectedPaths,
                        onOpen = {
                            val kind = when (selectedTab) { FolderTab.ALL -> "all"; FolderTab.MUSIC -> "music"; FolderTab.VIDEO -> "video" }
                            onOpen("$kind/${java.net.URLEncoder.encode(folder.path, "UTF-8")}/${java.net.URLEncoder.encode(folder.name, "UTF-8")}")
                        },
                        onToggleSelection = { toggleFolderSelection(folder) },
                    )
                }
            }
        }
    }

    if (sortSheetOpen) {
        FolderSortSheet(
            selectedKey = sortKey,
            ascending = ascending,
            countLabel = when (selectedTab) { FolderTab.ALL -> "Song & video count"; FolderTab.MUSIC -> "Song count"; FolderTab.VIDEO -> "Video count" },
            onSelectKey = { sortKey = it; sortSheetOpen = false },
            onAscending = { ascending = true },
            onDescending = { ascending = false },
            onDismiss = { sortSheetOpen = false },
        )
    }

    if (playlistSheetOpen) {
        HistoryFavoritePlaylistSheet(container, selectedItems, onDismiss = { playlistSheetOpen = false; clearSelection() })
    }
}

private fun folderQueueMessage(audio: Int, video: Int, next: Boolean): String {
    val detail = when {
        audio > 0 && video > 0 -> "$audio songs and $video videos"
        audio > 0 -> "$audio ${if (audio == 1) "song" else "songs"}"
        video > 0 -> "$video ${if (video == 1) "video" else "videos"}"
        else -> "Nothing"
    }
    return if (audio == 0 && video == 0) "No playable media selected." else "$detail added ${if (next) "to play next" else "to the queue"} ✓"
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FolderSortSheet(
    selectedKey: String,
    ascending: Boolean,
    countLabel: String,
    onSelectKey: (String) -> Unit,
    onAscending: () -> Unit,
    onDescending: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("FOLDERS", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, letterSpacing = 1.2.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Sort by", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 30.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Row(Modifier.clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainerHigh).padding(4.dp)) {
                    SortDirectionChip("↑ ASC", ascending, onAscending)
                    SortDirectionChip("↓ DESC", !ascending, onDescending)
                }
            }
            Spacer(Modifier.height(12.dp))
            listOf("name" to "Name", "count" to countLabel, "modified" to "Date modified", "added" to "Date Added").forEach { (key, label) ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectKey(key) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selectedKey == key) colors.primaryContainer else colors.surfaceSolid,
                ) {
                    Text(label, fontFamily = AirPlayrFonts.OpenSans, fontSize = 16.sp, fontWeight = if (selectedKey == key) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SortDirectionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = if (selected) colors.primaryContainer else androidx.compose.ui.graphics.Color.Transparent) {
        Text(label, fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
private fun RowScope.FolderTabButton(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Surface(
        modifier = Modifier.weight(1f).height(72.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(36.dp),
        color = if (selected) colors.primary else androidx.compose.ui.graphics.Color.Transparent,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontFamily = AirPlayrFonts.OpenSans, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = if (selected) colors.onPrimary else colors.textPrimary)
            Spacer(Modifier.width(7.dp))
            Text(count.toString(), fontFamily = AirPlayrFonts.OpenSans, fontSize = 14.sp, color = if (selected) colors.onPrimary else colors.textSecondary)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BrowseFilterScreen(
    title: String,
    value: String,
    type: MediaType,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    onOpenPlayer: () -> Unit = {},
    onOpenVideo: (String) -> Unit = {},
    onBack: () -> Unit = {},
    folderMode: String? = null,
    folderName: String? = null,
    container: AppContainer? = null,
) {
    val colors = LocalAirPlayrColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val isFolder = title.equals("Folders", true) && folderMode != null
    val source = when {
        isFolder && folderMode.equals("all", true) -> snapshot.audio + snapshot.video
        isFolder && folderMode.equals("music", true) -> snapshot.audio
        isFolder && folderMode.equals("video", true) -> snapshot.video
        type == MediaType.AUDIO -> snapshot.audio
        else -> snapshot.video
    }
    val filtered = remember(source, value, folderMode) {
        source.filter {
            when {
                isFolder -> mediaFolderPath(it).equals(value, true)
                title.equals("artists", true) -> it.artist.equals(value, true)
                title.equals("albums", true) -> it.album.equals(value, true)
                title.equals("genres", true) -> it.genre.equals(value, true)
                title.equals("folders", true) -> mediaFolderPath(it).equals(value, true)
                else -> true
            }
        }
    }
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    var sortKey by rememberSaveable { mutableStateOf("name") }
    var sortAscending by rememberSaveable { mutableStateOf(true) }
    var sortSheetOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedMusicItem by remember { mutableStateOf<MediaItem?>(null) }
    var musicActionOpen by remember { mutableStateOf(false) }
    var musicPlaylistOpen by remember { mutableStateOf(false) }
    var editMusicItem by remember { mutableStateOf<MediaItem?>(null) }
    var videoActionItem by remember { mutableStateOf<MediaItem?>(null) }
    var videoDetailsItem by remember { mutableStateOf<MediaItem?>(null) }
    var videoPlaylistItem by remember { mutableStateOf<MediaItem?>(null) }
    var deleteVideoItem by remember { mutableStateOf<MediaItem?>(null) }
    var renameVideoItem by remember { mutableStateOf<MediaItem?>(null) }
    var renameVideoName by remember { mutableStateOf("") }
    var pendingRename by remember { mutableStateOf<Pair<MediaItem, String>?>(null) }
    var videoViewMode by rememberSaveable { mutableStateOf("list") }

    val renameLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = pendingRename
        pendingRename = null
        if (result.resultCode == android.app.Activity.RESULT_OK && pending != null) {
            val (item, name) = pending
            val uri = item.mediaStoreId?.let { android.content.ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, it) }
            if (uri != null) {
                runCatching {
                    val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name) }
                    context.contentResolver.update(uri, values, null, null)
                }.onSuccess {
                    libraryViewModel.refresh()
                    com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Video renamed ✓")
                }.onFailure {
                    com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Unable to rename video")
                }
            }
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            libraryViewModel.refresh()
            AppFeedback.showSnackbar("Video deleted ✓")
        }
    }

    val renameVideo: (MediaItem, String) -> Unit = { item, name ->
        val storeId = item.mediaStoreId
        if (storeId == null) {
            AppFeedback.showSnackbar("Unable to rename video")
        } else {
            val uri = android.content.ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, storeId)
            runCatching {
                if (Build.VERSION.SDK_INT >= 30) {
                    pendingRename = item to name
                    val request = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                    renameLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                } else {
                    val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name) }
                    context.contentResolver.update(uri, values, null, null)
                    libraryViewModel.refresh()
                    AppFeedback.showSnackbar("Video renamed ✓")
                }
            }.onFailure {
                AppFeedback.showSnackbar("Unable to rename video")
            }
        }
    }

    val deleteVideo: (MediaItem) -> Unit = { item ->
        val storeId = item.mediaStoreId
        if (storeId == null) {
            AppFeedback.showSnackbar("Unable to delete video")
        } else {
            val uri = android.content.ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, storeId)
            runCatching {
                if (Build.VERSION.SDK_INT >= 30) {
                    val request = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                    deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                } else {
                    context.contentResolver.delete(uri, null, null)
                    libraryViewModel.refresh()
                    AppFeedback.showSnackbar("Video deleted ✓")
                }
            }.onFailure {
                AppFeedback.showSnackbar("Unable to delete video")
            }
        }
    }

    val ordered = remember(filtered, sortKey, sortAscending) {
        val sorted = when (sortKey) {
            "count" -> filtered.sortedBy { 1 }
            "modified" -> filtered.sortedBy { it.dateModified }
            "added" -> filtered.sortedBy { it.dateAdded }
            else -> filtered.sortedBy { it.title.lowercase() }
        }
        if (sortAscending) sorted else sorted.asReversed()
    }
    val folderAudio = ordered.count { it.mediaType == MediaType.AUDIO }
    val folderVideo = ordered.count { it.mediaType == MediaType.VIDEO }
    val subtitle = when {
        folderAudio > 0 && folderVideo > 0 -> "$folderAudio songs · $folderVideo videos"
        folderAudio > 0 -> "$folderAudio ${if (folderAudio == 1) "song" else "songs"}"
        else -> "$folderVideo ${if (folderVideo == 1) "video" else "videos"}"
    }
    val displayTitle = folderName ?: value.substringAfterLast('/').ifBlank { value }
    val musicFolder = isFolder && folderMode.equals("music", true)
    val selectedItems = remember(ordered, selectedIds) { ordered.filter { it.id in selectedIds } }
    val hasSelection = selectedIds.isNotEmpty()

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            AirPlayrScreenHeader(
                title = if (isFolder) displayTitle else value,
                subtitle = subtitle,
                onBack = if (selectionMode) { { selectedIds = emptySet(); selectionMode = false } } else onBack,
                trailing = if (isFolder) {
                    {
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Folder options", tint = colors.primary, modifier = Modifier.size(27.dp)) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (!selectionMode) {
                                    DropdownMenuItem(text = { Text("Select") }, leadingIcon = { Icon(Icons.Default.CheckCircle, null) }, onClick = { menuOpen = false; selectionMode = true; selectedIds = emptySet() })
                                    DropdownMenuItem(text = { Text("Refresh library") }, leadingIcon = { Icon(Icons.Default.Refresh, null) }, onClick = { menuOpen = false; libraryViewModel.refresh() })
                                    DropdownMenuItem(text = { Text("Sort by") }, leadingIcon = { Icon(Icons.Default.Sort, null) }, onClick = { menuOpen = false; sortSheetOpen = true })
                                    DropdownMenuItem(text = { Text("Blacklist") }, leadingIcon = { Icon(Icons.Default.Block, null) }, onClick = { menuOpen = false; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Blacklist is not available yet.") })
                                } else if (!hasSelection) {
                                    DropdownMenuItem(text = { Text("Select all") }, leadingIcon = { Icon(Icons.Default.CheckCircle, null) }, onClick = { menuOpen = false; selectedIds = ordered.map { it.id }.toSet() })
                                    DropdownMenuItem(text = { Text("Refresh library") }, leadingIcon = { Icon(Icons.Default.Refresh, null) }, onClick = { menuOpen = false; libraryViewModel.refresh() })
                                    DropdownMenuItem(text = { Text("Sort by") }, leadingIcon = { Icon(Icons.Default.Sort, null) }, onClick = { menuOpen = false; sortSheetOpen = true })
                                    DropdownMenuItem(text = { Text("Blacklist") }, leadingIcon = { Icon(Icons.Default.Block, null) }, onClick = { menuOpen = false; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Blacklist is not available yet.") })
                                } else {
                                    DropdownMenuItem(text = { Text("Deselect") }, leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, null) }, onClick = { menuOpen = false; selectedIds = emptySet(); selectionMode = false })
                                    DropdownMenuItem(text = { Text("Play next") }, leadingIcon = { Icon(Icons.Default.SkipNext, null) }, onClick = {
                                        menuOpen = false; selectedItems.asReversed().forEach(playback::playNext); com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("${selectedItems.size} ${if (selectedItems.size == 1) "song" else "songs"} added to play next ✓"); selectedIds = emptySet(); selectionMode = false
                                    })
                                    DropdownMenuItem(text = { Text("Add to queue") }, leadingIcon = { Icon(Icons.Default.QueueMusic, null) }, onClick = {
                                        menuOpen = false; selectedItems.forEach(playback::addToQueue); com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("${selectedItems.size} ${if (selectedItems.size == 1) "song" else "songs"} added to the queue ✓"); selectedIds = emptySet(); selectionMode = false
                                    })
                                    DropdownMenuItem(text = { Text("Add to playlist") }, leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) }, onClick = { menuOpen = false; musicPlaylistOpen = true })
                                }
                            }
                        }
                    }
                } else null,
            )
            if (selectionMode) {
                Text(
                    if (hasSelection) "${selectedIds.size} selected" else "Select songs",
                    fontFamily = AirPlayrFonts.OpenSans,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                )
            }
            if (isFolder && folderMode.equals("video", true) && !selectionMode) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ViewModeButton(Icons.Default.GridView, "Grid", videoViewMode == "grid") { videoViewMode = "grid" }
                    ViewModeButton(Icons.Default.ViewList, "List", videoViewMode == "list") { videoViewMode = "list" }
                    ViewModeButton(Icons.Default.ViewModule, "Big list", videoViewMode == "big") { videoViewMode = "big" }
                }
            }
            if (isFolder && folderMode.equals("video", true) && !selectionMode && videoViewMode == "grid") {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = airPlayrBottomContentPadding()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    gridItems(ordered, key = { it.id }) { item ->
                        VideoGridCard(item, onOpen = { onOpenVideo(item.id) }, onMore = { videoActionItem = item })
                    }
                }
            } else {
                LazyColumn(
                    state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },
                    contentPadding = PaddingValues(start = 0.dp, end = 0.dp, bottom = airPlayrBottomContentPadding()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (!snapshot.ready) {
                        item { Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.primary) } }
                    } else if (ordered.isEmpty()) {
                        item { AirPlayrSectionEmpty("No media in this folder", "The folder is indexed, but no playable media is currently available.") }
                    } else {
                        items(ordered, key = { it.id }) { item ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (isFolder && folderMode.equals("video", true)) 8.dp else 12.dp, vertical = 2.dp)
                                    .then(if (selectionMode && item.id in selectedIds) Modifier.border(2.dp, colors.primary, RoundedCornerShape(18.dp)) else Modifier)
                                    .clip(RoundedCornerShape(18.dp))
                            ) {
                                if (isFolder && folderMode.equals("video", true)) {
                                    VideoMediaRow(
                                        item = item,
                                        big = videoViewMode == "big",
                                        onOpen = { if (selectionMode) selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id else onOpenVideo(item.id) },
                                        onPlay = { onOpenVideo(item.id) },
                                        onMore = { videoActionItem = item },
                                    )
                                } else {
                                    MediaListCard(
                                        item = item,
                                        playing = item.mediaType == MediaType.AUDIO && playbackState.isPlaying && playbackState.currentId == item.id,
                                        showPlayOverlay = !musicFolder,
                                        onMore = if (musicFolder && !selectionMode) ({ selectedMusicItem = item; musicActionOpen = true }) else null,
                                    ) {
                                        if (selectionMode) {
                                            selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                                        } else if (item.mediaType == MediaType.AUDIO) playback.play(item) else onOpenVideo(item.id)
                                    }
                                }
                                if (selectionMode) {
                                    Surface(
                                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).size(30.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = if (item.id in selectedIds) colors.primary else colors.surfaceContainerHighest,
                                        shadowElevation = 0.dp,
                                    ) {
                                        Icon(if (item.id in selectedIds) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (item.id in selectedIds) colors.onPrimary else colors.textSecondary, modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (ordered.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    when {
                        folderAudio > 0 -> {
                            playback.playQueue(ordered.filter { it.mediaType == MediaType.AUDIO }, 0)
                            // Folder play intentionally stays in the current screen; the existing player/session continues in the background.
                        }
                        folderVideo > 0 -> onOpenVideo(ordered.first { it.mediaType == MediaType.VIDEO }.id)
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = airPlayrFloatingPlayBottomPadding()).size(68.dp),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 10.dp),
            ) { Icon(Icons.Default.PlayArrow, "Play folder", modifier = Modifier.size(31.dp)) }
        }
    }

    if (sortSheetOpen) {
        FolderSortSheet(
            selectedKey = sortKey,
            ascending = sortAscending,
            countLabel = if (musicFolder) "Song count" else if (folderMode.equals("video", true)) "Video count" else "Song & video count",
            onSelectKey = { key -> sortKey = key; sortSheetOpen = false },
            onAscending = { sortAscending = true },
            onDescending = { sortAscending = false },
            onDismiss = { sortSheetOpen = false },
        )
    }

    if (musicActionOpen && selectedMusicItem != null) {
        val item = selectedMusicItem!!
        ModalBottomSheet(onDismissRequest = { musicActionOpen = false; selectedMusicItem = null }, containerColor = colors.surfaceContainerLow) {
            ActionGroup {
                ActionButton("Play", Icons.Default.PlayArrow, onClick = { musicActionOpen = false; selectedMusicItem = null; playback.play(item) })
                ActionButton("Play next", Icons.Default.SkipNext, onClick = { musicActionOpen = false; selectedMusicItem = null; playback.playNext(item); com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("${item.title} added to play next ✓") })
                ActionButton("Add to queue", Icons.Default.QueueMusic, onClick = { musicActionOpen = false; selectedMusicItem = null; playback.addToQueue(item); com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("${item.title} added to the queue ✓") })
                ActionButton(if (snapshot.favorites.any { it.mediaId == item.id }) "Remove from likes" else "Like", Icons.Default.StarBorder, onClick = { musicActionOpen = false; selectedMusicItem = null; scope.launch { container?.structured?.toggleFavorite(item) } })
                ActionButton("Add to playlist", Icons.Default.PlaylistAdd, onClick = { musicActionOpen = false; musicPlaylistOpen = true })
                ActionButton("Edit song", Icons.Default.Edit, onClick = { musicActionOpen = false; editMusicItem = item; selectedMusicItem = null })
            }
        }
    }

    if (musicPlaylistOpen && container != null) {
        val playlistItems = if (selectionMode) selectedItems else listOfNotNull(selectedMusicItem)
        if (playlistItems.isNotEmpty()) {
            HistoryFavoritePlaylistSheet(container, playlistItems, onDismiss = {
                musicPlaylistOpen = false
                selectedMusicItem = null
                selectedIds = emptySet()
                selectionMode = false
            })
        }
    }

    editMusicItem?.let { item ->
        TagEditorSheet(context, item, com.airplayr.app.core.metadata.MetadataRepository(context), onClose = { editMusicItem = null }) {
            editMusicItem = null
            libraryViewModel.refresh()
        }
    }

    videoActionItem?.let { item ->
        val app = container ?: return@let
        VideoActionSheet(
            item = item,
            container = app,
            onDismiss = { videoActionItem = null },
            onRename = { videoActionItem = null; renameVideoItem = item; renameVideoName = item.filename.ifBlank { item.title } },
            onDetails = { videoActionItem = null; videoDetailsItem = item },
            onPlaylist = { videoActionItem = null; videoPlaylistItem = item },
            onDelete = { videoActionItem = null; deleteVideoItem = item },
        )
    }
    videoDetailsItem?.let { item -> VideoDetailsSheet(item, onDismiss = { videoDetailsItem = null }) }
    videoPlaylistItem?.let { item ->
        val app = container ?: return@let
        HistoryFavoritePlaylistSheet(app, listOf(item), onDismiss = { videoPlaylistItem = null })
    }
    renameVideoItem?.let { item ->
        AlertDialog(
            onDismissRequest = { renameVideoItem = null },
            title = { Text("Rename video") },
            text = { OutlinedTextField(renameVideoName, { renameVideoName = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("File name") }) },
            dismissButton = { TextButton(onClick = { renameVideoItem = null }) { Text("Cancel") } },
            confirmButton = { TextButton(onClick = {
                val name = renameVideoName.trim()
                if (name.isNotBlank()) {
                    val extension = item.filename.substringAfterLast('.', "").takeIf { it.isNotBlank() }
                    val finalName = if (extension != null && !name.substringAfterLast('.', "").equals(extension, true)) "$name.$extension" else name
                    renameVideoItem = null
                    renameVideo(item, finalName)
                }
            }) { Text("Rename") } },
        )
    }
    deleteVideoItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteVideoItem = null },
            title = { Text("Delete video?") },
            text = { Text("Delete ${item.title.ifBlank { item.filename }} from this device? Android may ask you to confirm.") },
            dismissButton = { TextButton(onClick = { deleteVideoItem = null }) { Text("Cancel") } },
            confirmButton = { TextButton(onClick = { deleteVideoItem = null; deleteVideo(item) }) { Text("Delete", color = colors.error) } },
        )
    }
}


@Composable
private fun ViewModeButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    IconButton(onClick = onClick) {
        Icon(icon, label, tint = if (selected) colors.primary else colors.textSecondary)
    }
}

@Composable
private fun VideoMediaRow(item: MediaItem, big: Boolean, onOpen: () -> Unit, onPlay: () -> Unit, onMore: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = if (big) 12.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AirPlayrMediaArtwork(
            artworkUri = item.thumbnailUri ?: item.artworkUri,
            modifier = Modifier.size(if (big) 132.dp else 78.dp).clip(RoundedCornerShape(if (big) 18.dp else 14.dp)),
            radius = if (big) 18.dp else 14.dp,
            title = item.title,
            mediaType = MediaType.VIDEO,
            fallbackIcon = AirPlayrIcons.Video,
            playing = false,
            onPlay = null,
            onMore = null,
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(item.title.ifBlank { item.filename }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = if (big) 18.sp else 16.sp, color = colors.textPrimary, maxLines = if (big) 2 else 1, overflow = TextOverflow.Ellipsis)
            Text(formatTime(item.durationMs), fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 3.dp))
        }
        IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, "Play", tint = colors.primary) }
        IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, "Video options", tint = colors.primary) }
    }
}

@Composable
private fun VideoGridCard(item: MediaItem, onOpen: () -> Unit, onMore: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(18.dp))) {
            AirPlayrMediaArtwork(item.thumbnailUri ?: item.artworkUri, Modifier.fillMaxSize(), 18.dp, item.title, MediaType.VIDEO, AirPlayrIcons.Video, playing = false, onPlay = null, onMore = null)
            Row(Modifier.align(Alignment.TopEnd).padding(4.dp)) { IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, "Video options", tint = colors.onSurface) } }
            IconButton(onClick = onOpen, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) { Icon(Icons.Default.PlayArrow, "Play", tint = colors.onSurface) }
        }
        Text(item.title.ifBlank { item.filename }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 16.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VideoActionSheet(
    item: MediaItem,
    container: AppContainer,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDetails: () -> Unit,
    onPlaylist: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AirPlayrMediaArtwork(item.thumbnailUri ?: item.artworkUri, Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)), 14.dp, item.title, MediaType.VIDEO, AirPlayrIcons.Video, playing = false, onPlay = null, onMore = null)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(item.title.ifBlank { item.filename }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Video", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textSecondary)
                }
            }
            ActionGroup {
                ActionButton("Move to vault", Icons.Default.BookmarkAdd, { AppFeedback.showSnackbar("Move to vault is not available yet.") })
                ActionButton("Play in background", Icons.Default.PlayArrow, { AppFeedback.showSnackbar("Play in background is not available yet.") })
                ActionButton("Add to playlist", Icons.Default.PlaylistAdd, onPlaylist)
            }
            ActionGroup {
                ActionButton("Rename", Icons.Default.Edit, onRename)
                ActionButton("Details", Icons.Default.Info, onDetails)
                ActionButton("Share", Icons.Default.Share, {
                    val uri = android.net.Uri.parse(item.uri)
                    val send = Intent(Intent.ACTION_SEND).apply { type = item.mimeType ?: "video/*"; putExtra(Intent.EXTRA_STREAM, uri); clipData = android.content.ClipData.newRawUri(item.title, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    runCatching { container.appContext.startActivity(Intent.createChooser(send, "Share ${item.title}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    onDismiss()
                })
                ActionButton("Delete", Icons.Default.Delete, onDelete, destructive = true)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VideoDetailsSheet(item: MediaItem, onDismiss: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            AirPlayrMediaArtwork(item.thumbnailUri ?: item.artworkUri, Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)), 20.dp, item.title, MediaType.VIDEO, AirPlayrIcons.Video, playing = false, onPlay = null, onMore = null)
            Text(item.title.ifBlank { item.filename }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 25.sp, color = colors.textPrimary)
            VideoDetailLine("Resolution", if (item.width > 0 && item.height > 0) "${item.width} × ${item.height}" else "Unknown")
            VideoDetailLine("Duration", formatTime(item.durationMs))
            VideoDetailLine("File size", formatBytes(item.sizeBytes))
            VideoDetailLine("Format", item.mimeType ?: item.filename.substringAfterLast('.', "Unknown").uppercase())
            VideoDetailLine("Date added", formatDate(item.dateAdded))
            VideoDetailLine("Date modified", formatDate(item.dateModified))
            VideoDetailLine("Location", item.path ?: item.relativePath ?: item.folder ?: item.uri)
        }
    }
}

@Composable
private fun VideoDetailLine(label: String, value: String) {
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, color = colors.textSecondary)
        Text(value, fontFamily = AirPlayrFonts.OpenSans, fontSize = 14.sp, color = colors.textPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble(); var index = 0
    while (value >= 1024 && index < units.lastIndex) { value /= 1024; index++ }
    return if (index == 0) "${bytes} ${units[index]}" else "%.1f %s".format(value, units[index])
}

private fun formatDate(seconds: Long): String = if (seconds <= 0L) "Unknown" else java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(seconds * 1000L))

@Composable
fun RecentScreen(
    type: String,
    viewModel: LibraryViewModel,
    playback: PlaybackController,
    onOpen: (String) -> Unit,
    container: AppContainer? = null,
    onOpenPlayer: () -> Unit = {},
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val audio = snapshot.audio
    val video = snapshot.video
    val historyRows = snapshot.history
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    val data = when (type) {
        "songs" -> audio.sortedByDescending { it.dateAdded }.take(100)
        "albums" -> historyRows.filter { it.mediaType == MediaType.AUDIO.value }.mapNotNull { h -> audio.firstOrNull { it.id == h.mediaId } }
            .groupBy { it.album ?: "Unknown album" }.values.mapNotNull { it.firstOrNull() }.take(100)
        "artists" -> historyRows.filter { it.mediaType == MediaType.AUDIO.value }.mapNotNull { h -> audio.firstOrNull { it.id == h.mediaId } }
            .groupBy { it.artist ?: "Unknown artist" }.values.mapNotNull { it.firstOrNull() }.take(100)
        "videos-added" -> video.sortedByDescending { it.dateAdded }.take(100)
        else -> historyRows.filter { it.mediaType == MediaType.VIDEO.value }.mapNotNull { h -> video.firstOrNull { it.id == h.mediaId } }.take(100)
    }
    val audioItems = remember(data, type, audio) {
        when (type) {
            "albums" -> {
                val albumNames = data.map { it.album ?: "Unknown album" }.toSet()
                audio.filter { (it.album ?: "Unknown album") in albumNames }
            }
            "artists" -> {
                val artistNames = data.map { it.artist ?: "Unknown artist" }.toSet()
                audio.filter { (it.artist ?: "Unknown artist") in artistNames }
            }
            else -> data.filter { it.mediaType == MediaType.AUDIO }
        }
    }
    val title = when (type) {
        "songs" -> "Recently added songs"
        "albums" -> "Recently played albums"
        "artists" -> "Recent Artists"
        "videos-added" -> "Recently added videos"
        else -> "Recently played videos"
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ListTopBar(title, "${data.size} local items")
            if (!snapshot.ready) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalAirPlayrColors.current.primary) }
            else if (data.isEmpty()) EmptyNativeState("Nothing here yet", "AirPlayr will build this section as your library and listening history grows.")
            else LazyColumn(state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = airPlayrBottomContentPadding()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(data, key = { it.id }) { item ->
                    MediaListCard(item, playing = playbackState.isPlaying && playbackState.currentId == item.id) {
                        if (item.mediaType == MediaType.AUDIO) playback.play(item) else onOpen(item.id)
                    }
                }
            }
        }
        if (data.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    if (audioItems.isNotEmpty()) {
                        playback.playQueue(audioItems, 0)
                        onOpenPlayer()
                    } else {
                        data.firstOrNull()?.let { onOpen(it.id) }
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = airPlayrFloatingPlayBottomPadding()).size(68.dp),
                containerColor = com.airplayr.app.ui.theme.LocalAirPlayrColors.current.primary,
                contentColor = com.airplayr.app.ui.theme.LocalAirPlayrColors.current.onPrimary,
                shape = RoundedCornerShape(22.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 10.dp),
            ) {
                Icon(Icons.Default.PlayArrow, "Play", modifier = Modifier.size(31.dp))
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyPlaylistsScreen(container: AppContainer, libraryViewModel: LibraryViewModel, playback: PlaybackController, onOpen: (String) -> Unit, onBack: () -> Unit = {}) {
    val colors = LocalAirPlayrColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val playlists = snapshot.playlists
    val library = snapshot.audio
    val libraryReady = snapshot.ready
    var selected by remember { mutableStateOf<com.airplayr.app.core.data.db.MusicPlaylistEntity?>(null) }
    var headerMenu by remember { mutableStateOf(false) }
    var fabOpen by remember { mutableStateOf(false) }
    var dialogMode by remember { mutableStateOf("create") }
    var dialogName by remember { mutableStateOf("") }
    var dialogOpen by remember { mutableStateOf(false) }
    var targetPicker by remember { mutableStateOf(false) }
    var targetSource by remember { mutableStateOf<com.airplayr.app.core.data.db.MusicPlaylistEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<com.airplayr.app.core.data.db.MusicPlaylistEntity?>(null) }
    val artworkLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        val p = selected
        if (uri != null && p != null) scope.launch { container.structured.setPlaylistArtwork(p.id, uri.toString()); selected = null; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist artwork updated.") }
    }
    var importPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }.toList()
                val normalized = { value: String -> value.lowercase().replace("\\", "/") }
                val matched = lines.mapNotNull { line -> library.firstOrNull { song -> val key=normalized(line); val u=normalized(song.uri); val f=normalized(song.filename.ifBlank { song.title }); key==u || key.endsWith("/$f") || key.endsWith(f) } }.distinctBy { it.id }
                if (matched.isEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("No matching songs from this device were found.") else { val p=container.structured.createPlaylist("Imported playlist"); matched.forEach { container.structured.addToPlaylist(p.id,it.id) }; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist imported: ${matched.size} songs.") }
            }.onFailure { com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("The playlist could not be imported.") }
        }
    }
    var exportPlaylist by remember { mutableStateOf<com.airplayr.app.core.data.db.MusicPlaylistEntity?>(null) }
    val idMap by produceState(initialValue = emptyMap<String, List<MediaItem>>(), playlists, library) {
        value = playlists.associate { p -> p.id to runCatching { container.structured.playlistIds(p.id).mapNotNull { id -> library.firstOrNull { it.id == id } } }.getOrDefault(emptyList()) }
    }
    fun itemsOf(p: com.airplayr.app.core.data.db.MusicPlaylistEntity) = idMap[p.id].orEmpty()

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        val p=exportPlaylist; exportPlaylist=null
        if (uri != null && p != null) scope.launch { runCatching { val content="#EXTM3U\n"+itemsOf(p).joinToString("\n") { "#EXTINF:${it.durationMs/1000},${it.title} - ${it.artist.orEmpty()}\n${it.uri}" }+"\n"; context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist saved.") }.onFailure { com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("The playlist could not be saved.") } }
    }
    fun createOrRename() {
        val n = dialogName.trim().ifBlank { "New playlist" }
        scope.launch {
            if (dialogMode == "rename" && selected != null) { container.structured.renamePlaylist(selected!!.id, n); com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist renamed.") }
            else { val created = container.structured.createPlaylist(n); com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist created."); onOpen(created.id) }
            dialogOpen = false; selected = null; dialogName = ""
        }
    }
    fun playPlaylist(p: com.airplayr.app.core.data.db.MusicPlaylistEntity, shuffle: Boolean = false) { val rows = itemsOf(p); if (rows.isEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("This playlist has no songs yet.") else { val ordered = if (shuffle) rows.shuffled() else rows; playback.playQueue(ordered, 0); selected = null } }
    fun queuePlaylist(p: com.airplayr.app.core.data.db.MusicPlaylistEntity, next: Boolean) { val rows = itemsOf(p); if (rows.isEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("This playlist has no songs yet.") else { rows.forEach { if (next) playback.playNext(it) else playback.addToQueue(it) }; selected = null; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("${rows.size} songs ${if (next) "added as next" else "added to the queue"}.") } }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary) }
                    Column(Modifier.weight(1f)) { Text("AIRPLAYR · YOUR ARCHIVE", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 11.sp, letterSpacing = 1.2.sp, color = colors.textPrimary); Text("My playlists", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 30.sp, color = colors.textPrimary) }
                    IconButton(onClick = { headerMenu = true }) { Icon(Icons.Default.MoreVert, "My playlists menu", tint = colors.primary) }
                }
            }
            item {
                AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 22.dp), shape = RoundedCornerShape(30.dp), containerColor = colors.primaryContainer, elevation = 5.dp) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(54.dp), shape = androidx.compose.foundation.shape.CircleShape, color = colors.primary) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.QueueMusic, null, tint = colors.onPrimary) } }; Column(Modifier.padding(start = 12.dp)) { Text("Your archive, your rules", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 21.sp, color = colors.textPrimary); Text("${playlists.size} playlists · ${idMap.values.sumOf { it.size }} songs", fontSize = 12.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp)) } }
                }
            }
            item { Text("Your playlists", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 24.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) }
            if (!libraryReady) item { Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.primary) } }
            else if (playlists.isEmpty()) item { AirPlayrSectionEmpty("No playlists yet", "Create a playlist and your archive will appear here.") }
            else items(playlists, key = { it.id }) { p ->
                val rows = itemsOf(p)
                AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 22.dp), shape = RoundedCornerShape(24.dp), containerColor = colors.surfaceContainer, elevation = 5.dp) {
                    Column(Modifier.clickable { onOpen(p.id) }.padding(10.dp)) {
                        Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceVariant)) {
                            if (p.artworkUri != null) com.airplayr.app.ui.components.Artwork(p.artworkUri, Modifier.fillMaxSize(), 20.dp, p.name)
                            else if (rows.isNotEmpty()) Row(Modifier.fillMaxSize()) { rows.take(4).forEach { r -> com.airplayr.app.ui.components.Artwork(r.artworkUri, Modifier.weight(1f).fillMaxHeight(), 0.dp, r.title) } }
                            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.QueueMusic, null, tint = colors.primary, modifier = Modifier.size(50.dp)) }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(p.name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${rows.size} ${if (rows.size == 1) "song" else "songs"}", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp)) }; IconButton(onClick = { selected = p }) { Icon(Icons.Default.MoreVert, "Playlist options", tint = colors.primary) } }
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 24.dp), horizontalAlignment = Alignment.End) {
            if (fabOpen) Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
                SmallFabOption("External playlist", Icons.Default.Public) { fabOpen = false; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("External playlist URLs are not connected to AirPlayr yet.") }
                SmallFabOption("Import playlist (.m3u)", Icons.Default.FileDownload) { fabOpen = false; importPickerLauncher.launch(arrayOf("audio/x-mpegurl","application/x-mpegURL","text/plain","*/*")) }
                SmallFabOption("Create playlist", Icons.Default.List) { fabOpen = false; dialogMode = "create"; dialogName = ""; dialogOpen = true }
            }
            Spacer(Modifier.height(10.dp))
            FloatingActionButton(onClick = { fabOpen = !fabOpen }, containerColor = if (fabOpen) colors.surfaceContainerHigh else colors.primary, contentColor = if (fabOpen) colors.primary else colors.onPrimary, modifier = Modifier.size(88.dp), shape = RoundedCornerShape(28.dp)) { Icon(if (fabOpen) Icons.Default.Close else Icons.Default.Add, if (fabOpen) "Close" else "Add", modifier = Modifier.size(34.dp)) }
        }
        if (headerMenu) ModalBottomSheet(onDismissRequest = { headerMenu = false }, containerColor = colors.surfaceContainerLow) {
            ActionGroup {
                ActionButton("Shuffle all playlist items", Icons.Default.Shuffle, onClick = { val all=playlists.flatMap { itemsOf(it) }.shuffled(); if(all.isEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("There are no songs in your playlists yet.") else playback.playQueue(all,0); headerMenu=false })
                ActionButton("Add all items to queue", Icons.Default.QueueMusic, onClick = { val all=playlists.flatMap { itemsOf(it) }; all.forEach(playback::addToQueue); headerMenu=false; if(all.isNotEmpty()) com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Added ${all.size} songs to the playing queue.") })
                ActionButton("Refresh playlists", Icons.Default.Refresh, onClick = { headerMenu = false; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlists refreshed.") })
            }
        }
        selected?.let { p ->
            ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = colors.surfaceContainerLow) {
                ActionGroup {
                    ActionButton("Play", Icons.Default.PlayArrow, onClick = { playPlaylist(p) })
                    ActionButton("Play next", Icons.Default.SkipNext, onClick = { queuePlaylist(p, true) })
                    ActionButton("Add to queue", Icons.Default.QueueMusic, onClick = { queuePlaylist(p, false) })
                    ActionButton("Add to playlist", Icons.Default.PlaylistAdd, onClick = { targetSource=p; targetPicker = true; selected=null })
                    ActionButton("Choose artwork", Icons.Default.Image, onClick = { artworkLauncher.launch("image/*") })
                    ActionButton("Rename", Icons.Default.Edit, onClick = { dialogMode = "rename"; dialogName = p.name; dialogOpen = true })
                    ActionButton("Save as file", Icons.Default.Save, onClick = { exportPlaylist=p; selected=null; exportLauncher.launch("${p.name}.m3u") })
                    ActionButton("Share playlist", Icons.Default.Share, onClick = { val text = "${p.name}\n\n${itemsOf(p).joinToString("\n") { it.title + (it.artist?.let { a -> " · $a" } ?: "") }}"; val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text) }; context.startActivity(android.content.Intent.createChooser(send, "Share playlist").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)); selected = null })
                    ActionButton("Delete playlist", Icons.Default.Delete, onClick = { deleteTarget = p; selected = null }, destructive = true)
                }
            }
        }
        if (targetPicker) ModalBottomSheet(onDismissRequest = { targetPicker = false }, containerColor = colors.surfaceContainerLow) { ActionGroup { playlists.filter { it.id != selected?.id }.forEach { target -> ActionButton(target.name, Icons.Default.List, onClick = { val ids = targetSource?.let { itemsOf(it).map { m -> m.id } }.orEmpty(); scope.launch { ids.forEach { container.structured.addToPlaylist(target.id, it) }; targetPicker = false; targetSource = null; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Added to ${target.name}.") } }) } } }
        if (dialogOpen) AlertDialog(onDismissRequest = { dialogOpen = false }, title = { Text(if (dialogMode == "rename") "Rename playlist" else "Create playlist") }, text = { OutlinedTextField(dialogName, { dialogName = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) }, dismissButton = { TextButton(onClick = { dialogOpen = false }) { Text("Cancel") } }, confirmButton = { TextButton(onClick = { createOrRename() }) { Text(if (dialogMode == "rename") "Save" else "OK") } })
        deleteTarget?.let { p -> AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete playlist?") }, text = { Text("“${p.name}” will be removed. Your media stays on the device.") }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }, confirmButton = { TextButton(onClick = { scope.launch { container.structured.deletePlaylist(p.id); deleteTarget = null; com.airplayr.app.ui.feedback.AppFeedback.showSnackbar("Playlist deleted.") } }) { Text("Delete", color = colors.error) } }) }
    }
}

@Composable private fun SmallFabOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { val c=LocalAirPlayrColors.current; Row(Modifier.clickable(onClick=onClick), verticalAlignment=Alignment.CenterVertically) { Surface(shape=RoundedCornerShape(12.dp), color=c.surfaceContainerHigh) { Text(label, color=c.textPrimary, fontFamily=AirPlayrFonts.SmoochSans, fontSize=15.sp, modifier=Modifier.padding(horizontal=15.dp,vertical=11.dp)) }; Spacer(Modifier.width(10.dp)); FloatingActionButton(onClick=onClick, modifier=Modifier.size(58.dp), containerColor=c.surfaceContainerHigh, contentColor=c.primary, shape=RoundedCornerShape(18.dp)) { Icon(icon,null,modifier=Modifier.size(22.dp)) } } }

@Composable
fun PlaylistDetailScreen(
    container: AppContainer,
    libraryViewModel: LibraryViewModel,
    id: String,
    playback: PlaybackController,
    onOpenPlayer: () -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val library = snapshot.audio
    var name by remember { mutableStateOf("Playlist") }
    var ids by remember { mutableStateOf<List<String>>(emptyList()) }
    var idsLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        val p = container.database.userDataDao().playlist(id)
        name = p?.name ?: "Playlist"
        ids = container.structured.playlistIds(id)
        idsLoaded = true
    }
    val items = library.filter { it.id in ids }.sortedBy { ids.indexOf(it.id) }
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ListTopBar(name, "${items.size} songs")
            LazyColumn(state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = airPlayrBottomContentPadding()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Button(onClick = { playback.playQueue(items); if (items.isNotEmpty()) onOpenPlayer() }, enabled = items.isNotEmpty()) { Text("Play playlist") } }
                if (!snapshot.ready || !idsLoaded) item { Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.primary) } }
                else if (items.isEmpty()) item { AirPlayrSectionEmpty("Playlist is empty", "Add songs to this playlist to see them here.") }
                else items(items, key = { it.id }) { item -> MediaListCard(item, playing = playbackState.isPlaying && playbackState.currentId == item.id) { playback.play(item) } }
            }
        }
        if (items.isNotEmpty()) {
            FloatingActionButton(
                onClick = { playback.playQueue(items); onOpenPlayer() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = airPlayrFloatingPlayBottomPadding())
                    .size(68.dp),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 10.dp),
            ) {
                Icon(Icons.Default.PlayArrow, "Play playlist", modifier = Modifier.size(31.dp))
            }
        }
    }
}

@Composable
fun EmptyNativeState(title: String, message: String) = AirPlayrSectionEmpty(title, message)

@Composable
private fun MediaListCard(
    item: MediaItem,
    playing: Boolean = false,
    showPlayOverlay: Boolean = true,
    onMore: (() -> Unit)? = null,
    onPlay: () -> Unit,
) {
    AirPlayrMediaRow(item, onOpen = onPlay, onMore = onMore, playing = playing, compact = false, showPlayOverlay = showPlayOverlay)
}

@Composable
private fun ListTopBar(title: String, subtitle: String) = AirPlayrScreenHeader(title, subtitle)

@Composable
fun BrowseIndexScreen(
    title: String,
    type: MediaType,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    onOpen: (String) -> Unit,
) {
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val source = if (type == MediaType.AUDIO) snapshot.audio else snapshot.video
    val keyer: (MediaItem) -> String = when (title.lowercase()) {
        "albums" -> { item -> item.album?.takeIf { it.isNotBlank() } ?: "Unknown album" }
        "artists" -> { item -> item.artist?.takeIf { it.isNotBlank() } ?: "Unknown artist" }
        "genres" -> { item -> item.genre?.takeIf { it.isNotBlank() } ?: "Unknown genre" }
        "folders" -> { item -> item.folder?.takeIf { it.isNotBlank() } ?: item.relativePath ?: "Unknown folder" }
        else -> { item -> item.title }
    }
    val groups = source.groupBy(keyer).toList().sortedBy { it.first.lowercase() }
    Column(Modifier.fillMaxSize()) {
        ListTopBar(title, "${groups.size} ${title.lowercase()}")
        LazyColumn(state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = airPlayrBottomContentPadding()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!snapshot.ready) item { Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalAirPlayrColors.current.primary) } }
            items(groups, key = { it.first }) { (name, items) ->
                AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 8.dp), shape = RoundedCornerShape(22.dp), containerColor = LocalAirPlayrColors.current.surfaceSolid, elevation = 5.dp, onClick = { onOpen(java.net.URLEncoder.encode(name, "UTF-8")) }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (title.lowercase()) {
                            "albums" -> Icons.Default.Album
                            "artists" -> Icons.Default.Person
                            "genres" -> Icons.Default.LocalOffer
                            "folders" -> Icons.Default.Folder
                            else -> AirPlayrIcons.Music
                        }
                        Icon(icon, null, tint = LocalAirPlayrColors.current.primary, modifier = Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = LocalAirPlayrColors.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${items.size} items", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = LocalAirPlayrColors.current.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
