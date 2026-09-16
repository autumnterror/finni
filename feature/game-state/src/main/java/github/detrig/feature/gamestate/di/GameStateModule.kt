package github.detrig.feature.gamestate.di

import github.detrig.feature.gamestate.GameStateDependencies
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.gamestate.api.GameStateApiImpl
import github.detrig.feature.gamestate.data.GameStateRepositoryImpl
import github.detrig.feature.gamestate.data.local.GameStateLocalDataSource
import github.detrig.feature.gamestate.domain.interactor.BuyZoneInteractor

internal class GameStateModule(
    private val dependencies: GameStateDependencies,
) : GameStateComponent {

    private val localDataSource by lazy {
        GameStateLocalDataSource(
            dao = dependencies.gameStateDao(),
            zoneDao = dependencies.roomZoneDao(),
            petPlayEffectDao = dependencies.petPlayEffectDao(),
            economyApi = dependencies.economyApi(),
            transactionRunner = dependencies.transactionRunner(),
            initialConfig = dependencies.initialConfig(),
            currentTimeMillis = dependencies::currentTimeMillis,
        )
    }

    private val repository by lazy {
        GameStateRepositoryImpl(localDataSource)
    }

    override val api: GameStateApi by lazy {
        GameStateApiImpl(repository, BuyZoneInteractor(repository))
    }
}
