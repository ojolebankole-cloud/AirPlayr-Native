package com.airplayr.app.core.video

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.playback.service.AirPlayrPlaybackService
import com.airplayr.app.core.playback.service.MediaItemMapper
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class NativeVideoController(private val context: Context) {
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    fun connectAsync(onReady: (MediaController) -> Unit) {
        controller?.let { onReady(it); return }
        if (future != null) return
        val token = SessionToken(context.applicationContext, ComponentName(context.applicationContext, AirPlayrPlaybackService::class.java))
        future = MediaController.Builder(context.applicationContext, token).buildAsync().also { f ->
            f.addListener({ runCatching { f.get() }.onSuccess { c -> controller = c; onReady(c) } }, MoreExecutors.directExecutor())
        }
    }

    fun playVideo(c: MediaController, item: MediaItem, startMs: Long = 0L) {
        c.setMediaItem(MediaItemMapper.toExo(item), startMs.coerceAtLeast(0L))
        c.prepare()
        c.play()
    }

    fun release() {
        controller?.release()
        controller = null
        future?.cancel(false)
        future = null
    }
}
