package com.airplayr.app.core.qa

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlin.math.abs

/**
 * Lightweight runtime playback watchdog for the final ANR/crash regression pass.
 * It does not restart playback blindly; it records a stall/error signal so
 * diagnostics can explain what happened instead of masking failures.
 */
class PlaybackHealthMonitor(private val clockMs: () -> Long = System::currentTimeMillis) : Player.Listener {
    data class Snapshot(
        val lastPositionMs: Long = 0L,
        val lastWallMs: Long = 0L,
        val stallCount: Int = 0,
        val errorCount: Int = 0,
        val transitions: Int = 0,
        val seekEvents: Int = 0,
        val lastError: String? = null,
    )

    @Volatile private var snapshot = Snapshot()
    fun snapshot(): Snapshot = snapshot

    fun observe(player: Player) {
        if (!player.isPlaying) return
        val now = clockMs()
        val current = snapshot
        val deltaWall = now - current.lastWallMs
        val position = player.currentPosition
        if (current.lastWallMs > 0L && deltaWall in 1..3500 && position <= current.lastPositionMs) {
            snapshot = current.copy(
                lastPositionMs = position,
                lastWallMs = now,
                stallCount = current.stallCount + 1,
            )
        } else {
            snapshot = current.copy(lastPositionMs = position, lastWallMs = now)
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            snapshot = snapshot.copy(seekEvents = snapshot.seekEvents + 1)
        }
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        snapshot = snapshot.copy(transitions = snapshot.transitions + 1)
    }

    override fun onPlayerError(error: PlaybackException) {
        val next = snapshot.copy(errorCount = snapshot.errorCount + 1, lastError = error.errorCodeName)
        snapshot = next
        Log.w("AirPlayrPlayback", "Playback error ${error.errorCodeName}: ${error.message}")
    }
}
