package com.airplayr.app.ui.playback

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.playback.REPEAT_ALL
import com.airplayr.app.core.playback.REPEAT_OFF
import com.airplayr.app.core.playback.REPEAT_ONE
import com.airplayr.app.core.playback.service.MediaItemMapper
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackController(private val appContext: Context) : ViewModel() {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var connection: ListenableFuture<MediaController>? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }
    }

    init {
        connect()
        viewModelScope.launch {
            while (isActive) {
                controller?.let(::publish)
                delay(500L)
            }
        }
    }

    private fun connect() {
        val token = SessionToken(
            appContext,
            ComponentName(appContext, com.airplayr.app.core.playback.service.AirPlayrPlaybackService::class.java),
        )
        connection = MediaController.Builder(appContext, token).buildAsync().also { future ->
            future.addListener({
                runCatching {
                    controller = future.get().also { mediaController ->
                        mediaController.addListener(listener)
                        publish(mediaController)
                    }
                }.onFailure { error ->
                    _state.value = _state.value.copy(connected = false, error = error.message ?: "Playback connection failed")
                }
            }, MoreExecutors.directExecutor())
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controller?.release()
        connection?.cancel(false)
        super.onCleared()
    }

    fun play(media: MediaItem) = playQueue(listOf(media), 0)

    fun playQueue(items: List<MediaItem>, startIndex: Int = 0) {
        val c = controller ?: return
        if (items.isEmpty()) return
        c.setMediaItems(items.map(MediaItemMapper::toExo), startIndex.coerceIn(0, items.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun addToQueue(media: MediaItem) { controller?.addMediaItem(MediaItemMapper.toExo(media)) }
    fun playNext(media: MediaItem) { controller?.addMediaItem((controller?.currentMediaItemIndex ?: -1) + 1, MediaItemMapper.toExo(media)) }
    fun removeFromQueue(index: Int) { controller?.removeMediaItem(index) }
    fun clearQueue() { controller?.clearMediaItems() }
    fun togglePlayPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0L)) }
    fun skipNext() { controller?.seekToNextMediaItem() }
    fun skipPrevious() { controller?.seekToPreviousMediaItem() }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled; publish(it) }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> REPEAT_ALL
                Player.REPEAT_MODE_ALL -> REPEAT_ONE
                else -> REPEAT_OFF
            }
            publish(it)
        }
    }

    fun setSpeed(speed: Float) { controller?.setPlaybackSpeed(speed.coerceIn(0.25f, 4f)); viewModelScope.launch { com.airplayr.app.core.playback.dsp.DspRepository(appContext).setSpeed(speed.toDouble()) } }

    fun setPitchSemitones(value: Int) { viewModelScope.launch { com.airplayr.app.core.playback.dsp.DspRepository(appContext).setPitch(value) } }
    fun setPreservePitch(value: Boolean) { viewModelScope.launch { com.airplayr.app.core.playback.dsp.DspRepository(appContext).setPreservePitch(value) } }
    fun setLockPitch(value: Boolean) { viewModelScope.launch { com.airplayr.app.core.playback.dsp.DspRepository(appContext).setLockPitch(value) } }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _state.value = PlaybackUiState(
            connected = true,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it >= 0L } ?: 0L,
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
            currentId = item?.mediaId,
            currentTitle = item?.mediaMetadata?.title?.toString(),
            currentArtist = item?.mediaMetadata?.artist?.toString(),
            currentQueueIndex = player.currentMediaItemIndex,
            queueSize = player.mediaItemCount,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            error = player.playerError?.message,
        )
    }
}

data class PlaybackUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val currentId: String? = null,
    val currentTitle: String? = null,
    val currentArtist: String? = null,
    val currentQueueIndex: Int = 0,
    val queueSize: Int = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = REPEAT_OFF,
    val error: String? = null,
)

class PlaybackControllerFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PlaybackController(context.applicationContext) as T
}
