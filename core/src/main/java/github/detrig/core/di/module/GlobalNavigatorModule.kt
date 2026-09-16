package github.detrig.core.di.module

import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.core.presentation.navigation.impl.GlobalNavigatorImpl

interface GlobalNavigatorModule {
    val globalNavigator: GlobalNavigator
}

internal class GlobalNavigatorModuleImpl : GlobalNavigatorModule {

    override val globalNavigator: GlobalNavigator by lazy {
        GlobalNavigatorImpl()
    }
}
