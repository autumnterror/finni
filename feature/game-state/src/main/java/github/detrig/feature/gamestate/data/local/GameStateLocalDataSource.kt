package github.detrig.feature.gamestate.data.local

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.gamestate.domain.GameStateInitialConfig
import github.detrig.feature.gamestate.domain.model.ZoneOffer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.MiniGameAccess
import github.detrig.feature.gamestate.domain.model.PetFeedingCompletion
import github.detrig.feature.gamestate.domain.model.PetFeedingResult
import github.detrig.feature.gamestate.domain.model.PetSatietyRules
import github.detrig.feature.gamestate.domain.model.PetNeedDecayConfig
import github.detrig.feature.gamestate.domain.model.TimedPetNeeds
import github.detrig.feature.gamestate.domain.model.reconcilePetNeeds
import github.detrig.feature.gamestate.domain.model.HungerAlertState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.RejectionReason

internal class GameStateLocalDataSource(
    private val dao: GameStateDao,
    private val zoneDao: RoomZoneDao,
    private val petPlayEffectDao: PetPlayEffectDao,
    private val economyApi: EconomyApi,
    private val transactionRunner: RoomTransactionRunner,
    private val initialConfig: GameStateInitialConfig,
    private val currentTimeMillis: () -> Long,
    private val needDecayConfig: PetNeedDecayConfig,
) {

    suspend fun initialize(): GameState = transactionRunner.runInTransaction {
        val stored = dao.getCurrentStateWithZones()
        if (stored != null) {
            reconcileTimedNeeds(currentTimeMillis())
            return@runInTransaction checkNotNull(dao.getCurrentStateWithZones()).toDomain()
        }

        dao.insertInitialState(initialConfig.createState().toEntity(currentTimeMillis()))
        // Возвращаем сохранённую запись; конфликт вставки не перезаписывает прогресс.
        checkNotNull(dao.getCurrentStateWithZones()).toDomain()
    }

    fun observeState(): Flow<GameState?> {
        return dao.observeCurrentStateWithZones()
            .map { it?.toDomain() }
            .distinctUntilChanged()
    }

    suspend fun reconcileTimedNeeds(nowMillis: Long): Unit = transactionRunner.runInTransaction {
        val state = dao.getCurrentState() ?: return@runInTransaction
        if (state.hunger == 0 && state.hungerAlertEpisode == 0L) {
            dao.seedZeroHungerAlertEpisode()
        }
        val current = TimedPetNeeds(
            state.hunger, state.happiness,
            state.hungerCheckpointMillis, state.happinessCheckpointMillis,
        )
        val updated = reconcilePetNeeds(current, nowMillis, needDecayConfig)
        if (updated != current) {
            check(dao.updateTimedNeeds(
                updated.hunger, updated.happiness,
                updated.hungerCheckpointMillis, updated.happinessCheckpointMillis,
            ) == 1)
        }
    }

    suspend fun hungerAlertState(): HungerAlertState? = dao.hungerAlertState()

    suspend fun markHungerAlertDelivered(episode: Long): Boolean =
        dao.markHungerAlertDelivered(episode) == 1

    suspend fun completePetPlay(completion: github.detrig.feature.gamestate.domain.model.PetPlayCompletion): Int =
        transactionRunner.runInTransaction {
            require(completion.profileId == GameStateEntity.CURRENT_STATE_ID)
            require(completion.sessionId.isNotBlank() && completion.gameId.isNotBlank())
            require(completion.validActionCount >= 0 && completion.activePlayMillis >= 0)
            val operationId = completion.profileId + ":" + completion.sessionId
            val existing = petPlayEffectDao.find(operationId)
            if (existing != null) {
                require(existing.gameId == completion.gameId) { "Session belongs to another game" }
                return@runInTransaction existing.happinessDelta
            }
            val current = initialize()
            require(MiniGameAccess.isOpen(completion.gameId, current.ownedZoneIds)) { "Game is not unlocked" }
            val delta = github.detrig.feature.gamestate.domain.model.PetPlayReward.delta(
                current.pet.happiness, completion,
            )
            check(dao.increaseHappiness(delta, currentTimeMillis()) == 1)
            petPlayEffectDao.insert(PetPlayEffectEntity(operationId, completion.profileId, completion.sessionId,
                completion.gameId, delta, currentTimeMillis()))
            delta
        }

    suspend fun consumeHungerForSleep(): Int = transactionRunner.runInTransaction {
        val current = initialize()
        check(dao.decreaseHunger(PetSatietyRules.SLEEP_COST) == 1)
        PetSatietyRules.afterCost(current.pet.hunger, PetSatietyRules.SLEEP_COST)
    }

    suspend fun startFirstNeed(): Int = transactionRunner.runInTransaction {
        val operationId = FIRST_NEED_EFFECT_OPERATION_ID
        val current = initialize()
        val existing = petPlayEffectDao.find(operationId)
        if (existing != null) return@runInTransaction current.pet.hunger
        check(dao.decreaseHunger(FIRST_NEED_HUNGER_COST) == 1)
        petPlayEffectDao.insert(
            PetPlayEffectEntity(
                operationId = operationId,
                profileId = GameStateEntity.CURRENT_STATE_ID,
                sessionId = operationId,
                gameId = FIRST_NEED_EFFECT_GAME_ID,
                happinessDelta = 0,
                appliedAtMillis = currentTimeMillis(),
            ),
        )
        PetSatietyRules.afterCost(current.pet.hunger, FIRST_NEED_HUNGER_COST)
    }

    /**
     * Reuses the existing idempotent pet-effect outbox. It lives in the same Room
     * transaction as the pet counters, so restoring the process cannot feed twice.
     */
    suspend fun feedPet(completion: PetFeedingCompletion): PetFeedingResult =
        transactionRunner.runInTransaction {
            val operationId = "feeding:${completion.operationId}"
            val current = initialize()
            val existing = petPlayEffectDao.find(operationId)
            if (existing != null) {
                require(existing.gameId == FEEDING_EFFECT_GAME_ID) { "Feeding operation id conflict" }
                return@runInTransaction PetFeedingResult(
                    hunger = current.pet.hunger,
                    happiness = current.pet.happiness,
                )
            }

            val hungerDelta = minOf(completion.satietyPercent, 100 - current.pet.hunger)
            val happinessDelta = minOf(completion.happinessPoints, 100 - current.pet.happiness)
            val nowMillis = currentTimeMillis()
            check(dao.increaseHunger(hungerDelta, nowMillis) == 1)
            check(dao.increaseHappiness(happinessDelta, nowMillis) == 1)
            petPlayEffectDao.insert(
                PetPlayEffectEntity(
                    operationId = operationId,
                    profileId = GameStateEntity.CURRENT_STATE_ID,
                    sessionId = completion.operationId,
                    gameId = FEEDING_EFFECT_GAME_ID,
                    happinessDelta = happinessDelta,
                    appliedAtMillis = currentTimeMillis(),
                ),
            )
            PetFeedingResult(
                hunger = current.pet.hunger + hungerDelta,
                happiness = current.pet.happiness + happinessDelta,
            )
        }

    suspend fun buyZone(offer: ZoneOffer): ZoneBuyResult = transactionRunner.runInTransaction {
        val current = initialize()
        val sessionId = GameStateEntity.CURRENT_STATE_ID
        if (MiniGameAccess.isOpen(offer.zoneId, current.ownedZoneIds)) {
            return@runInTransaction ZoneBuyResult.AlreadyOwned
        }
        if (current.playerLevel < offer.requiredLevel) {
            return@runInTransaction ZoneBuyResult.LevelTooLow(offer.requiredLevel)
        }
        val now = currentTimeMillis()
        when (val debit = economyApi.debit(
            operationId = "room-zone:${offer.zoneId}",
            amountRub = offer.priceRub.toLong(),
            context = OperationContext(reasonId = offer.zoneId, metadata = "source=room-zone"),
        )) {
            is FinancialOperationResult.Applied,
            is FinancialOperationResult.AlreadyApplied -> {
                zoneDao.insert(RoomZoneEntity(sessionId, offer.zoneId, now))
                ZoneBuyResult.Bought
            }
            is FinancialOperationResult.Rejected -> when (debit.reason) {
                RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS -> ZoneBuyResult.NotEnoughMoney(
                    Math.toIntExact(offer.priceRub.toLong() - debit.state.availableRub),
                )
                else -> error("Economy rejected room purchase: ${debit.reason}")
            }
        }
    }

    private companion object {
        const val FEEDING_EFFECT_GAME_ID = "feeding"
        const val FIRST_NEED_EFFECT_GAME_ID = "first_need"
        const val FIRST_NEED_EFFECT_OPERATION_ID = "first_need:hunger"
        const val FIRST_NEED_HUNGER_COST = 30
    }
}
