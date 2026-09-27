package github.detrig.feature.room.domain.interactor

import github.detrig.feature.room.domain.model.MoneyAllocation
import github.detrig.feature.room.domain.repository.RoomRepository

internal class RoomMoneyEventInteractor(private val repository: RoomRepository) {
    suspend fun pending() = repository.pendingMoneyEvent()
    suspend fun resolve(eventId: String, allocation: MoneyAllocation? = null) =
        repository.resolveMoneyEvent(eventId, allocation)
    suspend fun coverWithParents(eventId: String) = repository.coverMoneyEventWithParents(eventId)
}
