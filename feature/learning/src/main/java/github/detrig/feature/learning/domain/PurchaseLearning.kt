package github.detrig.feature.learning.domain

/** Educational facts for shop decisions. The shop reports an immutable snapshot; Learning judges it. */
object PurchaseLearning {
    val REASONABLE_DECISION = LearningActionType("purchase.reasonable_decision")
    val PROMOTION_DECISION = LearningActionType("purchase.promotion_decision")
    val IMPULSE_DECISION = LearningActionType("purchase.impulse_decision")

    fun rules(): List<MetricRuleDefinition> = listOf(
        MetricRuleDefinition(
            metricId = LearningMetricIds.PURCHASE_REASONABLE,
            actionTypes = setOf(REASONABLE_DECISION),
            milestones = listOf(
                ProgressMilestone(progressSteps = 1, requiredActions = 2),
                ProgressMilestone(
                    progressSteps = 2,
                    requiredActions = 3,
                    requiredDistinctPeriods = 3,
                    requiredCurrentStreak = 3,
                ),
            ),
        ),
        rule(LearningMetricIds.PURCHASE_PROMOTION, PROMOTION_DECISION),
        rule(LearningMetricIds.PURCHASE_IMPULSE, IMPULSE_DECISION),
    )

    private fun rule(
        metricId: String,
        actionType: LearningActionType,
    ) = MetricRuleDefinition(
        metricId = metricId,
        actionTypes = setOf(actionType),
        milestones = listOf(
            ProgressMilestone(progressSteps = 1, requiredActions = 1),
            ProgressMilestone(
                progressSteps = 2,
                requiredActions = 2,
                requiredDistinctPeriods = 2,
            ),
        ),
    )

    fun qualifyingActions(
        profileId: String,
        gamePeriod: Long,
        sourceOperationId: String,
        context: PurchaseDecisionContext,
        config: PurchaseAssessmentConfig = PurchaseAssessmentConfig(),
    ): List<LearningAction> = buildList {
        if (context.scenario == PurchaseScenario.STANDARD_PURCHASE && context.purchaseProblem(config) == null) {
            add(action(profileId, gamePeriod, sourceOperationId, REASONABLE_DECISION, "reasonable", context))
        }
        if (context.scenario == PurchaseScenario.PROMOTION && context.isGoodPromotionDecision(config)) {
            add(action(profileId, gamePeriod, sourceOperationId, PROMOTION_DECISION, "promotion", context))
        }
        if (context.scenario == PurchaseScenario.IMPULSE_WISH && context.isGoodImpulseDecision(config)) {
            add(action(profileId, gamePeriod, sourceOperationId, IMPULSE_DECISION, "impulse", context))
        }
    }

    private fun action(
        profileId: String,
        gamePeriod: Long,
        sourceOperationId: String,
        type: LearningActionType,
        suffix: String,
        context: PurchaseDecisionContext,
    ) = LearningAction(
        actionId = "purchase:$sourceOperationId:$suffix",
        profileId = profileId,
        gamePeriod = gamePeriod,
        type = type,
        context = context,
        sourceOperationId = sourceOperationId,
    )
}

data class PurchaseAssessmentConfig(
    /** A small extra is allowed when required food and future mandatory costs remain covered. */
    val maximumSmallExtraUnits: Int = 2,
    /** Units above the quantity needed to receive a promotion before it becomes stockpiling. */
    val maximumPromotionExtraUnits: Int = 2,
) {
    init {
        require(maximumSmallExtraUnits >= 0)
        require(maximumPromotionExtraUnits >= 0)
    }
}

enum class PurchaseScenario {
    STANDARD_PURCHASE,
    PROMOTION,
    IMPULSE_WISH,
}

enum class PurchaseDecision {
    PURCHASED,
    DECLINED,
}

enum class PurchaseProblem {
    REQUIRED_FOOD_MISSING,
    MANDATORY_MONEY_AT_RISK,
    PROMOTION_OVERBUY,
    TOO_MANY_EXTRAS,
}

/**
 * Snapshot captured at the moment of a decision. Monetary fields contain rubles and never change
 * after the action is committed, so replaying an economy operation cannot change its assessment.
 */
data class PurchaseDecisionContext(
    val scenario: PurchaseScenario,
    val decision: PurchaseDecision,
    val balanceBeforeRub: Long,
    val balanceAfterPurchaseRub: Long,
    val purchaseTotalRub: Long,
    val futureMandatoryRub: Long,
    val discretionaryRub: Long,
    val mandatoryFoodNeeded: Boolean,
    val foodUnitsPurchased: Int,
    val requiredFoodCostRub: Long,
    val extraUnitsPurchased: Int,
    val optionalPurchaseRub: Long,
    val eventTargetPurchased: Boolean = false,
    val eventTargetNeeded: Boolean = false,
    val eventTargetPriceRub: Long = 0,
    val promotionSavingRub: Long = 0,
    val eventTargetQuantity: Int = 0,
    val eventTargetMinimumQuantity: Int = 0,
) : LearningActionContext {
    init {
        require(balanceBeforeRub >= 0)
        require(balanceAfterPurchaseRub >= 0)
        require(purchaseTotalRub >= 0)
        require(futureMandatoryRub >= 0)
        require(discretionaryRub >= 0)
        require(foodUnitsPurchased >= 0)
        require(requiredFoodCostRub >= 0)
        require(extraUnitsPurchased >= 0)
        require(optionalPurchaseRub >= 0)
        require(eventTargetPriceRub >= 0)
        require(promotionSavingRub >= 0)
        require(eventTargetQuantity >= 0)
        require(eventTargetMinimumQuantity >= 0)
        require(balanceAfterPurchaseRub <= balanceBeforeRub)
    }

    override val fingerprint: String = listOf(
        "scenario=${scenario.name}",
        "decision=${decision.name}",
        "before=$balanceBeforeRub",
        "after=$balanceAfterPurchaseRub",
        "total=$purchaseTotalRub",
        "mandatory=$futureMandatoryRub",
        "discretionary=$discretionaryRub",
        "foodNeeded=$mandatoryFoodNeeded",
        "foodUnits=$foodUnitsPurchased",
        "requiredFoodCost=$requiredFoodCostRub",
        "extraUnits=$extraUnitsPurchased",
        "optionalCost=$optionalPurchaseRub",
        "targetPurchased=$eventTargetPurchased",
        "targetNeeded=$eventTargetNeeded",
        "targetPrice=$eventTargetPriceRub",
        "promotionSaving=$promotionSavingRub",
        "targetQuantity=$eventTargetQuantity",
        "targetMinimumQuantity=$eventTargetMinimumQuantity",
    ).joinToString(";")

    fun purchaseProblem(config: PurchaseAssessmentConfig): PurchaseProblem? {
        if (decision != PurchaseDecision.PURCHASED) {
            return null
        }
        if (mandatoryFoodNeeded && foodUnitsPurchased == 0) {
            return PurchaseProblem.REQUIRED_FOOD_MISSING
        }

        val mandatoryAfterRequiredFood = (futureMandatoryRub - requiredFoodCostRub).coerceAtLeast(0)
        if (balanceAfterPurchaseRub < mandatoryAfterRequiredFood) {
            return PurchaseProblem.MANDATORY_MONEY_AT_RISK
        }

        val promotionQuantityLimit = eventTargetMinimumQuantity + config.maximumPromotionExtraUnits
        if (
            eventTargetPurchased &&
            promotionSavingRub > 0 &&
            eventTargetMinimumQuantity > 0 &&
            eventTargetQuantity > promotionQuantityLimit
        ) {
            return PurchaseProblem.PROMOTION_OVERBUY
        }

        val extrasFitBudget = optionalPurchaseRub <= discretionaryRub
        val extrasAreSmall = extraUnitsPurchased <= config.maximumSmallExtraUnits
        return if (extrasAreSmall || extrasFitBudget) null else PurchaseProblem.TOO_MANY_EXTRAS
    }

    internal fun isReasonable(config: PurchaseAssessmentConfig): Boolean =
        decision == PurchaseDecision.PURCHASED && purchaseProblem(config) == null

    internal fun isGoodPromotionDecision(config: PurchaseAssessmentConfig): Boolean {
        if (scenario != PurchaseScenario.PROMOTION || promotionSavingRub <= 0) return false
        val mandatoryAfterTarget = if (eventTargetNeeded) {
            (futureMandatoryRub - eventTargetPriceRub).coerceAtLeast(0)
        } else {
            futureMandatoryRub
        }
        val canBuyWithoutRisk = balanceBeforeRub >= eventTargetPriceRub &&
            balanceBeforeRub - eventTargetPriceRub >= mandatoryAfterTarget
        val targetPurchaseIsReasonable = eventTargetNeeded && canBuyWithoutRisk
        return when (decision) {
            PurchaseDecision.PURCHASED ->
                eventTargetPurchased && targetPurchaseIsReasonable && isReasonable(config)
            PurchaseDecision.DECLINED -> !targetPurchaseIsReasonable
        }
    }

    internal fun isGoodImpulseDecision(config: PurchaseAssessmentConfig): Boolean {
        if (scenario != PurchaseScenario.IMPULSE_WISH || eventTargetPriceRub <= 0) return false
        val wishFitsBudget = balanceBeforeRub >= eventTargetPriceRub &&
            balanceBeforeRub - eventTargetPriceRub >= futureMandatoryRub &&
            eventTargetPriceRub <= discretionaryRub
        return when (decision) {
            PurchaseDecision.PURCHASED -> eventTargetPurchased && wishFitsBudget && isReasonable(config)
            PurchaseDecision.DECLINED -> !wishFitsBudget
        }
    }
}
