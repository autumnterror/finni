package github.detrig.feature.room.domain.interactor

import github.detrig.feature.room.domain.repository.RoomRepository

internal class LoadParentHelpInteractor(private val repository: RoomRepository) {
    fun offers() = repository.parentHelpOffers()
    suspend operator fun invoke() = repository.parentHelp()
}

internal class RequestParentHelpInteractor(private val repository: RoomRepository) {
    suspend operator fun invoke(offerId: String) = repository.requestParentHelp(offerId)
}
