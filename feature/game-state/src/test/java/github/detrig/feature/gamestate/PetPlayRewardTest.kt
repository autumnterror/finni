package github.detrig.feature.gamestate

import github.detrig.feature.gamestate.domain.model.PetPlayCompletion
import github.detrig.feature.gamestate.domain.model.PetPlayReward
import org.junit.Assert.assertEquals
import org.junit.Test

class PetPlayRewardTest {
    private fun completion(millis: Long = 10_000, actions: Int = 1, natural: Boolean = true) =
        PetPlayCompletion("current", "session", "flight", natural, validActionCount = actions, activePlayMillis = millis)

    @Test fun clampAndActivityAreIndependentOfScore() {
        assertEquals(3, PetPlayReward.delta(50, completion()))
        assertEquals(1, PetPlayReward.delta(99, completion()))
        assertEquals(0, PetPlayReward.delta(100, completion()))
        assertEquals(0, PetPlayReward.delta(50, completion(millis = 9999)))
        assertEquals(0, PetPlayReward.delta(50, completion(actions = 0)))
        assertEquals(0, PetPlayReward.delta(50, completion(natural = false)))
    }

    @Test fun existingFishingConstructorRetainsItsMeaning() {
        val fishing = PetPlayCompletion("current", "fishing-session", "fishing", true, 1)
        assertEquals(3, PetPlayReward.delta(50, fishing))
    }
}
