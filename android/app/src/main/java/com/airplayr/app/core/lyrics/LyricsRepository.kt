package com.airplayr.app.core.lyrics

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap
import com.airplayr.app.BuildConfig
import com.airplayr.app.core.data.db.LyricsCacheDao
import com.airplayr.app.core.data.db.LyricsCacheEntity
import org.json.JSONObject

/**
 * Single lyrics orchestration layer used by the whole app.
 * It owns cache, provider order, matching, diagnostics and result selection.
 */
class LyricsRepository(
    context: Context,
    private val cacheDao: LyricsCacheDao? = null,
) {
    companion object {
        private const val TAG = "LyricsRepository"
        private const val INDEX_KEY = "cached_ids"
        private const val CACHE_TTL_MS = 30L * 24L * 60L * 60L * 1000L
        private const val MAX_CACHE_ENTRIES = 200
    }

    private val prefs = context.getSharedPreferences("lyrics_cache", Context.MODE_PRIVATE)
    private val http = LyricsHttpClient()
    private val requestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<LyricsService.Result?>>()
    private val providers: List<LyricsProvider> = listOf(
        YouLyPlusProvider(),
        BetterLyricsProvider(),
        UnisonProvider(),
        LrclibProvider(),
        KugouProvider(),
        NetEaseProvider(),
        SimpMusicProvider(),
        PaxsenixProvider(),
        QqMusicProvider(),
        LyricsOvhProvider(),
    )

    /**
     * Returns a shared request for this song id. A second caller awaiting the same id joins
     * the existing network operation instead of starting another provider search. Cache lookup
     * still happens inside the shared operation, so background prefetch and the Lyrics sheet
     * converge on the same result.
     */
    fun fetchShared(id: String, track: LyricsTrack, force: Boolean = false): Deferred<LyricsService.Result?> {
        if (id.isBlank() || track.title.isBlank() || track.artist.isBlank()) {
            return kotlinx.coroutines.CompletableDeferred(null)
        }
        inFlight[id]?.let { return it }
        val created = requestScope.async(start = CoroutineStart.LAZY) {
            fetch(id, track, force)
        }
        val existing = inFlight.putIfAbsent(id, created)
        val actual = existing ?: created
        if (existing == null) {
            actual.invokeOnCompletion { inFlight.remove(id, actual) }
            actual.start()
        }
        return actual
    }

    fun fetch(id: String, track: LyricsTrack, force: Boolean = false): LyricsService.Result? {
        if (id.isBlank() || track.title.isBlank() || track.artist.isBlank()) {
            debug("fetch rejected: missing id/title/artist")
            return null
        }
        if (!force) {
            getCached(id)?.let { cached ->
                val score = LyricsMatcher.score(track, cached)
                if (score >= LyricsMatcher.MIN_ACCEPT_SCORE) {
                    debug("cache hit provider=${cached.provider} score=$score sync=${cached.syncLevel}")
                    return cached.copy(confidence = score)
                }
                debug("cache rejected provider=${cached.provider} score=$score; clearing")
                clear(id)
            }
        }

        var best: LyricsService.Result? = null
        var bestScore = 0
        var bestIndex = Int.MAX_VALUE
        providers.forEachIndexed { index, provider ->
            if (best?.syncLevel == 3 && bestScore >= 90) return@forEachIndexed
            debug("trying provider=${provider.id} title=\"${track.title}\" artist=\"${track.artist}\" duration=${track.duration}")
            val candidate = runCatching { provider.fetch(track, http) }.onFailure { error ->
                debug("provider=${provider.id} failed: ${error.javaClass.simpleName}: ${error.message.orEmpty()}")
            }.getOrNull() ?: return@forEachIndexed
            val score = LyricsMatcher.score(track, candidate)
            debug("provider=${provider.id} candidate title=\"${candidate.trackName}\" artist=\"${candidate.artistName}\" album=\"${candidate.albumName}\" duration=${candidate.duration} score=$score sync=${candidate.syncLevel} cues=${candidate.cues.size}")
            if (score < LyricsMatcher.MIN_ACCEPT_SCORE) {
                debug("provider=${provider.id} rejected by matcher threshold=${LyricsMatcher.MIN_ACCEPT_SCORE}")
                return@forEachIndexed
            }
            val normalized = candidate.copy(confidence = score)
            if (best == null || better(normalized, score, index, best, bestScore, bestIndex)) {
                best = normalized
                bestScore = score
                bestIndex = index
                debug("provider=${provider.id} is current best score=$score sync=${candidate.syncLevel}")
            }
        }

        best?.let {
            save(id, it)
            debug("selected provider=${it.provider} score=${it.confidence} sync=${it.syncLevel}")
        } ?: debug("all ${providers.size} lyrics providers exhausted without an accepted result")
        return best
    }

    private fun better(candidate: LyricsService.Result, score: Int, index: Int, current: LyricsService.Result?, currentScore: Int, currentIndex: Int): Boolean {
        if (current == null) return true
        if (candidate.syncLevel != current.syncLevel) return candidate.syncLevel > current.syncLevel
        if (score != currentScore) return score > currentScore
        return index < currentIndex
    }

    fun getCached(id: String): LyricsService.Result? {
        if (id.isBlank()) return null
        cacheDao?.let { dao ->
            val entry = dao.get(id)
            if (entry != null) {
                val now = System.currentTimeMillis()
                if (!isPermanent(entry.provider) && now - entry.fetchedAt > CACHE_TTL_MS) {
                    dao.delete(id)
                    debug("room cache expired id=$id provider=${entry.provider}")
                    return null
                }
                dao.touch(id, now)
                return entry.toResult()
            }
            // One-time migration path for lyrics cached by the pre-Phase-6 SharedPreferences store.
            val legacy = readLegacyCached(id)
            if (legacy != null) {
                dao.upsert(legacy.toCacheEntity(id, System.currentTimeMillis()))
                debug("migrated legacy lyrics cache id=$id provider=${legacy.provider}")
                return legacy
            }
            return null
        }

        return readLegacyCached(id)
    }

    private fun readLegacyCached(id: String): LyricsService.Result? {
        val raw = prefs.getString(id, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val fetchedAt = o.optLong("fetchedAt", 0L)
            if (!isPermanent(o.optString("provider")) && fetchedAt > 0L && System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS) {
                clear(id)
                return null
            }
            val synced = o.optString("syncedLyrics").takeIf { it.isNotBlank() }
            LyricsService.Result(
                provider = o.optString("provider"),
                trackName = o.optString("trackName"),
                artistName = o.optString("artistName"),
                albumName = o.optString("albumName"),
                duration = o.optDouble("duration", 0.0),
                plainLyrics = o.optString("plainLyrics").takeIf { it.isNotBlank() },
                syncedLyrics = synced,
                cues = LyricsProviderSupport.parseLrc(synced.orEmpty()),
                confidence = o.optInt("confidence", 0),
                syncLevel = o.optInt("syncLevel", if (!synced.isNullOrBlank()) 2 else 1),
            )
        }.onFailure { debug("cache parse failed id=$id: ${it.javaClass.simpleName}: ${it.message.orEmpty()}") }.getOrNull()
    }

    private fun LyricsService.Result.toCacheEntity(id: String, now: Long): LyricsCacheEntity = LyricsCacheEntity(
        id = id,
        provider = provider,
        trackName = trackName,
        artistName = artistName,
        albumName = albumName,
        duration = duration,
        plainLyrics = plainLyrics,
        syncedLyrics = syncedLyrics,
        confidence = confidence,
        syncLevel = syncLevel,
        fetchedAt = now,
        lastAccessAt = now,
    )

    fun save(id: String, result: LyricsService.Result) {
        if (id.isBlank()) return
        val now = System.currentTimeMillis()
        cacheDao?.let { dao ->
            dao.upsert(
                result.toCacheEntity(id, now),
            )
            val overflow = dao.count() - MAX_CACHE_ENTRIES
            if (overflow > 0) dao.deleteLeastRecentlyUsed(overflow)
            return
        }

        prefs.edit().putString(
            id,
            JSONObject().apply {
                put("provider", result.provider)
                put("trackName", result.trackName)
                put("artistName", result.artistName)
                put("albumName", result.albumName)
                put("duration", result.duration)
                put("plainLyrics", result.plainLyrics ?: "")
                put("syncedLyrics", result.syncedLyrics ?: "")
                put("confidence", result.confidence)
                put("syncLevel", result.syncLevel)
                put("fetchedAt", now)
            }.toString(),
        ).putStringSet(INDEX_KEY, (cachedIds() + id).toSet()).apply()
        prunePreferenceCache()
    }

    fun clear(id: String) {
        if (id.isBlank()) return
        cacheDao?.let { it.delete(id); return }
        prefs.edit().remove(id).putStringSet(INDEX_KEY, cachedIds().filterNot { it == id }.toSet()).apply()
    }

    fun cachedIds(): Set<String> = cacheDao?.ids()?.toSet() ?: prefs.getStringSet(INDEX_KEY, emptySet()).orEmpty()

    fun clearAll() {
        cacheDao?.clearAll()
        prefs.edit().clear().apply()
    }

    private fun prunePreferenceCache() {
        val ids = prefs.getStringSet(INDEX_KEY, emptySet()).orEmpty().toMutableSet()
        if (ids.size <= MAX_CACHE_ENTRIES) return
        val ordered = ids.mapNotNull { id ->
            val json = runCatching { JSONObject(prefs.getString(id, "") ?: "") }.getOrNull() ?: return@mapNotNull null
            if (isPermanent(json.optString("provider"))) return@mapNotNull null
            id to json.optLong("fetchedAt", 0L)
        }.sortedBy { it.second }
        val removeCount = ids.size - MAX_CACHE_ENTRIES
        ordered.take(removeCount).forEach { (id, _) -> prefs.edit().remove(id).apply(); ids.remove(id) }
        prefs.edit().putStringSet(INDEX_KEY, ids).apply()
    }

    private fun isPermanent(provider: String): Boolean = provider.equals("Manual", true) || provider.equals("File import", true) || provider.equals("Clipboard", true) || provider.equals("AirPlayr", true)

    private fun LyricsCacheEntity.toResult(): LyricsService.Result {
        val synced = syncedLyrics?.takeIf { it.isNotBlank() }
        return LyricsService.Result(
            provider = provider,
            trackName = trackName,
            artistName = artistName,
            albumName = albumName,
            duration = duration,
            plainLyrics = plainLyrics?.takeIf { it.isNotBlank() },
            syncedLyrics = synced,
            cues = LyricsProviderSupport.parseLrc(synced.orEmpty()),
            confidence = confidence,
            syncLevel = syncLevel,
        )
    }

    fun parseLrc(text: String): List<LyricsService.Cue> = LyricsProviderSupport.parseLrc(text)

    private fun debug(message: String) { if (BuildConfig.DEBUG) Log.d(TAG, message) }
}
