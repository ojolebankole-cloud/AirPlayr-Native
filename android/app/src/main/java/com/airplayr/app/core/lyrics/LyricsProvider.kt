package com.airplayr.app.core.lyrics

/** A single lyrics source. Providers must be side-effect free with respect to AirPlayr playback. */
interface LyricsProvider {
    val id: String
    fun fetch(track: LyricsTrack, http: LyricsHttpClient): LyricsService.Result?
}

data class LyricsTrack(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Double,
)
