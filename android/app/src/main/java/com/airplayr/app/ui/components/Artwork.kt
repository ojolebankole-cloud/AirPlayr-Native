package com.airplayr.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.ui.theme.AirPlayrIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

private object ArtworkMemoryCache {
    private val cache = object : LruCache<String, Bitmap>(32 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(4))
    private val inFlight = mutableMapOf<String, Deferred<Bitmap?>>()

    @Synchronized fun get(key: String): Bitmap? = cache.get(key)
    @Synchronized fun put(key: String, bitmap: Bitmap): Bitmap { cache.put(key, bitmap); return bitmap }

    suspend fun getOrLoad(key: String, loader: suspend () -> Bitmap?): Bitmap? {
        get(key)?.let { return it }
        val job = synchronized(this) {
            inFlight[key] ?: scope.async { loader() }.also { inFlight[key] = it }
        }
        return try {
            job.await()?.let { bitmap -> put(key, bitmap) }
        } finally {
            synchronized(this) { if (inFlight[key] === job) inFlight.remove(key) }
        }
    }
}

@Composable
fun Artwork(
    uri: String?,
    modifier: Modifier = Modifier.size(64.dp),
    radius: Dp = 16.dp,
    contentDescription: String? = null,
    mediaType: MediaType? = null,
    fallbackIcon: ImageVector = when (mediaType) { MediaType.VIDEO -> AirPlayrIcons.Video; else -> AirPlayrIcons.Music },
    fallbackIconSize: Dp = 22.dp,
    fallbackIconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = com.airplayr.app.ui.theme.AirPlayrArtworkTokens.fallbackIconAlpha),
    isPlaying: Boolean = false,
    initialTargetDp: Dp? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var measuredSize by remember(uri, mediaType) { mutableStateOf(IntSize.Zero) }
    val initialTargetPx = remember(initialTargetDp, density) { initialTargetDp?.let { with(density) { it.roundToPx() } } ?: 0 }
    val targetWidth = bucketTarget(measuredSize.width.takeIf { it > 0 } ?: initialTargetPx)
    val targetHeight = bucketTarget(measuredSize.height.takeIf { it > 0 } ?: initialTargetPx)
    val bitmap by produceState<ImageBitmap?>(null, uri, mediaType, targetWidth, targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            uri?.takeIf { it.isNotBlank() }?.let { raw ->
                val key = "${mediaType?.value ?: "audio"}:$raw:${targetWidth}x${targetHeight}"
                ArtworkMemoryCache.getOrLoad(key) { loadBitmap(context, raw, mediaType, targetWidth, targetHeight) }?.asImageBitmap()
            }
        }
    }
    Box(
        modifier = modifier
            .onSizeChanged { measuredSize = it }
            .clip(RoundedCornerShape(radius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) Image(bitmap!!, contentDescription, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        else Icon(fallbackIcon, contentDescription, tint = fallbackIconTint, modifier = Modifier.size(fallbackIconSize))
        if (isPlaying && mediaType != MediaType.VIDEO) PlayingWaveIndicator()
    }
}

@Composable private fun BoxScope.PlayingWaveIndicator() {
    val transition = rememberInfiniteTransition(label = "artwork-playing-wave")
    val a by transition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "wave-a")
    val b by transition.animateFloat(1f, 0.42f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "wave-b")
    val c by transition.animateFloat(0.5f, 0.95f, infiniteRepeatable(tween(460), RepeatMode.Reverse), label = "wave-c")
    Box(Modifier.align(Alignment.BottomEnd).padding(5.dp).size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        Row(Modifier.height(15.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) { WaveBar(a); WaveBar(b); WaveBar(c) }
    }
}
@Composable private fun WaveBar(scale: Float) {
    Box(Modifier.width(3.dp).height((14f * scale).dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onPrimary))
}

fun MediaItem.displayArtworkUri(): String? = when (mediaType) { MediaType.VIDEO -> thumbnailUri ?: uri; MediaType.AUDIO -> artworkUri ?: thumbnailUri }

private fun loadBitmap(context: android.content.Context, raw: String, mediaType: MediaType?, targetWidth: Int, targetHeight: Int): Bitmap? {
    if (mediaType == MediaType.VIDEO) return loadVideoThumbnail(context, raw, targetWidth, targetHeight)
    return runCatching {
        val uri = Uri.parse(raw)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSample(bounds.outWidth, bounds.outHeight, targetWidth, targetHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
}

private fun loadVideoThumbnail(context: android.content.Context, raw: String, targetWidth: Int, targetHeight: Int): Bitmap? {
    val uri = Uri.parse(raw)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching { context.contentResolver.loadThumbnail(uri, Size(targetWidth, targetHeight), null) }.getOrNull()?.let { return it }
    }
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                retriever.getScaledFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetWidth, targetHeight)
            } else {
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { frame -> scaleBitmap(frame, targetWidth, targetHeight) }
            }
        } finally { retriever.release() }
    }.getOrNull()
}

private fun scaleBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
    if (source.width <= targetWidth && source.height <= targetHeight) return source
    val scale = minOf(targetWidth.toFloat() / source.width, targetHeight.toFloat() / source.height)
    val w = (source.width * scale).toInt().coerceAtLeast(1)
    val h = (source.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, w, h, true)
}

private fun bucketTarget(value: Int): Int = if (value <= 0) 0 else ((value.coerceIn(64, 512) + 15) / 16) * 16

private fun calculateSample(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sample = 1
    while (width / (sample * 2) >= targetWidth && height / (sample * 2) >= targetHeight) sample *= 2
    return sample
}

@Composable
fun ArtworkBackdrop(
    uri: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 0.42f,
    scale: Float = 1.08f,
    targetSizePx: Int = 320,
) {
    val context = LocalContext.current
    val target = targetSizePx.coerceIn(160, 512)
    val bitmap = produceState<ImageBitmap?>(null, uri, target) {
        value = withContext(Dispatchers.IO) {
            uri?.takeIf { it.isNotBlank() }?.let { raw ->
                val key = "backdrop:$raw:$target"
                ArtworkMemoryCache.getOrLoad(key) { loadBitmap(context, raw, null, target, target) }?.asImageBitmap()
            }
        }
    }
    Box(modifier.clip(RoundedCornerShape(0.dp))) {
        bitmap.value?.let { Image(it, null, Modifier.matchParentSize().graphicsLayer { scaleX = scale; scaleY = scale }.alpha(alpha), contentScale = ContentScale.Crop) }
    }
}
