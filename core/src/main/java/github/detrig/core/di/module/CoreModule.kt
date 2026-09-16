package github.detrig.core.di.module

import android.content.Context
import android.content.res.Resources
import github.detrig.core.coroutines.CoroutinesDispatcherProvider
import github.detrig.core.di.CoreComponentDependencies
import github.detrig.core.exception.mapper.CoreExceptionMapper
import github.detrig.core.infrastructure.network.NetworkManager
import github.detrig.core.infrastructure.network.NetworkManagerImpl
import github.detrig.core.infrastructure.preferences.CorePreferences
import github.detrig.core.infrastructure.preferences.CorePreferencesImpl
import github.detrig.core.infrastructure.preferences.PersistentConfigStorage
import github.detrig.core.memory.GlobalSessionMemory
import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.core.presentation.message.impl.GlobalMessageControllerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * CoreModule говорит, что core умеет предоставить.
 */
interface CoreModule {
    val context: Context
    val resources: Resources
    val applicationScope: CoroutineScope
    val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
    val networkManager: NetworkManager
    val coreExceptionMapper: CoreExceptionMapper
    val globalMessageController: GlobalMessageController
    val globalSessionMemory: GlobalSessionMemory
    val corePreferences: CorePreferences
}

/**
 * CoreModuleImpl говорит, как это создать.
 */
internal class CoreModuleImpl(
    private val dependencies: CoreComponentDependencies,
) : CoreModule {

    private companion object {
        const val CORE_PREFERENCES_NAME = "core_preferences"
    }

    override val context: Context =
        dependencies.applicationContext

    override val resources: Resources =
        dependencies.applicationContext.resources

    // applicationScope сделан через by lazy, значит он создастся один раз на весь CoreComponent.
    override val applicationScope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    override val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
        get() = CoroutinesDispatcherProvider()

    override val networkManager: NetworkManager by lazy {
        NetworkManagerImpl(context)
    }

    override val coreExceptionMapper: CoreExceptionMapper by lazy {
        CoreExceptionMapper(networkManager)
    }

    override val globalMessageController: GlobalMessageController by lazy {
        GlobalMessageControllerImpl()
    }

    override val globalSessionMemory: GlobalSessionMemory by lazy {
        GlobalSessionMemory()
    }

    override val corePreferences: CorePreferences by lazy {
        val sharedPreferences = dependencies.applicationContext.getSharedPreferences(
            CORE_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val persistentStorage = PersistentConfigStorage(
            dependencies.applicationContext.getSharedPreferences(
                PersistentConfigStorage.FILE_NAME,
                Context.MODE_PRIVATE,
            )
        )
        CorePreferencesImpl(sharedPreferences, persistentStorage)
    }
}
