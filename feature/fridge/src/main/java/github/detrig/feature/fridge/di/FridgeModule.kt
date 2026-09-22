package github.detrig.feature.fridge.di

import github.detrig.feature.fridge.FridgeDependencies
import github.detrig.feature.fridge.api.FridgeApi
import github.detrig.feature.fridge.api.FridgeApiImpl
import github.detrig.feature.fridge.navigation.FridgeRouter
import github.detrig.feature.fridge.navigation.FridgeRouterImpl
import github.detrig.feature.fridge.presentation.FridgeViewModel

internal class FridgeModule(
    private val dependencies: FridgeDependencies,
) : FridgeComponent {
    override val roomApi = dependencies.roomApi()
    override val petApi = dependencies.petApi()
    override val inventoryApi = dependencies.inventoryApi()
    override val gameStateApi = dependencies.gameStateApi()
    override val artworkResolver = dependencies.artworkResolver()

    override val router: FridgeRouter by lazy {
        FridgeRouterImpl(dependencies.globalNavigator())
    }

    override val api: FridgeApi by lazy { FridgeApiImpl(router, this) }

    override fun viewModel(): FridgeViewModel = FridgeViewModel(
        inventoryApi = inventoryApi,
        router = router,
    )

    override fun feedingViewModel(): github.detrig.feature.fridge.presentation.FeedingViewModel =
        github.detrig.feature.fridge.presentation.FeedingViewModel(
            inventoryApi = inventoryApi,
            gameStateApi = gameStateApi,
            router = router,
        )
}
