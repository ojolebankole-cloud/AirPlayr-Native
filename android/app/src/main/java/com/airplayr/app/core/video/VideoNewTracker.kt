package com.airplayr.app.core.video

import android.content.Context
import com.airplayr.app.core.data.media.MediaItem

/** Durable "new video" state. New means discovered by a library refresh, not merely recent by date. */
class VideoNewTracker(context: Context) {
    private val prefs = context.getSharedPreferences("airplayr_video_new", Context.MODE_PRIVATE)
    private val lock = Any()

    fun syncLibrary(videos: List<MediaItem>) {
        synchronized(lock) {
            val current = videos.map { it.id }.toSet()
            if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
                prefs.edit()
                    .putStringSet(KEY_KNOWN, current)
                    .putStringSet(KEY_LAST_SYNC, current)
                    .putBoolean(KEY_INITIALIZED, true)
                    .apply()
                return
            }
            val previous = prefs.getStringSet(KEY_LAST_SYNC, emptySet()).orEmpty()
            val newlyDiscovered = current - previous
            val retainedNew = readSet(KEY_NEW).intersect(current)
            prefs.edit()
                .putStringSet(KEY_KNOWN, current)
                .putStringSet(KEY_LAST_SYNC, current)
                .putStringSet(KEY_NEW, retainedNew + newlyDiscovered)
                .apply()
        }
    }

    fun markVideoSeen(id: String) = synchronized(lock) { writeSet(KEY_NEW, readSet(KEY_NEW) - id) }

    fun folderNewCount(folderPath: String, videos: List<MediaItem>): Int {
        val seenAt = folderSeenAt(folderPath)
        return videos.count { it.id in newVideoIds() && it.dateAdded > seenAt }
    }

    fun markFolderSeen(folderPath: String, videos: List<MediaItem>) {
        val newest = videos.maxOfOrNull { it.dateAdded } ?: System.currentTimeMillis()
        prefs.edit().putLong("folder:$folderPath", newest).apply()
    }

    private fun folderSeenAt(path: String): Long = prefs.getLong("folder:$path", 0L)

    private fun readSet(key: String): Set<String> = prefs.getStringSet(key, emptySet<String>()).orEmpty().toSet()
    private fun writeSet(key: String, value: Set<String>) { prefs.edit().putStringSet(key, value).apply() }

    companion object {
        private const val KEY_INITIALIZED = "initialized"
        private const val KEY_KNOWN = "known"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_NEW = "new"
    }
}
