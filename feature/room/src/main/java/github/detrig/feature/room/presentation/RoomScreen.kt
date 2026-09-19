package github.detrig.feature.room.presentation

import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.core.mvvm.command.CommandsQueueEffect
import github.detrig.core.mvvm.command.ImmutableCommandsQueue
import github.detrig.feature.room.RoomFeature
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.navigation.RoomPreviewRequests
import github.detrig.feature.room.presentation.component.RoomBuyDialog
import github.detrig.feature.room.presentation.component.WeeklyPlanEditorDialog
import github.detrig.feature.room.presentation.component.WeeklyPlanProgressDialog
import github.detrig.feature.room.presentation.component.ParentHelpDialog
import github.detrig.feature.room.presentation.component.AllowanceReceiptDialog
import github.detrig.feature.room.presentation.component.ZeroBalanceHelpDialog

@Composable
internal fun RoomScreen(
    modifier: Modifier = Modifier,
    petContent: @Composable (Modifier) -> Unit = {},
    onMirrorClick: () -> Unit = {},
    previewRequests: RoomPreviewRequests,
) {
    val viewModel: RoomViewModel = viewModel { RoomFeature.component().getRoomViewModel() }
    val state by viewModel.state().observeAsState(RoomViewState.Loading)
    var dialogZoneId by rememberSaveable { mutableStateOf<String?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val focused = LocalWindowInfo.current.isWindowFocused
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val commands = remember(viewModel) { ImmutableCommandsQueue(viewModel.commands<RoomCommand>()) }
    CommandsQueueEffect(commands) { command ->
        when (command) {
            is RoomCommand.ShowBuyConfirmation -> dialogZoneId = command.zoneId
            is RoomCommand.CloseBuyConfirmation -> if (dialogZoneId == command.zoneId) dialogZoneId = null
        }
    }
    LaunchedEffect(viewModel) { viewModel.perform(RoomViewEvent.Load) }
    val requestedZoneId by previewRequests.zoneId.collectAsState()
    val content = state as? RoomViewState.Content
    RoomContent(
        state, viewModel::perform, modifier, petContent,
        onMirrorClick = onMirrorClick,
        active = resumed && focused && dialogZoneId == null && content?.planEditor == null &&
            content?.isPlanSummaryVisible != true && content?.parentHelpDialog == null && content?.allowanceNotice == null &&
            content?.zeroBalanceHelpNotice == null,
        previewZoneId = requestedZoneId,
        onPreviewReady = { id ->
            previewRequests.consume(id)
            viewModel.perform(RoomViewEvent.ZonePreviewed(id))
        },
    )
    val zone = content?.zones?.find { it.id == dialogZoneId }
    if (zone?.access is RoomZoneAccess.Buyable) {
        RoomBuyDialog(zone, content.progress, content.buyingZoneId != null,
            onConfirm = { viewModel.perform(RoomViewEvent.BuyConfirmed(zone.id)) },
            isSavingGoal = content.savingGoalZoneId == zone.id,
            onSaveAsGoal = { title -> viewModel.perform(RoomViewEvent.SaveZoneAsGoal(zone.id, title)) },
            onDismiss = { dialogZoneId = null })
    }
    content?.allowanceNotice?.let { notice ->
        AllowanceReceiptDialog(notice) { viewModel.perform(RoomViewEvent.CloseAllowanceNotice) }
    }
    content?.zeroBalanceHelpNotice?.let { notice ->
        ZeroBalanceHelpDialog(notice) { viewModel.perform(RoomViewEvent.CloseZeroBalanceHelpNotice) }
    }
    content?.parentHelpDialog?.takeIf { content.zeroBalanceHelpNotice == null }?.let { dialog ->
        ParentHelpDialog(
            state = dialog,
            isRequesting = content.isRequestingParentHelp,
            onOfferSelected = { viewModel.perform(RoomViewEvent.ParentHelpOfferClicked(it)) },
            onDismiss = { viewModel.perform(RoomViewEvent.CloseParentHelpDialog) },
        )
    }
    content?.planEditor?.takeIf {
        content.allowanceNotice == null && content.zeroBalanceHelpNotice == null
    }?.let { editor ->
        WeeklyPlanEditorDialog(
            editor = editor,
            isSaving = content.isSavingPlan,
            onPercentChanged = { category, percent ->
                viewModel.perform(RoomViewEvent.PlanPercentChanged(category, percent))
            },
            onSave = { viewModel.perform(RoomViewEvent.SavePlanClicked) },
        )
    }
    content?.progress?.planProgress?.takeIf { content.isPlanSummaryVisible }?.let { plan ->
        WeeklyPlanProgressDialog(plan) { viewModel.perform(RoomViewEvent.ClosePlanSummary) }
    }
    LaunchedEffect(zone?.access, content != null) {
        if (content != null && zone?.access !is RoomZoneAccess.Buyable) dialogZoneId = null
    }
}
