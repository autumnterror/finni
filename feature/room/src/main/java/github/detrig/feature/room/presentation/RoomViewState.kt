package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.HousePosition
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.RoomZoneUiModel
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpState

internal data class ParentHelpDialogState(
    val offers: List<ParentHelpOffer>,
    val activeHelp: ParentHelpState?,
)

internal data class AllowanceNoticeState(
    val grossRub: Long,
    val parentHelpRepaidRub: Long,
    val receivedRub: Long,
)

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
        val isSavingPlan: Boolean = false,
        val isPlanSummaryVisible: Boolean = false,
        val parentHelpDialog: ParentHelpDialogState? = null,
        val isRequestingParentHelp: Boolean = false,
        val allowanceNotice: AllowanceNoticeState? = null,
        val initialPosition: HousePosition = HouseLayout.initialPosition(),
    ) : RoomViewState
}
