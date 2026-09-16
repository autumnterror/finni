package github.detrig.feature.room.di

import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.room.presentation.RoomViewModel

internal interface RoomComponent {
    val api: RoomApi
    fun getRoomViewModel(): RoomViewModel
}
