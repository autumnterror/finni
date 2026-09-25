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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.gamestate.domain.progression.GameProgress
import github.detrig.feature.gamestate.domain.progression.GrantXpResult
import github.detrig.feature.gamestate.domain.progression.MiniGameXpPolicy
import github.detrig.feature.gamestate.domain.progression.ProgressionRules
import github.detrig.feature.gamestate.domain.progression.XpRewards
import github.detrig.feature.gamestate.domain.progression.XpSources

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

    fun observeProgress(profileId: String): Flow<GameProgress> = flow {
        require(profileId == GameStateEntity.CURRENT_STATE_ID) { "Unknown profile $profileId" }
        initialize()
        emitAll(
            dao.observeCurrentState()
                .filterNotNull()
                .map { ProgressionRules.progress(it.totalXp) }
                .distinctUntilChanged(),
        )
    }

    suspend fun grantXp(
        grantId: String,
        profileId: String,
        amount: Int,
        source: String,
    ): GrantXpResult = transactionRunner.runInTransaction {
        initialize()
        grantXpInTransaction(grantId, profileId, amount, source)
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
                grantMiniGameXp(completion)
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
            grantMiniGameXp(completion)
            delta
        }

    suspend fun consumeHungerForSleep(): Int = transactionRunner.runInTransaction {
        val current = initialize()
        check(dao.decreaseHunger(PetSatietyRules.SLEEP_COST) == 1)
        PetSatietyRules.afterCost(current.pet.hunger, PetSatietyRules.SLEEP_COST)
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

    suspend fun buyZone(offer: ZoneOffer, useSavings: Boolean = false): ZoneBuyResult = transactionRunner.runInTransaction {
        val current = initialize()
        val sessionId = GameStateEntity.CURRENT_STATE_ID
        if (MiniGameAccess.isOpen(offer.zoneId, current.ownedZoneIds)) {
            return@runInTransaction ZoneBuyResult.AlreadyOwned
        }
        if (current.playerLevel < offer.requiredLevel) {
            return@runInTransaction ZoneBuyResult.LevelTooLow(offer.requiredLevel)
        }
        if (useSavings) {
            val goal = economyApi.getActiveGoal()
            if (goal?.id != "room-zone:${offer.zoneId}") {
                return@runInTransaction ZoneBuyResult.NotEnoughMoney(offer.priceRub)
            }
            when (val transfer = economyApi.transferFromSavings(
                operationId = "room-zone:${offer.zoneId}:from-savings",
                amountRub = offer.priceRub.toLong(),
                context = OperationContext(reasonId = goal.id, metadata = "source=room-zone-purchase"),
            )) {
                is FinancialOperationResult.Applied,
                is FinancialOperationResult.AlreadyApplied -> Unit
                is FinancialOperationResult.Rejected -> when (transfer.reason) {
                    RejectionReason.INSUFFICIENT_SAVINGS -> return@runInTransaction ZoneBuyResult.NotEnoughMoney(
                        Math.toIntExact(offer.priceRub.toLong() - transfer.state.savingsRub),
                    )
                    else -> error("Economy rejected savings purchase: ${transfer.reason}")
                }
            }
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
                grantXpInTransaction(
                    grantId = "content-unlock:${offer.zoneId}",
                    profileId = sessionId,
                    amount = XpRewards.CONTENT_UNLOCKED,
                    source = XpSources.CONTENT_UNLOCKED,
                ).requireNoConflict()
                ZoneBuyResult.Bought
            }
            is FinancialOperationResult.Rejected -> when (debit.reason) {
                RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS -> {
                    if (useSavings) error("Savings transfer succeeded but room purchase debit was rejected")
                    ZoneBuyResult.NotEnoughMoney(
                        Math.toIntExact(offer.priceRub.toLong() - debit.state.availableRub),
                    )
                }
                else -> error("Economy rejected room purchase: ${debit.reason}")
            }
        }
    }

    private suspend fun grantMiniGameXp(
        completion: github.detrig.feature.gamestate.domain.model.PetPlayCompletion,
    ) {
        val amount = MiniGameXpPolicy.reward(
            completedNaturally = completion.completedNaturally,
            validActionCount = completion.validActionCount,
            activePlayMillis = completion.activePlayMillis,
        )
        if (amount == 0) return
        grantXpInTransaction(
            grantId = "mini-game:${completion.profileId}:${completion.sessionId}",
            profileId = completion.profileId,
            amount = amount,
            source = XpSources.MINI_GAME,
        ).requireNoConflict()
    }

    private suspend fun grantXpInTransaction(
        grantId: String,
        profileId: String,
        amount: Int,
        source: String,
    ): GrantXpResult {
        require(grantId.isNotBlank()) { "XP grant ID must not be blank" }
        require(profileId == GameStateEntity.CURRENT_STATE_ID) { "Unknown profile $profileId" }
        require(amount > 0) { "XP amount must be positive" }
        require(source.isNotBlank()) { "XP source must not be blank" }

        val current = checkNotNull(dao.getCurrentState()) { "Game state must be initialized" }
        val currentProgress = ProgressionRules.progress(current.totalXp)
        dao.getExperienceGrant(grantId)?.let { stored ->
            return if (stored.profileId == profileId && stored.amount == amount && stored.source == source) {
                GrantXpResult.AlreadyGranted(currentProgress)
            } else {
                GrantXpResult.OperationIdConflict(currentProgress)
            }
        }

        val inserted = dao.insertExperienceGrant(
            ExperienceGrantEntity(
                grantId = grantId,
                profileId = profileId,
                amount = amount,
                source = source,
                grantedAtMillis = currentTimeMillis(),
            ),
        )
        check(inserted != -1L) { "XP grant insertion raced inside a transaction" }
        val updated = ProgressionRules.progress(Math.addExact(current.totalXp, amount))
        check(dao.updateProgression(updated.totalXp, updated.level) == 1)
        return GrantXpResult.Granted(updated, amount)
    }

    private fun GrantXpResult.requireNoConflict() {
        check(this !is GrantXpResult.OperationIdConflict) { "Conflicting XP grant" }
    }

    private companion object {
        const val FEEDING_EFFECT_GAME_ID = "feeding"
    }
}
