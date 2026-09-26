package github.detrig.feature.savings.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SavingsConfigurationTest {
    @Test
    fun lockedMiniGamesAreAvailableAsStarterGoalsWithRoomPrices() {
        val goals = SavingsConfiguration().starterGoals.associateBy { it.id }

        assertEquals(900, goals.getValue("room-zone:drawing").targetRub)
        assertEquals(1_800, goals.getValue("room-zone:music").targetRub)
        assertEquals(600, goals.getValue("room-zone:fishing").targetRub)
    }
}
