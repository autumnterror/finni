package github.detrig.feature.room.domain.interactor

import github.detrig.feature.room.domain.repository.RoomRepository
import github.detrig.feature.savings.api.SavingsApi
import github.detrig.feature.savings.api.SavingsGoalDraft

internal class OpenSavingsInteractor(private val savings: SavingsApi) {
    operator fun invoke(
        firstRunOnboarding: Boolean = false,
        suggestedGoalId: String? = null,
    ) = savings.open(firstRunOnboarding, suggestedGoalId)
}

internal class LoadActiveSavingsGoalInteractor(private val savings: SavingsApi) {
    suspend operator fun invoke() = savings.getActiveGoalProgress()
}

internal class SaveZoneAsSavingsGoalInteractor(
    private val repository: RoomRepository,
    private val savings: SavingsApi,
) {
    suspend operator fun invoke(zoneId: String, title: String) {
        val zone = requireNotNull(repository.zones().find { it.id == zoneId })
        require(zone.priceRub > 0)
        savings.createGoal(SavingsGoalDraft(
            id = "room-zone:${zone.id}",
            title = title,
            targetRub = zone.priceRub.toLong(),
            metadata = "source=room-zone;zoneId=${zone.id}",
        ))
        savings.open()
    }
}
