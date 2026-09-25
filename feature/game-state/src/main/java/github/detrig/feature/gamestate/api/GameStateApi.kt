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

    /** Проверяет условия, списывает валюту и сохраняет зону в одной транзакции. */
    suspend fun buyZone(offer: ZoneOffer): ZoneBuyResult

    /** Применяет эффект игры один раз; повтор возвращает фактически записанную дельту. */
    suspend fun completePetPlay(completion: github.detrig.feature.gamestate.domain.model.PetPlayCompletion): Int

    /** Applies a concrete food portion only once, even after process restoration. */
    suspend fun feedPet(completion: PetFeedingCompletion): PetFeedingResult

    /** Вызывается при успешном End day внутри общей транзакции Week. */
    suspend fun consumeHungerForSleep(): Int

    /** Safe to call repeatedly from foreground or background; does not create a pet. */
    suspend fun reconcileTimedNeeds(nowMillis: Long)

    /** Read-only notification outbox state; null before the game is created. */
    suspend fun hungerAlertState(): github.detrig.feature.gamestate.domain.model.HungerAlertState?

    suspend fun markHungerAlertDelivered(episode: Long): Boolean
}
