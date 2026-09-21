package github.detrig.feature.room.domain.interactor

import github.detrig.feature.planning.domain.PlanPercentages
import github.detrig.feature.planning.domain.PlanAssessment
import github.detrig.feature.planning.domain.SavePlanResult
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.room.domain.repository.RoomRepository

internal data class SaveWeeklyPlanOutcome(
    val progress: WeeklyPlanProgress,
    val learningFeedback: WeeklyPlanLearningFeedback,
)

internal class SaveWeeklyPlanInteractor(
    private val repository: RoomRepository,
    private val learning: WeeklyPlanLearningInteractor,
) {
    suspend operator fun invoke(
        weekNumber: Long,
        availableRub: Long,
        percentages: PlanPercentages,
    ): SaveWeeklyPlanOutcome {
        val result = repository.savePlan(weekNumber, availableRub, percentages)
        val progress = when (result) {
            is SavePlanResult.Saved -> result.progress
            is SavePlanResult.AlreadySaved -> result.progress
        }
        val learningFeedback = when (repository.assessPlan(progress.plan.percentages)) {
            PlanAssessment.Adequate -> learning.recordConfirmed(progress.plan)
            is PlanAssessment.NeedsChanges -> WeeklyPlanLearningFeedback(
                showSuccessExplanation = false,
                newlyUnlocked = emptyList(),
            )
        }
        return SaveWeeklyPlanOutcome(
            progress = progress,
            learningFeedback = learningFeedback,
        )
    }
}
