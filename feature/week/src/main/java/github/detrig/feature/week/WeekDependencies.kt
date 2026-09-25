package github.detrig.feature.week

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.week.data.local.WeekDao
import github.detrig.feature.week.domain.PetDayEffects

interface WeekDependencies {
    fun weekDao(): WeekDao
    fun economyApi(): EconomyApi
    fun petDayEffects(): PetDayEffects
    fun transactionRunner(): RoomTransactionRunner
}
