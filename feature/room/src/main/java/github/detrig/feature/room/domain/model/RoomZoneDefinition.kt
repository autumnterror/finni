package github.detrig.feature.room.domain.model

internal data class RoomZoneDefinition(
    val id: String,
    val gameId: String,
    val appearanceKey: String,
    val priceRub: Int,
    val requiredLevel: Int,
    val initiallyOpen: Boolean = false,
)
