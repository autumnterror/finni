package github.detrig.feature.room.data.repository

import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.ZoneOffer
import github.detrig.feature.room.data.catalog.RoomZoneCatalog
import github.detrig.feature.room.data.mapper.toRoomProgress
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.RoomZoneDefinition
import github.detrig.feature.room.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.domain.PlanPercentages
import github.detrig.feature.planning.domain.PlanAssessment
import github.detrig.feature.planning.domain.SavePlanResult
import github.detrig.feature.planning.domain.PaymentClassification
import github.detrig.feature.planning.domain.PlanActualOperation
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.economy.domain.ParentHelpState
import kotlinx.coroutines.flow.first
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.economy.domain.canOfferParentHelp
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.gamestate.domain.model.MiniGameAccess
import github.detrig.feature.savings.api.SavingsGoalPurchaseResult

internal class RoomRepositoryImpl(
    private val catalog: RoomZoneCatalog,
    private val gameStateApi: GameStateApi,
    private val economyApi: EconomyApi,
    private val weekApi: WeekApi,
    private val planningApi: PlanningApi,
    private val minimumProductPriceRub: Long,
) : RoomRepository {
    override fun zones(): List<RoomZoneDefinition> = catalog.zones

    override suspend fun initialize() {
        gameStateApi.initialize()
        economyApi.initialize()
        weekApi.initialize()
    }

    override fun observeProgress(): Flow<RoomProgress> = combine(
        gameStateApi.observeState().filterNotNull(),
        economyApi.observeState(),
        weekApi.observeState(),
    ) { game, economy, week -> Triple(game, economy, week) }.flatMapLatest { (game, economy, week) ->
        planningApi.observePlanProgress(week.weekNumber).map { plan ->
            game.toRoomProgress(economy, week, plan)
        }
    }.distinctUntilChanged()

    override suspend fun buyZone(zone: RoomZoneDefinition): ZoneBuyResult {
        val result = gameStateApi.buyZone(ZoneOffer(zone.id, zone.priceRub, zone.requiredLevel))
        if (result == ZoneBuyResult.Bought) {
            val week = weekApi.observeState().first()
            if (planningApi.getPlanProgress(week.weekNumber) != null) {
                planningApi.recordActual(
                    PlanActualOperation.Payment(
                        operationId = "room-zone:${zone.id}",
                        weekNumber = week.weekNumber,
                        amountRub = zone.priceRub.toLong(),
                        classification = PaymentClassification.OPTIONAL,
                    ),
                )
            }
        }
        return result
    }

    override suspend fun endDay(expectedAbsoluteDay: Long): EndDayResult = weekApi.endDay(expectedAbsoluteDay)

    override fun assessPlan(percentages: PlanPercentages): PlanAssessment = planningApi.assessPlan(percentages)

    override suspend fun savePlan(
        weekNumber: Long,
        availableRub: Long,
        percentages: PlanPercentages,
    ): SavePlanResult = planningApi.savePlan(weekNumber, availableRub, percentages)

    override fun parentHelpOffers(): List<ParentHelpOffer> = economyApi.parentHelpOffers()

    override suspend fun parentHelp(): ParentHelpState? = economyApi.getParentHelp()

    override suspend fun requestParentHelp(offerId: String): ParentHelpRequestResult {
        val economy = economyApi.getState()
        val activeHelp = economyApi.getParentHelp()
        if (activeHelp == null && !canOfferParentHelp(
                availableRub = economy.availableRub,
                savingsRub = economy.savingsRub,
                debtRub = economy.debtRub,
                hasActiveParentHelp = false,
                minimumRequiredBalanceRub = minimumProductPriceRub,
            )
        ) return ParentHelpRequestResult.Rejected(RejectionReason.PARENT_HELP_NOT_AVAILABLE, economy)
        val week = weekApi.observeState().first()
        return economyApi.requestParentHelp(
            operationId = "parent-help:${week.weekNumber}:$offerId",
            offerId = offerId,
        )
    }

    override suspend fun endWeekEarlyWithParentHelp(
        expectedAbsoluteDay: Long,
        minimumProductPriceRub: Long,
    ) = weekApi.endWeekEarlyWithParentHelp(expectedAbsoluteDay, minimumProductPriceRub)

    override suspend fun buySavingsGoal(goal: SavingsGoal): SavingsGoalPurchaseResult {
        val zoneId = goal.metadata.metadataValue("zoneId")
            ?: return SavingsGoalPurchaseResult.UnsupportedGoal
        if (goal.metadata.metadataValue("source") != "room-zone") {
            return SavingsGoalPurchaseResult.UnsupportedGoal
        }
        val zone = catalog.zones.firstOrNull { it.id == zoneId }
            ?: return SavingsGoalPurchaseResult.UnsupportedGoal
        val game = gameStateApi.initialize()
        if (MiniGameAccess.isOpen(zone.id, game.ownedZoneIds)) {
            return SavingsGoalPurchaseResult.AlreadyPurchased
        }
        if (game.playerLevel < zone.requiredLevel) {
            return SavingsGoalPurchaseResult.LevelTooLow(zone.requiredLevel)
        }
        val operationId = "savings-goal-purchase:${goal.id}:withdraw"
        val withdrawal = economyApi.transferFromSavings(
            operationId = operationId,
            amountRub = zone.priceRub.toLong(),
            context = OperationContext(
                reasonId = goal.id,
                metadata = "source=savings-goal-purchase;zoneId=${zone.id}",
            ),
        )
        when (withdrawal) {
            is FinancialOperationResult.Applied,
            is FinancialOperationResult.AlreadyApplied,
            -> Unit
            is FinancialOperationResult.Rejected -> {
                if (withdrawal.reason == RejectionReason.INSUFFICIENT_SAVINGS) {
                    return SavingsGoalPurchaseResult.NotEnoughSavings(
                        (zone.priceRub - withdrawal.state.savingsRub).coerceAtLeast(0),
                    )
                }
                error("Savings withdrawal rejected: ${withdrawal.reason}")
            }
        }
        return when (val purchase = buyZone(zone)) {
            ZoneBuyResult.Bought -> SavingsGoalPurchaseResult.Purchased
            ZoneBuyResult.AlreadyOwned -> SavingsGoalPurchaseResult.AlreadyPurchased
            is ZoneBuyResult.LevelTooLow -> SavingsGoalPurchaseResult.LevelTooLow(purchase.requiredLevel)
            is ZoneBuyResult.NotEnoughMoney -> error("Withdrawn goal funds were not available for purchase")
        }
    }
}

private fun String?.metadataValue(key: String): String? = this
    ?.split(';')
    ?.firstOrNull { it.startsWith("$key=") }
    ?.substringAfter('=')
