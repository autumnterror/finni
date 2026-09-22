package github.detrig.feature.fridge.navigation

import github.detrig.core.presentation.navigation.GlobalNavigator

internal interface FridgeRouter {
    fun open()
    fun openFeeding()
    fun back()
}

internal class FridgeRouterImpl(
    private val navigator: GlobalNavigator,
) : FridgeRouter {
    override fun open() = navigator.navigate(FridgeRoute.Home, launchSingleTop = true)

    override fun openFeeding() = navigator.navigate(FridgeRoute.Feeding, launchSingleTop = true)

    override fun back() = navigator.back()
}
