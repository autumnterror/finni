package github.detrig.feature.phone.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller

interface PhoneApi {
    /** Starts the process-wide game-day message scheduler. Safe to call repeatedly. */
    fun initialize()
    fun open()
    fun openMessages()
    fun entries(): EntryHostProviderInstaller

    @Composable
    fun RoomNotifications(
        content: @Composable (
            state: PhoneRoomNotificationState,
            dismissFirstPrompt: () -> Unit,
        ) -> Unit,
    )
}

@Immutable
data class PhoneRoomNotificationState(
    val unreadCount: Int = 0,
    val firstPrompt: String? = null,
)
