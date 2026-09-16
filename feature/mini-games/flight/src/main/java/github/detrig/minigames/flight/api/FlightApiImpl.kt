package github.detrig.minigames.flight.api

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.minigames.flight.navigation.FlightRoute
import github.detrig.minigames.flight.navigation.FlightRouter
import github.detrig.minigames.flight.presentation.FlightScreen

internal class FlightApiImpl(private val router: FlightRouter) : FlightApi {
    override fun open() = router.open()
    override fun installEntries(scope: EntryProviderScope<NavKey>) {
        scope.composable<FlightRoute.Play> { FlightScreen() }
    }
}
