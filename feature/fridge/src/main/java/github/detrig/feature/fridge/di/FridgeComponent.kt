package github.detrig.feature.fridge.di

import github.detrig.feature.fridge.api.FridgeApi
import github.detrig.feature.fridge.navigation.FridgeRouter
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.core.audio.GameAudio

internal interface FridgeComponent {
    val api: FridgeApi
    val router: FridgeRouter
    val roomApi: RoomApi
    val petApi: PetApi
    val inventoryApi: InventoryApi
    val gameStateApi: GameStateApi
    val artworkResolver: ShopArtworkResolver
    val gameAudio: GameAudio
    fun viewModel(): github.detrig.feature.fridge.presentation.FridgeViewModel
    fun feedingViewModel(): github.detrig.feature.fridge.presentation.FeedingViewModel
}
