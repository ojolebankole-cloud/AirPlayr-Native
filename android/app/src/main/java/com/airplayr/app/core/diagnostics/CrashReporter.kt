package com.airplayr.app.core.diagnostics

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashReporter(private val context: Context) {
    private val file = File(context.filesDir, "airplayr_last_crash.txt")

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                file.writeText(buildString {
                    appendLine(SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date()))
                    appendLine("Thread: ${thread.name}")
                    appendLine(throwable.stackTraceToString())
                })
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun lastCrash(): String? = runCatching { if (file.exists()) file.readText().take(16000) else null }.getOrNull()
    fun clear() { runCatching { file.delete() } }
}
