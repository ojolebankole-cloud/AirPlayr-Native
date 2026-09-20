package com.airplayr.app.ui.screens

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.PlaybackStatsRepository
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.AirPlayrCard
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.components.AirPlayrMediaRow
import com.airplayr.app.ui.components.AIRPLAYR_FLOATING_PLAY_BOTTOM
import com.airplayr.app.ui.components.AIRPLAYR_FLOATING_PLAY_BOTTOM_WITH_MINI
import com.airplayr.app.ui.components.AirPlayrScreenHeader
import com.airplayr.app.ui.components.Artwork
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.navigation.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StatsScreen(container: AppContainer, libraryViewModel: LibraryViewModel, playback: PlaybackController, onOpenMedia: (MediaItem) -> Unit = {}, onOpenVideo: (String) -> Unit = {}, onBack: (() -> Unit)? = null) {
    val colors = LocalAirPlayrColors.current
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val music = snapshot.audio
    val video = snapshot.video
    var period by rememberSaveable { mutableStateOf("month") }
    var mode by rememberSaveable { mutableStateOf("music") }
    var stats by remember { mutableStateOf<PlaybackStatsRepository.Summary?>(null) }
    val playbackState by playback.stableState.collectAsStateWithLifecycle()

    LaunchedEffect(period, music, video, snapshot.ready) {
        if (!snapshot.ready) return@LaunchedEffect
        stats = withContext(Dispatchers.IO) {
            runCatching { container.stats.summary(period, music + video, 10) }.getOrNull()
        }
    }

    val s = stats
    if (!snapshot.ready || s == null) {
        Column(Modifier.fillMaxSize().background(colors.background)) {
            StatsHeader(onBack, colors, "Listening & watching stats", "Your AirPlayr activity")
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        return
    }

    val topArtistItem = music.firstOrNull { it.artist?.trim().equals(s.topArtist?.trim(), ignoreCase = true) }
    val modeData = if (mode == "music") s.activityMusic else s.activityVideo
    val modeTitle = if (mode == "music") "When you listen" else "When you watch"
    val modeCaption = if (mode == "music") "MUSIC" else "VIDEO"
    val listeningLabel = formatDuration(s.listeningSeconds)
    val watchingLabel = formatDuration(s.watchingSeconds)
    val totalLabel = formatDuration(s.listeningSeconds + s.watchingSeconds)

    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                StatsHeader(onBack, colors, "Listening & watching stats", "${s.periodLabel} · ${s.tracks} tracks · ${s.artists} artists · ${s.albums} albums · ${s.videos} videos")
            }
            item { PeriodSelector(period, colors) { period = it } }
            item {
                HeroStatsCard(
                    periodLabel = s.periodLabel,
                    listening = listeningLabel,
                    watching = watchingLabel,
                    total = totalLabel,
                    colors = colors,
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StreakCard(s.streak, colors, Modifier.weight(1f))
                    TopArtistCard(topArtistItem, s.topArtist, colors, Modifier.weight(1f))
                }
            }
            item { SessionsCard(s, colors) }
            item { ActivityCard(modeTitle, modeCaption, modeData, mode, colors) { mode = it } }
            item { GenresCard(s.genres, colors) }
            item {
                TopMediaCard(
                    title = "Top songs",
                    items = s.topSongs,
                    lookup = music.associateBy { it.id },
                    colors = colors,
                    onOpen = { item -> onOpenMedia(item) },
                    playingId = playbackState.takeIf { it.isPlaying }?.currentId,
                    fallback = "Your most-played songs will appear here.",
                )
            }
            item {
                TopMediaCard(
                    title = "Top videos",
                    items = s.topVideos,
                    lookup = video.associateBy { it.id },
                    colors = colors,
                    onOpen = { item -> onOpenMedia(item) },
                    playingId = playbackState.takeIf { it.isPlaying }?.currentId,
                    fallback = "Your most-watched videos will appear here.",
                )
            }
            item { StoryFooter(colors) }
        }

        val musicById = remember(music) { music.associateBy { it.id } }
        val videoById = remember(video) { video.associateBy { it.id } }
        val topSong = s.topSongs.asSequence().mapNotNull { musicById[it.id] }.firstOrNull()
        val topVideo = s.topVideos.asSequence().mapNotNull { videoById[it.id] }.firstOrNull()
        val fabTarget = topSong ?: topVideo
        if (fabTarget != null) {
            FloatingActionButton(
                onClick = {
                    if (fabTarget.mediaType == MediaType.AUDIO) {
                        onOpenMedia(fabTarget)
                    } else {
                        onOpenVideo(fabTarget.id)
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 24.dp).size(68.dp),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                shape = RoundedCornerShape(22.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 10.dp),
            ) {
                Icon(Icons.Default.PlayArrow, "Play", modifier = Modifier.size(31.dp))
            }
        }
    }

}


@Composable
private fun StatsHeader(onBack: (() -> Unit)?, colors: com.airplayr.app.ui.theme.AirPlayrColors, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                androidx.compose.material3.IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(AirPlayrIcons.Back, "Back", tint = colors.primary, modifier = Modifier.size(30.dp))
                }
            }
        }
        Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-0.7).sp, color = colors.textPrimary)
        Text(subtitle, fontFamily = AirPlayrFonts.OpenSans, fontSize = 16.sp, lineHeight = 23.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
    }
}

@Composable
private fun PeriodSelector(period: String, colors: com.airplayr.app.ui.theme.AirPlayrColors, onSelect: (String) -> Unit) {
    AirPlayrCard(shape = RoundedCornerShape(30.dp), containerColor = colors.surfaceContainerLow, elevation = 4.dp) {
        Row(Modifier.fillMaxWidth().height(58.dp).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("week" to "Week", "month" to "Month", "all" to "All time").forEach { (key, label) ->
                val selected = period == key
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(25.dp)).background(if (selected) colors.primaryContainer else Color.Transparent).clickable { onSelect(key) },
                    contentAlignment = Alignment.Center,
                ) { Text(label, fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 15.sp, color = colors.textPrimary) }
            }
        }
    }
}

@Composable
private fun HeroStatsCard(periodLabel: String, listening: String, watching: String, total: String, colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    AirPlayrCard(shape = RoundedCornerShape(30.dp), containerColor = colors.primaryContainer, elevation = 5.dp) {
        Column(Modifier.fillMaxWidth().padding(28.dp)) {
            Text("TOTAL MEDIA TIME · ${periodLabel.uppercase()}", fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 13.sp, letterSpacing = 1.5.sp, color = colors.textPrimary)
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                HeroValue(listening, "listening", colors, Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(54.dp).background(colors.outlineVariant.copy(alpha = .7f)))
                HeroValue(watching, "watching", colors, Modifier.weight(1f))
            }
            Text("Combined tracked time · $total", fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 24.dp))
            if (total == "0s") Text("Your listening story starts here — keep the music moving.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun HeroValue(value: String, label: String, colors: com.airplayr.app.ui.theme.AirPlayrColors, modifier: Modifier) {
    Column(modifier.padding(horizontal = 12.dp)) {
        Text(value, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-1).sp, color = colors.textPrimary)
        Text(label, fontFamily = AirPlayrFonts.OpenSansMedium, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun StreakCard(streak: Int, colors: com.airplayr.app.ui.theme.AirPlayrColors, modifier: Modifier) {
    AirPlayrCard(modifier, shape = RoundedCornerShape(28.dp), containerColor = colors.surface, elevation = 5.dp) {
        Column(Modifier.fillMaxWidth().heightIn(min = 176.dp).padding(22.dp)) {
            Icon(Icons.Default.LocalFireDepartment, null, tint = colors.tertiary, modifier = Modifier.size(30.dp))
            Text(streak.toString(), fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 44.sp, lineHeight = 48.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 22.dp))
            Text("DAYS IN A ROW", fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 12.sp, letterSpacing = 1.1.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun TopArtistCard(item: MediaItem?, artistName: String?, colors: com.airplayr.app.ui.theme.AirPlayrColors, modifier: Modifier) {
    AirPlayrCard(modifier, shape = RoundedCornerShape(28.dp), containerColor = colors.primaryContainer, elevation = 5.dp) {
        Column(Modifier.fillMaxWidth().heightIn(min = 176.dp).padding(22.dp)) {
            Artwork(item?.artworkUri, Modifier.size(58.dp), 18.dp, artistName)
            Text("TOP ARTIST", fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 11.sp, letterSpacing = 1.4.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 13.dp))
            Text(artistName ?: "—", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 24.sp, lineHeight = 28.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            Text(if (artistName != null) "Tracked music plays" else "No plays yet", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SessionsCard(s: PlaybackStatsRepository.Summary, colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    AirPlayrCard(shape = RoundedCornerShape(28.dp), containerColor = colors.surface, elevation = 5.dp) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Text("Sessions", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 26.sp, lineHeight = 30.sp, color = colors.textPrimary)
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(Icons.Default.PlayCircleOutline, "total", s.sessions.toString(), colors, Modifier.weight(1f))
                StatPill(Icons.Default.AccessTime, "avg length", formatDuration(s.averageSessionSeconds), colors, Modifier.weight(1f))
                StatPill(Icons.Default.AccessTime, "longest", formatDuration(s.longestSessionSeconds), colors, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, colors: com.airplayr.app.ui.theme.AirPlayrColors, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainerLow).padding(12.dp)) {
        Icon(icon, null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Text(value, fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 22.sp, lineHeight = 25.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 8.dp))
        Text(label, fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ActivityCard(title: String, caption: String, data: List<List<Long>>, mode: String, colors: com.airplayr.app.ui.theme.AirPlayrColors, onMode: (String) -> Unit) {
    val max = (data.flatten().maxOrNull() ?: 0L).coerceAtLeast(1L)
    val buckets = listOf("Night", "Early", "Morn", "Midday", "Eve", "Late")
    AirPlayrCard(shape = RoundedCornerShape(28.dp), containerColor = colors.surface, elevation = 5.dp) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 26.sp, lineHeight = 30.sp, color = colors.textPrimary)
                    Text("Weekly activity by time of day", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                }
                Text(caption, fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 11.sp, letterSpacing = 1.5.sp, color = colors.textPrimary)
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(22.dp)).background(colors.surfaceContainerLow).padding(4.dp)) {
                listOf("music" to "Music", "video" to "Videos").forEach { (key, label) ->
                    val selected = mode == key
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(19.dp)).background(if (selected) colors.primaryContainer else Color.Transparent).clickable { onMode(key) }, contentAlignment = Alignment.Center) {
                        Text(label, fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 13.sp, color = colors.textPrimary)
                    }
                }
            }
            Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(24.dp))
                    buckets.forEach { Text(it, Modifier.width(49.dp), textAlign = TextAlign.Center, fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 10.sp, color = colors.textPrimary) }
                }
                data.take(7).forEachIndexed { row, values ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(listOf("M","T","W","T","F","S","S").getOrElse(row) { "" }, Modifier.width(24.dp), fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 12.sp, color = colors.textPrimary)
                        values.take(6).forEach { value ->
                            val alpha = if (value == 0L) 1f else (.16f + .84f * (value.toFloat() / max.toFloat()).coerceIn(0f, 1f))
                            Box(Modifier.padding(horizontal = 2.5.dp).size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (value == 0L) colors.surfaceContainerHigh else colors.primary.copy(alpha = alpha)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenresCard(genres: List<PlaybackStatsRepository.GenreStat>, colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    var filter by rememberSaveable { mutableStateOf("all") }
    val filtered = when (filter) {
        "music" -> genres.filter { it.musicPlays > 0 }.map { it.copy(plays = it.musicPlays, share = it.musicPlays.toDouble() / genres.sumOf { g -> g.musicPlays }.coerceAtLeast(1)) }
        "video" -> genres.filter { it.videoPlays > 0 }.map { it.copy(plays = it.videoPlays, share = it.videoPlays.toDouble() / genres.sumOf { g -> g.videoPlays }.coerceAtLeast(1)) }
        else -> genres
    }
    AirPlayrCard(shape = RoundedCornerShape(28.dp), containerColor = colors.surface, elevation = 5.dp) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Genres", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 26.sp, lineHeight = 30.sp, color = colors.textPrimary)
                    Text("Where your plays are going", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
                }
                Text(if (filtered.isNotEmpty()) "${filtered.size} genres" else "NO DATA", fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 11.sp, letterSpacing = 1.5.sp, color = colors.textPrimary)
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(22.dp)).background(colors.surfaceContainerLow).padding(4.dp)) {
                listOf("all" to "All", "music" to "Music", "video" to "Videos").forEach { (key, label) ->
                    val selected = filter == key
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(19.dp)).background(if (selected) colors.primaryContainer else Color.Transparent).clickable { filter = key }, contentAlignment = Alignment.Center) {
                        Text(label, fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 13.sp, color = colors.textPrimary)
                    }
                }
            }
            if (filtered.isEmpty()) {
                Text("Genre insights will appear as you play music or videos with genre metadata.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 14.sp, lineHeight = 20.sp, color = colors.textPrimary, modifier = Modifier.padding(vertical = 14.dp))
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }).padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    filtered.forEach { genre -> GenreRingItem(genre, colors) }
                }
            }
        }
    }
}

@Composable
private fun GenreRingItem(item: PlaybackStatsRepository.GenreStat, colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    val size = 82.dp
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val stroke = 8.dp.toPx()
        drawArc(colors.surfaceContainerHigh, -90f, 360f, false, style = Stroke(stroke))
        drawArc(colors.primary, -90f, (item.share.coerceIn(0.0, 1.0) * 360.0).toFloat(), false, style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
    Column(Modifier.width(104.dp).padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(item.plays.toString(), fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 18.sp, color = colors.textPrimary, modifier = Modifier.offset(y = (-70).dp))
        Text("${(item.share * 100).toInt()}%", fontFamily = AirPlayrFonts.OpenSans, fontSize = 9.sp, color = colors.textPrimary, modifier = Modifier.offset(y = (-70).dp))
        Text(item.name, fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 13.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.offset(y = (-46).dp))
        Text("${item.musicPlays} music · ${item.videoPlays} video", fontFamily = AirPlayrFonts.OpenSans, fontSize = 8.sp, color = colors.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.offset(y = (-44).dp))
    }
}

@Composable
private fun TopMediaCard(title: String, items: List<PlaybackStatsRepository.TopItem>, lookup: Map<String, MediaItem>, colors: com.airplayr.app.ui.theme.AirPlayrColors, onOpen: (MediaItem) -> Unit, playingId: String?, fallback: String) {
    AirPlayrCard(shape = RoundedCornerShape(28.dp), containerColor = colors.surface, elevation = 5.dp) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 26.sp, lineHeight = 30.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Text("PLAYS", fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 11.sp, letterSpacing = 1.5.sp, color = colors.textPrimary)
            }
            if (items.isEmpty()) {
                Text(fallback, fontFamily = AirPlayrFonts.OpenSans, fontSize = 14.sp, lineHeight = 20.sp, color = colors.textPrimary, modifier = Modifier.padding(vertical = 14.dp))
            } else {
                items.forEachIndexed { index, top ->
                    val media = lookup[top.id]
                    if (media != null) {
                        Row(Modifier.fillMaxWidth().padding(top = if (index == 0) 12.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}", fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 20.sp, color = colors.textPrimary, modifier = Modifier.width(26.dp))
                            Box(Modifier.weight(1f).clickable { onOpen(media) }) { AirPlayrMediaRow(media, onOpen = { onOpen(media) }, playing = playingId == media.id, compact = true) }
                            Text(top.periodPlays.toString(), fontFamily = AirPlayrFonts.OpenSansBold, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryFooter(colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    AirPlayrCard(shape = RoundedCornerShape(22.dp), containerColor = colors.surfaceContainerLow, elevation = 5.dp) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top) {
            Icon(AirPlayrIcons.More, null, tint = colors.primary, modifier = Modifier.size(24.dp))
            Text("Your AirPlayr story, built from the moments you actually listened and watched.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, lineHeight = 18.sp, color = colors.textPrimary, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

private fun formatDuration(seconds: Long): String = when {
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
}
