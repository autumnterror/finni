package github.detrig.minigames.flight

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.core.audio.GameAudio
import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.feature.pet.api.PetApi
import github.detrig.minigames.flight.api.FlightHost
import github.detrig.minigames.flight.data.FlightDao

interface FlightDependencies {
    fun host(): FlightHost
    fun flightDao(): FlightDao
    fun transactionRunner(): RoomTransactionRunner
    fun globalNavigator(): GlobalNavigator
    fun configurationJson(): String
    fun currentTimeMillis(): Long
    fun petApi(): PetApi
    fun gameAudio(): GameAudio
}
