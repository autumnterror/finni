package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.HousePosition
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.RoomZoneUiModel

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
        val initialPosition: HousePosition = HouseLayout.initialPosition(),
    ) : RoomViewState
}
