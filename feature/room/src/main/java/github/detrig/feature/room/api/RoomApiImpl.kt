package github.detrig.feature.room.api

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.feature.room.presentation.RoomScreen
import github.detrig.feature.room.navigation.RoomPreviewRequests
import github.detrig.feature.room.presentation.component.RoomSpriteCache

internal class RoomApiImpl(
    private val requests: RoomPreviewRequests,
    private val resources: Resources,
) : RoomApi {
    init {
        RoomSpriteCache.preload(resources)
    }

    override suspend fun preloadAssets() {
        RoomSpriteCache.awaitPreloaded(resources)
    }

    override fun requestZonePreview(zoneId: String) = requests.request(zoneId)
    @Composable
    override fun Content(
        modifier: Modifier,
        petName: String,
        canShowDialogs: Boolean,
        petContent: @Composable (Modifier) -> Unit,
        petPortrait: @Composable (Modifier) -> Unit,
        onMirrorClick: () -> Unit,
        onPhoneClick: () -> Unit,
        phoneUnreadCount: Int,
        phoneNotificationPrompt: String?,
        onPhonePromptOpen: () -> Unit,
        onPhonePromptDismiss: () -> Unit,
        onFoodClick: () -> Unit,
        onFeedingClick: () -> Unit,
        tableFoodContent: @Composable (Modifier) -> Unit,
        active: Boolean,
        focusObjectId: String?,
        petAnchorObjectId: String?,
        petZIndex: Float,
        petBaselineFraction: Float?,
    ) {
        RoomScreen(
            modifier = modifier,
            petName = petName,
            canShowDialogs = canShowDialogs,
            petContent = petContent,
            petPortrait = petPortrait,
            onMirrorClick = onMirrorClick,
            onPhoneClick = onPhoneClick,
            phoneUnreadCount = phoneUnreadCount,
            phoneNotificationPrompt = phoneNotificationPrompt,
            onPhonePromptOpen = onPhonePromptOpen,
            onPhonePromptDismiss = onPhonePromptDismiss,
            onFoodClick = onFoodClick,
            onFeedingClick = onFeedingClick,
            tableFoodContent = tableFoodContent,
            externalActive = active,
            previewRequests = requests,
            focusObjectId = focusObjectId,
            petAnchorObjectId = petAnchorObjectId,
            petZIndex = petZIndex,
            petBaselineFraction = petBaselineFraction,
        )
    }
}
