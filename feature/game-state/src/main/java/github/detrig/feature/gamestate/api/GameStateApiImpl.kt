package github.detrig.feature.gamestate.api

import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.gamestate.domain.GameStateRepository
import kotlinx.coroutines.flow.Flow
import github.detrig.feature.gamestate.domain.interactor.BuyZoneInteractor
import github.detrig.feature.gamestate.domain.model.ZoneOffer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.PetFeedingCompletion
import github.detrig.feature.gamestate.domain.model.PetFeedingResult
import kotlinx.coroutines.flow.onEach

internal class GameStateApiImpl(
    private val repository: GameStateRepository,
    private val buyZoneInteractor: BuyZoneInteractor,
) : GameStateApi {

    @Volatile
    private var observedState: GameState? = null

    override val latestObservedState: GameState? get() = observedState

    override suspend fun initialize(): GameState = repository.initialize()

    override fun observeState(): Flow<GameState?> = repository.observeState().onEach {
        observedState = it
    }

    override suspend fun buyZone(offer: ZoneOffer, useSavings: Boolean): ZoneBuyResult =
        buyZoneInteractor(offer, useSavings)

    override suspend fun completePetPlay(completion: github.detrig.feature.gamestate.domain.model.PetPlayCompletion): Int =
        repository.completePetPlay(completion)

    override suspend fun washPet() = repository.washPet()

    override suspend fun adjustPetDirtStageForDebug(delta: Int): Int =
        repository.adjustPetDirtStageForDebug(delta)

    override suspend fun feedPet(completion: PetFeedingCompletion): PetFeedingResult =
        repository.feedPet(completion)

    override suspend fun consumeHungerForSleep(): Int = repository.consumeHungerForSleep()

    override suspend fun reconcileTimedNeeds(nowMillis: Long) = repository.reconcileTimedNeeds(nowMillis)

    override suspend fun hungerAlertState() = repository.hungerAlertState()

    override suspend fun markHungerAlertDelivered(episode: Long): Boolean =
        repository.markHungerAlertDelivered(episode)
}
