package github.detrig.feature.room.domain.interactor

import github.detrig.feature.room.domain.model.RoomImpulseWishSource

internal class LoadRoomImpulseWishInteractor(
    private val source: RoomImpulseWishSource,
) {
    suspend operator fun invoke() = source.currentWish()
    suspend fun currentWishes() = source.currentWishes()
    suspend fun currentWishes(
        absoluteDay: Long,
        activeGoalId: String?,
    ) = source.currentWishes(absoluteDay, activeGoalId)
    suspend fun claimDialogue(wish: github.detrig.feature.room.domain.model.RoomImpulseWish) =
        source.claimDialogueWish(wish)
    suspend fun recordDeclined(wish: github.detrig.feature.room.domain.model.RoomImpulseWish) =
        source.recordDeclined(wish)
    suspend fun recordFulfilled(wish: github.detrig.feature.room.domain.model.RoomImpulseWish) =
        source.recordFulfilled(wish)
    suspend fun currentFulfillments() = source.currentFulfillments()
    suspend fun acknowledgeFulfillment(id: String) = source.acknowledgeFulfillment(id)
}
