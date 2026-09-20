package com.airplayr.app.core.video

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.playback.service.MediaItemMapper

/**
 * Video-only player. Videos deliberately do not connect to AirPlayrPlaybackService/MediaSession,
 * because that session is reserved for music and is therefore responsible for the Android
 * playback notification, lock-screen controls, and music mini-player state.
 */
class NativeVideoController(private val context: Context) {
    private var player: ExoPlayer? = null

    fun connectAsync(onReady: (Player) -> Unit) {
        val existing = player
        if (existing != null) {
            onReady(existing)
            return
        }

        val created = ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = created
        onReady(created)
    }

    fun playVideo(p: Player, item: MediaItem, startMs: Long = 0L) {
        p.setMediaItem(MediaItemMapper.toExo(item), startMs.coerceAtLeast(0L))
        p.prepare()
        p.play()
    }

    fun playExternal(p: Player, uri: String, mimeType: String?, title: String) {
        val resolvedMime = mimeType?.takeIf { it.isNotBlank() }
            ?: inferVideoMimeType(uri)
        val item = androidx.media3.common.MediaItem.Builder()
            .setMediaId("external-video:$uri")
            .setUri(uri)
            .apply { if (resolvedMime != null) setMimeType(resolvedMime) }
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title.ifBlank { "Video" })
                    .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_VIDEO)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
        p.setMediaItem(item)
        p.prepare()
        p.play()
    }

    fun release() {
        player?.release()
        player = null
    }
}


private fun inferVideoMimeType(uri: String): String? {
    val name = uri.substringBefore('?').substringAfterLast('/')
    return when (name.substringAfterLast('.', "").lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "avi" -> "video/avi"
        "wmv" -> "video/x-ms-wmv"
        "3gp" -> "video/3gpp"
        "ts", "m2ts", "mts" -> "video/mp2t"
        "flv" -> "video/x-flv"
        else -> null
    }
}
