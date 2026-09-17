package com.airplayr.app.ui.components

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType

@Composable
fun Artwork(
    uri: String?,
    modifier: Modifier = Modifier.size(64.dp),
    radius: Dp = 16.dp,
    contentDescription: String? = null,
    mediaType: MediaType? = null,
    fallbackIcon: ImageVector = Icons.Default.MusicNote,
) {
    val context = LocalContext.current
    val bitmap = produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri, mediaType) {
        value = withContext(Dispatchers.IO) {
            uri?.takeIf { it.isNotBlank() }?.let { raw ->
                if (mediaType == MediaType.VIDEO) loadVideoThumbnail(context, raw)
                else loadArtwork(context, raw)
            }
        }
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(radius)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap.value
        if (image != null) {
            Image(image, contentDescription, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(fallbackIcon, contentDescription, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size((modifierWidth(modifier) * 0.36f).coerceAtLeast(18f).dp))
        }
    }
}

fun MediaItem.displayArtworkUri(): String? = when (mediaType) {
    MediaType.VIDEO -> thumbnailUri ?: uri
    MediaType.AUDIO -> artworkUri ?: thumbnailUri
}

private fun loadArtwork(context: android.content.Context, raw: String): androidx.compose.ui.graphics.ImageBitmap? =
    runCatching {
        context.contentResolver.openInputStream(Uri.parse(raw))?.use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()

private fun loadVideoThumbnail(context: android.content.Context, raw: String): androidx.compose.ui.graphics.ImageBitmap? {
    val uri = Uri.parse(raw)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            context.contentResolver.loadThumbnail(uri, Size(640, 360), null)?.asImageBitmap()
        }.getOrNull()?.let { return it }
    }
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.asImageBitmap()
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

private fun modifierWidth(modifier: Modifier): Float = 64f

@Composable
fun ArtworkBackdrop(
    uri: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 0.42f,
    scale: Float = 1.08f,
) {
    val context = LocalContext.current
    val bitmap = produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            uri?.takeIf { it.isNotBlank() }?.let { raw ->
                runCatching {
                    context.contentResolver.openInputStream(android.net.Uri.parse(raw))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    Box(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(0.dp))) {
        bitmap.value?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .alpha(alpha),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
