package github.detrig.feature.gamestate.data

import github.detrig.feature.gamestate.data.local.GameStateLocalDataSource
import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.gamestate.domain.GameStateRepository
import kotlinx.coroutines.flow.Flow
import github.detrig.feature.gamestate.domain.model.ZoneOffer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.PetFeedingCompletion
import github.detrig.feature.gamestate.domain.model.PetFeedingResult

internal class GameStateRepositoryImpl(
    private val localDataSource: GameStateLocalDataSource,
) : GameStateRepository {

    override suspend fun initialize(): GameState = localDataSource.initialize()

    override fun observeState(): Flow<GameState?> = localDataSource.observeState()

    override suspend fun buyZone(offer: ZoneOffer): ZoneBuyResult = localDataSource.buyZone(offer)

    override suspend fun completePetPlay(completion: github.detrig.feature.gamestate.domain.model.PetPlayCompletion): Int =
        localDataSource.completePetPlay(completion)

    override suspend fun feedPet(completion: PetFeedingCompletion): PetFeedingResult =
        localDataSource.feedPet(completion)
}
