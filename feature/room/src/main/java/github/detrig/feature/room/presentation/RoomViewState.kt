package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.HousePosition
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.RoomZoneUiModel
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpState
import github.detrig.feature.planning.domain.PlanAdjustmentReason
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.room.domain.model.FirstRunOnboardingStep
import github.detrig.feature.room.domain.model.RoomImpulseWish

internal data class ParentHelpDialogState(
    val offers: List<ParentHelpOffer>,
    val activeHelp: ParentHelpState?,
)

internal data class AllowanceNoticeState(
    val grossRub: Long,
    val parentHelpRepaidRub: Long,
    val receivedRub: Long,
)

internal data object EarlyWeekParentHelpNoticeState

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
        else -> emptySet()
    }

    val allowedObjectIds: Set<String> get() = when (step) {
        FirstRunOnboardingStep.GAME_SELECTION -> selectableGoalZoneIds
        FirstRunOnboardingStep.WAITING_FOR_PIGGY -> setOf("piggy_bank")
        else -> emptySet()
    }
}

internal val FIRST_SAVINGS_GOAL_ZONE_IDS = linkedSetOf("drawing", "music", "fishing")

internal data class PlanAchievementFeedback(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = true,
)

internal enum class PlanTutorialStep {
    INTRODUCTION,
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
        val achievements: List<PlanAchievementFeedback> = emptyList(),
        val isAchievementsVisible: Boolean = false,
        val parentHelpDialog: ParentHelpDialogState? = null,
        val isRequestingParentHelp: Boolean = false,
        val allowanceNotice: AllowanceNoticeState? = null,
        val earlyWeekParentHelpNotice: EarlyWeekParentHelpNoticeState? = null,
        val impulseWish: RoomImpulseWish? = null,
        val onboarding: FirstRunOnboardingState? = null,
        val initialPosition: HousePosition = HouseLayout.initialPosition(),
    ) : RoomViewState
}
