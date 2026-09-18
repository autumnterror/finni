package github.detrig.feature.savings.navigation

import github.detrig.core.presentation.navigation.GlobalNavigator

internal interface SavingsRouter {
    fun open()
    fun back()
}

internal class SavingsRouterImpl(private val navigator: GlobalNavigator) : SavingsRouter {
    override fun open() { navigator.navigate(SavingsRoute.Home, launchSingleTop = true) }
    override fun back() { navigator.back() }
}
