package github.detrig.feature.room.domain.model

data class RoomWishFulfillment(
    val id: String,
    val kind: Kind,
    val title: String = "",
) {
    enum class Kind { ITEM, MINI_GAME, SAVINGS_TOP_UP, SAVINGS_HALF_WAY, SAVINGS_REACHED, GAME_UNLOCKED }
}
