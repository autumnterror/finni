package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.HousePosition
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.RoomZoneUiModel
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpState
import github.detrig.feature.planning.domain.PlanAdjustmentReason

internal data class ParentHelpDialogState(
    val offers: List<ParentHelpOffer>,
    val activeHelp: ParentHelpState?,
)

internal data class AllowanceNoticeState(
    val grossRub: Long,
    val parentHelpRepaidRub: Long,
    val receivedRub: Long,
)

internal data class ZeroBalanceHelpNoticeState(val amountRub: Long)

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
        val sleeping: Boolean = false,
        val planEditor: PlanEditorState? = null,
        val planTutorialStep: PlanTutorialStep? = null,
        val planDialogue: PlanDialogueState? = null,
        val isSavingPlan: Boolean = false,
        val isPlanSummaryVisible: Boolean = false,
        val achievements: List<PlanAchievementFeedback> = emptyList(),
        val isAchievementsVisible: Boolean = false,
        val achievementBanner: PlanAchievementFeedback? = null,
        val pendingAchievementBanners: List<PlanAchievementFeedback> = emptyList(),
        val parentHelpDialog: ParentHelpDialogState? = null,
        val isRequestingParentHelp: Boolean = false,
        val allowanceNotice: AllowanceNoticeState? = null,
        val zeroBalanceHelpNotice: ZeroBalanceHelpNoticeState? = null,
        val initialPosition: HousePosition = HouseLayout.initialPosition(),
    ) : RoomViewState
}
