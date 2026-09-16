package com.airplayr.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaLibraryRepository
import com.airplayr.app.core.data.media.MediaType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val refreshing: Boolean = false,
    val lastRefreshAudio: Int? = null,
    val lastRefreshVideo: Int? = null,
    val error: String? = null,
)

class LibraryViewModel(
    private val repository: MediaLibraryRepository,
) : ViewModel() {
    val audio: StateFlow<List<MediaItem>> = repository.observe(MediaType.AUDIO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val video: StateFlow<List<MediaItem>> = repository.observe(MediaType.VIDEO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state

    fun refresh() {
        if (_state.value.refreshing) return
        viewModelScope.launch {
            _state.value = _state.value.copy(refreshing = true, error = null)
            runCatching { repository.deltaRefresh() }
                .onSuccess { result ->
                    _state.value = LibraryUiState(
                        refreshing = false,
                        lastRefreshAudio = result.audioCount,
                        lastRefreshVideo = result.videoCount,
                    )
                }
                .onFailure { error ->
                    _state.value = LibraryUiState(refreshing = false, error = error.message ?: "Library refresh failed")
                }
        }
    }

    fun onPermissionsChanged(granted: Boolean) {
        if (granted) refresh()
    }
}

class LibraryViewModelFactory(
    private val repository: MediaLibraryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LibraryViewModel(repository) as T
}
