package com.airplayr.app.ui.playback

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.preferences.PreferencesRepository
import com.airplayr.app.core.playback.REPEAT_ALL
import com.airplayr.app.core.playback.REPEAT_OFF
import com.airplayr.app.core.playback.REPEAT_ONE
import com.airplayr.app.core.playback.service.MediaItemMapper
import com.airplayr.app.core.playback.queue.QueueStore
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackController(private val appContext: Context) : ViewModel() {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private val queueStore = QueueStore(appContext)
    private val preferences = PreferencesRepository(appContext)
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

    fun playExternal(uri: String, mimeType: String?, title: String) {
        val c = controller ?: return
        val metadata = MediaMetadata.Builder().setTitle(title).build()
        val media = ExoMediaItem.Builder().setMediaId("external:$uri").setUri(uri).setMimeType(mimeType?.takeIf { it.isNotBlank() } ?: MimeTypes.APPLICATION_OCTET_STREAM).setMediaMetadata(metadata).build()
        c.setMediaItem(media, 0L); c.prepare(); c.play()
    }

    fun playQueue(items: List<MediaItem>, startIndex: Int = 0) {
        val c = controller ?: return
        if (items.isEmpty()) return
        c.setMediaItems(items.map(MediaItemMapper::toExo), startIndex.coerceIn(0, items.lastIndex), 0L)
        c.prepare()
        viewModelScope.launch {
            val settings = preferences.settings.first()
            c.setPlaybackSpeed(settings.defaultSpeed.toFloat().coerceIn(0.25f, 4f))
            c.volume = if (settings.muteOnStart) 0f else settings.defaultVolume.toFloat().coerceIn(0f, 1f)
            if (settings.resumeMode == "start") c.seekTo(0L)
            c.play()
        }
    }

    fun addToQueue(media: MediaItem) { controller?.addMediaItem(MediaItemMapper.toExo(media)) }
    fun playNext(media: MediaItem) { controller?.addMediaItem((controller?.currentMediaItemIndex ?: -1) + 1, MediaItemMapper.toExo(media)) }
    fun playLast(media: MediaItem) { controller?.addMediaItem(MediaItemMapper.toExo(media)) }
    fun removeFromQueue(index: Int) { controller?.removeMediaItem(index) }
    fun clearQueue() { controller?.clearMediaItems() }

    suspend fun saveCurrentQueue(): Boolean {
        val c = controller ?: return false
        val items = (0 until c.mediaItemCount).mapNotNull { index ->
            val m = c.getMediaItemAt(index)
            val uri = m.localConfiguration?.uri?.toString() ?: return@mapNotNull null
            MediaItem(
                id = m.mediaId.ifBlank { "queue:$index:$uri" }, mediaStoreId = null, uri = uri,
                title = m.mediaMetadata.title?.toString()?.ifBlank { "Media" } ?: "Media", filename = m.mediaMetadata.title?.toString() ?: "Media",
                artist = m.mediaMetadata.artist?.toString(), album = m.mediaMetadata.albumTitle?.toString(), albumArtist = m.mediaMetadata.albumArtist?.toString(),
                durationMs = m.mediaMetadata.durationMs ?: 0L, width = 0, height = 0, sizeBytes = 0L, dateModified = 0L, dateAdded = 0L,
                mediaType = if (m.localConfiguration.mimeType?.startsWith("video/") == true) com.airplayr.app.core.data.media.MediaType.VIDEO else com.airplayr.app.core.data.media.MediaType.AUDIO,
                mimeType = m.localConfiguration.mimeType, albumId = null, artworkUri = m.mediaMetadata.artworkUri?.toString(), thumbnailUri = null, genre = null,
                sourceDirectoryUri = null, folderId = null, folder = null, path = null, relativePath = null
            )
        }
        queueStore.save(items, c.currentMediaItemIndex, c.currentMediaItem?.mediaId, c.currentPosition, c.shuffleModeEnabled, c.repeatMode, c.playWhenReady)
        return true
    }

    suspend fun clearSavedQueue() = queueStore.clear()
    fun togglePlayPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun pause() { controller?.pause() }
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
    fun setVolume(value: Float) { controller?.volume = value.coerceIn(0f, 1f) }
    fun volume(): Float = controller?.volume ?: 1f

    fun startSleepTimer(minutes: Int, finishCurrent: Boolean, fadeOut: Boolean) {
        viewModelScope.launch {
            preferences.startSleepTimer(minutes, finishCurrent, fadeOut, volume().toDouble())
        }
    }

    fun cancelSleepTimer() {
        viewModelScope.launch { preferences.cancelSleepTimer() }
    }

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
            currentAlbum = item?.mediaMetadata?.albumTitle?.toString(),
            artworkUri = item?.mediaMetadata?.artworkUri?.toString(),
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
    val currentAlbum: String? = null,
    val artworkUri: String? = null,
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
