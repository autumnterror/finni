package github.detrig.internetbooster.mediators

import androidx.annotation.MainThread
import github.detrig.core.Mediator
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.gamestate.GameStateDependencies
import github.detrig.feature.gamestate.GameStateFeature
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.gamestate.api.ProgressionApi
import github.detrig.feature.gamestate.data.local.GameStateDao
import github.detrig.feature.gamestate.data.local.RoomZoneDao
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.gamestate.domain.GameStateInitialConfig
import github.detrig.internetbooster.database.AppDatabaseModule

internal class GameStateMediator(
    private val databaseModule: AppDatabaseModule,
    private val economyMediator: EconomyMediator,
) : Mediator<GameStateApi> {

    @MainThread
    fun init() {
        GameStateFeature.dependenciesProvider = ModuleDependenciesProvider {
            GameStateDependenciesImpl(databaseModule, economyMediator.getApi())
        }
    }

    @MainThread
    override fun getApi(): GameStateApi = GameStateFeature.getApi()

    fun getProgressionApi(): ProgressionApi = GameStateFeature.getProgressionApi()
}

private class GameStateDependenciesImpl(
    private val databaseModule: AppDatabaseModule,
    private val economyApi: EconomyApi,
) : GameStateDependencies {

    override fun gameStateDao(): GameStateDao = databaseModule.gameStateDao

    override fun roomZoneDao(): RoomZoneDao = databaseModule.roomZoneDao

    override fun petPlayEffectDao() = databaseModule.petPlayEffectDao

    override fun economyApi(): EconomyApi = economyApi

    override fun transactionRunner(): RoomTransactionRunner = databaseModule.transactionRunner

    override fun initialConfig(): GameStateInitialConfig = GameStateInitialConfig()

    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
