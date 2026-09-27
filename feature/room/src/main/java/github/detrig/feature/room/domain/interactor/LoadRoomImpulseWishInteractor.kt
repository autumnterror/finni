package github.detrig.feature.room.domain.interactor

import github.detrig.feature.room.domain.model.RoomImpulseWishSource

internal class LoadRoomImpulseWishInteractor(
    private val source: RoomImpulseWishSource,
) {
    suspend operator fun invoke() = source.claimCurrentWish()
    suspend fun recordDeclined(wish: github.detrig.feature.room.domain.model.RoomImpulseWish) =
        source.recordDeclined(wish)
}
