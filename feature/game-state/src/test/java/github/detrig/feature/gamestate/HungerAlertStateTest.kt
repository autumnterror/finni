package github.detrig.feature.gamestate

import github.detrig.feature.gamestate.domain.model.HungerAlertState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HungerAlertStateTest {
    @Test fun pendingOnlyForAnUndeliveredZeroHungerEpisode() {
        assertEquals(2L, HungerAlertState(0, 2, 1).pendingEpisode)
        assertNull(HungerAlertState(0, 2, 2).pendingEpisode)
        assertNull(HungerAlertState(10, 2, 1).pendingEpisode)
    }
}
