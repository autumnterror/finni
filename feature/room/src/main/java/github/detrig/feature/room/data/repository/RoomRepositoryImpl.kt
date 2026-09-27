package github.detrig.feature.room.data.repository

import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.gamestate.domain.model.PetSatietyRules
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
import github.detrig.feature.planning.domain.PlanWeekContext
import github.detrig.feature.planning.domain.WeeklyPlan
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
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.savings.api.SavingsApi
import github.detrig.feature.savings.api.SavingsTransferResult
import github.detrig.feature.room.domain.model.RoomMoneyEvent
import github.detrig.feature.room.data.local.RoomMoneyEventStorage
import github.detrig.feature.room.domain.model.MoneyAllocation
import github.detrig.feature.room.domain.model.MoneyEventResolution

internal class RoomRepositoryImpl(
    private val catalog: RoomZoneCatalog,
    private val gameStateApi: GameStateApi,
    private val economyApi: EconomyApi,
    private val weekApi: WeekApi,
    private val planningApi: PlanningApi,
    private val savingsApi: SavingsApi,
    private val minimumProductPriceRub: Long,
    private val inventoryApi: InventoryApi? = null,
    private val moneyEventStorage: RoomMoneyEventStorage,
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
            val extraIncome = if (plan == null) emptyList() else economyApi.getIncomeHistory()
                .filter { it.context.metadata?.contains("source=money-event;week=${week.weekNumber};") == true }
            game.toRoomProgress(economy, week, plan?.copy(
                extraIncomeRub = extraIncome.sumOf { it.amountRub },
                extraWantsRub = extraIncome.filter { it.context.metadata?.contains("allocation=wants") == true }
                    .sumOf { it.amountRub },
                extraSavingsRub = extraIncome.filter { it.context.metadata?.contains("allocation=goal") == true }
                    .sumOf { it.amountRub },
                extraReserveRub = extraIncome.filter { it.context.metadata?.contains("allocation=reserve") == true }
                    .sumOf { it.amountRub },
            ))
        }
    }.distinctUntilChanged()

    override suspend fun buyZone(zone: RoomZoneDefinition, useSavings: Boolean): ZoneBuyResult {
        val result = gameStateApi.buyZone(ZoneOffer(zone.id, zone.priceRub, zone.requiredLevel), useSavings)
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

    override suspend fun pendingMoneyEvent(): RoomMoneyEvent? {
        val week = weekApi.observeState().first()
        if (planningApi.getPlanProgress(week.weekNumber) == null) return null
        val operations = economyApi.getHistory().associateBy { it.id }
        val hasActiveGoal = savingsApi.getActiveGoalProgress() != null
        // The economy operation is the durable outbox if the app was stopped before
        // the planning projection was written.
        val weekEvents = moneyEventStorage.events(week.weekNumber)
        weekEvents.forEach { event ->
            if (operations[event.id] != null && event.kind != RoomMoneyEvent.Kind.EXTRA_INCOME) {
                planningApi.recordActual(if (event.kind == RoomMoneyEvent.Kind.KNOWN_EXPENSE) {
                    PlanActualOperation.Payment(event.id, event.weekNumber, event.amountRub,
                        PaymentClassification.MANDATORY)
                } else {
                    PlanActualOperation.UnexpectedMandatoryExpense(
                        event.id, event.weekNumber, event.amountRub)
                })
            }
        }
        return weekEvents
            .sortedBy { it.dayOfWeek }
            .firstOrNull { event ->
                event.dayOfWeek <= week.dayOfWeek && (operations[event.id] == null ||
                    event.kind == RoomMoneyEvent.Kind.EXTRA_INCOME &&
                    operations[event.id]?.context?.metadata?.contains("allocation=goal") == true &&
                    operations["${event.id}:goal"] == null && hasActiveGoal)
            }?.let { event ->
                event.copy(savedAllocation = if (operations[event.id]?.context?.metadata
                        ?.contains("allocation=goal") == true) MoneyAllocation.GOAL else null)
            }
    }

    override suspend fun resolveMoneyEvent(
        eventId: String,
        allocation: MoneyAllocation?,
    ): MoneyEventResolution {
        val event = pendingMoneyEvent()?.takeIf { it.id == eventId }
            ?: return MoneyEventResolution.Completed
        if (event.kind == RoomMoneyEvent.Kind.EXTRA_INCOME) {
            val chosen = event.savedAllocation ?: allocation ?: MoneyAllocation.FREE
            if (chosen == MoneyAllocation.GOAL && savingsApi.getActiveGoalProgress() == null) {
                return MoneyEventResolution.NoActiveGoal
            }
            val income = economyApi.credit(
                event.id, event.amountRub,
                OperationContext(reasonId = event.id,
                    metadata = "source=money-event;week=${event.weekNumber};allocation=${chosen.name.lowercase()}"),
            )
            check(income !is FinancialOperationResult.Rejected) { "Money event income rejected" }
            if (chosen == MoneyAllocation.GOAL) {
                val transfer = savingsApi.transferToActiveGoal("${event.id}:goal", event.amountRub)
                if (transfer is SavingsTransferResult.NoActiveGoal) return MoneyEventResolution.NoActiveGoal
                if (transfer is SavingsTransferResult.Completed &&
                    transfer.financial is FinancialOperationResult.Rejected) {
                    return MoneyEventResolution.InsufficientFunds
                }
            }
        } else {
            val payment = economyApi.debit(
                event.id, event.amountRub,
                OperationContext(reasonId = event.id,
                    metadata = "source=money-event;week=${event.weekNumber};kind=${event.kind.name.lowercase()}"),
            )
            if (payment is FinancialOperationResult.Rejected) {
                if (payment.reason == RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS) {
                    return MoneyEventResolution.InsufficientFunds
                }
                error("Money event expense rejected: ${payment.reason}")
            }
            val actual = if (event.kind == RoomMoneyEvent.Kind.KNOWN_EXPENSE) {
                PlanActualOperation.Payment(event.id, event.weekNumber, event.amountRub,
                    PaymentClassification.MANDATORY)
            } else {
                PlanActualOperation.UnexpectedMandatoryExpense(
                    event.id, event.weekNumber, event.amountRub)
            }
            planningApi.recordActual(actual)
        }
        return MoneyEventResolution.Completed
    }

    override suspend fun coverMoneyEventWithParents(eventId: String): MoneyEventResolution {
        val event = pendingMoneyEvent()?.takeIf { it.id == eventId }
            ?: return MoneyEventResolution.Completed
        require(event.kind != RoomMoneyEvent.Kind.EXTRA_INCOME)
        val state = economyApi.getState()
        if (state.savingsRub > 0 || state.debtRub == 0L || state.availableRub >= event.amountRub) {
            return MoneyEventResolution.InsufficientFunds
        }
        val shortfall = event.amountRub - state.availableRub
        val help = economyApi.credit(
            "${event.id}:parent-coverage", shortfall,
            OperationContext(reasonId = event.id,
                metadata = "source=parents;week=${event.weekNumber};mandatory-event-shortfall"),
        )
        check(help !is FinancialOperationResult.Rejected)
        return resolveMoneyEvent(event.id)
    }

    override fun assessPlan(plan: WeeklyPlan): PlanAssessment = planningApi.assessPlan(plan)

    override suspend fun savePlan(
        weekNumber: Long,
        availableRub: Long,
        percentages: PlanPercentages,
        context: PlanWeekContext,
    ): SavePlanResult = planningApi.savePlan(weekNumber, availableRub, percentages, context)

    override fun parentHelpOffers(): List<ParentHelpOffer> = economyApi.parentHelpOffers()

    override suspend fun parentHelp(): ParentHelpState? = economyApi.getParentHelp()

    override suspend fun requestParentHelp(offerId: String): ParentHelpRequestResult {
        val economy = economyApi.getState()
        val activeHelp = economyApi.getParentHelp()
        val requiredForEvent = pendingMoneyEvent()
            ?.takeIf { it.kind != RoomMoneyEvent.Kind.EXTRA_INCOME }
            ?.amountRub ?: 0L
        val requiredBalance = maxOf(minimumProductPriceRub, requiredForEvent)
        val hasFoodInFridge = requiredForEvent == 0L &&
            inventoryApi?.observeStock()?.first()?.any { it.quantity > 0 } == true
        val canSleepUntilAllowance = requiredForEvent == 0L &&
            gameStateApi.observeState().first()?.let { game ->
                PetSatietyRules.canSleep(game.pet.hunger)
            } == true
        if (activeHelp == null && !canOfferParentHelp(
                availableRub = economy.availableRub,
                savingsRub = economy.savingsRub,
                debtRub = economy.debtRub,
                hasActiveParentHelp = false,
                minimumRequiredBalanceRub = requiredBalance,
                hasFoodInFridge = hasFoodInFridge,
                canSleepUntilAllowance = canSleepUntilAllowance,
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
