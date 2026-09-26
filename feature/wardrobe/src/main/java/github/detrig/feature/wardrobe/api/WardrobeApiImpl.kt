package github.detrig.feature.wardrobe.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.feature.wardrobe.navigation.WardrobeRoute
import github.detrig.feature.wardrobe.navigation.WardrobeRouter
import github.detrig.feature.wardrobe.presentation.WardrobeScreen

internal class WardrobeApiImpl(private val router: WardrobeRouter) : WardrobeApi {
    override fun open() = router.open()

    override fun entries(): EntryHostProviderInstaller = {
        composable<WardrobeRoute.Home> { WardrobeScreen() }
    }
}
