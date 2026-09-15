package com.airplayr.app.core.playback

import com.airplayr.app.core.playback.dsp.ReplayGainCalculator
import com.airplayr.app.core.playback.dsp.ReplayGainTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase19RuntimeTest {
    @Test fun pitchRatioMatchesTwelveSemitones() {
        assertEquals(2f, pitchRatio(12), 0.0001f)
        assertEquals(0.5f, pitchRatio(-12), 0.0001f)
        assertEquals(1f, pitchRatio(0), 0.0001f)
    }

    @Test fun linkedPitchFollowsSpeed() {
        assertEquals(0, linkedPitchSemitones(1.0))
        assertEquals(12, linkedPitchSemitones(2.0))
        assertEquals(-12, linkedPitchSemitones(0.5))
    }

    @Test fun replayGainTrackAndClippingAreApplied() {
        val value = ReplayGainCalculator.multiplier(
            ReplayGainTags(trackDb = -6.0, peak = 1.0),
            mode = "track",
            preampDb = 0.0,
            preventClipping = true,
        )
        assertEquals(0.5012f, value, 0.002f)
    }

    @Test fun replayGainClippingProtectionLimitsPeak() {
        val value = ReplayGainCalculator.multiplier(
            ReplayGainTags(trackDb = 6.0, peak = 1.0),
            mode = "track",
            preampDb = 0.0,
            preventClipping = true,
        )
        assertEquals(1f, value, 0.0001f)
    }
}
