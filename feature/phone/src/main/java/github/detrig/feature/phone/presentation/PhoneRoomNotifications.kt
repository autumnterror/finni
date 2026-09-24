package github.detrig.feature.phone.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.CoreViewState
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.phone.PhoneFeature
import github.detrig.feature.phone.R
import github.detrig.feature.phone.api.PhoneRoomNotificationState
import github.detrig.feature.phone.data.MessagesRepository
import github.detrig.feature.phone.domain.MessagesCoordinator
import kotlinx.coroutines.Job

internal data class RoomNotificationsViewState(
    val unreadCount: Int = 0,
    val firstPromptPending: Boolean = false,
) : CoreViewState

internal sealed interface RoomNotificationsViewEvent : CoreViewEvent {
    data object Load : RoomNotificationsViewEvent
    data object DismissFirstPrompt : RoomNotificationsViewEvent
}

internal class RoomNotificationsViewModel(
    private val repository: MessagesRepository,
    private val coordinator: MessagesCoordinator,
) : CoreViewModel<RoomNotificationsViewState, RoomNotificationsViewEvent>(
    RoomNotificationsViewState(),
) {
    private var observationJob: Job? = null

    override fun perform(viewEvent: RoomNotificationsViewEvent) {
        when (viewEvent) {
            RoomNotificationsViewEvent.Load -> observeInbox()
            RoomNotificationsViewEvent.DismissFirstPrompt -> launchCoroutine {
                coordinator.consumeFirstRoomPrompt()
            }
        }
    }

    private fun observeInbox() {
        if (observationJob?.isActive == true) return
        observationJob = launchCoroutine(
            handleAction = ExceptionConsumer { true },
        ) {
            repository.observeInbox().collect { inbox ->
                updateState {
                    copy(
                        unreadCount = inbox.unreadCount,
                        firstPromptPending = inbox.firstRoomPromptPending,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PhoneRoomNotifications(
    content: @Composable (PhoneRoomNotificationState, () -> Unit) -> Unit,
) {
    val viewModel: RoomNotificationsViewModel = viewModel {
        PhoneFeature.component().roomNotificationsViewModel()
    }
    val state by viewModel.state().observeAsState(RoomNotificationsViewState())
    LaunchedEffect(viewModel) { viewModel.perform(RoomNotificationsViewEvent.Load) }
    val firstPrompt = if (state.firstPromptPending) {
        stringResource(R.string.messages_first_room_prompt)
    } else {
        null
    }
    content(
        PhoneRoomNotificationState(
            unreadCount = state.unreadCount,
            firstPrompt = firstPrompt,
        ),
        { viewModel.perform(RoomNotificationsViewEvent.DismissFirstPrompt) },
    )
}
