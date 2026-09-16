package github.detrig.minigames.flight.navigation

import github.detrig.core.presentation.navigation.GlobalNavigator

internal interface FlightRouter {
    fun open()
    fun back()
}

internal class FlightRouterImpl(private val navigator: GlobalNavigator) : FlightRouter {
    override fun open() = navigator.navigate(FlightRoute.Play, launchSingleTop = true)
    override fun back() = navigator.back()
}
