package github.detrig.feature.gamestate.domain

import kotlinx.coroutines.flow.Flow
import github.detrig.feature.gamestate.domain.model.ZoneOffer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.PetFeedingCompletion
import github.detrig.feature.gamestate.domain.model.PetFeedingResult

internal interface GameStateRepository {

    suspend fun initialize(): GameState

    fun observeState(): Flow<GameState?>

    suspend fun buyZone(offer: ZoneOffer): ZoneBuyResult

    suspend fun completePetPlay(completion: github.detrig.feature.gamestate.domain.model.PetPlayCompletion): Int

    suspend fun feedPet(completion: PetFeedingCompletion): PetFeedingResult
    suspend fun consumeHungerForSleep(): Int
    suspend fun startFirstNeed(): Int
    suspend fun reconcileTimedNeeds(nowMillis: Long)
    suspend fun hungerAlertState(): github.detrig.feature.gamestate.domain.model.HungerAlertState?
    suspend fun markHungerAlertDelivered(episode: Long): Boolean
}
