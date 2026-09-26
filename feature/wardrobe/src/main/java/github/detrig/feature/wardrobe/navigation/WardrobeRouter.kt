package github.detrig.feature.wardrobe.navigation

import github.detrig.core.presentation.navigation.GlobalNavigator

internal interface WardrobeRouter {
    fun open()
    fun back()
}

internal class WardrobeRouterImpl(private val navigator: GlobalNavigator) : WardrobeRouter {
    override fun open() = navigator.navigate(WardrobeRoute.Home, launchSingleTop = true)
    override fun back() = navigator.back()
}
