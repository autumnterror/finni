package github.detrig.feature.fridge.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.feature.fridge.di.FridgeComponent
import github.detrig.feature.fridge.navigation.FridgeRoute
import github.detrig.feature.fridge.navigation.FridgeRouter
import github.detrig.feature.fridge.presentation.FridgeScreen
import github.detrig.feature.fridge.presentation.FridgeTableContent
import github.detrig.feature.fridge.presentation.FeedingScreen

internal class FridgeApiImpl(
    private val router: FridgeRouter,
    private val component: FridgeComponent,
) : FridgeApi {
    override fun open() = router.open()

    override fun openFeeding() = router.openFeeding()

    override fun entries(): EntryHostProviderInstaller = {
        composable<FridgeRoute.Home> { FridgeScreen() }
        composable<FridgeRoute.Feeding> { FeedingScreen() }
    }

    @Composable
    override fun TableContent(modifier: Modifier) {
        FridgeTableContent(
            inventoryApi = component.inventoryApi,
            artworkResolver = component.artworkResolver,
            modifier = modifier,
        )
    }
}
