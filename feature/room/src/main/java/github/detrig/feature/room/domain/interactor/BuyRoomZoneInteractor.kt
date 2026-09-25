package github.detrig.feature.room.domain.interactor

import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.room.domain.repository.RoomRepository

internal class BuyRoomZoneInteractor(
    private val repository: RoomRepository,
) {
    suspend operator fun invoke(zoneId: String, useSavings: Boolean = false): ZoneBuyResult {
        val zone = requireNotNull(repository.zones().find { it.id == zoneId })
        if (zone.initiallyOpen) return ZoneBuyResult.AlreadyOwned
        return repository.buyZone(zone, useSavings)
    }
}
