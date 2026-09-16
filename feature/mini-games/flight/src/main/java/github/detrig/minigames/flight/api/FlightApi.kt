package github.detrig.minigames.flight.api

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

interface FlightApi {
    fun open()
    fun installEntries(scope: EntryProviderScope<NavKey>)
}
