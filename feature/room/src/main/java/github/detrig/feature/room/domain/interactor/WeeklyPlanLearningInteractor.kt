package github.detrig.feature.room.domain.interactor

import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.learning.domain.AchievementUnlock
import github.detrig.feature.learning.domain.BudgetPlanningLearning
import github.detrig.feature.learning.domain.SavingsLearning
import github.detrig.feature.learning.domain.RecordLearningResult
import github.detrig.feature.planning.domain.WeeklyPlan
import github.detrig.feature.planning.domain.PlanCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal data class WeeklyPlanAchievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
)

internal data class WeeklyPlanLearningFeedback(
    val showSuccessExplanation: Boolean,
    val newlyUnlocked: List<AchievementUnlock>,
)

internal class WeeklyPlanLearningInteractor(
    private val learningApi: LearningApi,
) {
    fun observeAchievements(): Flow<List<WeeklyPlanAchievement>> =
        learningApi.observeAchievements(CURRENT_PROFILE_ID).map { achievements ->
            achievements.map { achievement ->
                WeeklyPlanAchievement(
                    id = achievement.definition.achievementId,
                    title = achievement.definition.childTitle,
                    description = achievement.definition.childDescription,
                    isUnlocked = achievement.isUnlocked,
                )
            }
        }

    suspend fun claimIntroduction(): Boolean = learningApi.claimFirstExplanation(
        profileId = CURRENT_PROFILE_ID,
        explanationId = INTRODUCTION_EXPLANATION_ID,
    )

    suspend fun recordConfirmed(plan: WeeklyPlan): WeeklyPlanLearningFeedback {
        val result = learningApi.record(plan.toLearningAction())
        val newlyUnlocked = when (result) {
            is RecordLearningResult.Processed -> result.newlyUnlocked
            is RecordLearningResult.AlreadyProcessed -> emptyList()
            is RecordLearningResult.OperationIdConflict -> error(
                "Conflicting learning action ${result.actionId}",
            )
            is RecordLearningResult.UnsupportedAction -> error(
                "Unsupported learning action ${result.actionType.value}",
            )
        }
        val savingsUnlocks = recordSavingsPlan(plan)
        val showSuccessExplanation = learningApi.claimFirstExplanation(
            profileId = CURRENT_PROFILE_ID,
            explanationId = SUCCESS_EXPLANATION_ID,
        )
        return WeeklyPlanLearningFeedback(showSuccessExplanation, newlyUnlocked + savingsUnlocks)
    }

    suspend fun reconcile(plan: WeeklyPlan) {
        when (val result = learningApi.record(plan.toLearningAction())) {
            is RecordLearningResult.Processed,
            is RecordLearningResult.AlreadyProcessed,
            -> Unit
            is RecordLearningResult.OperationIdConflict -> error(
                "Conflicting learning action ${result.actionId}",
            )
            is RecordLearningResult.UnsupportedAction -> error(
                "Unsupported learning action ${result.actionType.value}",
            )
        }
        recordSavingsPlan(plan)
    }

    private suspend fun recordSavingsPlan(plan: WeeklyPlan): List<AchievementUnlock> {
        val amount = plan.plannedRub(PlanCategory.SAVINGS)
        if (amount <= 0) return emptyList()
        return when (val result = learningApi.record(
            SavingsLearning.savingsPlanned(CURRENT_PROFILE_ID, plan.weekNumber, amount),
        )) {
            is RecordLearningResult.Processed -> result.newlyUnlocked
            is RecordLearningResult.AlreadyProcessed -> emptyList()
            is RecordLearningResult.OperationIdConflict -> error("Conflicting learning action ${result.actionId}")
            is RecordLearningResult.UnsupportedAction -> error("Unsupported learning action ${result.actionType.value}")
        }
    }

    private fun WeeklyPlan.toLearningAction() = BudgetPlanningLearning.confirmedPlanAction(
        profileId = CURRENT_PROFILE_ID,
        weekNumber = weekNumber,
        mandatoryPercent = percentages.mandatory,
        wantsPercent = percentages.wants,
        savingsPercent = percentages.savings,
        reservePercent = percentages.reserve,
    )

    private companion object {
        const val CURRENT_PROFILE_ID = "current"
        const val INTRODUCTION_EXPLANATION_ID = "budget-plan:introduction"
        const val SUCCESS_EXPLANATION_ID = "budget-plan:adequate:first-explanation"
    }
}
