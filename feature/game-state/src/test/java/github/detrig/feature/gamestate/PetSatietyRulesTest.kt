package github.detrig.feature.gamestate

import github.detrig.feature.gamestate.domain.model.PetSatietyRules
import org.junit.Assert.assertEquals
import org.junit.Test

class PetSatietyRulesTest {
    @Test fun sleepNeverMakesSatietyNegative() {
        assertEquals(50, PetSatietyRules.afterCost(80, PetSatietyRules.SLEEP_COST))
        assertEquals(70, PetSatietyRules.afterCost(100, PetSatietyRules.SLEEP_COST))
        assertEquals(0, PetSatietyRules.afterCost(1, PetSatietyRules.SLEEP_COST))
        assertEquals(0, PetSatietyRules.afterCost(0, PetSatietyRules.SLEEP_COST))
    }
}
