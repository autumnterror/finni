package github.detrig.feature.wardrobe.api

import androidx.compose.runtime.Composable
import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.feature.wardrobe.navigation.WardrobeRoute
import github.detrig.feature.wardrobe.navigation.WardrobeRouter
import github.detrig.feature.wardrobe.presentation.WardrobeScreen
import github.detrig.feature.wardrobe.presentation.WardrobeMode

internal class WardrobeApiImpl(private val router: WardrobeRouter) : WardrobeApi {
    override fun open() = router.open()

    @Composable
    override fun StoreContent(onBack: () -> Unit) = WardrobeScreen(
        mode = WardrobeMode.SHOP,
        onBack = onBack,
    )

    override fun entries(): EntryHostProviderInstaller = {
        composable<WardrobeRoute.Home> { WardrobeScreen(mode = WardrobeMode.OWNED) }
    }
}
