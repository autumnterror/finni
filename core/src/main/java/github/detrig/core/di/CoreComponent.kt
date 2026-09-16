package github.detrig.core.di

import android.content.Context
import android.content.res.Resources
import github.detrig.core.coroutines.CoroutinesDispatcherProvider
import github.detrig.core.di.module.CoreModule
import github.detrig.core.di.module.CoreModuleImpl
import github.detrig.core.di.module.GlobalNavigatorModule
import github.detrig.core.di.module.GlobalNavigatorModuleImpl
import github.detrig.core.exception.mapper.CoreExceptionMapper
import github.detrig.core.infrastructure.network.NetworkManager
import github.detrig.core.infrastructure.preferences.CorePreferences
import github.detrig.core.memory.GlobalSessionMemory
import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.core.presentation.navigation.GlobalNavigator
import kotlinx.coroutines.CoroutineScope

interface CoreComponent {

    val context: Context
    val resources: Resources
    val applicationScope: CoroutineScope
    val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
    val globalNavigator: GlobalNavigator
    val globalMessageController: GlobalMessageController
    val networkManager: NetworkManager
    val coreExceptionMapper: CoreExceptionMapper
    val globalSessionMemory: GlobalSessionMemory
    val corePreferences: CorePreferences
}

fun CoreComponent(
    dependencies: CoreComponentDependencies,
): CoreComponent {
    val coreModule = CoreModuleImpl(dependencies)
    val globalNavigatorModule = GlobalNavigatorModuleImpl()

    return object : CoreComponent,
        CoreModule by coreModule,
        GlobalNavigatorModule by globalNavigatorModule {
    }
}

// Это входы для core.
interface CoreComponentDependencies {

    val applicationContext: Context
}
