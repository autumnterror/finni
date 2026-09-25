package github.detrig.minigames.fishing.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.minigames.fishing.FishingFeature
import github.detrig.minigames.fishing.navigation.FishingRoute
import github.detrig.minigames.fishing.navigation.FishingRouter
import github.detrig.minigames.fishing.presentation.FishingScreen

internal class FishingApiImpl(private val router: FishingRouter) : FishingApi {
    override fun open() = router.open()
    override fun entries(): EntryHostProviderInstaller = {
        composable<FishingRoute.Home> {
            FishingScreen(petApi = FishingFeature.component().petApi,
                gameAudio = FishingFeature.component().gameAudio)
        }
    }
}
