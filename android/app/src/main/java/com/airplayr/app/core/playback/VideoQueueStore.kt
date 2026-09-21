package com.airplayr.app.core.playback

import com.airplayr.app.core.data.media.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shared video queue data for the folder actions and the later video-queue UI.
 * Video playback itself remains owned by the native video player; this store only
 * owns the ordered upcoming list so folder actions do not discard video selections.
 */
object VideoQueueStore {
    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    fun add(items: List<MediaItem>) {
        if (items.isEmpty()) return
        _queue.update { current ->
            (current + items).distinctBy { it.id }
        }
    }

    fun playNext(items: List<MediaItem>) {
        if (items.isEmpty()) return
        _queue.update { current ->
            (items + current).distinctBy { it.id }
        }
    }

    fun remove(id: String) {
        _queue.update { it.filterNot { item -> item.id == id } }
    }

    fun clear() {
        _queue.value = emptyList()
    }
}
