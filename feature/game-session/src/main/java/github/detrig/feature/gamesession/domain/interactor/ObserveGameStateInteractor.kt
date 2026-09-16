package github.detrig.feature.gamesession.domain.interactor

import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.EconomyState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class ObserveGameStateInteractor(
    private val gameStateApi: GameStateApi,
    private val economyApi: EconomyApi,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {

    operator fun invoke(): Flow<GameSessionState> = flow {
        gameStateApi.initialize()
        economyApi.initialize()
        economyApi.processPeriodicIncome(currentTimeMillis())
        emitAll(
            combine(gameStateApi.observeState(), economyApi.observeState()) { game, economy ->
                GameSessionState(checkNotNull(game), economy)
            },
        )
    }
}

internal data class GameSessionState(
    val game: GameState,
    val economy: EconomyState,
)
