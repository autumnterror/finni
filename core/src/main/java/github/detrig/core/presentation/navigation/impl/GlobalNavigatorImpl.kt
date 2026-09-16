package github.detrig.core.presentation.navigation.impl

import androidx.navigation3.runtime.NavKey
import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.core.presentation.navigation.NavigationHandler
import github.detrig.core.presentation.navigation.v3.NavGraphKey
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

/**
 * Реализация глобального навигатора.
 *
 * Сам не меняет back stack. Его задача - принять команду и передать ее
 * текущему [NavigationHandler]. Если handler еще не готов, команда временно
 * сохраняется в [commandBuffer].
 */
class GlobalNavigatorImpl : GlobalNavigator {

    /**
     * Команды, которые пришли до того, как Activity передала свой NavigationHandler.
     */
    private val commandBuffer = arrayListOf<NavigatorCommand>()

    /**
     * Слабая ссылка на handler активной Activity.
     *
     * GlobalNavigator живет дольше Activity, поэтому не должен удерживать ее жесткой ссылкой.
     */
    private var navigationHandler: WeakReference<NavigationHandler?> = WeakReference(null)

    override fun setNavigationHandler(
        navigationHandler: NavigationHandler,
        executePendingCommands: Boolean,
    ) {
        this.navigationHandler = WeakReference(navigationHandler)

        if (executePendingCommands) {
            commandBuffer.forEach { command ->
                command.run(navigationHandler)
            }
            commandBuffer.clear()
        }
    }

    override fun resetNavigationHandler() {
        navigationHandler = WeakReference(null)
    }

    override fun currentNavigationHandler(): NavigationHandler? {
        return navigationHandler.get()
    }

    override fun navigate(route: NavKey, launchSingleTop: Boolean) {
        handleCommand(NavigatorCommand.Navigate(route, launchSingleTop))
    }

    override fun replace(route: NavKey, launchSingleTop: Boolean) {
        handleCommand(NavigatorCommand.Replace(route, launchSingleTop))
    }

    override fun back() {
        handleCommand(NavigatorCommand.Back)
    }

    override fun backTo(route: NavKey) {
        handleCommand(NavigatorCommand.BackToRoute(route))
    }

    override fun backTo(type: KClass<out NavKey>) {
        handleCommand(NavigatorCommand.BackToType(type))
    }

    override fun backToStartRoute() {
        handleCommand(NavigatorCommand.BackToStartRoute)
    }

    override fun backToHostStartRoute() {
        handleCommand(NavigatorCommand.BackToHostStartRoute)
    }

    override fun backToGraph(key: NavGraphKey) {
        handleCommand(NavigatorCommand.BackToGraph(key))
    }

    override fun openNewStartRoute(route: NavKey) {
        handleCommand(NavigatorCommand.OpenNewStartRoute(route))
    }

    override fun addBackStack(key: NavGraphKey, stack: MutableList<NavKey>) {
        handleCommand(NavigatorCommand.AddBackStack(key, stack))
    }

    override fun getCurrentScreenName(): String {
        return currentNavigationHandler()?.getCurrentScreenName().orEmpty()
    }

    private fun handleCommand(command: NavigatorCommand) {
        val handler = navigationHandler.get()
        if (handler != null) {
            command.run(handler)
        } else {
            commandBuffer.add(command)
        }
    }

    /**
     * Команды навигации.
     *
     * Каждая команда знает, как выполниться на конкретном NavigationHandler.
     * Такой слой нужен, чтобы GlobalNavigator мог буферизовать действия до готовности Activity.
     */
    private sealed interface NavigatorCommand {

        fun run(navigationHandler: NavigationHandler)

        data class Navigate(
            val route: NavKey,
            val launchSingleTop: Boolean,
        ) : NavigatorCommand {
            override fun run(navigationHandler: NavigationHandler) {
                navigationHandler.navigate(route, launchSingleTop)
            }
        }

        data class Replace(
            val route: NavKey,
            val launchSingleTop: Boolean,
        ) : NavigatorCommand {
            override fun run(navigationHandler: NavigationHandler) {
                navigationHandler.replace(route, launchSingleTop)
            }
        }

        data object Back : NavigatorCommand {
            override fun run(navigationHandler: NavigationHandler) {
                navigationHandler.back()
            }
        }

        data class BackToRoute(
            val route: NavKey,
        ) : NavigatorCommand {
            override fun run(navigationHandler: NavigationHandler) {
                navigationHandler.backTo(route)
            }
        }

        data class BackToType(
            val type: KClass<out NavKey>,
        ) : NavigatorCommand {
            override fun run(navigationHandler: NavigationHandler) {
                navigationHandler.backTo(type)
            }
        }

        data object BackToStartRoute : NavigatorCommand {
            override fun run(navigationHandler: NavigationHandler) {
                navigationHandler.backToStartRoute()
            }
        }

        data object BackToHostStartRoute : NavigatorCommand {
            override fun run(navigationHandler: NavigationHandler) {
                navigationHandler.backToHostStartRoute()
            }
        }

        data class BackToGraph(
            val key: NavGraphKey,
        ) : NavigatorCommand {
            override fun run(navigationHandler: NavigationHandler) {
                navigationHandler.backToGraph(key)
            }
        }

        data class OpenNewStartRoute(
            val route: NavKey,
        ) : NavigatorCommand {
            override fun run(navigationHandler: NavigationHandler) {
                navigationHandler.openNewStartRoute(route)
            }
        }

        data class AddBackStack(
            val key: NavGraphKey,
            val stack: MutableList<NavKey>,
        ) : NavigatorCommand {
            override fun run(navigationHandler: NavigationHandler) {
                navigationHandler.addBackStack(key, stack)
            }
        }
    }
}
