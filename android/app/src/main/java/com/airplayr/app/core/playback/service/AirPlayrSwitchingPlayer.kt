package com.airplayr.app.core.playback.service

import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * Stable Player identity for MediaSession while allowing local crossfade slots
 * to swap underneath it. Media3 handles listener/state propagation for us.
 */
@UnstableApi
class AirPlayrSwitchingPlayer(initial: Player) : ForwardingSimpleBasePlayer(initial) {
    fun switchTo(next: Player) {
        if (getPlayer() !== next) setPlayer(next)
    }
}
