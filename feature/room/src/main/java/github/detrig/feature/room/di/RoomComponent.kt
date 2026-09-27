package github.detrig.feature.room.di

import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.room.presentation.RoomViewModel
import github.detrig.feature.room.domain.furniture.FurnitureCatalog
import github.detrig.feature.room.domain.furniture.FurnitureStore

internal interface RoomComponent {
    val api: RoomApi
    val furnitureCatalog: FurnitureCatalog
    val furnitureStore: FurnitureStore
    fun getRoomViewModel(): RoomViewModel
}
