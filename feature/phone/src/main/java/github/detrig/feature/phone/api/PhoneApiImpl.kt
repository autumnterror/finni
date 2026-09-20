package github.detrig.feature.phone.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.feature.phone.navigation.PhoneRoute
import github.detrig.feature.phone.navigation.PhoneRouter
import github.detrig.feature.phone.presentation.PhoneScreen

internal class PhoneApiImpl(
    private val router: PhoneRouter,
) : PhoneApi {
    override fun open() = router.open()

    override fun entries(): EntryHostProviderInstaller = {
        composable<PhoneRoute.Home> { PhoneScreen(PhoneRoute.Home) }
        composable<PhoneRoute.App> { route -> PhoneScreen(route) }
    }
}
