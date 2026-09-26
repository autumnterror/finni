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
        if (context.scenario == PurchaseScenario.STANDARD_PURCHASE && context.isReasonable(config)) {
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
    /** Units above the quantity needed to receive a promotion before it becomes stockpiling. */
    val maximumPromotionExtraUnits: Int = 2,
    val minimumUsefulHappinessGain: Int = 10,
    val usefulFoodHungerThreshold: Int = 70,
) {
    init {
        require(maximumPromotionExtraUnits >= 0)
        require(minimumUsefulHappinessGain in 1..100)
        require(usefulFoodHungerThreshold in 0..100)
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
    RESERVE_AT_RISK,
    PROMOTION_OVERBUY,
    TOO_MANY_EXTRAS,
}

enum class PurchaseOutcome { GOOD, PLAN_ADJUSTMENT, RISKY }

data class PurchaseAssessment(
    val outcome: PurchaseOutcome,
    val problem: PurchaseProblem? = null,
    val categoryOverrunRub: Long = 0,
    val savingsPlanReductionRub: Long = 0,
)

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
    val mandatoryPurchaseRub: Long = requiredFoodCostRub,
    val mandatoryCategoryRemainingRub: Long = futureMandatoryRub,
    val optionalCategoryRemainingRub: Long = discretionaryRub,
    val reserveRemainingRub: Long = 0,
    val savingsPlanRemainingRub: Long = 0,
    val consciouslyAdjustedPlan: Boolean = false,
    val eventTargetUseful: Boolean = eventTargetNeeded || scenario == PurchaseScenario.IMPULSE_WISH,
    val futureMandatoryAfterPurchaseRub: Long =
        (futureMandatoryRub - mandatoryPurchaseRub).coerceAtLeast(0),
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
        require(mandatoryPurchaseRub >= 0)
        require(mandatoryCategoryRemainingRub >= 0)
        require(optionalCategoryRemainingRub >= 0)
        require(reserveRemainingRub >= 0)
        require(savingsPlanRemainingRub >= 0)
        require(futureMandatoryAfterPurchaseRub >= 0)
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
        "mandatoryPurchase=$mandatoryPurchaseRub",
        "mandatoryCategory=$mandatoryCategoryRemainingRub",
        "optionalCategory=$optionalCategoryRemainingRub",
        "reserve=$reserveRemainingRub",
        "savingsPlan=$savingsPlanRemainingRub",
        "adjusted=$consciouslyAdjustedPlan",
        "targetUseful=$eventTargetUseful",
        "mandatoryAfter=$futureMandatoryAfterPurchaseRub",
    ).joinToString(";")

    fun assess(config: PurchaseAssessmentConfig): PurchaseAssessment {
        if (decision != PurchaseDecision.PURCHASED) return PurchaseAssessment(PurchaseOutcome.RISKY)
        val mandatoryAfterPurchase = futureMandatoryAfterPurchaseRub
        if (balanceAfterPurchaseRub < mandatoryAfterPurchase) {
            return PurchaseAssessment(PurchaseOutcome.RISKY, PurchaseProblem.MANDATORY_MONEY_AT_RISK)
        }
        if (optionalPurchaseRub > 0 &&
            balanceAfterPurchaseRub < mandatoryAfterPurchase + reserveRemainingRub
        ) {
            return PurchaseAssessment(PurchaseOutcome.RISKY, PurchaseProblem.RESERVE_AT_RISK)
        }

        val promotionQuantityLimit = eventTargetMinimumQuantity + config.maximumPromotionExtraUnits
        if (
            eventTargetPurchased &&
            promotionSavingRub > 0 &&
            eventTargetMinimumQuantity > 0 &&
            eventTargetQuantity > promotionQuantityLimit
        ) {
            return PurchaseAssessment(PurchaseOutcome.RISKY, PurchaseProblem.PROMOTION_OVERBUY)
        }
        val savingsPossibleBefore = minOf(
            savingsPlanRemainingRub,
            (balanceBeforeRub - futureMandatoryRub - reserveRemainingRub).coerceAtLeast(0),
        )
        val savingsPossibleAfter = minOf(
            savingsPlanRemainingRub,
            (balanceAfterPurchaseRub - mandatoryAfterPurchase - reserveRemainingRub).coerceAtLeast(0),
        )
        val savingsReduction = (savingsPossibleBefore - savingsPossibleAfter).coerceAtLeast(0)
        val categoryOverrun = (mandatoryPurchaseRub - mandatoryCategoryRemainingRub).coerceAtLeast(0) +
            (optionalPurchaseRub - optionalCategoryRemainingRub).coerceAtLeast(0)
        return if (categoryOverrun > 0 || savingsReduction > 0) {
            PurchaseAssessment(
                PurchaseOutcome.PLAN_ADJUSTMENT,
                categoryOverrunRub = categoryOverrun,
                savingsPlanReductionRub = savingsReduction,
            )
        } else {
            PurchaseAssessment(PurchaseOutcome.GOOD)
        }
    }

    fun purchaseProblem(config: PurchaseAssessmentConfig): PurchaseProblem? = assess(config).problem

    internal fun isReasonable(config: PurchaseAssessmentConfig): Boolean =
        decision == PurchaseDecision.PURCHASED && assess(config).outcome == PurchaseOutcome.GOOD

    internal fun isGoodPromotionDecision(config: PurchaseAssessmentConfig): Boolean {
        if (scenario != PurchaseScenario.PROMOTION || promotionSavingRub <= 0) return false
        val mandatoryAfterTarget = if (eventTargetNeeded) {
            (futureMandatoryRub - eventTargetPriceRub).coerceAtLeast(0)
        } else {
            futureMandatoryRub
        }
        val targetCategoryRemaining = if (eventTargetNeeded) {
            mandatoryCategoryRemainingRub
        } else {
            optionalCategoryRemainingRub
        }
        val targetPurchaseIsReasonable = eventTargetUseful &&
            balanceBeforeRub >= eventTargetPriceRub &&
            eventTargetPriceRub <= targetCategoryRemaining &&
            balanceBeforeRub - eventTargetPriceRub >=
                mandatoryAfterTarget + reserveRemainingRub + savingsPlanRemainingRub
        return when (decision) {
            PurchaseDecision.PURCHASED ->
                eventTargetPurchased && eventTargetUseful && isReasonable(config)
            PurchaseDecision.DECLINED -> !targetPurchaseIsReasonable
        }
    }

    internal fun isGoodImpulseDecision(config: PurchaseAssessmentConfig): Boolean {
        if (scenario != PurchaseScenario.IMPULSE_WISH || eventTargetPriceRub <= 0) return false
        val wishFitsBudget = eventTargetUseful && balanceBeforeRub >= eventTargetPriceRub &&
            balanceBeforeRub - eventTargetPriceRub >=
                futureMandatoryRub + reserveRemainingRub + savingsPlanRemainingRub &&
            eventTargetPriceRub <= optionalCategoryRemainingRub
        return when (decision) {
            PurchaseDecision.PURCHASED -> eventTargetPurchased && wishFitsBudget && isReasonable(config)
            PurchaseDecision.DECLINED -> !wishFitsBudget
        }
    }
}
