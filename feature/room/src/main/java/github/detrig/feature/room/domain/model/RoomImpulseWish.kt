package github.detrig.feature.room.domain.model

data class RoomImpulseWish(
    val eventId: String,
    val productTitle: String,
    val phraseVariant: Int,
    val showIntroduction: Boolean,
) {
    init {
        require(eventId.isNotBlank())
        require(productTitle.isNotBlank())
        require(phraseVariant in 0 until PHRASE_VARIANT_COUNT)
    }

    companion object {
        const val PHRASE_VARIANT_COUNT = 4
    }
}

fun interface RoomImpulseWishSource {
    suspend fun claimCurrentWish(): RoomImpulseWish?
}
