package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.HousePosition
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.RoomZoneUiModel
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpState
import github.detrig.feature.economy.domain.SavingsGoalProgress
import github.detrig.feature.planning.domain.PlanAdjustmentReason
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.room.api.FirstRunOnboardingStep
import github.detrig.feature.room.domain.model.RoomImpulseWish
import github.detrig.feature.learning.domain.ParentProgressRow

data class ParentHelpDialogState(
    val offers: List<ParentHelpOffer>,
    val activeHelp: ParentHelpState?,
    val isSubmitting: Boolean = false,
    val availableRub: Long = 0,
    val savingsRub: Long = 0,
    val debtRub: Long = 0,
    val minimumRequiredBalanceRub: Long = 0,
)

internal data class AllowanceNoticeState(
    val grossRub: Long,
    val parentHelpRepaidRub: Long,
    val receivedRub: Long,
)

internal data object EarlyWeekParentHelpNoticeState

internal data object SavingsRecoveryPromptState

internal data object ParentHelpPhonePromptState

internal data class DayTransitionNoticeState(
    val dayOfWeek: Int,
    val weekNumber: Long,
)
internal enum class WeekSummaryTutorialStep {
    INCOME,
    EXPENSES,
    REMAINDER,
}

internal data class FirstWeekNeedHint(val fridgeIsEmpty: Boolean)
internal data class FirstWeekGoalHint(val remainingRub: Long)

internal data class FirstRunOnboardingState(
    val step: FirstRunOnboardingStep,
    val suggestedGoalZoneId: String? = null,
    val selectableGoalZoneIds: Set<String> = FIRST_SAVINGS_GOAL_ZONE_IDS,
    val goalTitle: String? = null,
    val goalTargetRub: Long? = null,
    val goalSavedRub: Long = 0,
) {
    val focusObjectId: String? get() = when (step) {
        FirstRunOnboardingStep.GAME_DISCOVERY,
        FirstRunOnboardingStep.GAME_DISCOVERY_DETAILS,
        FirstRunOnboardingStep.GAME_SELECTION,
        -> "drawing"
        FirstRunOnboardingStep.GAME_SELECTED -> suggestedGoalZoneId ?: "drawing"
        FirstRunOnboardingStep.PIGGY_BANK,
        FirstRunOnboardingStep.PIGGY_TAP,
        FirstRunOnboardingStep.WAITING_FOR_PIGGY,
        FirstRunOnboardingStep.WAITING_FOR_GOAL,
        -> "piggy_bank"
        FirstRunOnboardingStep.PHONE_GUIDANCE,
        FirstRunOnboardingStep.WAITING_FOR_PHONE,
        -> "phone"
        FirstRunOnboardingStep.FRIDGE_GUIDANCE,
        FirstRunOnboardingStep.WAITING_FOR_FRIDGE,
        -> "fridge"
        FirstRunOnboardingStep.TABLE_PROMPT,
        FirstRunOnboardingStep.TABLE_GUIDANCE,
        -> "dining_table"
        FirstRunOnboardingStep.BEDTIME_LATE,
        FirstRunOnboardingStep.BEDTIME_GUIDANCE,
        FirstRunOnboardingStep.WAITING_FOR_BED,
        -> "bed"
        else -> null
    }

    val highlightedObjectIds: Set<String> get() = when (step) {
        FirstRunOnboardingStep.GAME_DISCOVERY,
        FirstRunOnboardingStep.GAME_DISCOVERY_DETAILS,
        FirstRunOnboardingStep.GAME_SELECTION,
        -> selectableGoalZoneIds
        FirstRunOnboardingStep.GAME_SELECTED -> setOfNotNull(suggestedGoalZoneId)
        FirstRunOnboardingStep.PIGGY_BANK,
        FirstRunOnboardingStep.PIGGY_TAP,
        FirstRunOnboardingStep.WAITING_FOR_PIGGY,
        -> setOf("piggy_bank")
        FirstRunOnboardingStep.PHONE_GUIDANCE,
        FirstRunOnboardingStep.WAITING_FOR_PHONE,
        -> setOf("phone")
        FirstRunOnboardingStep.FRIDGE_GUIDANCE,
        FirstRunOnboardingStep.WAITING_FOR_FRIDGE,
        -> setOf("fridge")
        FirstRunOnboardingStep.TABLE_PROMPT,
        FirstRunOnboardingStep.TABLE_GUIDANCE,
        -> setOf("dining_table")
        FirstRunOnboardingStep.BEDTIME_LATE,
        FirstRunOnboardingStep.BEDTIME_GUIDANCE,
        FirstRunOnboardingStep.WAITING_FOR_BED,
        -> setOf("bed")
        else -> emptySet()
    }

    val allowedObjectIds: Set<String> get() = when (step) {
        FirstRunOnboardingStep.GAME_SELECTION -> selectableGoalZoneIds
        FirstRunOnboardingStep.WAITING_FOR_PIGGY -> setOf("piggy_bank")
        FirstRunOnboardingStep.WAITING_FOR_PHONE -> setOf("phone")
        FirstRunOnboardingStep.WAITING_FOR_FRIDGE -> setOf("fridge")
        FirstRunOnboardingStep.TABLE_GUIDANCE -> setOf("dining_table")
        FirstRunOnboardingStep.WAITING_FOR_BED -> setOf("bed")
        else -> emptySet()
    }
}

internal val FIRST_SAVINGS_GOAL_ZONE_IDS = linkedSetOf("drawing", "music", "fishing")

internal data class PlanAchievementFeedback(
    val id: String,
    val topicId: String = "",
    val title: String,
    val description: String,
    val isUnlocked: Boolean = true,
    val xpReward: Int = 0,
    val unlockOrder: Long? = null,
)

internal enum class RoomMenuDestination { NONE, MENU, ALL_ACHIEVEMENTS, PARENT_GATE, PARENT_CABINET }

internal data class ParentGateState(
    val firstNumber: Int,
    val secondNumber: Int,
    val answer: String = "",
    val hasError: Boolean = false,
)

internal enum class PlanTutorialStep {
    MANDATORY,
    WANTS,
    SAVINGS,
    RESERVE,
    PRACTICE;

    fun nextOrNull(): PlanTutorialStep? = entries.getOrNull(ordinal + 1)
}

internal sealed interface PlanDialogueState {
    data class NeedsChanges(
        val reason: PlanAdjustmentReason,
        val recommendedPercent: Int,
    ) : PlanDialogueState

    data class Saved(
        val showSuccessExplanation: Boolean,
        val hasSavings: Boolean,
    ) : PlanDialogueState
}

internal sealed interface RoomViewState : CoreViewState {
    data object Loading : RoomViewState
    data object Error : RoomViewState
    data class Content(
        val zones: List<RoomZoneUiModel>,
        val progress: RoomProgress,
        val buyingZoneId: String? = null,
        val savingGoalZoneId: String? = null,
        val sleepConfirmationVisible: Boolean = false,
        val sleeping: Boolean = false,
        val planEditor: PlanEditorState? = null,
        val planTutorialStep: PlanTutorialStep? = null,
        val planDialogue: PlanDialogueState? = null,
        val isSavingPlan: Boolean = false,
        val isPlanSummaryVisible: Boolean = false,
        val weekResult: WeeklyPlanProgress? = null,
        val weekSummaryTutorialStep: WeekSummaryTutorialStep? = null,
        val showFirstGamePurchaseFeedback: Boolean = false,
        val firstGamePurchaseZoneId: String? = null,
        val readyFirstGameZoneId: String? = null,
        val activeSavingsGoal: SavingsGoalProgress? = null,
        val firstWeekNeedHint: FirstWeekNeedHint? = null,
        val firstWeekGoalHint: FirstWeekGoalHint? = null,
        val achievements: List<PlanAchievementFeedback> = emptyList(),
        val parentRows: List<ParentProgressRow> = emptyList(),
        val menuDestination: RoomMenuDestination = RoomMenuDestination.NONE,
        val areMenuAchievementsExpanded: Boolean = false,
        val parentGate: ParentGateState? = null,
        val parentHelpDialog: ParentHelpDialogState? = null,
        val isRequestingParentHelp: Boolean = false,
        val parentHelpPhonePrompt: ParentHelpPhonePromptState? = null,
        val savingsRecoveryPrompt: SavingsRecoveryPromptState? = null,
        val allowanceNotice: AllowanceNoticeState? = null,
        val earlyWeekParentHelpNotice: EarlyWeekParentHelpNoticeState? = null,
        val dayTransitionNotice: DayTransitionNoticeState? = null,
        val impulseWish: RoomImpulseWish? = null,
        val onboarding: FirstRunOnboardingState? = null,
        val initialPosition: HousePosition = HouseLayout.initialPosition(),
    ) : RoomViewState
}
