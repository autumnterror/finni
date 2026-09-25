package github.detrig.feature.gamestate

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.gamestate.data.local.GameStateDao
import github.detrig.feature.gamestate.data.local.RoomZoneDao
import github.detrig.feature.gamestate.domain.GameStateInitialConfig
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.gamestate.domain.model.PetNeedDecayConfig

interface GameStateDependencies {

    fun gameStateDao(): GameStateDao

    fun roomZoneDao(): RoomZoneDao

    fun petPlayEffectDao(): github.detrig.feature.gamestate.data.local.PetPlayEffectDao

    fun economyApi(): EconomyApi

    fun transactionRunner(): RoomTransactionRunner

    fun initialConfig(): GameStateInitialConfig

    fun currentTimeMillis(): Long

    fun needDecayConfig(): PetNeedDecayConfig = PetNeedDecayConfig()
}
