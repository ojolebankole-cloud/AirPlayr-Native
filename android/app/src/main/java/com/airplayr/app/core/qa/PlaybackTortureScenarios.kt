package com.airplayr.app.core.qa

/** Deterministic non-UI scenario generator used by the final playback regression audit. */
object PlaybackTortureScenarios {
    enum class Op { PLAY, PAUSE, SEEK_START, SEEK_MIDDLE, SEEK_END, NEXT, PREVIOUS, SHUFFLE, REPEAT, VOLUME_DOWN, VOLUME_UP }

    val baseline: List<Op> = listOf(
        Op.PLAY, Op.PAUSE, Op.PLAY,
        Op.SEEK_START, Op.SEEK_MIDDLE, Op.SEEK_END,
        Op.NEXT, Op.PREVIOUS,
        Op.SHUFFLE, Op.REPEAT,
        Op.VOLUME_DOWN, Op.VOLUME_UP,
        Op.PLAY,
    )

    fun repeatedSeek(rounds: Int = 25): List<Op> = buildList {
        repeat(rounds.coerceAtLeast(1)) {
            add(Op.SEEK_START); add(Op.SEEK_MIDDLE); add(Op.SEEK_END)
        }
    }

    fun queueChurn(rounds: Int = 10): List<Op> = buildList {
        repeat(rounds.coerceAtLeast(1)) {
            add(Op.NEXT); add(Op.NEXT); add(Op.PREVIOUS); add(Op.PLAY)
        }
    }
}
