package github.detrig.feature.gamestate.data

import github.detrig.feature.gamestate.data.local.GameStateLocalDataSource
import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.gamestate.domain.GameStateRepository
import kotlinx.coroutines.flow.Flow
import github.detrig.feature.gamestate.domain.model.ZoneOffer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.PetFeedingCompletion
import github.detrig.feature.gamestate.domain.model.PetFeedingResult
import github.detrig.feature.gamestate.domain.progression.GameProgress
import github.detrig.feature.gamestate.domain.progression.GrantXpResult

internal class GameStateRepositoryImpl(
    private val localDataSource: GameStateLocalDataSource,
) : GameStateRepository {

    override suspend fun initialize(): GameState = localDataSource.initialize()

    override fun observeState(): Flow<GameState?> = localDataSource.observeState()

    override suspend fun buyZone(offer: ZoneOffer, useSavings: Boolean): ZoneBuyResult =
        localDataSource.buyZone(offer, useSavings)

    override suspend fun completePetPlay(completion: github.detrig.feature.gamestate.domain.model.PetPlayCompletion): Int =
        localDataSource.completePetPlay(completion)

    override suspend fun feedPet(completion: PetFeedingCompletion): PetFeedingResult =
        localDataSource.feedPet(completion)

    override suspend fun consumeHungerForSleep(): Int = localDataSource.consumeHungerForSleep()

    override suspend fun reconcileTimedNeeds(nowMillis: Long) = localDataSource.reconcileTimedNeeds(nowMillis)

    override suspend fun hungerAlertState() = localDataSource.hungerAlertState()

    override suspend fun markHungerAlertDelivered(episode: Long): Boolean =
        localDataSource.markHungerAlertDelivered(episode)

    override suspend fun grantXp(
        grantId: String,
        profileId: String,
        amount: Int,
        source: String,
    ): GrantXpResult = localDataSource.grantXp(grantId, profileId, amount, source)

    override fun observeProgress(profileId: String): Flow<GameProgress> =
        localDataSource.observeProgress(profileId)
}
