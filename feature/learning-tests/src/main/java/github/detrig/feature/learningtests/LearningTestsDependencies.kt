package github.detrig.feature.learningtests

import android.content.SharedPreferences
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.gamestate.api.ProgressionApi
import github.detrig.feature.learningtests.data.local.LearningTestsDao
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.week.api.WeekApi

interface LearningTestsDependencies {
    fun learningTestsDao(): LearningTestsDao
    fun tutorialPreferences(): SharedPreferences
    fun transactionRunner(): RoomTransactionRunner
    fun economyApi(): EconomyApi
    fun progressionApi(): ProgressionApi
    fun weekApi(): WeekApi
    fun petApi(): PetApi
    fun globalNavigator(): GlobalNavigator
}
