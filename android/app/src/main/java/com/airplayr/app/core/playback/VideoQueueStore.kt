package com.airplayr.app.core.playback

import com.airplayr.app.core.data.media.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shared video playback queue.  The native video player remains responsible for actual
 * decoding/playback; this store owns ordering and the current queue cursor so the UI can
 * provide reliable previous/next/queue operations.
 */
object VideoQueueStore {
    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private val _currentId = MutableStateFlow<String?>(null)
    val currentId: StateFlow<String?> = _currentId.asStateFlow()

    fun setCurrent(item: MediaItem) {
        _currentId.value = item.id
        _queue.update { current ->
            if (current.any { it.id == item.id }) current else current + item
        }
    }

    fun add(items: List<MediaItem>) {
        if (items.isEmpty()) return
        _queue.update { current -> current + items.filter { candidate -> current.none { it.id == candidate.id } } }
    }

    fun addNext(items: List<MediaItem>, currentId: String? = _currentId.value) {
        if (items.isEmpty()) return
        _queue.update { current ->
            val incoming = items.filter { candidate -> current.none { it.id == candidate.id } }
            if (incoming.isEmpty()) return@update current
            val index = current.indexOfFirst { it.id == currentId }
            if (index < 0) current + incoming
            else current.toMutableList().apply { addAll(index + 1, incoming) }
        }
    }

    fun playNext(items: List<MediaItem>) = addNext(items, _currentId.value)

    fun remove(id: String) {
        if (id == _currentId.value) return
        _queue.update { it.filterNot { item -> item.id == id } }
    }

    fun move(id: String, delta: Int) {
        if (delta == 0) return
        _queue.update { current ->
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            val target = (index + delta).coerceIn(0, current.lastIndex)
            if (target == index) return@update current
            current.toMutableList().apply { add(target, removeAt(index)) }
        }
    }
    fun moveUpNext(id: String, delta: Int) {
        if (delta == 0) return
        _queue.update { current ->
            val index = current.indexOfFirst { it.id == id }
            if (index < 0) return@update current
            val currentIndex = current.indexOfFirst { it.id == _currentId.value }
            val minimum = if (currentIndex >= 0) currentIndex + 1 else 0
            val target = (index + delta).coerceIn(minimum, current.lastIndex)
            if (target == index) return@update current
            current.toMutableList().apply { add(target, removeAt(index)) }
        }
    }

    fun next(currentId: String? = _currentId.value): MediaItem? {
        val id = currentId ?: return null
        val items = _queue.value
        val index = items.indexOfFirst { it.id == id }
        return if (index >= 0 && index + 1 < items.size) items[index + 1] else null
    }

    fun previous(currentId: String? = _currentId.value): MediaItem? {
        val id = currentId ?: return null
        val items = _queue.value
        val index = items.indexOfFirst { it.id == id }
        return if (index > 0) items[index - 1] else null
    }

    fun clearUpNext() {
        val current = _currentId.value
        _queue.update { items -> if (current == null) emptyList() else items.filter { it.id == current } }
    }

    fun clear() {
        _queue.value = emptyList()
        _currentId.value = null
    }
}
