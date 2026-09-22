package github.detrig.internetbooster.mediators

import androidx.annotation.MainThread
import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.fridge.FridgeDependencies
import github.detrig.feature.fridge.FridgeFeature
import github.detrig.feature.fridge.api.FridgeApi

internal class FridgeMediator(
    private val coreComponent: CoreComponent,
    private val roomMediator: RoomMediator,
    private val petMediator: PetMediator,
    private val inventoryMediator: InventoryMediator,
    private val gameStateMediator: GameStateMediator,
    private val shopMediator: ShopMediator,
) : Mediator<FridgeApi> {
    @MainThread
    fun init() {
        FridgeFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : FridgeDependencies {
                override fun globalNavigator() = coreComponent.globalNavigator
                override fun roomApi() = roomMediator.getApi()
                override fun petApi() = petMediator.getApi()
                override fun inventoryApi() = inventoryMediator.getApi()
                override fun gameStateApi() = gameStateMediator.getApi()
                override fun artworkResolver() = shopMediator.artworkResolver()
            }
        }
    }

    @MainThread
    override fun getApi(): FridgeApi = FridgeFeature.getApi()
}
