package com.airplayr.app.core.playback

import com.airplayr.app.core.data.media.MediaItem

const val REPEAT_OFF = 0
const val REPEAT_ONE = 1
const val REPEAT_ALL = 2

/** Small immutable snapshot exposed to the UI; the service remains the playback authority. */
data class PlaybackSnapshot(
    val state: Int = 1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val current: MediaItem? = null,
    val queue: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = REPEAT_OFF,
    val error: String? = null,
)
