package github.detrig.feature.gamestate.data.local

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.gamestate.domain.GameStateInitialConfig
import github.detrig.feature.gamestate.domain.model.ZoneOffer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.MiniGameAccess
import github.detrig.feature.gamestate.domain.model.MiniGameHappinessRewards
import github.detrig.feature.gamestate.domain.model.PetFeedingCompletion
import github.detrig.feature.gamestate.domain.model.PetFeedingResult
import github.detrig.feature.gamestate.domain.model.PetSatietyRules
import github.detrig.feature.gamestate.domain.model.PetHappinessRules
import github.detrig.feature.gamestate.domain.model.PetWishHappinessRewards
import github.detrig.feature.gamestate.domain.model.HungerAlertState
import github.detrig.feature.gamestate.domain.model.PetDirtAnchor
import github.detrig.feature.gamestate.domain.model.PetDirtRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
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
    private val dirtStorage: PetDirtStorage,
    private val currentWeekNumber: suspend () -> Long,
    private val currentAbsoluteDay: suspend () -> Long,
) {
    private val refreshVersion = MutableStateFlow(0L)

    suspend fun initialize(): GameState = transactionRunner.runInTransaction {
        val stored = dao.getCurrentStateWithZones()
        if (stored != null) {
            if (stored.state.hunger == 0 && stored.state.hungerAlertEpisode == 0L) {
                dao.seedZeroHungerAlertEpisode()
            }
            val now = currentTimeMillis()
            val plays = petPlayEffectDao.completedMiniGameCount()
            val anchor = dirtStorage.ensureInitialized(now, plays)
            return@runInTransaction checkNotNull(dao.getCurrentStateWithZones())
                .toDomain().withDirt(anchor, now, plays)
        }

        val now = currentTimeMillis()
        dao.insertInitialState(initialConfig.createState().toEntity(now))
        dirtStorage.reset(now, petPlayEffectDao.completedMiniGameCount())
        // Возвращаем сохранённую запись; конфликт вставки не перезаписывает прогресс.
        checkNotNull(dao.getCurrentStateWithZones()).toDomain()
    }.also { refreshVersion.update { it + 1 } }

    fun observeState(): Flow<GameState?> {
        val clock = flow {
            while (true) {
                emit(currentTimeMillis())
                delay(60_000L)
            }
        }
        return combine(dao.observeCurrentStateWithZones(), dirtStorage.anchor, clock, refreshVersion) {
            state, anchor, _, _ -> state to anchor
        }.map { (state, anchor) ->
            state?.toDomain()?.let { game ->
                if (anchor == null) game else game.withDirt(
                    anchor, currentTimeMillis(), petPlayEffectDao.completedMiniGameCount())
            }
        }
            .distinctUntilChanged()
    }

    private fun GameState.withDirt(anchor: PetDirtAnchor, now: Long, plays: Long): GameState =
        copy(pet = pet.copy(dirtStage = PetDirtRules.stage(anchor, now, plays)))

    suspend fun washPet(): Unit = transactionRunner.runInTransaction {
        initialize()
        val now = currentTimeMillis()
        dirtStorage.reset(now, petPlayEffectDao.completedMiniGameCount())
    }

    suspend fun adjustPetDirtStageForDebug(delta: Int): Int = transactionRunner.runInTransaction {
        require(delta == -1 || delta == 1)
        val currentStage = initialize().pet.dirtStage
        val targetStage = (currentStage + delta).coerceIn(0, 3)
        if (targetStage != currentStage) {
            val now = currentTimeMillis()
            dirtStorage.setAnchor(PetDirtRules.anchorAtStage(
                targetStage, now, petPlayEffectDao.completedMiniGameCount()))
        }
        targetStage
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
            check(dao.increaseHappiness(delta) == 1)
            petPlayEffectDao.insert(PetPlayEffectEntity(operationId, completion.profileId, completion.sessionId,
                completion.gameId, delta, currentTimeMillis()))
            if (github.detrig.feature.gamestate.domain.model.PetPlayReward.delta(0, completion) > 0) {
                petPlayEffectDao.insert(PetPlayEffectEntity(
                    operationId = "pet-wish-play:$operationId",
                    profileId = completion.profileId,
                    sessionId = completion.sessionId,
                    gameId = "pet-wish-play:${completion.gameId}",
                    happinessDelta = 0,
                    appliedAtMillis = currentTimeMillis(),
                ))
            }
            grantMiniGameXp(completion)
            delta
        }

    suspend fun petWishActivities() = petPlayEffectDao.wishActivities().map { effect ->
        github.detrig.feature.gamestate.domain.model.PetWishActivity(
            id = effect.operationId,
            source = effect.gameId,
            sourceOperationId = effect.sessionId,
            appliedAtMillis = effect.appliedAtMillis,
        )
    }

    suspend fun rewardMiniGameLaunch(gameId: String, absoluteDay: Long): Int =
        transactionRunner.runInTransaction {
            require(absoluteDay >= 1)
            val points = MiniGameHappinessRewards.firstLaunchPoints(gameId)
            require(points > 0) { "No first-launch reward for $gameId" }
            val current = initialize()
            require(MiniGameAccess.isOpen(gameId, current.ownedZoneIds)) { "Game is not unlocked" }
            applyHappinessRewardOnce(
                current = current,
                operationId = "mini-game-launch:$gameId:$absoluteDay",
                sourceId = "mini-game-launch:$gameId",
                sourceOperationId = absoluteDay.toString(),
                points = points,
            )
        }

    suspend fun rewardClothingPurchase(purchaseOperationId: String, happinessPoints: Int): Int =
        transactionRunner.runInTransaction {
            require(purchaseOperationId.startsWith("clothing_purchase:") &&
                purchaseOperationId.length > "clothing_purchase:".length)
            require(happinessPoints in 12..25)
            applyHappinessRewardOnce(
                current = initialize(),
                operationId = "clothing-happiness:$purchaseOperationId",
                sourceId = "clothing-purchase",
                sourceOperationId = purchaseOperationId,
                points = happinessPoints,
            )
        }

    suspend fun rewardPetWishHappiness(wishRewardId: String, happinessPoints: Int): Int =
        transactionRunner.runInTransaction {
            require(wishRewardId.isNotBlank())
            require(happinessPoints in 5..20)
            applyHappinessRewardOnce(
                current = initialize(),
                operationId = "pet-wish-happiness:$wishRewardId",
                sourceId = "pet-wish-happiness",
                sourceOperationId = wishRewardId,
                points = happinessPoints,
            )
        }

    suspend fun activateSavingsHappinessProtection(
        contributionOperationId: String,
        throughAbsoluteDay: Long,
    ) = transactionRunner.runInTransaction {
        require(contributionOperationId.isNotBlank())
        require(throughAbsoluteDay >= 1L)
        val operationId = "savings-happiness-protection:$contributionOperationId"
        petPlayEffectDao.find(operationId)?.let { existing ->
            require(existing.gameId == SAVINGS_PROTECTION_GAME_ID && existing.sessionId.toLongOrNull() != null) {
                "Savings protection operation ID conflict"
            }
            return@runInTransaction
        }
        initialize()
        petPlayEffectDao.insert(PetPlayEffectEntity(
            operationId = operationId,
            profileId = GameStateEntity.CURRENT_STATE_ID,
            sessionId = throughAbsoluteDay.toString(),
            gameId = SAVINGS_PROTECTION_GAME_ID,
            happinessDelta = 0,
            appliedAtMillis = currentTimeMillis(),
        ))
    }

    private suspend fun applyHappinessRewardOnce(
        current: GameState,
        operationId: String,
        sourceId: String,
        sourceOperationId: String,
        points: Int,
    ): Int {
        petPlayEffectDao.find(operationId)?.let { existing ->
            require(existing.gameId == sourceId && existing.sessionId == sourceOperationId) {
                "Happiness operation ID conflict"
            }
            return 0
        }
        val delta = minOf(points, (100 - current.pet.happiness).coerceAtLeast(0))
        check(dao.increaseHappiness(delta) == 1)
        petPlayEffectDao.insert(PetPlayEffectEntity(
            operationId = operationId,
            profileId = GameStateEntity.CURRENT_STATE_ID,
            sessionId = sourceOperationId,
            gameId = sourceId,
            happinessDelta = delta,
            appliedAtMillis = currentTimeMillis(),
        ))
        return delta
    }

    suspend fun applyDayNeeds(): Unit = transactionRunner.runInTransaction {
        val current = initialize()
        check(PetSatietyRules.canSleep(current.pet.hunger)) {
            "Pet needs at least ${PetSatietyRules.SLEEP_COST} satiety to end the day"
        }
        val absoluteDay = currentAbsoluteDay()
        val protectionThrough = petPlayEffectDao.latestSessionForGame(SAVINGS_PROTECTION_GAME_ID)
            ?.toLongOrNull() ?: 0L
        val happinessCost = if (absoluteDay <= protectionThrough) {
            PetHappinessRules.SAVINGS_PROTECTED_SLEEP_COST
        } else {
            PetHappinessRules.SLEEP_COST
        }
        check(dao.decreaseNeedsForDay(PetSatietyRules.SLEEP_COST, happinessCost) == 1)
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
            check(dao.increaseHunger(hungerDelta) == 1)
            check(dao.increaseHappiness(happinessDelta) == 1)
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
                if (offer.zoneId in SAVINGS_GAME_ZONE_IDS &&
                    economyApi.getActiveGoal()?.id == "room-zone:${offer.zoneId}"
                ) {
                    applyHappinessRewardOnce(
                        current = current,
                        operationId = "pet-wish-game-unlocked:${offer.zoneId}",
                        sourceId = "pet-wish-game-unlocked",
                        sourceOperationId = offer.zoneId,
                        points = PetWishHappinessRewards.UNLOCKED_GAME,
                    )
                }
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
        val grantId = "mini-game:${completion.profileId}:${completion.sessionId}"
        dao.getExperienceGrant(grantId)?.let { existing ->
            check(existing.profileId == completion.profileId &&
                (existing.source == XpSources.MINI_GAME ||
                    existing.source.startsWith("${XpSources.MINI_GAME}:week:"))) {
                "Conflicting mini-game XP grant"
            }
            return
        }
        val weekNumber = currentWeekNumber()
        require(weekNumber >= 1)
        val source = "${XpSources.MINI_GAME}:week:$weekNumber"
        val earnedThisWeek = dao.totalExperienceForSource(completion.profileId, source)
        val grantedAmount = MiniGameXpPolicy.rewardWithinWeek(earnedThisWeek)
        if (grantedAmount > 0) {
            grantXpInTransaction(grantId, completion.profileId, grantedAmount, source).requireNoConflict()
        } else {
            // A zero-value ledger entry keeps a capped session from earning XP on replay next week.
            check(dao.insertExperienceGrant(ExperienceGrantEntity(
                grantId = grantId,
                profileId = completion.profileId,
                amount = 0,
                source = source,
                grantedAtMillis = currentTimeMillis(),
            )) != -1L)
        }
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
        const val SAVINGS_PROTECTION_GAME_ID = "savings-happiness-protection"
        val SAVINGS_GAME_ZONE_IDS = setOf("fishing", "drawing", "music")
    }
}
