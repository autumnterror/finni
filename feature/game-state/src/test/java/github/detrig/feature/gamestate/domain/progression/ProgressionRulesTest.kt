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
        assertProgress(250, 3, PetGrowthStage.TEEN)
        assertProgress(450, 4, PetGrowthStage.TEEN)
        assertProgress(700, 5, PetGrowthStage.ADULT)
        assertProgress(900, 5, PetGrowthStage.ADULT)
    }

    @Test
    fun growthBandsStayEvenWhenMoreLevelsAreConfigured() {
        assertEquals(5, ProgressionRules.levels.size)
        assertEquals(PetGrowthStage.BABY, ProgressionRules.stageForLevel(8, 24))
        assertEquals(PetGrowthStage.TEEN, ProgressionRules.stageForLevel(9, 24))
        assertEquals(PetGrowthStage.TEEN, ProgressionRules.stageForLevel(16, 24))
        assertEquals(PetGrowthStage.ADULT, ProgressionRules.stageForLevel(17, 24))
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
        assertEquals(2, MiniGameXpPolicy.reward(true, 1, 30_000))
        assertEquals(2, MiniGameXpPolicy.reward(true, 1, 60_000))
        assertEquals(2, MiniGameXpPolicy.rewardWithinWeek(8))
        assertEquals(0, MiniGameXpPolicy.rewardWithinWeek(10))
    }

    private fun assertProgress(totalXp: Int, level: Int, stage: PetGrowthStage) {
        val progress = ProgressionRules.progress(totalXp)
        assertEquals(level, progress.level)
        assertEquals(stage, progress.petStage)
    }
}
