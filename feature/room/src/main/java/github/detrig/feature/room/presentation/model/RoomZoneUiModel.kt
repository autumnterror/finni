package github.detrig.feature.room.presentation.model

import github.detrig.feature.room.domain.model.RoomZoneAccess

internal data class RoomZoneUiModel(
    val id: String,
    val gameId: String,
    val appearance: RoomZoneAppearance,
    val access: RoomZoneAccess,
    val canAfford: Boolean,
)
