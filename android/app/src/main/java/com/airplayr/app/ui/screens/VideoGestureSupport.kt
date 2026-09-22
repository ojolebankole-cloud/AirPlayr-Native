package com.airplayr.app.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings

internal object VideoGestureSupport {
    private const val MIN_GESTURE_DELTA = 0.02f

    fun musicVolumeFraction(context: Context): Float {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0f
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max.toFloat()
    }

    fun adjustMusicVolume(context: Context, fractionDelta: Float): Float {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0f
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val next = if (kotlin.math.abs(fractionDelta) >= MIN_GESTURE_DELTA) {
            (current + kotlin.math.round(fractionDelta * max)).toInt().coerceIn(0, max)
        } else {
            current
        }
        if (next != current) audio.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
        return next.toFloat() / max.toFloat()
    }

    /**
     * Returns the effective screen brightness without requiring WRITE_SETTINGS. When the
     * activity has not overridden brightness, the current system brightness is read and then
     * the app window can take over for the duration of the gesture.
     */
    fun currentWindowBrightness(activity: Activity): Float {
        val override = activity.window.attributes.screenBrightness
        if (override >= 0f) return override.coerceIn(0f, 1f)
        val system = runCatching {
            Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(128)
        return (system / 255f).coerceIn(0f, 1f)
    }

    fun setWindowBrightness(activity: Activity, fraction: Float) {
        val value = fraction.coerceIn(0.02f, 1f)
        val attributes = activity.window.attributes
        attributes.screenBrightness = value
        activity.window.attributes = attributes
    }

    fun restoreWindowBrightness(activity: Activity, previous: Float) {
        val attributes = activity.window.attributes
        attributes.screenBrightness = previous
        activity.window.attributes = attributes
    }
}
