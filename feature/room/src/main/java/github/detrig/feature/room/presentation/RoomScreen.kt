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

@Composable
internal fun RoomScreen(
    modifier: Modifier = Modifier,
    petContent: @Composable (Modifier) -> Unit = {},
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
    RoomContent(
        state, viewModel::perform, modifier, petContent,
        active = resumed && focused && dialogZoneId == null,
        previewZoneId = requestedZoneId,
        onPreviewReady = { id ->
            previewRequests.consume(id)
            viewModel.perform(RoomViewEvent.ZonePreviewed(id))
        },
    )
    val content = state as? RoomViewState.Content
    val zone = content?.zones?.find { it.id == dialogZoneId }
    if (zone?.access is RoomZoneAccess.Buyable) {
        RoomBuyDialog(zone, content.progress, content.buyingZoneId != null,
            onConfirm = { viewModel.perform(RoomViewEvent.BuyConfirmed(zone.id)) },
            onDismiss = { dialogZoneId = null })
    }
    LaunchedEffect(zone?.access, content != null) {
        if (content != null && zone?.access !is RoomZoneAccess.Buyable) dialogZoneId = null
    }
}
