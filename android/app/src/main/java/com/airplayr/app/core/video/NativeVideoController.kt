package com.airplayr.app.core.video

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.playback.service.MediaItemMapper

/** Video-only player. It never joins the music MediaSession/notification pipeline. */
class NativeVideoController(private val context: Context) {
    private var player: ExoPlayer? = null
    private var decoderMode: String = "auto"

    fun connectAsync(mode: String = "auto", onReady: (Player) -> Unit) {
        val normalized = mode.lowercase().ifBlank { "auto" }
        val existing = player
        if (existing != null && decoderMode == normalized) {
            onReady(existing)
            return
        }
        if (existing != null) existing.release()

        val selector = when (normalized) {
            "software" -> MediaCodecSelector.PREFER_SOFTWARE
            "hardware" -> HardwareFirstMediaCodecSelector
            else -> MediaCodecSelector.DEFAULT
        }
        val renderers = DefaultRenderersFactory(context.applicationContext)
            .setMediaCodecSelector(selector)
            .setEnableDecoderFallback(true)
        val created = ExoPlayer.Builder(context.applicationContext)
            .setRenderersFactory(renderers)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        decoderMode = normalized
        player = created
        onReady(created)
    }

    fun playVideo(p: Player, item: MediaItem, startMs: Long = 0L) {
        p.setMediaItem(MediaItemMapper.toExo(item), startMs.coerceAtLeast(0L))
        p.prepare()
        p.play()
    }

    fun playExternal(p: Player, uri: String, mimeType: String?, title: String) {
        val resolvedMime = mimeType?.takeIf { it.isNotBlank() } ?: inferVideoMimeType(uri)
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

    @OptIn(androidx.media3.common.util.UnstableApi::class)
    private object HardwareFirstMediaCodecSelector : MediaCodecSelector {
        override fun getDecoderInfos(
            mimeType: String,
            requiresSecureDecoder: Boolean,
            requiresTunnelingDecoder: Boolean,
        ): List<MediaCodecInfo> {
            return MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                .sortedBy { isSoftwareCodec(it.name) }
        }

        private fun isSoftwareCodec(name: String): Boolean {
            val n = name.lowercase()
            return n.contains("google") || n.contains("software") || n.contains("c2.android") || n.contains(".sw") || n.contains("sw.")
        }
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
