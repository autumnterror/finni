package github.detrig.feature.room.di

import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.room.presentation.RoomViewModel
import github.detrig.feature.room.domain.furniture.FurnitureCatalog
import github.detrig.feature.room.domain.furniture.FurnitureStore
import github.detrig.feature.room.domain.surface.SurfaceCatalog

internal interface RoomComponent {
    val api: RoomApi
    val furnitureCatalog: FurnitureCatalog
    val furnitureStore: FurnitureStore
    val surfaceCatalog: SurfaceCatalog
    fun getRoomViewModel(): RoomViewModel
}
