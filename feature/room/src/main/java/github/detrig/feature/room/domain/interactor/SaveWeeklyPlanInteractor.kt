package github.detrig.feature.room.domain.interactor

import github.detrig.feature.planning.domain.PlanPercentages
import github.detrig.feature.room.domain.repository.RoomRepository

internal class SaveWeeklyPlanInteractor(private val repository: RoomRepository) {
    suspend operator fun invoke(weekNumber: Long, availableRub: Long, percentages: PlanPercentages) =
        repository.savePlan(weekNumber, availableRub, percentages)
}
