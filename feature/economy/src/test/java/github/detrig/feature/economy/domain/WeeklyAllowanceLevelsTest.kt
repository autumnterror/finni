package github.detrig.feature.economy.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyAllowanceLevelsTest {
    @Test
    fun weeklyAllowanceFollowsPetLevel() {
        val config = EconomyConfig()

        assertEquals(900L, config.weeklyAllowanceForLevel(1))
        assertEquals(900L, config.weeklyAllowanceForLevel(2))
        assertEquals(1_400L, config.weeklyAllowanceForLevel(3))
        assertEquals(1_400L, config.weeklyAllowanceForLevel(4))
        assertEquals(2_200L, config.weeklyAllowanceForLevel(5))
    }
}
