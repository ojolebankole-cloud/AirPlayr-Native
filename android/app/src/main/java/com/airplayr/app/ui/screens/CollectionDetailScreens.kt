@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.airplayr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.airPlayrBottomContentPadding
import com.airplayr.app.ui.airPlayrFloatingPlayBottomPadding
import com.airplayr.app.ui.components.AirPlayrAlbumRailCard
import com.airplayr.app.ui.components.AirPlayrCard
import com.airplayr.app.ui.components.AirPlayrMediaRow
import com.airplayr.app.ui.components.AirPlayrMediaMenuAction
import com.airplayr.app.ui.components.AirPlayrPremiumMediaActionSheet
import com.airplayr.app.ui.components.ActionButton
import com.airplayr.app.ui.components.ActionGroup
import com.airplayr.app.ui.components.AirPlayrScreenHeader
import com.airplayr.app.ui.components.AirPlayrLongListFastScrollHandle
import com.airplayr.app.ui.components.AirPlayrLongGridFastScrollHandle
import com.airplayr.app.ui.components.Artwork
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.feedback.AppFeedback
import java.net.URLEncoder
import java.util.Locale

private fun encodeRoute(value: String) = URLEncoder.encode(value, "UTF-8")

private fun playSequence(playback: PlaybackController, items: List<MediaItem>, shuffle: Boolean = false) {
    val songs = items.filter { it.mediaType == MediaType.AUDIO }.distinctBy { it.id }
    if (songs.isEmpty()) return
    playback.playCollection(if (shuffle) songs.shuffled() else songs, 0)
}

private fun playSequenceAt(playback: PlaybackController, items: List<MediaItem>, item: MediaItem) {
    val songs = items.filter { it.mediaType == MediaType.AUDIO }.distinctBy { it.id }
    if (songs.isEmpty()) return
    val index = songs.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
    playback.playCollection(songs, index)
}

private fun durationLabel(items: List<MediaItem>): String {
    val seconds = items.sumOf { it.durationMs.coerceAtLeast(0L) } / 1000L
    return when {
        seconds < 60L -> "${seconds}s"
        seconds < 3600L -> "${seconds / 60L}m"
        else -> "${seconds / 3600L}h ${(seconds % 3600L) / 60L}m"
    }
}

@Composable
private fun CollectionHeroBackdrop(
    artworkUri: String?,
    title: String,
    height: Dp = 330.dp,
) {
    val colors = LocalAirPlayrColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(colors.surfaceContainerLow),
    ) {
        Artwork(
            artworkUri,
            Modifier
                .fillMaxSize()
                .blur(28.dp)
                .alpha(.48f),
            0.dp,
            title,
            MediaType.AUDIO,
            AirPlayrIcons.AlbumDisc,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.background.copy(alpha = .08f),
                            colors.background.copy(alpha = .20f),
                            colors.background.copy(alpha = .96f),
                        )
                    )
                )
        )
    }
}

@Composable
private fun CollectionTopActions(
    onBack: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary, modifier = Modifier.size(26.dp))
        }
        IconButton(
            onClick = onMore,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
        ) {
            Icon(Icons.Default.MoreVert, "More", tint = colors.primary, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun CollectionPlayButtons(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    playLabel: String = "Play all",
) {
    val colors = LocalAirPlayrColors.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onPlay,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text(playLabel, fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 14.sp)
        }
        OutlinedButton(
            onClick = onShuffle,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            border = null,
        ) {
            Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text("Shuffle", fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun CollectionSongRow(
    index: Int,
    item: MediaItem,
    playing: Boolean,
    onPlay: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            index.toString(),
            fontFamily = AirPlayrFonts.OpenSansBold,
            fontSize = 12.sp,
            color = colors.textSecondary,
            modifier = Modifier.width(26.dp),
            textAlign = TextAlign.Center,
        )
        Box(Modifier.weight(1f)) {
            AirPlayrMediaRow(
                item = item,
                onOpen = onPlay,
                onMore = onMore,
                playing = playing,
                compact = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionActionSheet(
    title: String,
    representative: MediaItem,
    items: List<MediaItem>,
    playback: PlaybackController,
    playItems: List<MediaItem> = items,
    onOpen: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val songs = items.filter { it.mediaType == MediaType.AUDIO }.distinctBy { it.id }
    val playQueueItems = playItems.filter { it.mediaType == MediaType.AUDIO }.distinctBy { it.id }
    AirPlayrPremiumMediaActionSheet(
        item = representative.copy(title = title),
        playing = false,
        favorite = false,
        groups = listOf(
            listOf(
                AirPlayrMediaMenuAction(
                    "Play",
                    Icons.Default.PlayArrow,
                    { onDismiss(); playSequence(playback, playQueueItems) },
                    subtitle = "${playQueueItems.size} songs",
                ),
                AirPlayrMediaMenuAction(
                    "Shuffle",
                    Icons.Default.Shuffle,
                    { onDismiss(); playSequence(playback, playQueueItems, true) },
                ),
                AirPlayrMediaMenuAction(
                    "Add to queue",
                    Icons.Default.QueueMusic,
                    { onDismiss(); songs.forEach(playback::addToQueue) },
                ),
            ),
            listOfNotNull(onOpen?.let { open -> AirPlayrMediaMenuAction("Open", Icons.Default.Album, { onDismiss(); open() }) }),
        ),
        onDismiss = onDismiss,
    )
}

@Composable
fun AlbumDetailScreen(
    albumName: String,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val songs = remember(snapshot.audio, albumName) {
        snapshot.audio.filter { it.album.equals(albumName, true) }
            .sortedWith(compareBy<MediaItem> { it.title.lowercase(Locale.ROOT) }.thenBy { it.dateAdded })
    }
    val artist = songs.firstOrNull()?.albumArtist?.takeIf { it.isNotBlank() }
        ?: songs.firstOrNull()?.artist?.takeIf { it.isNotBlank() }
        ?: "Unknown artist"
    val artistSongs = remember(snapshot.audio, artist) {
        snapshot.audio.filter {
            it.artist.equals(artist, true) || it.albumArtist.equals(artist, true)
        }
    }
    val moreAlbums = remember(artistSongs, albumName) {
        artistSongs.groupBy { it.album?.takeIf(String::isNotBlank) ?: return@groupBy "" }
            .filterKeys { it.isNotBlank() && !it.equals(albumName, true) }
            .toList()
            .take(8)
    }
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var songMenu by remember { mutableStateOf<MediaItem?>(null) }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(390.dp)) {
                    CollectionHeroBackdrop(songs.firstOrNull()?.artworkUri, albumName)
                    CollectionTopActions(onBack = onBack, onMore = { menuOpen = true })
                    if (songs.isNotEmpty()) {
                        Artwork(
                            songs.first().artworkUri,
                            Modifier
                                .size(216.dp)
                                .align(Alignment.BottomCenter)
                                .offset(y = 18.dp)
                                .shadow(18.dp, RoundedCornerShape(28.dp))
                                .clip(RoundedCornerShape(28.dp)),
                            28.dp,
                            albumName,
                            MediaType.AUDIO,
                            AirPlayrIcons.AlbumDisc,
                        )
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 2.dp)) {
                    Text(
                        albumName,
                        fontFamily = AirPlayrFonts.SmoochSans,
                        fontSize = 35.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${songs.size} songs · ${durationLabel(songs)}",
                        fontFamily = AirPlayrFonts.OpenSans,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                    Surface(
                        onClick = { if (artist != "Unknown artist") onOpenArtist(artist) },
                        enabled = artist != "Unknown artist",
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceContainerLow,
                        modifier = Modifier.padding(top = 13.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Artwork(songs.firstOrNull()?.artworkUri, Modifier.size(38.dp), 19.dp, artist, MediaType.AUDIO, AirPlayrIcons.Artist)
                            Column(Modifier.padding(start = 9.dp)) {
                                Text("ARTIST", fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 8.sp, letterSpacing = 1.1.sp, color = colors.textSecondary)
                                Text(artist, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 16.sp, color = colors.textPrimary)
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(AirPlayrIcons.Next, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    CollectionPlayButtons(
                        onPlay = { playSequence(playback, songs) },
                        onShuffle = { playSequence(playback, songs, true) },
                    )
                }
            }
            item {
                Text(
                    "Songs",
                    fontFamily = AirPlayrFonts.SmoochSans,
                    fontSize = 25.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 2.dp),
                )
            }
            items(songs, key = { it.id }) { song ->
                CollectionSongRow(
                    index = songs.indexOf(song) + 1,
                    item = song,
                    playing = playbackState.currentId == song.id && playbackState.isPlaying,
                    onPlay = { playSequenceAt(playback, songs, song) },
                    onMore = { songMenu = song },
                )
            }
            if (moreAlbums.isNotEmpty()) {
                item {
                    Text(
                        "More from $artist",
                        fontFamily = AirPlayrFonts.SmoochSans,
                        fontSize = 25.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
                    )
                }
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        moreAlbums.forEach { (name, group) ->
                            AirPlayrAlbumRailCard(
                                artworkUri = group.firstOrNull()?.artworkUri,
                                albumTitle = name,
                                artist = artist,
                                onOpen = { onOpenAlbum(name) },
                                onPlay = { playSequence(playback, group) },
                                size = 152.dp,
                            )
                        }
                    }
                }
            }
        }

        if (songs.isNotEmpty()) {
            FloatingActionButton(
                onClick = { playSequence(playback, songs) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = airPlayrFloatingPlayBottomPadding()).size(66.dp),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp),
            ) { Icon(Icons.Default.PlayArrow, "Play album", modifier = Modifier.size(30.dp)) }
        }
    }

    if (menuOpen && songs.isNotEmpty()) {
        CollectionActionSheet(
            title = albumName,
            representative = songs.first(),
            items = songs,
            playback = playback,
            onOpen = null,
            onDismiss = { menuOpen = false },
        )
    }
    songMenu?.let { song ->
        var favorite by remember(song.id) { mutableStateOf(false) }
        AirPlayrPremiumMediaActionSheet(
            item = song,
            playing = playbackState.currentId == song.id && playbackState.isPlaying,
            favorite = favorite,
            groups = listOf(
                listOf(
                    AirPlayrMediaMenuAction("Play now", Icons.Default.PlayArrow, { playSequenceAt(playback, songs, song); songMenu = null }),
                    AirPlayrMediaMenuAction("Play next", Icons.Default.SkipNext, { playback.playNext(song); songMenu = null }),
                    AirPlayrMediaMenuAction("Add to queue", Icons.Default.QueueMusic, { playback.addToQueue(song); songMenu = null }),
                )
            ),
            onFavorite = { favorite = !favorite },
            onDismiss = { songMenu = null },
        )
    }
}

@Composable
fun ArtistDetailScreen(
    artistName: String,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val context = LocalContext.current
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    val songs = remember(snapshot.audio, artistName) {
        snapshot.audio.filter {
            it.artist.equals(artistName, true) || it.albumArtist.equals(artistName, true)
        }.sortedBy { it.title.lowercase(Locale.ROOT) }
    }
    val albumGroups = remember(songs) {
        songs.groupBy { it.album?.trim().takeIf { value -> !value.isNullOrBlank() } ?: "" }
            .filterKeys { it.isNotBlank() }
            .toList()
    }
    val singleGroups = remember(songs) {
        songs.filter { it.album.isNullOrBlank() }
    }
    var menuOpen by remember { mutableStateOf(false) }
    var pinned by remember(artistName) { mutableStateOf(false) }
    var songMenu by remember { mutableStateOf<MediaItem?>(null) }
    var showAllSongs by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(artistName) {
        pinned = context.getSharedPreferences("airplayr_collection_pins", 0).getBoolean("artist:$artistName", false)
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(370.dp)) {
                    CollectionHeroBackdrop(songs.firstOrNull()?.artworkUri, artistName, 320.dp)
                    CollectionTopActions(onBack = onBack, onMore = { menuOpen = true })
                    if (songs.isNotEmpty()) {
                        Artwork(
                            songs.first().artworkUri,
                            Modifier
                                .size(210.dp)
                                .align(Alignment.BottomCenter)
                                .offset(y = 20.dp)
                                .shadow(18.dp, RoundedCornerShape(105.dp))
                                .clip(CircleShape),
                            105.dp,
                            artistName,
                            MediaType.AUDIO,
                            AirPlayrIcons.Artist,
                        )
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
                    Text(artistName, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 37.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text("${albumGroups.size} albums · ${songs.size} songs", fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 5.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CollectionPlayButtons(
                            onPlay = { playSequence(playback, songs) },
                            onShuffle = { playSequence(playback, songs, true) },
                        )
                        IconButton(
                            onClick = {
                                pinned = !pinned
                                context.getSharedPreferences("airplayr_collection_pins", 0).edit().putBoolean("artist:$artistName", pinned).apply()
                                AppFeedback.showSnackbar(if (pinned) "Artist pinned" else "Artist unpinned")
                            },
                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainerLow),
                        ) {
                            Icon(Icons.Default.PinDrop, "Pin artist", tint = if (pinned) colors.primary else colors.textSecondary)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Songs", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 25.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    if (songs.size > 8) {
                        TextButton(onClick = { showAllSongs = !showAllSongs }) {
                            Text(if (showAllSongs) "SHOW LESS" else "SEE ALL", fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 10.sp, color = colors.primary)
                        }
                    }
                }
            }
            items(if (showAllSongs) songs else songs.take(8), key = { it.id }) { song ->
                CollectionSongRow(
                    index = songs.indexOf(song) + 1,
                    item = song,
                    playing = playbackState.currentId == song.id && playbackState.isPlaying,
                    onPlay = { playSequenceAt(playback, songs, song) },
                    onMore = { songMenu = song },
                )
            }
            if (albumGroups.isNotEmpty()) {
                item { Text("Albums", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 25.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(((albumGroups.size.coerceAtMost(6) + 1) / 2 * 225).dp),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        gridItems(albumGroups.take(6), key = { it.first }) { (album, group) ->
                            AlbumGridCard(
                                title = album,
                                artist = artistName,
                                artwork = group.firstOrNull()?.artworkUri,
                                onOpen = { onOpenAlbum(album) },
                                onPlay = { playSequence(playback, group) },
                            )
                        }
                    }
                }
            }
            if (singleGroups.isNotEmpty()) {
                item { Text("Singles", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 25.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(((singleGroups.size.coerceAtMost(6) + 1) / 2 * 225).dp),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        gridItems(singleGroups.take(6), key = { it.id }) { song ->
                            AlbumGridCard(
                                title = song.title,
                                artist = artistName,
                                artwork = song.artworkUri,
                                onOpen = { playSequence(playback, listOf(song)) },
                                onPlay = { playSequence(playback, listOf(song)) },
                            )
                        }
                    }
                }
            }
        }

        if (songs.isNotEmpty()) {
            FloatingActionButton(
                onClick = { playSequence(playback, songs) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = airPlayrFloatingPlayBottomPadding()).size(66.dp),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp),
            ) { Icon(Icons.Default.PlayArrow, "Play artist", modifier = Modifier.size(30.dp)) }
        }
    }

    if (menuOpen && songs.isNotEmpty()) {
        CollectionActionSheet(
            title = artistName,
            representative = songs.first(),
            items = songs,
            playback = playback,
            onOpen = null,
            onDismiss = { menuOpen = false },
        )
    }
    songMenu?.let { song ->
        AirPlayrPremiumMediaActionSheet(
            item = song,
            playing = playbackState.currentId == song.id && playbackState.isPlaying,
            favorite = false,
            groups = listOf(
                listOf(
                    AirPlayrMediaMenuAction("Play now", Icons.Default.PlayArrow, { playSequenceAt(playback, songs, song); songMenu = null }),
                    AirPlayrMediaMenuAction("Play next", Icons.Default.SkipNext, { playback.playNext(song); songMenu = null }),
                    AirPlayrMediaMenuAction("Add to queue", Icons.Default.QueueMusic, { playback.addToQueue(song); songMenu = null }),
                )
            ),
            onDismiss = { songMenu = null },
        )
    }
}

@Composable
private fun AlbumGridCard(
    title: String,
    artist: String,
    artwork: String?,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Box {
            Artwork(artwork, Modifier.fillMaxWidth().aspectRatio(1f), 20.dp, title, MediaType.AUDIO, AirPlayrIcons.AlbumDisc)
            IconButton(
                onClick = onPlay,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(36.dp).clip(CircleShape).background(colors.surfaceContainer.copy(alpha = .92f)),
            ) {
                Icon(Icons.Default.PlayArrow, "Play $title", tint = colors.primary, modifier = Modifier.size(19.dp))
            }
        }
        Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 16.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(artist, fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}



@Composable
fun RecentCollectionsScreen(
    type: String,
    container: AppContainer,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlayer: () -> Unit = {},
) {
    val colors = LocalAirPlayrColors.current
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    val historyRows by container.structured.history(500).collectAsStateWithLifecycle(emptyList())
    val repeated by container.structured.repeated(100).collectAsStateWithLifecycle(emptyList())

    val lookup = remember(snapshot.audio) { snapshot.audio.associateBy { it.id } }

    val title = when (type) {
        "on-repeat" -> "On repeat"
        "songs" -> "Recently added songs"
        "albums" -> "Recently played albums"
        "artists" -> "Recent Artists"
        else -> "Music"
    }

    val recentAlbumGroups = remember(historyRows, snapshot.audio) {
        historyRows
            .filter { it.mediaType == MediaType.AUDIO.value }
            .mapNotNull { h -> lookup[h.mediaId]?.let { h.playedAt to it } }
            .mapNotNull { (playedAt, item) ->
                val name = item.album?.trim().takeUnless { it.isNullOrBlank() } ?: return@mapNotNull null
                playedAt to item.copy(album = name)
            }
            .groupBy { (_, item) -> item.album!! }
            .mapNotNull { (name, rows) ->
                val lastPlayed = rows.maxOfOrNull { it.first } ?: 0L
                val albumSongs = snapshot.audio
                    .filter { it.album?.trim()?.equals(name, true) == true }
                    .sortedBy { it.title.lowercase(Locale.ROOT) }
                if (albumSongs.isEmpty()) null else Triple(name, albumSongs, lastPlayed)
            }
            .sortedByDescending { it.third }
            .map { it.first to it.second }
    }

    val recentArtistGroups = remember(historyRows, snapshot.audio) {
        historyRows
            .filter { it.mediaType == MediaType.AUDIO.value }
            .mapNotNull { h -> lookup[h.mediaId]?.let { h.playedAt to it } }
            .mapNotNull { (playedAt, item) ->
                val name = item.artist?.trim().takeUnless { it.isNullOrBlank() } ?: return@mapNotNull null
                playedAt to item.copy(artist = name)
            }
            .groupBy { (_, item) -> item.artist!! }
            .map { (name, rows) ->
                val lastPlayed = rows.maxOfOrNull { it.first } ?: 0L
                val songs = snapshot.audio.filter { it.artist?.trim()?.equals(name, true) == true }
                    .sortedBy { it.title.lowercase(Locale.ROOT) }
                Triple(name, songs, lastPlayed)
            }
            .sortedByDescending { it.third }
            .map { it.first to it.second }
            .filter { it.second.isNotEmpty() }
    }

    val recentSongs = remember(snapshot.audio) { snapshot.audio.sortedWith(compareByDescending<MediaItem> { it.dateAdded }.thenByDescending { it.dateModified }) }
    val repeatSongs = remember(repeated, lookup) {
        repeated.map { row ->
            lookup[row.mediaId] ?: MediaItem(
                id = row.mediaId,
                mediaStoreId = null,
                uri = row.mediaId,
                title = row.title,
                filename = row.title,
                artist = row.artist,
                album = row.album,
                albumArtist = row.artist,
                durationMs = 0L,
                width = 0,
                height = 0,
                sizeBytes = 0L,
                dateModified = 0L,
                dateAdded = row.playedAt,
                mediaType = MediaType.AUDIO,
                mimeType = null,
                albumId = null,
                artworkUri = row.artworkUri,
                thumbnailUri = null,
                genre = null,
                sourceDirectoryUri = null,
                folderId = null,
                folder = null,
                path = null,
                relativePath = null,
            )
        }.distinctBy { it.id }
    }

    val albumContinuousQueue = remember(recentAlbumGroups) { recentAlbumGroups.flatMap { it.second }.distinctBy { it.id } }
    val artistContinuousQueue = remember(recentArtistGroups) { recentArtistGroups.flatMap { it.second }.distinctBy { it.id } }

    var collectionMenu by remember { mutableStateOf<Pair<String, List<MediaItem>>?>(null) }
    var songMenu by remember { mutableStateOf<MediaItem?>(null) }
    val recentListState = rememberSaveable(
        saver = androidx.compose.foundation.lazy.LazyListState.Saver
    ) {
        androidx.compose.foundation.lazy.LazyListState(0)
    }
    val recentGridState = rememberSaveable(saver = androidx.compose.foundation.lazy.grid.LazyGridState.Saver) { androidx.compose.foundation.lazy.grid.LazyGridState() }


    val playSongAt = { items: List<MediaItem>, item: MediaItem ->
        val index = items.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        playback.playCollection(items, index)
        onOpenPlayer()
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            AirPlayrScreenHeader(
                title = title,
                subtitle = when (type) {
                    "on-repeat" -> "${repeatSongs.size} songs · sorted by repeat count"
                    "songs" -> "${recentSongs.size} songs on this device"
                    "albums" -> "${recentAlbumGroups.size} albums"
                    "artists" -> "${recentArtistGroups.size} artists"
                    else -> null
                },
                onBack = onBack,
            )

            when {
                type == "songs" || type == "on-repeat" -> {
                    val rows = if (type == "songs") recentSongs else repeatSongs
                    if (!snapshot.ready) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                    } else if (rows.isEmpty()) {
                        EmptyNativeState("Nothing here yet", if (type == "on-repeat") "Songs you play repeatedly will appear here." else "Songs added to this device will appear here.")
                    } else {
                        LazyColumn(
                            state = recentListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = airPlayrBottomContentPadding()),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(rows, key = { it.id }) { item ->
                                AirPlayrMediaRow(
                                    item = item,
                                    onOpen = { playSongAt(rows, item) },
                                    onMore = { songMenu = item },
                                    playing = playbackState.isPlaying && playbackState.currentId == item.id,
                                )
                            }
                        }
                    }
                    AirPlayrLongListFastScrollHandle(recentListState, bottomPadding = airPlayrFloatingPlayBottomPadding() + 76.dp)
                }

                type == "albums" -> {
                    if (recentAlbumGroups.isEmpty()) {
                        EmptyNativeState("No recently played albums", "Play music from an album and it will appear here.")
                    } else {
                        LazyVerticalGrid(
                            state = recentGridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = airPlayrBottomContentPadding() + 84.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            gridItems(recentAlbumGroups, key = { it.first }) { (name, songs) ->
                                RecentAlbumCard(
                                    name = name,
                                    artist = songs.firstOrNull()?.albumArtist ?: songs.firstOrNull()?.artist ?: "Unknown artist",
                                    artwork = songs.firstOrNull()?.artworkUri,
                                    onOpen = { onOpenAlbum(name) },
                                    onPlay = { playSequence(playback, songs) },
                                    onMore = { collectionMenu = name to songs },
                                )
                            }
                        }
                    }
                }

                type == "artists" -> {
                    if (recentArtistGroups.isEmpty()) {
                        EmptyNativeState("No recent artists", "Play music from an artist and it will appear here.")
                    } else {
                        LazyVerticalGrid(
                            state = recentGridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = airPlayrBottomContentPadding() + 84.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            gridItems(recentArtistGroups, key = { it.first }) { (name, songs) ->
                                RecentArtistCard(
                                    name = name,
                                    artwork = songs.firstOrNull()?.artworkUri,
                                    onOpen = { onOpenArtist(name) },
                                    onPlay = { playSequence(playback, songs) },
                                    onMore = { collectionMenu = name to songs },
                                )
                            }
                        }
                    }
                    AirPlayrLongGridFastScrollHandle(recentGridState, bottomPadding = airPlayrFloatingPlayBottomPadding() + 76.dp)
                }
            }
        }

        val continuousQueue = when (type) {
            "albums" -> albumContinuousQueue
            "artists" -> artistContinuousQueue
            "songs" -> recentSongs
            "on-repeat" -> repeatSongs
            else -> emptyList()
        }
        if (continuousQueue.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    playSequence(playback, continuousQueue)
                    onOpenPlayer()
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = airPlayrFloatingPlayBottomPadding()).size(66.dp),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp),
            ) {
                Icon(Icons.Default.PlayArrow, "Play all", modifier = Modifier.size(30.dp))
            }
        }
    }

    collectionMenu?.let { (name, songs) ->
        CollectionActionSheet(
            title = name,
            representative = songs.first(),
            items = songs,
            playback = playback,
            playItems = when (type) {
                "albums" -> albumContinuousQueue
                "artists" -> artistContinuousQueue
                else -> songs
            },
            onOpen = if (type == "albums") ({ onOpenAlbum(name) }) else if (type == "artists") ({ onOpenArtist(name) }) else null,
            onDismiss = { collectionMenu = null },
        )
    }

    songMenu?.let { item ->
        AirPlayrPremiumMediaActionSheet(
            item = item,
            playing = playbackState.currentId == item.id && playbackState.isPlaying,
            groups = listOf(
                listOf(
                    AirPlayrMediaMenuAction("Play now", Icons.Default.PlayArrow, {
                        val rows = when (type) {
                            "songs" -> recentSongs
                            "on-repeat" -> repeatSongs
                            else -> listOf(item)
                        }
                        playSongAt(rows, item)
                        songMenu = null
                    }),
                    AirPlayrMediaMenuAction("Play next", Icons.Default.SkipNext, { playback.playNext(item); songMenu = null }),
                    AirPlayrMediaMenuAction("Add to queue", Icons.Default.QueueMusic, { playback.addToQueue(item); songMenu = null }),
                ),
            ),
            onDismiss = { songMenu = null },
        )
    }
}

@Composable
private fun RecentAlbumCard(
    name: String,
    artist: String,
    artwork: String?,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxWidth()) {
        Box {
            Artwork(artwork, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).clickable(onClick = onOpen), 20.dp, name, MediaType.AUDIO, AirPlayrIcons.AlbumDisc)
            IconButton(
                onClick = onPlay,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(38.dp).clip(CircleShape).background(colors.surfaceContainer.copy(alpha = .94f)),
            ) { Icon(Icons.Default.PlayArrow, "Play $name", tint = colors.primary, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = onMore, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(38.dp)) {
                Icon(Icons.Default.MoreVert, "More options", tint = colors.onSurface, modifier = Modifier.size(23.dp))
            }
        }
        Text(name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 17.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
        Text(artist, fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RecentArtistCard(
    name: String,
    artwork: String?,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Artwork(artwork, Modifier.size(154.dp).clip(CircleShape), 77.dp, name, MediaType.AUDIO, AirPlayrIcons.Artist)
            IconButton(
                onClick = onPlay,
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).size(38.dp).clip(CircleShape).background(colors.surfaceContainer.copy(alpha = .94f)),
            ) { Icon(Icons.Default.PlayArrow, "Play $name", tint = colors.primary, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = onMore, modifier = Modifier.align(Alignment.TopEnd).size(38.dp)) {
                Icon(Icons.Default.MoreVert, "More options", tint = colors.onSurface, modifier = Modifier.size(23.dp))
            }
        }
        Text(name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
    }
}


@Composable
fun GenreDetailScreen(
    genreName: String,
    libraryViewModel: LibraryViewModel,
    playback: PlaybackController,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val playbackState by playback.stableState.collectAsStateWithLifecycle()
    var genreMenuOpen by remember { mutableStateOf(false) }
    var songMenu by remember { mutableStateOf<MediaItem?>(null) }
    val songs = remember(snapshot.audio, genreName) {
        snapshot.audio.filter { it.genre?.equals(genreName, true) == true }
    }
    val albums = remember(songs) {
        songs.filter { !it.album.isNullOrBlank() }
            .groupBy { it.album!!.trim() }
            .toList()
            .take(12)
    }
    val heroArts = remember(songs) { songs.mapNotNull { it.artworkUri }.distinct().take(5) }

    fun playAll(shuffle: Boolean = false) {
        val queue = if (shuffle) songs.shuffled() else songs
        if (queue.isNotEmpty()) playback.playCollection(queue, 0)
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = airPlayrBottomContentPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(420.dp)) {
                    Box(Modifier.fillMaxSize().background(colors.surfaceContainerLow)) {
                        if (heroArts.isNotEmpty()) {
                            heroArts.take(5).forEachIndexed { index, uri ->
                                val sizes = listOf(190.dp, 150.dp, 135.dp, 145.dp, 120.dp)
                                val aligns = listOf(Alignment.TopCenter, Alignment.TopStart, Alignment.CenterEnd, Alignment.BottomStart, Alignment.BottomEnd)
                                Artwork(
                                    uri,
                                    Modifier
                                        .size(sizes[index])
                                        .align(aligns[index])
                                        .offset(
                                            x = if (index == 1 || index == 3) (-20).dp else 20.dp,
                                            y = when (index) { 0 -> 30.dp; 1, 2 -> 70.dp; else -> (-15).dp },
                                        )
                                        .shadow(14.dp, RoundedCornerShape(20.dp))
                                        .clip(RoundedCornerShape(20.dp)),
                                    20.dp,
                                    genreName,
                                    MediaType.AUDIO,
                                    AirPlayrIcons.Music,
                                )
                            }
                        }
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, colors.background.copy(alpha = .05f), colors.background.copy(alpha = .97f))
                                )
                            )
                        )
                    }
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.statusBarsPadding().padding(start = 10.dp, top = 8.dp).size(46.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary)
                    }
                    IconButton(
                        onClick = { genreMenuOpen = true },
                        modifier = Modifier.statusBarsPadding().padding(end = 10.dp, top = 8.dp).align(Alignment.TopEnd).size(46.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, "Genre options", tint = colors.primary)
                    }
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp, vertical = 18.dp)
                    ) {
                        Text(genreName.uppercase(), fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 12.sp, letterSpacing = 1.8.sp, color = colors.primary)
                        Text(genreName, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 42.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("${songs.size} songs", fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = colors.textSecondary)
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { playAll(false) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Play all", fontFamily = AirPlayrFonts.OpenSansBold)
                    }
                    IconButton(
                        onClick = { playAll(true) },
                        modifier = Modifier.size(52.dp).background(colors.surfaceContainerHigh, CircleShape)
                    ) { Icon(Icons.Default.Shuffle, "Shuffle", tint = colors.primary) }
                }
            }
            if (albums.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Albums", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 25.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Text("SEE ALL", fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 9.sp, letterSpacing = 1.sp, color = colors.primary)
                    }
                }
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        albums.forEach { (album, group) ->
                            AirPlayrAlbumRailCard(
                                artworkUri = group.firstOrNull()?.artworkUri,
                                albumTitle = album,
                                artist = group.firstOrNull()?.artist.orEmpty(),
                                onOpen = { onOpenAlbum(album) },
                                onPlay = { playback.playCollection(group, 0) },
                                size = 142.dp,
                            )
                        }
                    }
                }
            }
            item {
                Text("Songs", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 25.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 22.dp, vertical = 5.dp))
            }
            items(songs, key = { it.id }) { song ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        playback.playCollection(songs, songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0))
                    }.padding(horizontal = 22.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(song.artworkUri, Modifier.size(58.dp), 16.dp, song.title, MediaType.AUDIO, AirPlayrIcons.Music, isPlaying = playbackState.currentId == song.id && playbackState.isPlaying)
                    Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                        Text(song.title.ifBlank { song.filename }, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${song.artist ?: "Unknown Artist"}  ·  ${durationLabel(listOf(song))}", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { songMenu = song }) {
                        Icon(Icons.Default.MoreVert, "Song options", tint = colors.textSecondary)
                    }
                }
            }
        }

        if (genreMenuOpen) {
            ModalBottomSheet(onDismissRequest = { genreMenuOpen = false }, containerColor = colors.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(genreName, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 24.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
                    ActionGroup {
                        ActionButton("Play all", Icons.Default.PlayArrow, { genreMenuOpen = false; playAll(false) })
                        ActionButton("Shuffle", Icons.Default.Shuffle, { genreMenuOpen = false; playAll(true) })
                        ActionButton("Add all to queue", Icons.Default.QueueMusic, { genreMenuOpen = false; songs.forEach(playback::addToQueue); if (songs.isNotEmpty()) AppFeedback.showSnackbar("Added ${songs.size} songs to the playing queue.") })
                    }
                }
            }
        }

        songMenu?.let { song ->
            AirPlayrPremiumMediaActionSheet(
                item = song,
                playing = playbackState.currentId == song.id && playbackState.isPlaying,
                favorite = false,
                groups = listOf(
                    listOf(
                        AirPlayrMediaMenuAction("Play now", Icons.Default.PlayArrow, { songMenu = null; playSequenceAt(playback, songs, song) }),
                        AirPlayrMediaMenuAction("Play next", Icons.Default.SkipNext, { songMenu = null; playback.playNext(song) }),
                        AirPlayrMediaMenuAction("Add to queue", Icons.Default.QueueMusic, { songMenu = null; playback.addToQueue(song) }),
                    ),
                ),
                onDismiss = { songMenu = null },
            )
        }
    }
}
