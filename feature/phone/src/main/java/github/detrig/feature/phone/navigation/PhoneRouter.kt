package github.detrig.feature.phone.navigation

import github.detrig.core.presentation.navigation.GlobalNavigator

internal interface PhoneRouter {
    fun open()
    fun openApp(appId: String)
    fun back()
    fun close()
}

internal class PhoneRouterImpl(
    private val navigator: GlobalNavigator,
) : PhoneRouter {
    override fun open() = navigator.navigate(PhoneRoute.Home, launchSingleTop = true)

    override fun openApp(appId: String) = navigator.navigate(
        PhoneRoute.App(appId),
        launchSingleTop = true,
    )

    override fun back() = navigator.back()

    override fun close() = navigator.back()
}
