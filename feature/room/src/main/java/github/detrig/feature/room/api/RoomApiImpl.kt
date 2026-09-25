package github.detrig.feature.room.api

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.feature.room.presentation.RoomScreen
import github.detrig.feature.room.navigation.RoomPreviewRequests
import github.detrig.feature.room.presentation.component.RoomSpriteCache
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.savings.api.SavingsGoalPurchaseResult
import github.detrig.feature.room.domain.interactor.PurchaseSavingsGoalInteractor

internal class RoomApiImpl(
    private val requests: RoomPreviewRequests,
    private val resources: Resources,
    private val purchaseSavingsGoal: PurchaseSavingsGoalInteractor,
    override val firstRunGuide: FirstRunGuideApi,
) : RoomApi {
    init {
        RoomSpriteCache.preload(resources)
    }

    override suspend fun preloadAssets() {
        RoomSpriteCache.awaitPreloaded(resources)
    }

    override fun requestZonePreview(zoneId: String) = requests.request(zoneId)
    override suspend fun purchaseSavingsGoal(goal: SavingsGoal): SavingsGoalPurchaseResult =
        purchaseSavingsGoal(goal)
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
        showHud: Boolean,
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
            showHud = showHud,
            previewRequests = requests,
            focusObjectId = focusObjectId,
            petAnchorObjectId = petAnchorObjectId,
            petZIndex = petZIndex,
            petBaselineFraction = petBaselineFraction,
        )
    }
}
