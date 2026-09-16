package github.detrig.feature.economy

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.economy.data.local.EconomyDao
import github.detrig.feature.economy.domain.EconomyConfig

interface EconomyDependencies {
    fun economyDao(): EconomyDao
    fun transactionRunner(): RoomTransactionRunner
    fun config(): EconomyConfig
    fun currentTimeMillis(): Long
}
