package github.detrig.feature.room.domain.model

data class RoomImpulseWish(
    val eventId: String,
    val productTitle: String,
    val phraseVariant: Int,
    val showIntroduction: Boolean,
    val kind: Kind = Kind.GROCERY,
    val priceRub: Long = 0,
    val productId: String? = null,
    val createdOnAbsoluteDay: Long? = null,
    /** Exclusive game day when this wish expires. Null means it is tied to a persistent goal. */
    val expiresOnAbsoluteDayExclusive: Long? = null,
) {
    enum class Kind { GROCERY, CLOTHING, MINI_GAME, TOY, SAVINGS_TOP_UP, FREE }
    init {
        require(eventId.isNotBlank())
        require(productTitle.isNotBlank())
        require(phraseVariant in 0 until PHRASE_VARIANT_COUNT)
    }

    companion object {
        const val PHRASE_VARIANT_COUNT = 4
    }

    fun daysRemaining(absoluteDay: Long): Int? = expiresOnAbsoluteDayExclusive
        ?.let { (it - absoluteDay).coerceAtLeast(1L).toInt() }

    val isPurchasable: Boolean
        get() = kind == Kind.GROCERY || kind == Kind.CLOTHING || kind == Kind.TOY

    val happinessBonus: Int
        get() = when {
            isPurchasable -> github.detrig.feature.gamestate.domain.model.PetWishHappinessRewards.optionalPurchase(eventId)
            kind == Kind.MINI_GAME -> github.detrig.feature.gamestate.domain.model.MiniGameHappinessRewards
                .firstLaunchPoints(productId.orEmpty()).takeIf { it > 0 }
                ?: github.detrig.feature.gamestate.domain.model.PetPlayReward.HAPPINESS_PER_ROUND
            else -> 0
        }
}

data class RoomWishObjectCandidate(
    val id: String,
    val title: String,
    val priceRub: Long,
)

fun interface RoomImpulseWishSource {
    suspend fun currentWish(): RoomImpulseWish?
    suspend fun currentWishes(): List<RoomImpulseWish> = listOfNotNull(currentWish())
    suspend fun currentWishes(
        absoluteDay: Long,
        roomObjects: List<RoomWishObjectCandidate>,
        activeGoalId: String?,
    ): List<RoomImpulseWish> = currentWishes()
    suspend fun claimCurrentWish(): RoomImpulseWish? = currentWish()
    suspend fun claimDialogueWish(wish: RoomImpulseWish): RoomImpulseWish? = wish
    suspend fun recordDeclined(wish: RoomImpulseWish) = Unit
    suspend fun recordFulfilled(wish: RoomImpulseWish) = Unit
    suspend fun currentFulfillments(): List<RoomWishFulfillment> = emptyList()
    suspend fun acknowledgeFulfillment(id: String) = Unit
}
