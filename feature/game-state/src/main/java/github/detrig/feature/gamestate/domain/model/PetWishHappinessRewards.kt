package github.detrig.feature.gamestate.domain.model

/** Stable values keep a wish reward unchanged when its purchase is replayed after restoration. */
object PetWishHappinessRewards {
    fun optionalPurchase(wishId: String): Int = 5 + Math.floorMod(wishId.hashCode(), 6)

    const val SAVINGS_HALF_WAY = 5
    const val SAVINGS_GOAL_REACHED = 10
    const val UNLOCKED_GAME = 18
}
