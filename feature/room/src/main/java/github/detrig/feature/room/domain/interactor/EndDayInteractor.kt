package github.detrig.feature.room.domain.interactor

import github.detrig.feature.room.domain.repository.RoomRepository

internal class EndDayInteractor(private val repository: RoomRepository) {
    suspend operator fun invoke(expectedAbsoluteDay: Long) = repository.endDay(expectedAbsoluteDay)
}
