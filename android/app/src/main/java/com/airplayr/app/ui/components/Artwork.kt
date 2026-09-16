package com.airplayr.app.ui.components

import android.graphics.BitmapFactory
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Artwork(
    uri: String?,
    modifier: Modifier = Modifier.size(64.dp),
    radius: Dp = 16.dp,
    contentDescription: String? = null,
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
    Box(
        modifier = modifier.clip(RoundedCornerShape(radius)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap.value
        if (image != null) {
            Image(image, contentDescription, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.MusicNote, contentDescription, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size((modifierWidth(modifier) * 0.36f).coerceAtLeast(18f).dp))
        }
    }
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
