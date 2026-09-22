package github.detrig.feature.fridge

import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.shop.api.ShopArtworkResolver

interface FridgeDependencies {
    fun globalNavigator(): GlobalNavigator
    fun roomApi(): RoomApi
    fun petApi(): PetApi
    fun inventoryApi(): InventoryApi
    fun gameStateApi(): GameStateApi
    fun artworkResolver(): ShopArtworkResolver
}
