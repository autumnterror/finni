package github.detrig.core.presentation.navigation.v3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import github.detrig.core.CoreApplication

inline fun <reified T : NavKey> EntryProviderScope<NavKey>.composable(
    noinline content: @Composable (T) -> Unit,
) {
    entry<T>(
        content = content,
    )
}

inline fun <reified T : NavKey> EntryProviderScope<NavKey>.nestedGraph(
    noinline nestedGraphProvider: (T) -> Nav3NestedGraphSpec,
) {
    entry<T> { params ->
        val navigationHandler = CoreApplication.app.coreComponent
            .globalNavigator
            .currentNavigationHandler()

        check(navigationHandler is Nav3NavigationHandler) {
            "Expected Nav3NavigationHandler but was $navigationHandler"
        }

        val graphSpec = nestedGraphProvider(params)
        val backStack = rememberNavBackStack(*graphSpec.startBackStack.toTypedArray())

        NavDisplay(
            backStack = backStack,
            onBack = { navigationHandler.back() },
            entryProvider = entryProvider { graphSpec.builder(this) },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        )

        LaunchedEffect(Unit) {
            val backStackKey = graphSpec.graphKey ?: DefaultNavGraphKey(backStack)
            navigationHandler.addBackStack(backStackKey, backStack)
        }
    }
}
