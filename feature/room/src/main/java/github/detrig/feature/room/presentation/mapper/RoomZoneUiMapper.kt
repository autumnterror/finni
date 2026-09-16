package github.detrig.feature.room.presentation.mapper

import github.detrig.feature.room.domain.model.RoomData
import github.detrig.feature.room.presentation.model.RoomZoneAppearance
import github.detrig.feature.room.presentation.model.RoomZoneUiModel

internal fun RoomData.toRoomZones(): List<RoomZoneUiModel> = zones.map { zone ->
    RoomZoneUiModel(
        id = zone.definition.id,
        gameId = zone.definition.gameId,
        appearance = RoomZoneAppearance.fromKey(zone.definition.appearanceKey),
        access = zone.access,
        canAfford = progress.balanceRub >= zone.definition.priceRub,
    )
}
