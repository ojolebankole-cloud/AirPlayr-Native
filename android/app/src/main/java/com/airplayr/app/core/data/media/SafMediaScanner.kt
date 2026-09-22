package com.airplayr.app.core.data.media

import android.content.Context
import android.net.Uri
import android.media.MediaMetadataRetriever
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafMediaScanner(private val context: Context) {
    suspend fun scanTree(treeUri: Uri, mediaStoreRepository: MediaLibraryRepository): Int {
        if (DocumentFile.fromTreeUri(context, treeUri) == null) return 0
        return scanTreeDetailed(treeUri, mediaStoreRepository).total
    }

    suspend fun scanTreeDetailed(
        treeUri: Uri,
        mediaStoreRepository: MediaLibraryRepository,
        onProgress: ((found: Int) -> Unit)? = null,
    ): SafScanResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("The selected folder is no longer accessible")
        val rootString = treeUri.toString()
        val items = ArrayList<MediaItem>()
        walk(root, rootUri = rootString, relativePath = "", output = items, onProgress = onProgress)
        val refresh = mediaStoreRepository.syncSupplementalRoot(rootString, items)
        SafScanResult(
            total = items.size,
            addedAudio = refresh.addedAudio,
            updatedAudio = refresh.updatedAudio,
            addedVideo = refresh.addedVideo,
            updatedVideo = refresh.updatedVideo,
        )
    }

    private fun walk(
        node: DocumentFile,
        rootUri: String,
        relativePath: String,
        output: MutableList<MediaItem>,
        onProgress: ((found: Int) -> Unit)?,
    ) {
        if (output.size >= MediaLibraryRepository.MAX_LIBRARY_ITEMS) return
        for (child in node.listFiles()) {
            if (child.isDirectory) {
                val childPath = listOf(relativePath, child.name.orEmpty())
                    .filter { it.isNotBlank() }
                    .joinToString("/")
                walk(child, rootUri, childPath, output, onProgress)
                continue
            }
            if (!child.isFile) continue
            val mime = child.type ?: ""
            val name = child.name ?: "Media"
            val mediaType = when {
                mime.startsWith("audio/") || name.extensionLower() in AUDIO_EXTENSIONS -> MediaType.AUDIO
                mime.startsWith("video/") || name.extensionLower() in VIDEO_EXTENSIONS -> MediaType.VIDEO
                else -> null
            } ?: continue
            val id = "${mediaType.value}:saf:${stableId(child.uri)}"
            val relative = relativePath.ifBlank { null }
            val genre = extractGenre(child.uri)
            output += MediaItem(
                id = id,
                mediaStoreId = null,
                uri = child.uri.toString(),
                title = name.substringBeforeLast('.').ifBlank { name },
                filename = name,
                artist = null,
                album = null,
                albumArtist = null,
                durationMs = 0,
                width = 0,
                height = 0,
                sizeBytes = child.length().coerceAtLeast(0L),
                dateModified = child.lastModified().coerceAtLeast(0L) / 1000L,
                dateAdded = 0L,
                mediaType = mediaType,
                mimeType = mime.ifBlank { null },
                albumId = null,
                artworkUri = null,
                thumbnailUri = if (mediaType == MediaType.VIDEO) child.uri.toString() else null,
                genre = genre,
                sourceDirectoryUri = rootUri,
                folderId = stableId(node.uri),
                folder = relative,
                path = relative,
                relativePath = relative,
            )
            if (output.size == 1 || output.size % 25 == 0) onProgress?.invoke(output.size)
            if (output.size >= MediaLibraryRepository.MAX_LIBRARY_ITEMS) return
        }
    }

    private fun extractGenre(uri: Uri): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.trim()?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun stableId(value: Uri): String = fnv1a(value.toString())

    private fun fnv1a(value: String): String {
        var hash = 2166136261L
        value.forEach { ch ->
            hash = (hash xor ch.code.toLong()) * 16777619L and 0xFFFF_FFFFL
        }
        return hash.toString(16)
    }

    companion object {
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus", "wma", "amr", "aiff", "alac", "mka")
        private val VIDEO_EXTENSIONS = setOf("mp4", "m4v", "mov", "mkv", "webm", "avi", "wmv", "3gp", "ts", "m2ts", "mts", "flv")
    }
}

data class SafScanResult(
    val total: Int,
    val addedAudio: Int,
    val updatedAudio: Int,
    val addedVideo: Int,
    val updatedVideo: Int,
)

private fun String.extensionLower(): String = substringAfterLast('.', "").lowercase()
