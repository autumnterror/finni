package github.detrig.feature.gamestate.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionRulesTest {
    @Test
    fun thresholdsMapToConfiguredLevelsAndPetStages() {
        assertProgress(0, 1, PetGrowthStage.BABY)
        assertProgress(99, 1, PetGrowthStage.BABY)
        assertProgress(100, 2, PetGrowthStage.BABY)
        assertProgress(250, 3, PetGrowthStage.EXPLORER)
        assertProgress(450, 4, PetGrowthStage.EXPLORER)
        assertProgress(700, 5, PetGrowthStage.COMPANION)
        assertProgress(900, 5, PetGrowthStage.COMPANION)
    }

    @Test
    fun hudProgressIsRelativeToCurrentLevel() {
        val progress = ProgressionRules.progress(175)

        assertEquals(75, progress.currentLevelXp)
        assertEquals(150, progress.nextLevelXp)
        assertEquals(.5f, progress.levelProgress)
        assertFalse(progress.isMaxLevel)
        assertTrue(ProgressionRules.progress(700).isMaxLevel)
        assertFalse(ProgressionRules.progress(99).hasNewReactions)
        assertTrue(ProgressionRules.progress(100).hasNewReactions)
        assertFalse(ProgressionRules.progress(449).hasNewOpportunities)
        assertTrue(ProgressionRules.progress(450).hasNewOpportunities)
    }

    @Test
    fun miniGameRewardStaysSmallAndRequiresCompletedPlay() {
        assertEquals(0, MiniGameXpPolicy.reward(false, 10, 90_000))
        assertEquals(0, MiniGameXpPolicy.reward(true, 0, 90_000))
        assertEquals(5, MiniGameXpPolicy.reward(true, 1, 30_000))
        assertEquals(10, MiniGameXpPolicy.reward(true, 1, 60_000))
    }

    private fun assertProgress(totalXp: Int, level: Int, stage: PetGrowthStage) {
        val progress = ProgressionRules.progress(totalXp)
        assertEquals(level, progress.level)
        assertEquals(stage, progress.petStage)
    }
}
