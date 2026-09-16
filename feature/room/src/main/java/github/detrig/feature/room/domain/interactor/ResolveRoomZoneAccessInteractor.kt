package github.detrig.feature.room.domain.interactor

import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.domain.model.RoomZoneDefinition

internal class ResolveRoomZoneAccessInteractor {
    operator fun invoke(zone: RoomZoneDefinition, progress: RoomProgress): RoomZoneAccess = when {
        zone.initiallyOpen || github.detrig.feature.gamestate.domain.model.MiniGameAccess.isOpen(zone.gameId, progress.ownedZoneIds) -> RoomZoneAccess.Open
        progress.playerLevel < zone.requiredLevel -> RoomZoneAccess.Unavailable(zone.requiredLevel)
        else -> RoomZoneAccess.Buyable(zone.priceRub)
    }
}
