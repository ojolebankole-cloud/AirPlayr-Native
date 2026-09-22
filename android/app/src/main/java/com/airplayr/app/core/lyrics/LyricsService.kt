package com.airplayr.app.core.lyrics

import android.content.Context

/**
 * Backwards-compatible facade for existing AirPlayr UI/editor call sites.
 * New lyrics work should go through LyricsRepository/providers behind this facade.
 */
class LyricsService(private val context: Context) {
    data class Cue(val start: Double, val end: Double, val text: String)

    data class Result(
        val provider: String,
        val trackName: String,
        val artistName: String,
        val albumName: String,
        val duration: Double,
        val plainLyrics: String?,
        val syncedLyrics: String?,
        val cues: List<Cue>,
        val confidence: Int,
        val syncLevel: Int = if (!syncedLyrics.isNullOrBlank()) 2 else if (!plainLyrics.isNullOrBlank()) 1 else 0,
    )

    companion object {
        const val TAG = "LyricsService"
        const val MIN_ACCEPT_SCORE = LyricsMatcher.MIN_ACCEPT_SCORE

        internal fun metadataScoreForTest(title: String, artist: String, album: String, duration: Double, candidate: Result): Int =
            LyricsMatcher.score(LyricsTrack(title, artist, album, duration), candidate)
    }

    private val repository by lazy { LyricsRepository(context.applicationContext) }

    fun fetch(id: String, title: String, artist: String, album: String?, duration: Double, force: Boolean = false): Result? =
        repository.fetch(id, LyricsTrack(title.trim(), artist.trim(), album.orEmpty().trim(), duration.coerceAtLeast(0.0)), force)

    fun getCached(id: String): Result? = repository.getCached(id)
    fun clear(id: String) = repository.clear(id)
    fun cachedIds(): Set<String> = repository.cachedIds()
    fun clearAll() = repository.clearAll()
    fun save(id: String, result: Result) = repository.save(id, result)
    fun parseLrc(text: String): List<Cue> = repository.parseLrc(text)
}
