package com.airplayr.app.core.data.media

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafMediaScanner(private val context: Context) {
    suspend fun scanTree(treeUri: Uri, mediaStoreRepository: MediaLibraryRepository): Int = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0
        val rootString = treeUri.toString()
        // Reconcile this SAF root before inserting the current scan. This removes files that
        // were deleted or moved since the previous scan instead of leaving stale library rows.
        mediaStoreRepository.clearSupplementalRoot(MediaType.AUDIO, rootString)
        mediaStoreRepository.clearSupplementalRoot(MediaType.VIDEO, rootString)
        val items = ArrayList<MediaItem>()
        walk(root, rootUri = rootString, relativePath = "", output = items)
        if (items.isNotEmpty()) {
            val audio = items.filter { it.mediaType == MediaType.AUDIO }
            val video = items.filter { it.mediaType == MediaType.VIDEO }
            // SAF media is supplementary to MediaStore. Persisting it through the same Room
            // table gives the native library one consistent source for UI consumers.
            if (audio.isNotEmpty()) mediaStoreRepository.insertSupplemental(MediaType.AUDIO, audio)
            if (video.isNotEmpty()) mediaStoreRepository.insertSupplemental(MediaType.VIDEO, video)
        }
        items.size
    }

    private fun walk(
        node: DocumentFile,
        rootUri: String,
        relativePath: String,
        output: MutableList<MediaItem>,
    ) {
        if (output.size >= MediaLibraryRepository.MAX_LIBRARY_ITEMS) return
        for (child in node.listFiles()) {
            if (child.isDirectory) {
                val childPath = listOf(relativePath, child.name.orEmpty()).filter { it.isNotBlank() }.joinToString("/")
                walk(child, rootUri, childPath, output)
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
                genre = null,
                sourceDirectoryUri = rootUri,
                folderId = stableId(node.uri),
                folder = relative,
                path = relative,
                relativePath = relative,
            )
            if (output.size >= MediaLibraryRepository.MAX_LIBRARY_ITEMS) return
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

private fun String.extensionLower(): String = substringAfterLast('.', "").lowercase()
