package github.detrig.feature.room

import android.content.SharedPreferences
import android.content.res.Resources

import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.savings.api.SavingsApi
import github.detrig.feature.room.api.RoomGameLauncher

interface RoomDependencies {
    fun housePreferences(): SharedPreferences
    fun gameStateApi(): GameStateApi
    fun economyApi(): EconomyApi
    fun weekApi(): WeekApi
    fun planningApi(): PlanningApi
    fun learningApi(): LearningApi
    fun savingsApi(): SavingsApi
    fun gameLauncher(): RoomGameLauncher
    fun marketLauncher(): github.detrig.feature.room.api.RoomMarketLauncher
    fun globalMessageController(): GlobalMessageController
    fun resources(): Resources
}
