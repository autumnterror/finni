package github.detrig.feature.gamestate.api

import github.detrig.feature.gamestate.domain.GameState
import kotlinx.coroutines.flow.Flow
import github.detrig.feature.gamestate.domain.model.ZoneOffer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.PetFeedingCompletion
import github.detrig.feature.gamestate.domain.model.PetFeedingResult

interface GameStateApi {

    /** Восстанавливает игру или создаёт её один раз; существующие данные не сбрасывает. */
    suspend fun initialize(): GameState

    /** Наблюдает сохранение без создания игры. Отсутствующее сохранение выдаёт null. */
    fun observeState(): Flow<GameState?>

    /** Last observed value for an immediate first frame while the persisted flow resumes. */
    val latestObservedState: GameState? get() = null

    /** Проверяет условия, списывает валюту и сохраняет зону в одной транзакции. */
    suspend fun buyZone(offer: ZoneOffer, useSavings: Boolean = false): ZoneBuyResult

    /** Применяет эффект игры один раз; повтор возвращает фактически записанную дельту. */
    suspend fun completePetPlay(completion: github.detrig.feature.gamestate.domain.model.PetPlayCompletion): Int

    /** Completes bathing and starts a fresh real-time and mini-game dirt cycle. */
    suspend fun washPet()

    /** Adjusts the visible dirt stage by one for the debug menu, clamped to 0..3. */
    suspend fun adjustPetDirtStageForDebug(delta: Int): Int
    /** Grants the first launch bonus once per game and game day; returns this call's gain. */
    suspend fun rewardMiniGameLaunch(gameId: String, absoluteDay: Long): Int

    /** Grants a new clothing item's bonus once for its committed purchase. */
    suspend fun rewardClothingPurchase(purchaseOperationId: String, happinessPoints: Int): Int

    /** Applies a concrete food portion only once, even after process restoration. */
    suspend fun feedPet(completion: PetFeedingCompletion): PetFeedingResult

    /** Applies hunger and happiness loss in the successful End day transaction. */
    suspend fun applyDayNeeds()

    /** Read-only notification outbox state; null before the game is created. */
    suspend fun hungerAlertState(): github.detrig.feature.gamestate.domain.model.HungerAlertState?

    suspend fun markHungerAlertDelivered(episode: Long): Boolean
}
