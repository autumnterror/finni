package github.detrig.feature.learning.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MvpAchievementCatalogTest {
    @Test
    fun catalogHasTwoMappedAchievementsForEveryMetric() {
        val catalog = MvpAchievementCatalog.create(LearningConfig())

        assertEquals(24, catalog.definitions.size)
        assertEquals(24, catalog.definitions.map { it.achievementId }.distinct().size)
        assertEquals(12, catalog.definitions.count { it.stage == AchievementStage.INTRODUCTION })
        assertEquals(12, catalog.definitions.count { it.stage == AchievementStage.LEARNED })
        assertTrue(catalog.definitions.all { it.xpReward > 0 })
        assertTrue(catalog.definitions
            .filter { it.stage == AchievementStage.INTRODUCTION }
            .all { it.parentText.startsWith("Ребёнок ознакомился") })
        assertTrue(catalog.definitions
            .filter { it.stage == AchievementStage.LEARNED }
            .all { it.parentText.startsWith("Ребёнок научился") })
        assertTrue(catalog.definitions.groupBy { it.metricId }.values.all { it.size == 2 })
    }

    @Test
    fun financialSecurityUsesThirdStepForLearnedAchievement() {
        val catalog = MvpAchievementCatalog.create(LearningConfig())

        val security = catalog.definitions.filter {
            it.topicId == LearningTopicIds.FINANCIAL_SECURITY
        }
        assertTrue(security.filter { it.stage == AchievementStage.INTRODUCTION }
            .all { it.requiredProgressSteps == 1 })
        assertTrue(security.filter { it.stage == AchievementStage.LEARNED }
            .all { it.requiredProgressSteps == 3 })
    }

    @Test
    fun budgetPlanningRuleCanUnlockBothReasonablePlanAchievements() {
        val rule = BudgetPlanningLearning.reasonablePlanRule()
        val catalog = MvpAchievementCatalog.create(LearningConfig(metricRules = listOf(rule)))

        assertEquals(
            2,
            catalog.definitionsForMetric(LearningMetricIds.BUDGET_REASONABLE_PLAN).size,
        )
        assertEquals(2, rule.milestones.maxOf { it.progressSteps })
    }
}
