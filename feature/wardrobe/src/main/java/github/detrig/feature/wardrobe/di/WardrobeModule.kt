package github.detrig.feature.wardrobe.di

import github.detrig.feature.wardrobe.WardrobeDependencies
import github.detrig.feature.wardrobe.api.WardrobeApi
import github.detrig.feature.wardrobe.api.WardrobeApiImpl
import github.detrig.feature.wardrobe.domain.ClothingPurchaseInteractor
import github.detrig.feature.wardrobe.navigation.WardrobeRouterImpl
import github.detrig.feature.wardrobe.presentation.WardrobeViewModel
import github.detrig.feature.wardrobe.presentation.WardrobeMode

internal class WardrobeModule(private val dependencies: WardrobeDependencies) : WardrobeComponent {
    override val petApi: github.detrig.feature.pet.api.PetApi by lazy { dependencies.petApi() }
    private val router by lazy { WardrobeRouterImpl(dependencies.globalNavigator()) }
    private val purchase by lazy {
        ClothingPurchaseInteractor(
            dependencies.petApi(), dependencies.economyApi(),
            dependencies.planningApi(), dependencies.weekApi(), dependencies.gameStateApi(),
            wishIdForClothing = dependencies::wishIdForClothing,
        )
    }
    override val api: WardrobeApi by lazy { WardrobeApiImpl(router) }

    override fun viewModel(mode: WardrobeMode): WardrobeViewModel = WardrobeViewModel(
        mode, dependencies.petApi(), dependencies.economyApi(), purchase, router,
    )
}
