package github.detrig.feature.savings.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SavingsConfigurationTest {
    @Test
    fun lockedMiniGamesAreAvailableAsStarterGoalsWithRoomPrices() {
        val goals = SavingsConfiguration().starterGoals.associateBy { it.id }

        assertEquals(200, goals.getValue("room-zone:drawing").targetRub)
        assertEquals(350, goals.getValue("room-zone:music").targetRub)
        assertEquals(400, goals.getValue("room-zone:fishing").targetRub)
    }
}
