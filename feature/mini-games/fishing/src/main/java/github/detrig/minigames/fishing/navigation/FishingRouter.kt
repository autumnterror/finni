package github.detrig.minigames.fishing.navigation

import github.detrig.core.presentation.navigation.GlobalNavigator

internal interface FishingRouter { fun open(); fun back() }
internal class FishingRouterImpl(private val navigator: GlobalNavigator) : FishingRouter {
    override fun open() { navigator.navigate(FishingRoute.Home, launchSingleTop = true) }
    override fun back() { navigator.back() }
}
