package github.detrig.feature.room

import android.content.SharedPreferences
import android.content.res.Resources

import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.core.audio.GameAudio
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.savings.api.SavingsApi
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.room.api.RoomGameLauncher
import github.detrig.feature.room.api.RoomWardrobeLauncher
import github.detrig.feature.room.domain.model.RoomImpulseWishSource

interface RoomDependencies {
    fun housePreferences(): SharedPreferences
    fun gameStateApi(): GameStateApi
    fun economyApi(): EconomyApi
    fun weekApi(): WeekApi
    fun planningApi(): PlanningApi
    fun learningApi(): LearningApi
    fun savingsApi(): SavingsApi
    fun inventoryApi(): InventoryApi
    fun gameLauncher(): RoomGameLauncher
    fun marketLauncher(): github.detrig.feature.room.api.RoomMarketLauncher
    fun wardrobeLauncher(): RoomWardrobeLauncher
    fun impulseWishSource(): RoomImpulseWishSource
    fun globalMessageController(): GlobalMessageController
    fun resources(): Resources
    fun gameAudio(): GameAudio
    fun minimumProductPriceRub(): Long
}
