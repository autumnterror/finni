package github.detrig.feature.room.domain.model

internal sealed interface RoomZoneAccess {
    data object Open : RoomZoneAccess
    data class Unavailable(val requiredLevel: Int) : RoomZoneAccess
    data class Buyable(val priceRub: Int) : RoomZoneAccess
}
