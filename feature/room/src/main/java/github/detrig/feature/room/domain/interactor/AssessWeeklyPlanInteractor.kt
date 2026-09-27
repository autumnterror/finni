package github.detrig.feature.room.domain.interactor

import github.detrig.feature.planning.domain.WeeklyPlan
import github.detrig.feature.room.domain.repository.RoomRepository

internal class AssessWeeklyPlanInteractor(
    private val repository: RoomRepository,
) {
    operator fun invoke(plan: WeeklyPlan) = repository.assessPlan(plan)
}
