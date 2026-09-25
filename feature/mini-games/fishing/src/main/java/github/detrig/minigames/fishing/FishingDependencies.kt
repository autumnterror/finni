package github.detrig.minigames.fishing

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.core.audio.GameAudio
import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.feature.pet.api.PetApi
import github.detrig.minigames.fishing.api.FishingHost
import github.detrig.minigames.fishing.data.FishingDao

interface FishingDependencies {
    fun host(): FishingHost
    fun fishingDao(): FishingDao
    fun transactionRunner(): RoomTransactionRunner
    fun globalNavigator(): GlobalNavigator
    fun configurationJson(): String
    fun currentTimeMillis(): Long
    fun petApi(): PetApi
    fun gameAudio(): GameAudio
}
