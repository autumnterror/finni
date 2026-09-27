package github.detrig.feature.room.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.feature.room.domain.model.RoomImpulseWish

fun interface RoomWishArtwork {
    @Composable
    fun Content(wish: RoomImpulseWish, modifier: Modifier)
}
