@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.airplayr.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors

/** The single canonical media-details sheet used throughout AirPlayr. */
@Composable
fun MediaDetailsSheet(
    item: MediaItem,
    playing: Boolean = false,
    onDismiss: () -> Unit,
    onEditMetadata: (() -> Unit)? = null,
) {
    val colors = LocalAirPlayrColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Artwork(
                item.artworkUri ?: item.thumbnailUri,
                if (item.mediaType == MediaType.VIDEO) Modifier.fillMaxWidth().height(190.dp) else Modifier.size(110.dp),
                20.dp,
                item.title,
                item.mediaType,
                if (item.mediaType == MediaType.VIDEO) com.airplayr.app.ui.theme.AirPlayrIcons.Video else com.airplayr.app.ui.theme.AirPlayrIcons.Music,
                isPlaying = playing,
            )
            Text(item.title.ifBlank { item.filename }, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 25.sp, color = colors.textPrimary)
            Text(
                listOfNotNull(item.artist, item.album, item.genre).joinToString(" • ").ifBlank { item.filename },
                color = colors.textSecondary,
            )
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLine("Album", item.album)
                    DetailLine("Genre", item.genre)
                    if (item.mediaType == MediaType.VIDEO && item.width > 0 && item.height > 0) {
                        DetailLine("Resolution", "${item.width} × ${item.height}")
                    }
                    DetailLine("Duration", formatDetailTime(item.durationMs))
                    DetailLine("Size", formatDetailBytes(item.sizeBytes))
                    DetailLine("Format", item.mimeType ?: item.filename.substringAfterLast('.', "Unknown").uppercase())
                    DetailLine("Location", item.path ?: item.relativePath ?: item.folder)
                }
            }
            if (item.mediaType == MediaType.VIDEO) {
                DetailLine("Date added", formatDetailDate(item.dateAdded))
                DetailLine("Date modified", formatDetailDate(item.dateModified))
            }
            if (onEditMetadata != null && item.mediaType == MediaType.AUDIO) {
                Button(onClick = onEditMetadata, modifier = Modifier.fillMaxWidth()) { Text("Edit metadata") }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(90.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

private fun formatDetailTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000L
    val h = total / 3600L
    val m = (total % 3600L) / 60L
    val s = total % 60L
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatDetailBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}

private fun formatDetailDate(seconds: Long): String =
    if (seconds <= 0L) "Unknown" else java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.MEDIUM,
        java.text.DateFormat.SHORT,
    ).format(java.util.Date(seconds * 1000L))
