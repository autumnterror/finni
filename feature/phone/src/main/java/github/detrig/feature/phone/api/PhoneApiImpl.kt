package github.detrig.feature.phone.api

import androidx.compose.runtime.Composable
import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.feature.phone.domain.MessagesCoordinator
import github.detrig.feature.phone.navigation.PhoneRoute
import github.detrig.feature.phone.navigation.PhoneRouter
import github.detrig.feature.phone.presentation.PhoneScreen
import github.detrig.feature.phone.presentation.PhoneRoomNotifications
import kotlinx.coroutines.CoroutineScope

internal class PhoneApiImpl(
    private val router: PhoneRouter,
    private val coordinator: MessagesCoordinator,
    private val applicationScope: CoroutineScope,
) : PhoneApi {
    override fun initialize() = coordinator.start(applicationScope)

    override fun open() = router.open()

    override fun openMessages() = router.openApp(MESSAGES_APP_ID)

    override fun entries(): EntryHostProviderInstaller = {
        composable<PhoneRoute.Home> { PhoneScreen(PhoneRoute.Home) }
        composable<PhoneRoute.App> { route -> PhoneScreen(route) }
    }

    @Composable
    override fun RoomNotifications(
        content: @Composable (PhoneRoomNotificationState, () -> Unit) -> Unit,
    ) = PhoneRoomNotifications(content)
}

internal const val MESSAGES_APP_ID = "messages"
