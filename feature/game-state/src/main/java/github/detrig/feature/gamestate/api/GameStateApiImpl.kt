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
    override suspend fun rewardMiniGameLaunch(gameId: String, absoluteDay: Long): Int =
        repository.rewardMiniGameLaunch(gameId, absoluteDay)

    override suspend fun rewardClothingPurchase(purchaseOperationId: String, happinessPoints: Int): Int =
        repository.rewardClothingPurchase(purchaseOperationId, happinessPoints)

    override suspend fun rewardPetWishHappiness(wishRewardId: String, happinessPoints: Int): Int =
        repository.rewardPetWishHappiness(wishRewardId, happinessPoints)

    override suspend fun petWishActivities() = repository.petWishActivities()

    override suspend fun activateSavingsHappinessProtection(
        contributionOperationId: String,
        throughAbsoluteDay: Long,
    ) = repository.activateSavingsHappinessProtection(contributionOperationId, throughAbsoluteDay)

    override suspend fun feedPet(completion: PetFeedingCompletion): PetFeedingResult =
        repository.feedPet(completion)

    override suspend fun applyDayNeeds() = repository.applyDayNeeds()

    override suspend fun hungerAlertState() = repository.hungerAlertState()

    override suspend fun markHungerAlertDelivered(episode: Long): Boolean =
        repository.markHungerAlertDelivered(episode)
}
