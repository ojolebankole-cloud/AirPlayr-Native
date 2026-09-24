package com.airplayr.app.core.intelligence

import com.airplayr.app.core.data.db.FavoriteEntity
import com.airplayr.app.core.data.db.PlaybackHistoryEntity
import com.airplayr.app.core.data.media.MediaItem
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import kotlin.math.ln
import kotlin.random.Random

/** Configuration for one reusable dedicated music-mix experience. */
data class MusicMixConfig(
    val id: String,
    val title: String,
    val priorityGenres: List<String> = emptyList(),
)

/** Stable, availability-aware daily mix generation. No songs are invented. */
object MusicMixGenerator {
    fun configForHour(hour: Int): MusicMixConfig = when (hour.coerceIn(0, 23)) {
        in 5..8 -> MusicMixConfig("early-morning", "Early Morning Jam", listOf("Gospel", "Highlife", "Afrobeat"))
        in 9..11 -> MusicMixConfig("late-morning", "Late Morning Jam", listOf("Pop"))
        in 12..16 -> MusicMixConfig("afternoon", "Afternoon Mix", listOf("Afrobeat"))
        in 17..19 -> MusicMixConfig("evening", "Evening Jam", listOf("Alté"))
        in 20..22 -> MusicMixConfig("night", "Night Jam", listOf("Hip Hop", "Trap"))
        in 23..23, in 0..1 -> MusicMixConfig("night-extra", "Night Extra")
        else -> MusicMixConfig("late-night", "Late Night Jam", listOf("R&B"))
    }

    fun availableGenres(items: List<MediaItem>): List<String> = items.asSequence()
        .filter { it.mediaType == com.airplayr.app.core.data.media.MediaType.AUDIO }
        .flatMap { splitGenres(it.genre).asSequence() }
        .distinctBy(::normalizeGenre)
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
        .toList()

    fun dailyMix(
        config: MusicMixConfig,
        library: List<MediaItem>,
        history: List<PlaybackHistoryEntity>,
        favorites: List<FavoriteEntity>,
        selectedGenre: String? = null,
        date: LocalDate = LocalDate.now(),
        cycle: Int = 0,
    ): List<MediaItem> {
        val audio = library.filter { it.mediaType == com.airplayr.app.core.data.media.MediaType.AUDIO }
        if (audio.isEmpty()) return emptyList()

        val normalizedFilter = selectedGenre?.let(::normalizeGenre)
        val filtered = if (normalizedFilter == null || normalizedFilter == normalizeGenre("All")) {
            audio
        } else {
            audio.filter { item -> splitGenres(item.genre).any { normalizeGenre(it) == normalizedFilter } }
        }
        if (filtered.isEmpty()) return emptyList()

        val historyById = history.associateBy { it.mediaId }
        val favoriteIds = favorites.asSequence().map { it.mediaId }.toSet()
        val seedText = "${date}|${config.id}|${normalizedFilter.orEmpty()}|cycle=$cycle"
        val random = Random(seedText.hashCode())

        val weighted = when {
            config.priorityGenres.isNotEmpty() -> filtered.map { item ->
                val tier = priorityTier(item, config.priorityGenres)
                val weight = when (tier) { 0 -> 4.0; 1 -> 2.0; else -> 1.0 }
                WeightedItem(item, weight, tier)
            }
            else -> filtered.map { item -> WeightedItem(item, 1.0, 2) }
        }

        // For You is deliberately influenced by real listening/favourite state rather than
        // pretending every library item has equal relevance.
        val listeningWeighted = if (config.id == FOR_YOU_ID) {
            weighted.map { entry ->
                val historyEntry = historyById[entry.item.id]
                val favoriteBoost = if (entry.item.id in favoriteIds) 4.0 else 0.0
                val playBoost = minOf(historyEntry?.playCount ?: 0, 5) * 1.25
                val recentBoost = if (historyEntry != null && date.toEpochDay() - historyEntry.playedAt.toEpochDay() <= 7) 3.0 else 0.0
                entry.copy(weight = 1.0 + favoriteBoost + playBoost + recentBoost)
            }
        } else weighted

        val ordered = listeningWeighted
            .map { entry ->
                val u = random.nextDouble().coerceIn(Double.MIN_VALUE, 1.0)
                entry to (ln(u) / entry.weight)
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .toMutableList()

        if (config.priorityGenres.isNotEmpty()) deClusterPriority(ordered)
        return ordered.map { it.item }
    }

    private fun deClusterPriority(items: MutableList<WeightedItem>) {
        if (items.size < 4) return
        var index = 2
        while (index < items.size) {
            val a = items[index - 2].tier
            val b = items[index - 1].tier
            val c = items[index].tier
            if (a == b && b == c) {
                val replacement = (index + 1 until items.size).firstOrNull { items[it].tier != c }
                if (replacement != null) {
                    val temp = items[index]
                    items[index] = items[replacement]
                    items[replacement] = temp
                }
            }
            index++
        }
    }

    private fun priorityTier(item: MediaItem, priorities: List<String>): Int {
        val genres = splitGenres(item.genre).map(::normalizeGenre)
        val first = priorities.indexOfFirst { normalizeGenre(it) in genres }
        return if (first >= 0) first else priorities.size
    }

    private fun splitGenres(raw: String?): List<String> = raw.orEmpty()
        .split(',', ';', '/', '|')
        .map(String::trim)
        .filter(String::isNotBlank)

    private fun normalizeGenre(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase(Locale.ROOT)
        .filter(Char::isLetterOrDigit)

    private fun Long.toEpochDay(): Long = java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()

    private data class WeightedItem(val item: MediaItem, val weight: Double, val tier: Int)

    const val FOR_YOU_ID = "for-you"
}
