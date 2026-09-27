package github.detrig.feature.gamestate

import github.detrig.feature.gamestate.domain.model.PetDirtAnchor
import github.detrig.feature.gamestate.domain.model.PetDirtRules
import org.junit.Assert.assertEquals
import org.junit.Test

class PetDirtRulesTest {
    private val hour = 60 * 60 * 1_000L

    @Test fun realTimeThresholdsAreTwoFiveAndNineHours() {
        val clean = PetDirtAnchor(1_000L, 0)
        fun stage(elapsed: Long) = PetDirtRules.stage(clean, clean.cleanedAtMillis + elapsed, 0)

        assertEquals(0, stage(2 * hour - 1))
        assertEquals(1, stage(2 * hour))
        assertEquals(1, stage(5 * hour - 1))
        assertEquals(2, stage(5 * hour))
        assertEquals(2, stage(9 * hour - 1))
        assertEquals(3, stage(9 * hour))
    }

    @Test fun completedGamesAddStagesAndBathResetsBothSources() {
        val anchor = PetDirtAnchor(1_000L, 10)
        assertEquals(0, PetDirtRules.stage(anchor, 1_000L, 14))
        assertEquals(1, PetDirtRules.stage(anchor, 1_000L, 15))
        assertEquals(2, PetDirtRules.stage(anchor, 1_000L + 2 * hour, 15))
        assertEquals(3, PetDirtRules.stage(anchor, 1_000L + 2 * hour, 20))

        val washed = PetDirtAnchor(1_000L + 2 * hour, 20)
        assertEquals(0, PetDirtRules.stage(washed, washed.cleanedAtMillis, 20))
        assertEquals(0, PetDirtRules.stage(washed, washed.cleanedAtMillis + 2 * hour - 1, 24))
        assertEquals(1, PetDirtRules.stage(washed, washed.cleanedAtMillis, 25))
    }

    @Test fun clockMovingBackAndRepeatedSessionsDoNotReduceOrDuplicateStages() {
        val anchor = PetDirtAnchor(10_000L, 12)
        assertEquals(0, PetDirtRules.stage(anchor, 9_000L, 11))
        assertEquals(3, PetDirtRules.stage(anchor, 10_000L, 100))
    }

    @Test fun debugStageAdjustmentKeepsLaterNaturalProgression() {
        val now = 10 * hour
        val completedGames = 17L
        for (stage in 0..3) {
            val anchor = PetDirtRules.anchorAtStage(stage, now, completedGames)
            assertEquals(stage, PetDirtRules.stage(anchor, now, completedGames))
        }
        val secondStage = PetDirtRules.anchorAtStage(2, now, completedGames)
        assertEquals(2, PetDirtRules.stage(secondStage, now + 4 * hour - 1, completedGames))
        assertEquals(3, PetDirtRules.stage(secondStage, now + 4 * hour, completedGames))
        assertEquals(3, PetDirtRules.stage(secondStage, now, completedGames + 5))
    }
}
