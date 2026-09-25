package github.detrig.feature.gamestate

import github.detrig.feature.gamestate.domain.model.PetNeedDecayConfig
import github.detrig.feature.gamestate.domain.model.TimedPetNeeds
import github.detrig.feature.gamestate.domain.model.reconcilePetNeeds
import org.junit.Assert.assertEquals
import org.junit.Test

class PetNeedDecayTest {
    private val hour = 60 * 60 * 1_000L
    private val config = PetNeedDecayConfig()
    private val initial = TimedPetNeeds(100, 70, hour, hour)

    @Test fun catchesUpAfterAppWasClosed() {
        val updated = reconcilePetNeeds(initial, 5 * hour, config)
        assertEquals(60, updated.hunger)
        assertEquals(70, updated.happiness)
        assertEquals(5 * hour, updated.hungerCheckpointMillis)
        assertEquals(hour, updated.happinessCheckpointMillis)
        assertEquals(updated, reconcilePetNeeds(updated, 5 * hour, config))
    }

    @Test fun happinessDecaysEveryEightHours() {
        val updated = reconcilePetNeeds(initial, 25 * hour, config)
        assertEquals(0, updated.hunger)
        assertEquals(64, updated.happiness)
        assertEquals(25 * hour, updated.hungerCheckpointMillis)
        assertEquals(25 * hour, updated.happinessCheckpointMillis)
        assertEquals(updated, reconcilePetNeeds(updated, 25 * hour, config))
    }

    @Test fun frequentChecksKeepPartialIntervals() {
        val first = reconcilePetNeeds(initial, hour + hour / 2, config)
        assertEquals(100, first.hunger)
        val second = reconcilePetNeeds(first, 2 * hour, config)
        assertEquals(90, second.hunger)
        assertEquals(hour, first.hungerCheckpointMillis)
        assertEquals(2 * hour, second.hungerCheckpointMillis)
    }

    @Test fun zeroIsFloorAndClockRollbackDoesNotRestoreOrDrain() {
        val low = initial.copy(hunger = 1, happiness = 2)
        val afterWeek = reconcilePetNeeds(low, 8 * 24 * hour, config)
        assertEquals(0, afterWeek.hunger)
        assertEquals(0, afterWeek.happiness)
        assertEquals(afterWeek, reconcilePetNeeds(afterWeek, hour, config))
    }

    @Test fun missingCheckpointStartsAtCurrentTimeWithoutRetroactiveLoss() {
        val updated = reconcilePetNeeds(initial.copy(hungerCheckpointMillis = 0), 20 * hour, config)
        assertEquals(100, updated.hunger)
        assertEquals(20 * hour, updated.hungerCheckpointMillis)
    }
}
