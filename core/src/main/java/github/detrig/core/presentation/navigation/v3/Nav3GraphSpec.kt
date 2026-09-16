package github.detrig.core.presentation.navigation.v3

import android.os.Bundle
import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

typealias EntryHostProviderInstaller = EntryProviderScope<NavKey>.() -> Unit

@Immutable
data class Nav3HostGraphSpec(
    val graphKey: NavGraphKey? = null,
    val startBackStack: (Bundle) -> List<NavKey>,
    val builder: EntryHostProviderInstaller,
) {
    companion object {
        operator fun invoke(
            graphKey: NavGraphKey? = null,
            startBackStack: List<NavKey>,
            builder: EntryHostProviderInstaller,
        ) = Nav3HostGraphSpec(
            graphKey = graphKey,
            startBackStack = { startBackStack },
            builder = builder,
        )

        operator fun invoke(
            graphKey: NavGraphKey? = null,
            startRoute: NavKey,
            builder: EntryHostProviderInstaller,
        ) = Nav3HostGraphSpec(
            graphKey = graphKey,
            startBackStack = { listOf(startRoute) },
            builder = builder,
        )

        @JvmName("invokeWithRouteProvider")
        operator fun invoke(
            graphKey: NavGraphKey? = null,
            startRoute: (Bundle) -> NavKey,
            builder: EntryHostProviderInstaller,
        ) = Nav3HostGraphSpec(
            graphKey = graphKey,
            startBackStack = { bundle -> listOf(startRoute(bundle)) },
            builder = builder,
        )
    }
}

@Immutable
class Nav3NestedGraphSpec(
    val graphKey: NavGraphKey? = null,
    val startBackStack: List<NavKey>,
    val builder: EntryHostProviderInstaller,
) {
    companion object {
        operator fun invoke(
            graphKey: NavGraphKey? = null,
            startRoute: NavKey,
            builder: EntryHostProviderInstaller,
        ) = Nav3NestedGraphSpec(
            graphKey = graphKey,
            startBackStack = listOf(startRoute),
            builder = builder,
        )
    }
}
