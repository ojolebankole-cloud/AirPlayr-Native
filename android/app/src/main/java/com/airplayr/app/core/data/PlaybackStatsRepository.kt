package com.airplayr.app.core.data

import com.airplayr.app.core.data.db.*
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/** Room-backed local playback statistics store. */
class PlaybackStatsRepository(private val databaseProvider: () -> AirPlayrDatabase) {
    private val db: AirPlayrDatabase get() = databaseProvider()
    data class TopItem(val id: String, val title: String, val artist: String?, val album: String?, val artworkUri: String?, val mediaType: String, val periodPlays: Int, val allTimePlays: Int)
    data class GenreStat(val name: String, val plays: Int, val musicPlays: Int, val videoPlays: Int, val share: Double)
    data class Summary(
        val period: String,
        val periodLabel: String,
        val musicPlays: Int,
        val videoPlays: Int,
        val listeningSeconds: Long,
        val watchingSeconds: Long,
        val totalTrackedSeconds: Long,
        val tracks: Int,
        val artists: Int,
        val albums: Int,
        val videos: Int,
        val completedVideos: Int,
        val favorites: Int,
        val sessions: Int,
        val averageSessionSeconds: Long,
        val longestSessionSeconds: Long,
        val streak: Int,
        val topArtist: String?,
        val topSongs: List<TopItem>,
        val topVideos: List<TopItem>,
        val genres: List<GenreStat>,
        val activityMusic: List<List<Long>>,
        val activityVideo: List<List<Long>>,
    )

    private data class Active(var session: PlaybackSessionEntity, var lastWall: Long)
    private var active: Active? = null
    private val pending = mutableMapOf<String, Long>()
    private var lastFlush = 0L

    suspend fun start(item: MediaItem, now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val old = active
        if (old?.session?.mediaId == item.id && old.session.mediaType == item.mediaType.value) return@withContext
        flushLocked(now)
        val session = PlaybackSessionEntity(UUID.randomUUID().toString(), item.id, item.mediaType.value, item.title, item.artist, item.album, item.artworkUri, now, null, 0, 0)
        db.userDataDao().upsertPlaybackSession(session)
        db.userDataDao().insertPlaybackEvent(PlaybackEventEntity(mediaId = item.id, mediaType = item.mediaType.value, playedAt = now))
        active = Active(session, now)
    }

    suspend fun track(item: MediaItem, positionMs: Long, now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val current = active
        if (current == null || current.session.mediaId != item.id || current.session.mediaType != item.mediaType.value) {
            start(item, now)
            return@withContext
        }
        val deltaWall = ((now - current.lastWall) / 1000L).coerceAtLeast(0L).coerceAtMost(3L)
        current.lastWall = now
        if (deltaWall <= 0L) return@withContext
        current.session = current.session.copy(seconds = current.session.seconds + deltaWall, lastMediaTime = positionMs.coerceAtLeast(0L))
        val key = activityKey(dayKey(now), bucketForHour(Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)), item.mediaType.value)
        pending[key] = (pending[key] ?: 0L) + deltaWall
        if (now - lastFlush >= 5000L) flushLocked(now)
    }

    suspend fun finish(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) { flushLocked(now); active?.let { a -> db.userDataDao().upsertPlaybackSession(a.session.copy(endedAt = now)); active = null } }

    private suspend fun flushLocked(now: Long) {
        pending.forEach { (k, delta) ->
            val bits = k.split('|')
            if (bits.size == 3 && delta > 0) {
                val day = bits[0]; val bucket = bits[1]; val type = bits[2]
                val existing = db.userDataDao().playbackActivity(day, day).firstOrNull { it.bucket == bucket && it.mediaType == type }
                db.userDataDao().upsertPlaybackActivity(PlaybackActivityEntity(day, bucket, type, (existing?.seconds ?: 0L) + delta))
            }
        }
        pending.clear()
        active?.let { db.userDataDao().upsertPlaybackSession(it.session) }
        lastFlush = now
    }

    suspend fun summary(period: String, items: List<MediaItem>, limit: Int = 10): Summary = withContext(Dispatchers.IO) {
        flushLocked(System.currentTimeMillis())
        val now = System.currentTimeMillis()
        val from = periodStart(period, now)
        val events = db.userDataDao().playbackEvents(from, Long.MAX_VALUE)
        val history = db.userDataDao().history(500)
        val itemMap = items.associateBy { it.id }
        val favoriteCount = db.userDataDao().favoriteIds().size
        val musicEvents = events.filter { it.mediaType == MediaType.AUDIO.value }
        val videoEvents = events.filter { it.mediaType == MediaType.VIDEO.value }
        val playCounts = events.groupingBy { it.mediaType + "|" + it.mediaId }.eachCount()
        fun top(type: String): List<TopItem> = playCounts.filterKeys { it.startsWith("$type|") }.mapNotNull { (key, count) ->
            val id = key.substringAfter('|')
            val item = itemMap[id]
            val h = history.firstOrNull { it.mediaId == id }
            item?.let { TopItem(it.id, it.title, it.artist, it.album, it.artworkUri, it.mediaType.value, count, h?.playCount ?: count) }
        }.sortedWith(compareByDescending<TopItem> { it.periodPlays }.thenBy { it.title.lowercase(Locale.ROOT) }).take(limit)
        val artistCounts = musicEvents.mapNotNull { e -> itemMap[e.mediaId]?.artist?.takeIf(String::isNotBlank) }.groupingBy { it }.eachCount()
        val topArtist = artistCounts.maxByOrNull { it.value }?.key
        data class GenreCounts(var music: Int = 0, var video: Int = 0)
        val genreMap = mutableMapOf<String, GenreCounts>()
        musicEvents.forEach { e -> itemMap[e.mediaId]?.genre?.trim()?.takeIf(String::isNotBlank)?.let { g -> genreMap.getOrPut(g) { GenreCounts() }.music++ } }
        videoEvents.forEach { e -> itemMap[e.mediaId]?.genre?.trim()?.takeIf(String::isNotBlank)?.let { g -> genreMap.getOrPut(g) { GenreCounts() }.video++ } }
        val genreTotal = max(1, genreMap.values.sumOf { it.music + it.video })
        val genres = genreMap.map { (name, counts) -> val total = counts.music + counts.video; GenreStat(name, total, counts.music, counts.video, total.toDouble() / genreTotal) }.sortedByDescending { it.plays }.take(12)
        val sessions = db.userDataDao().playbackSessions(from, Long.MAX_VALUE)
        val activity = db.userDataDao().playbackActivity(dayKey(from), dayKey(now))
        val completionEvents = db.userDataDao().playbackEventsByType(VIDEO_COMPLETION_EVENT)
        val completionCounts = completionEvents.groupingBy { it.mediaId }.eachCount()
        val completedInHistory = history.filter { it.mediaType == MediaType.VIDEO.value && it.completed }.associate { it.mediaId to 1 }
        val completedVideoCount = (completionCounts.keys + completedInHistory.keys).sumOf { id ->
            max(completionCounts[id] ?: 0, completedInHistory[id] ?: 0)
        }
        Summary(
            period = period,
            periodLabel = when (period) { "week" -> "This week"; "all" -> "All time"; else -> "This month" },
            musicPlays = musicEvents.size,
            videoPlays = videoEvents.size,
            listeningSeconds = activity.filter { it.mediaType == MediaType.AUDIO.value }.sumOf { it.seconds },
            watchingSeconds = activity.filter { it.mediaType == MediaType.VIDEO.value }.sumOf { it.seconds },
            totalTrackedSeconds = activity.sumOf { it.seconds },
            tracks = musicEvents.map { it.mediaId }.distinct().size,
            artists = musicEvents.mapNotNull { itemMap[it.mediaId]?.artist?.takeIf(String::isNotBlank) }.distinct().size,
            albums = musicEvents.mapNotNull { itemMap[it.mediaId]?.album?.takeIf(String::isNotBlank) }.distinct().size,
            videos = videoEvents.map { it.mediaId }.distinct().size,
            completedVideos = completedVideoCount,
            favorites = favoriteCount,
            sessions = sessions.size,
            averageSessionSeconds = if (sessions.isEmpty()) 0L else sessions.map { it.seconds }.average().toLong(),
            longestSessionSeconds = sessions.maxOfOrNull { it.seconds } ?: 0L,
            streak = calculateStreak(activity, MediaType.AUDIO.value),
            topArtist = topArtist,
            topSongs = top("audio"),
            topVideos = top("video"),
            genres = genres,
            activityMusic = activityGrid(activity.filter { it.mediaType == MediaType.AUDIO.value }, from, now),
            activityVideo = activityGrid(activity.filter { it.mediaType == MediaType.VIDEO.value }, from, now),
        )
    }

    private fun periodStart(period: String, now: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        when (period) {
            "week" -> { val delta = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7; c.add(Calendar.DAY_OF_YEAR, -delta) }
            "month" -> c.set(Calendar.DAY_OF_MONTH, 1)
            else -> return 0L
        }
        return c.timeInMillis
    }

    private fun calculateStreak(rows: List<PlaybackActivityEntity>, type: String): Int {
        val activeDays = rows.filter { it.mediaType == type && it.seconds > 0 }.map { it.dayKey }.toSet()
        if (activeDays.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT); var d = Date(System.currentTimeMillis()); var count = 0
        while (activeDays.contains(sdf.format(d))) { count++; d = Date(d.time - 86_400_000L) }
        return count
    }

    private fun activityGrid(rows: List<PlaybackActivityEntity>, from: Long, now: Long): List<List<Long>> {
        val bucketOrder = listOf("Night", "Early", "Morn", "Midday", "Eve", "Late")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val days = (0..6).map { Date(now - ((6 - it) * 86_400_000L)) }.map(sdf::format)
        return days.map { dayKey -> bucketOrder.map { bucket -> rows.filter { it.dayKey == dayKey && it.bucket == bucket }.sumOf { it.seconds } } }
    }
    private fun dayOfWeek(day: String): Int = runCatching { Calendar.getInstance().apply { time = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(day)!! }.let { (it.get(Calendar.DAY_OF_WEEK) + 5) % 7 } }.getOrDefault(0)
    private fun dayKey(time: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(time))
    private fun activityKey(day: String, bucket: String, type: String) = "$day|$bucket|$type"
    private fun bucketForHour(hour: Int) = when (hour) { in 0..4 -> "Night"; in 5..8 -> "Early"; in 9..11 -> "Morn"; in 12..16 -> "Midday"; in 17..19 -> "Eve"; else -> "Late" }
}
