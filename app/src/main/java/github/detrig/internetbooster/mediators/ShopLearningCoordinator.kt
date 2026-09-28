package github.detrig.internetbooster.mediators

import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperation
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.learning.domain.PurchaseAssessmentConfig
import github.detrig.feature.learning.domain.PurchaseAssessment
import github.detrig.feature.learning.domain.PurchaseDecision
import github.detrig.feature.learning.domain.PurchaseDecisionContext
import github.detrig.feature.learning.domain.PurchaseLearning
import github.detrig.feature.learning.domain.PurchaseOutcome
import github.detrig.feature.learning.domain.PurchaseProblem
import github.detrig.feature.learning.domain.PurchaseScenario
import github.detrig.feature.learning.domain.RecordLearningResult
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.domain.PaymentClassification
import github.detrig.feature.planning.domain.PlanActualOperation
import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.feature.planning.domain.PlanAdjustmentReason
import github.detrig.feature.planning.domain.PlanAssessment
import github.detrig.feature.planning.domain.RecordActualResult
import github.detrig.feature.shop.api.ShopCheckoutRequest
import github.detrig.feature.shop.domain.ShopCatalogRegistry
import github.detrig.feature.shop.domain.ShopDecisionEvent
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.feature.shop.domain.priceLine
import github.detrig.feature.week.api.WeekApi
import github.detrig.products.FoodItem
import github.detrig.products.GroceryStoreIds
import github.detrig.products.ProductId
import kotlinx.coroutines.flow.first

internal data class ShopPurchaseTrackingConfig(
    val minimumFoodStockUnits: Int = 1,
    val fallbackMandatoryReserveRub: Long = 60,
    val assessment: PurchaseAssessmentConfig = PurchaseAssessmentConfig(),
) {
    init {
        require(minimumFoodStockUnits >= 0)
        require(fallbackMandatoryReserveRub >= 0)
    }

    fun isFoodPurchaseRequired(stockFoodUnits: Int): Boolean {
        require(stockFoodUnits >= 0)
        return stockFoodUnits < minimumFoodStockUnits
    }
}

internal data class PreparedShopCheckout(
    val lineTotalOverrides: Map<ProductId, Long>,
    val learningMetadata: String,
    val assessment: PurchaseAssessment,
    val totalRub: Long,
    val suppressPlanWarning: Boolean,
)

/** Coordinates source-of-truth game facts with the central Learning rules. */
internal class ShopLearningCoordinator(
    private val economyApi: EconomyApi,
    private val weekApi: WeekApi,
    private val planningApi: PlanningApi,
    private val inventoryApi: InventoryApi,
    private val learningApi: LearningApi,
    private val catalogRegistry: ShopCatalogRegistry,
    private val config: ShopPurchaseTrackingConfig = ShopPurchaseTrackingConfig(),
    private val petNeeds: suspend () -> Pair<Int, Int> = { 70 to 70 },
) {
    suspend fun prepareCheckout(request: ShopCheckoutRequest): PreparedShopCheckout {
        val event = request.decisionEvent?.takeIf { it.storeId == request.storeId }
        val overrides = event.promotionLineTotalOverrides(request.lines)
        val payload = buildPayload(
            sourceOperationId = request.operationId,
            storeId = request.storeId,
            lines = request.lines.associate { it.itemId to it.quantity },
            event = event,
            declinedWithoutCheckout = false,
            lineTotalOverrides = overrides,
        )
        val assessment = payload.standardPurchase.assess(config.assessment)
        val purchase = payload.standardPurchase
        val planAssessment = planningApi.getPlanProgress(payload.gamePeriod)?.planAssessment
        val mandatoryPlanTooLow = (planAssessment as? PlanAssessment.NeedsChanges)
            ?.reason == PlanAdjustmentReason.MANDATORY_TOO_LOW
        return PreparedShopCheckout(
            lineTotalOverrides = overrides,
            learningMetadata = ShopPurchaseLearningPayloadCodec.encode(payload),
            assessment = assessment,
            totalRub = payload.standardPurchase.purchaseTotalRub,
            suppressPlanWarning = mandatoryPlanTooLow &&
                assessment.outcome == PurchaseOutcome.PLAN_ADJUSTMENT &&
                purchase.mandatoryPurchaseRub > purchase.mandatoryCategoryRemainingRub &&
                purchase.optionalPurchaseRub <= purchase.optionalCategoryRemainingRub,
        )
    }

    suspend fun recordCompletedPurchase(metadata: String?) {
        val payload = metadata.learningPayloadOrNull() ?: return
        record(payload)
    }

    suspend fun recordDeclinedEvent(event: ShopDecisionEvent) {
        val payload = buildPayload(
            sourceOperationId = event.eventId,
            storeId = event.storeId,
            lines = emptyMap(),
            event = event,
            declinedWithoutCheckout = true,
            lineTotalOverrides = emptyMap(),
        )
        payload.eventDecision?.let { context ->
            PurchaseLearning.qualifyingActions(
                profileId = CURRENT_PROFILE_ID,
                gamePeriod = payload.gamePeriod,
                sourceOperationId = payload.sourceOperationId,
                context = context,
                config = config.assessment,
            ).forEach { action -> recordAction(action) }
        }
    }

    suspend fun recordDeclinedClothingWish(eventId: String, priceRub: Long) {
        if (priceRub <= 0) return
        val week = weekApi.initialize()
        val economy = economyApi.getState()
        val plan = planningApi.getPlanProgress(week.weekNumber)
        val mandatoryRemaining = plan?.category(PlanCategory.MANDATORY)?.let {
            (it.plannedRub - it.actualRub).coerceAtLeast(0)
        } ?: config.fallbackMandatoryReserveRub
        val wantsRemaining = plan?.category(PlanCategory.WANTS)?.let {
            (it.plannedRub - it.actualRub).coerceAtLeast(0)
        } ?: (economy.availableRub - mandatoryRemaining).coerceAtLeast(0)
        val reserveRemaining = ((plan?.plan?.reserveRub ?: 0L) -
            (plan?.unexpectedMandatoryRub ?: 0L)).coerceAtLeast(0)
        val savingsRemaining = plan?.category(PlanCategory.SAVINGS)?.let {
            (it.plannedRub - it.actualRub).coerceAtLeast(0)
        } ?: 0L
        val context = PurchaseDecisionContext(
            scenario = PurchaseScenario.IMPULSE_WISH,
            decision = PurchaseDecision.DECLINED,
            balanceBeforeRub = economy.availableRub,
            balanceAfterPurchaseRub = economy.availableRub,
            purchaseTotalRub = 0,
            futureMandatoryRub = mandatoryRemaining,
            discretionaryRub = wantsRemaining,
            mandatoryFoodNeeded = false,
            foodUnitsPurchased = 0,
            requiredFoodCostRub = 0,
            extraUnitsPurchased = 0,
            optionalPurchaseRub = 0,
            eventTargetPriceRub = priceRub,
            eventTargetUseful = true,
            optionalCategoryRemainingRub = wantsRemaining,
            mandatoryCategoryRemainingRub = mandatoryRemaining,
            reserveRemainingRub = reserveRemaining,
            savingsPlanRemainingRub = savingsRemaining,
        )
        PurchaseLearning.qualifyingActions(
            profileId = CURRENT_PROFILE_ID,
            gamePeriod = week.weekNumber,
            sourceOperationId = eventId,
            context = context,
            config = config.assessment,
        ).forEach { recordAction(it) }
    }

    suspend fun reconcile(history: List<FinancialOperation>) {
        history.asSequence()
            .filter { it.context.reasonId?.startsWith(SHOP_REASON_PREFIX) == true }
            .mapNotNull { it.context.metadata.learningPayloadOrNull() }
            .forEach { record(it) }
    }

    private suspend fun buildPayload(
        sourceOperationId: String,
        storeId: github.detrig.products.StoreId,
        lines: Map<ProductId, Int>,
        event: ShopDecisionEvent?,
        declinedWithoutCheckout: Boolean,
        lineTotalOverrides: Map<ProductId, Long>,
    ): ShopPurchaseLearningPayload {
        val catalog = requireNotNull(catalogRegistry.catalog(storeId))
        val week = weekApi.initialize()
        val economy = economyApi.getState()
        val stock = inventoryApi.observeStock().first()
        val plan = planningApi.getPlanProgress(week.weekNumber)
        val moneyEventIncome = economyApi.getIncomeHistory()
            .filter { it.context.metadata?.contains("source=money-event;week=${week.weekNumber};") == true }
        val addedWantsRub = moneyEventIncome
            .filter { it.context.metadata?.contains("allocation=wants") == true }
            .sumOf { it.amountRub }
        val addedReserveRub = moneyEventIncome
            .filter { it.context.metadata?.contains("allocation=reserve") == true }
            .sumOf { it.amountRub }
        val paidKnownExpenseRub = economyApi.getExpenseHistory()
            .filter { it.context.metadata?.contains("source=money-event;week=${week.weekNumber};kind=known_expense") == true }
            .sumOf { it.amountRub }
        val (petHunger, petHappiness) = petNeeds()
        val itemsById = catalog.storefront.items.associateBy { it.id }
        val foodCatalog = catalogRegistry.catalog(GroceryStoreIds.Store)?.storefront?.items
            ?.filterIsInstance<FoodItem>().orEmpty()
        val foodItemsById = foodCatalog.associateBy { it.id }
        val stockFoodUnits = stock.sumOf { stockItem ->
            if ((foodItemsById[stockItem.productId]?.effects?.satietyPercent ?: 0) > 0) {
                stockItem.quantity
            } else 0
        }
        val mandatoryFoodNeeded = config.isFoodPurchaseRequired(stockFoodUnits)
        val pricedLines = lines.map { (productId, quantity) ->
            val item = requireNotNull(itemsById[productId])
            val lineTotal = lineTotalOverrides[productId]
                ?: Math.multiplyExact(item.priceRub, quantity.toLong())
            val singleUnitPrice = event.priceLine(item.id, item.priceRub, 1).chargedTotalRub
            PricedLine(item, quantity, singleUnitPrice, lineTotal)
        }
        val purchaseTotal = pricedLines.sumOf(PricedLine::lineTotalRub)
        val foodLines = pricedLines.filter {
            ((it.item as? FoodItem)?.effects?.satietyPercent ?: 0) > 0
        }
        val stockedSatiety = stock.sumOf { stockItem ->
            (foodItemsById[stockItem.productId]?.effects?.satietyPercent ?: 0).toLong() *
                stockItem.quantity
        }
        val foodUnits = foodLines.sumOf(PricedLine::quantity)
        val purchasedSatiety = foodLines.sumOf { line ->
            ((line.item as FoodItem).effects.satietyPercent).toLong() * line.quantity
        }
        val requiredFoodCost = if (mandatoryFoodNeeded && foodUnits > 0) {
            foodLines.minOf(PricedLine::unitPriceRub)
        } else {
            0L
        }
        val totalUnits = pricedLines.sumOf(PricedLine::quantity)
        val extraUnits = (totalUnits - if (requiredFoodCost > 0) 1 else 0).coerceAtLeast(0)
        val planActuals = classifyShopPlanActuals(
            lines = pricedLines.map { line ->
                ShopPlanLine(
                    restoresSatiety = ((line.item as? FoodItem)?.effects?.satietyPercent ?: 0) > 0,
                    totalRub = line.lineTotalRub,
                )
            },
        )
        val optionalCost = planActuals.wantsRub
        val mandatoryCategoryRemaining = plan?.category(PlanCategory.MANDATORY)?.let {
            (it.plannedRub - it.actualRub).coerceAtLeast(0)
        } ?: config.fallbackMandatoryReserveRub.coerceAtMost(economy.availableRub)
        val satietyFood = foodCatalog
            .filter { it.effects.satietyPercent > 0 }
        fun remainingFoodCost(stockedPoints: Long): Long {
            val neededPoints = (week.daysUntilAllowance * 50L - petHunger + 1L - stockedPoints)
                .coerceAtLeast(0L)
            if (neededPoints == 0L) return 0L
            return satietyFood.minOfOrNull { food ->
                val portions = (neededPoints + food.effects.satietyPercent - 1) /
                    food.effects.satietyPercent
                portions * food.priceRub
            } ?: config.fallbackMandatoryReserveRub
        }
        val knownMandatory = ((plan?.plan?.context?.knownMandatoryExpenseRub ?: 0L) - paidKnownExpenseRub)
            .coerceAtLeast(0L)
        val futureMandatory = maxOf(
            mandatoryCategoryRemaining,
            remainingFoodCost(stockedSatiety) + knownMandatory,
        )
        val futureMandatoryAfterPurchase = maxOf(
            (mandatoryCategoryRemaining - planActuals.mandatoryRub).coerceAtLeast(0L),
            remainingFoodCost(stockedSatiety + purchasedSatiety) + knownMandatory,
        )
        val optionalCategoryRemaining = plan?.let {
            val wants = it.category(PlanCategory.WANTS)
            (wants.plannedRub + addedWantsRub - wants.actualRub).coerceAtLeast(0)
        } ?: (economy.availableRub - futureMandatory).coerceAtLeast(0)
        val reserveRemaining = ((plan?.plan?.reserveRub ?: 0L) + addedReserveRub -
            (plan?.unexpectedMandatoryRub ?: 0L)).coerceAtLeast(0L)
        val savingsPlanRemaining = plan?.category(PlanCategory.SAVINGS)?.let {
            (it.plannedRub - it.actualRub).coerceAtLeast(0)
        } ?: 0L
        val balanceAfter = (economy.availableRub - purchaseTotal).coerceAtLeast(0)
        val eventTargetQuantity = event?.let { lines[it.productId] ?: 0 } ?: 0
        val eventTargetMinimumQuantity = event
            ?.takeIf { it.type == ShopDecisionEventType.PROMOTION }
            ?.minimumPromotionQuantity
            ?: 0
        val promotionLinePrice = event
            ?.takeIf { it.type == ShopDecisionEventType.PROMOTION && eventTargetQuantity > 0 }
            ?.priceLine(event.productId, event.regularPriceRub, eventTargetQuantity)
        val standardContext = PurchaseDecisionContext(
            scenario = PurchaseScenario.STANDARD_PURCHASE,
            decision = PurchaseDecision.PURCHASED,
            balanceBeforeRub = economy.availableRub,
            balanceAfterPurchaseRub = balanceAfter,
            purchaseTotalRub = purchaseTotal,
            futureMandatoryRub = futureMandatory,
            futureMandatoryAfterPurchaseRub = futureMandatoryAfterPurchase,
            discretionaryRub = optionalCategoryRemaining,
            mandatoryFoodNeeded = mandatoryFoodNeeded,
            foodUnitsPurchased = foodUnits,
            requiredFoodCostRub = requiredFoodCost,
            extraUnitsPurchased = extraUnits,
            optionalPurchaseRub = optionalCost,
            mandatoryPurchaseRub = planActuals.mandatoryRub,
            mandatoryCategoryRemainingRub = mandatoryCategoryRemaining,
            optionalCategoryRemainingRub = optionalCategoryRemaining,
            reserveRemainingRub = reserveRemaining,
            savingsPlanRemainingRub = savingsPlanRemaining,
            // Confirming the warning permits payment; it does not edit the weekly plan.
            consciouslyAdjustedPlan = false,
            eventTargetPurchased = eventTargetQuantity > 0,
            eventTargetQuantity = eventTargetQuantity,
            eventTargetMinimumQuantity = eventTargetMinimumQuantity,
            promotionSavingRub = promotionLinePrice?.savingRub ?: 0,
        )
        val eventContext = event?.let {
            val target = requireNotNull(itemsById[it.productId])
            val targetQuantity = eventTargetQuantity
            val targetPurchased = !declinedWithoutCheckout && targetQuantity > 0
            val assessedQuantity = when {
                it.type == ShopDecisionEventType.IMPULSE_WISH -> 1
                targetPurchased -> targetQuantity
                else -> it.minimumPromotionQuantity
            }
            val targetPrice = it.priceLine(it.productId, target.priceRub, assessedQuantity)
            val decision = if (targetPurchased) PurchaseDecision.PURCHASED else PurchaseDecision.DECLINED
            val targetFood = target as? FoodItem
            val targetNeeded = mandatoryFoodNeeded && (targetFood?.effects?.satietyPercent ?: 0) > 0
            val targetUseful = targetNeeded || targetFood == null ||
                ((targetFood.effects.satietyPercent > 0 &&
                    petHunger <= config.assessment.usefulFoodHungerThreshold) ||
                    minOf(targetFood.effects.happinessPoints, 100 - petHappiness) >=
                        config.assessment.minimumUsefulHappinessGain)
            PurchaseDecisionContext(
                scenario = when (it.type) {
                    ShopDecisionEventType.PROMOTION -> PurchaseScenario.PROMOTION
                    ShopDecisionEventType.IMPULSE_WISH -> PurchaseScenario.IMPULSE_WISH
                },
                decision = decision,
                balanceBeforeRub = economy.availableRub,
                balanceAfterPurchaseRub = if (targetPurchased) balanceAfter else economy.availableRub,
                purchaseTotalRub = if (targetPurchased) purchaseTotal else 0,
                futureMandatoryRub = futureMandatory,
                futureMandatoryAfterPurchaseRub = futureMandatoryAfterPurchase,
                discretionaryRub = optionalCategoryRemaining,
                mandatoryFoodNeeded = mandatoryFoodNeeded,
                foodUnitsPurchased = foodUnits,
                requiredFoodCostRub = requiredFoodCost,
                extraUnitsPurchased = extraUnits,
                optionalPurchaseRub = optionalCost,
                mandatoryPurchaseRub = planActuals.mandatoryRub,
                mandatoryCategoryRemainingRub = mandatoryCategoryRemaining,
                optionalCategoryRemainingRub = optionalCategoryRemaining,
                reserveRemainingRub = reserveRemaining,
                savingsPlanRemainingRub = savingsPlanRemaining,
                consciouslyAdjustedPlan = false,
                eventTargetPurchased = targetPurchased,
                eventTargetNeeded = targetNeeded,
                eventTargetUseful = targetUseful,
                eventTargetPriceRub = targetPrice.chargedTotalRub,
                promotionSavingRub = targetPrice.savingRub,
                eventTargetQuantity = targetQuantity,
                eventTargetMinimumQuantity = if (it.type == ShopDecisionEventType.PROMOTION) {
                    it.minimumPromotionQuantity
                } else {
                    1
                },
            )
        }
        return ShopPurchaseLearningPayload(
            gamePeriod = week.weekNumber,
            sourceOperationId = sourceOperationId,
            standardPurchase = standardContext,
            eventDecision = eventContext,
            mandatoryPlanRub = planActuals.mandatoryRub,
            wantsPlanRub = planActuals.wantsRub,
        )
    }

    private suspend fun record(payload: ShopPurchaseLearningPayload) {
        recordPlanActuals(payload)
        PurchaseLearning.qualifyingActions(
            profileId = CURRENT_PROFILE_ID,
            gamePeriod = payload.gamePeriod,
            sourceOperationId = payload.sourceOperationId,
            context = payload.standardPurchase,
            config = config.assessment,
        ).forEach { action -> recordAction(action) }
        payload.eventDecision?.let { context ->
            PurchaseLearning.qualifyingActions(
                profileId = CURRENT_PROFILE_ID,
                gamePeriod = payload.gamePeriod,
                sourceOperationId = payload.sourceOperationId,
                context = context,
                config = config.assessment,
            ).forEach { action -> recordAction(action) }
        }
    }

    private suspend fun recordPlanActuals(payload: ShopPurchaseLearningPayload) {
        if (planningApi.getPlanProgress(payload.gamePeriod) == null) return
        if (payload.mandatoryPlanRub > 0) {
            recordPlanActual(
                operationId = "${payload.sourceOperationId}:mandatory",
                weekNumber = payload.gamePeriod,
                category = PlanCategory.MANDATORY,
                amountRub = payload.mandatoryPlanRub,
            )
        }
        if (payload.wantsPlanRub > 0) {
            recordPlanActual(
                operationId = "${payload.sourceOperationId}:optional",
                weekNumber = payload.gamePeriod,
                category = PlanCategory.WANTS,
                amountRub = payload.wantsPlanRub,
            )
        }
    }

    private suspend fun recordPlanActual(
        operationId: String,
        weekNumber: Long,
        category: PlanCategory,
        amountRub: Long,
    ) {
        when (
            planningApi.recordActual(
                PlanActualOperation.Payment(
                    operationId = operationId,
                    weekNumber = weekNumber,
                    amountRub = amountRub,
                    classification = when (category) {
                        PlanCategory.MANDATORY -> PaymentClassification.MANDATORY
                        PlanCategory.WANTS -> PaymentClassification.OPTIONAL
                        PlanCategory.SAVINGS -> error("Shop purchase cannot be a savings contribution")
                    },
                ),
            )
        ) {
            RecordActualResult.Recorded,
            RecordActualResult.AlreadyRecorded,
            -> Unit
            RecordActualResult.OperationIdConflict -> error("Conflicting plan actual $operationId")
        }
    }

    private suspend fun recordAction(action: github.detrig.feature.learning.domain.LearningAction) {
        when (val result = learningApi.record(action)) {
            is RecordLearningResult.Processed,
            is RecordLearningResult.AlreadyProcessed,
            -> Unit
            is RecordLearningResult.OperationIdConflict -> error("Conflicting learning action ${result.actionId}")
            is RecordLearningResult.UnsupportedAction -> error("Unsupported learning action ${result.actionType.value}")
        }
    }

    private fun ShopDecisionEvent?.promotionLineTotalOverrides(
        lines: List<github.detrig.products.StoreCartLine>,
    ): Map<ProductId, Long> {
        val event = this?.takeIf { it.type == ShopDecisionEventType.PROMOTION } ?: return emptyMap()
        val targetLine = lines.firstOrNull { it.itemId == event.productId } ?: return emptyMap()
        val linePrice = event.priceLine(event.productId, event.regularPriceRub, targetLine.quantity)
        return if (linePrice.savingRub > 0) {
            mapOf(event.productId to linePrice.chargedTotalRub)
        } else {
            emptyMap()
        }
    }

    private fun String?.learningPayloadOrNull(): ShopPurchaseLearningPayload? {
        val encoded = this
            ?.split(';')
            ?.firstOrNull { it.startsWith(LEARNING_METADATA_PREFIX) }
            ?.removePrefix(LEARNING_METADATA_PREFIX)
            ?: return null
        return ShopPurchaseLearningPayloadCodec.decode(encoded)
    }

    private data class PricedLine(
        val item: github.detrig.products.SellableItem,
        val quantity: Int,
        val unitPriceRub: Long,
        val lineTotalRub: Long,
    )

    private companion object {
        const val CURRENT_PROFILE_ID = "current"
        const val SHOP_REASON_PREFIX = "shop:"
        const val LEARNING_METADATA_PREFIX = "learning="
    }
}
