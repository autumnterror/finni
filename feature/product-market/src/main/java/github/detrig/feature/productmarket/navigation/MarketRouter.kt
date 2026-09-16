package github.detrig.feature.productmarket.navigation

import github.detrig.core.presentation.navigation.GlobalNavigator

internal interface MarketRouter { fun open(); fun back() }
internal class MarketRouterImpl(private val navigator: GlobalNavigator) : MarketRouter {
    override fun open() { navigator.navigate(MarketRoute.Home, launchSingleTop = true) }
    override fun back() { navigator.back() }
}
