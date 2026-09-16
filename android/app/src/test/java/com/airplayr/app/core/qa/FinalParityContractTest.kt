package com.airplayr.app.core.qa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalParityContractTest {
    @Test fun rootRoutesMatchExpoContract() {
        assertEquals(listOf("home", "music", "videos", "settings"), AirPlayrInteractionContract.rootRoutes)
    }

    @Test fun playerActionsAreComplete() {
        assertTrue(AirPlayrInteractionContract.playerActions.containsAll(listOf("seek", "shuffle", "repeat", "lyrics", "queue")))
        assertTrue(AirPlayrInteractionContract.moreActions.containsAll(listOf("save-queue", "play-next", "play-last", "share", "delete")))
    }

    @Test fun tortureScenarioIsMeaningful() {
        assertEquals(13, PlaybackTortureScenarios.baseline.size)
        assertEquals(75, PlaybackTortureScenarios.repeatedSeek(25).size)
        assertTrue(PlaybackTortureScenarios.queueChurn(10).isNotEmpty())
    }
}
