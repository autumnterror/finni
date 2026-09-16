package github.detrig.feature.gamestate.data.local

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.gamestate.domain.GameStateInitialConfig
import github.detrig.feature.gamestate.domain.model.ZoneOffer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.MiniGameAccess
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
) {

    suspend fun initialize(): GameState = transactionRunner.runInTransaction {
        val stored = dao.getCurrentStateWithZones()
        if (stored != null) return@runInTransaction stored.toDomain()

        dao.insertInitialState(initialConfig.createState().toEntity())
        // Возвращаем сохранённую запись; конфликт вставки не перезаписывает прогресс.
        checkNotNull(dao.getCurrentStateWithZones()).toDomain()
    }

    fun observeState(): Flow<GameState?> {
        return dao.observeCurrentStateWithZones()
            .map { it?.toDomain() }
            .distinctUntilChanged()
    }

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
            check(dao.increaseHappiness(delta) == 1)
            petPlayEffectDao.insert(PetPlayEffectEntity(operationId, completion.profileId, completion.sessionId,
                completion.gameId, delta, currentTimeMillis()))
            delta
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
}
