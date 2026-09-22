package github.detrig.feature.gamestate.api

import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.gamestate.domain.GameStateRepository
import kotlinx.coroutines.flow.Flow
import github.detrig.feature.gamestate.domain.interactor.BuyZoneInteractor
import github.detrig.feature.gamestate.domain.model.ZoneOffer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.PetFeedingCompletion
import github.detrig.feature.gamestate.domain.model.PetFeedingResult

internal class GameStateApiImpl(
    private val repository: GameStateRepository,
    private val buyZoneInteractor: BuyZoneInteractor,
) : GameStateApi {

    override suspend fun initialize(): GameState = repository.initialize()

    override fun observeState(): Flow<GameState?> = repository.observeState()

    override suspend fun buyZone(offer: ZoneOffer): ZoneBuyResult = buyZoneInteractor(offer)

    override suspend fun completePetPlay(completion: github.detrig.feature.gamestate.domain.model.PetPlayCompletion): Int =
        repository.completePetPlay(completion)

    override suspend fun feedPet(completion: PetFeedingCompletion): PetFeedingResult =
        repository.feedPet(completion)
}
