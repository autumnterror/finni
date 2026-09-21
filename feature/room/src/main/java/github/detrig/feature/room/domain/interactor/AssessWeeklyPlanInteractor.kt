package github.detrig.feature.room.domain.interactor

import github.detrig.feature.planning.domain.PlanPercentages
import github.detrig.feature.room.domain.repository.RoomRepository

internal class AssessWeeklyPlanInteractor(
    private val repository: RoomRepository,
) {
    operator fun invoke(percentages: PlanPercentages) = repository.assessPlan(percentages)
}
