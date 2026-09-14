package com.airplayr.app.core.diagnostics

import android.content.Context
import android.os.Build
import android.os.StatFs
import com.airplayr.app.AirPlayrApplication
import kotlinx.coroutines.flow.first

class DiagnosticsRepository(private val context: Context) {
    suspend fun snapshot(): DiagnosticsSnapshot {
        val app = context.applicationContext as AirPlayrApplication
        val settings = app.container.preferences.settings.first()
        val db = app.container.database
        val audio = runCatching { db.mediaItemDao().count("audio") }.getOrDefault(-1)
        val video = runCatching { db.mediaItemDao().count("video") }.getOrDefault(-1)
        val free = StatFs(context.filesDir.absolutePath).availableBytes
        return DiagnosticsSnapshot(
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
            androidVersion = Build.VERSION.RELEASE.orEmpty(), sdk = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER.orEmpty(), model = Build.MODEL.orEmpty(),
            deviceId = "redacted",
            audioCount = audio, videoCount = video, freeBytes = free,
            autoRefresh = settings.autoRefreshLibrary, backgroundPlayback = settings.backgroundPlayback,
            dynamicTheme = settings.theme == "system", highContrast = settings.highContrast, reduceMotion = settings.reduceMotion,
        )
    }

    fun format(snapshot: DiagnosticsSnapshot): String = buildString {
        appendLine("AirPlayr Diagnostics"); appendLine("App ${snapshot.appVersion}"); appendLine("Android ${snapshot.androidVersion} (SDK ${snapshot.sdk})")
        appendLine("Device ${snapshot.manufacturer} ${snapshot.model}"); appendLine("Audio library ${snapshot.audioCount}"); appendLine("Video library ${snapshot.videoCount}")
        appendLine("Free app storage ${snapshot.freeBytes} bytes"); appendLine("Auto refresh ${snapshot.autoRefresh}"); appendLine("Background playback ${snapshot.backgroundPlayback}")
        appendLine("High contrast ${snapshot.highContrast}"); appendLine("Reduce motion ${snapshot.reduceMotion}")
    }
}

data class DiagnosticsSnapshot(
    val appVersion: String, val androidVersion: String, val sdk: Int, val manufacturer: String, val model: String,
    val deviceId: String, val audioCount: Int, val videoCount: Int, val freeBytes: Long, val autoRefresh: Boolean,
    val backgroundPlayback: Boolean, val dynamicTheme: Boolean, val highContrast: Boolean, val reduceMotion: Boolean,
)
