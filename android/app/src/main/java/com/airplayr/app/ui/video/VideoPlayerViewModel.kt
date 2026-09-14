package com.airplayr.app.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airplayr.app.core.data.StructuredRepository
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaLibraryRepository
import com.airplayr.app.core.data.media.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VideoPlayerViewModel(private val library: MediaLibraryRepository, private val structured: StructuredRepository) : ViewModel() {
    private val _video = MutableStateFlow<MediaItem?>(null)
    val video: StateFlow<MediaItem?> = _video.asStateFlow()
    private val _resume = MutableStateFlow(0L)
    val resume: StateFlow<Long> = _resume.asStateFlow()
    fun load(id: String) { viewModelScope.launch { _video.value = library.get(id); _resume.value = structured.videoProgress(id).first()?.positionMs ?: 0L } }
    fun saveProgress(id: String, position: Long, duration: Long) { viewModelScope.launch { structured.saveVideoProgress(id, position, duration) } }
    fun bookmark(id: String, position: Long, label: String?) { viewModelScope.launch { structured.addBookmark(id, position, label) } }
    fun removeBookmark(id: String, position: Long) { viewModelScope.launch { structured.removeBookmark(id, position) } }
    suspend fun isFavorite(id: String): Boolean = structured.isFavorite(id)
    fun toggleFavorite(item: MediaItem) { viewModelScope.launch { structured.toggleFavorite(item) } }
}
class VideoPlayerViewModelFactory(private val library: MediaLibraryRepository, private val structured: StructuredRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = VideoPlayerViewModel(library, structured) as T
}
