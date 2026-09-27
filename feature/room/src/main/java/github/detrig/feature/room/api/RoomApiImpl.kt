package github.detrig.feature.room.api

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.feature.room.presentation.RoomScreen
import github.detrig.feature.room.navigation.RoomPreviewRequests
import github.detrig.feature.room.presentation.component.RoomSpriteCache
import github.detrig.feature.room.presentation.component.RoomSurfaceCache
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.savings.api.SavingsGoalPurchaseResult
import github.detrig.feature.room.domain.interactor.PurchaseSavingsGoalInteractor
import github.detrig.feature.room.domain.furniture.FurnitureCatalog
import github.detrig.feature.room.domain.furniture.FurnitureStore
import github.detrig.feature.room.domain.surface.SurfaceCatalog
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.room.presentation.furniture.FurnitureStoreScreen
import github.detrig.feature.room.domain.model.ParentHelpPromptRepository

internal class RoomApiImpl(
    private val requests: RoomPreviewRequests,
    private val resources: Resources,
    private val purchaseSavingsGoal: PurchaseSavingsGoalInteractor,
    private val repository: github.detrig.feature.room.domain.repository.RoomRepository,
    override val firstRunGuide: FirstRunGuideApi,
    private val furnitureCatalog: FurnitureCatalog,
    private val surfaceCatalog: SurfaceCatalog,
    private val furnitureStore: FurnitureStore,
    private val economyApi: EconomyApi,
    private val parentHelpPromptRepository: ParentHelpPromptRepository,
    private val wishArtwork: RoomWishArtwork,
) : RoomApi {
    init {
        RoomSpriteCache.preload(resources)
        RoomSpriteCache.preloadVariants(resources, furnitureCatalog.bySlot["room_lamp"].orEmpty())
        RoomSpriteCache.preloadVariants(resources,
            furnitureStore.state.value.equipped.values.mapNotNull(furnitureCatalog.byId::get)
                .filter { it.slotId !in furnitureCatalog.slots.filter { slot -> slot.roomId == "bathroom" }.map { slot -> slot.id } })
        RoomSurfaceCache.preloadThumbnails(resources, surfaceCatalog)
    }

    override suspend fun preloadAssets() {
        RoomSpriteCache.awaitPreloaded(resources)
        RoomSurfaceCache.awaitFull(resources,
            furnitureStore.state.value.equippedSurfaces.values.mapNotNull(surfaceCatalog.byId::get))
    }

    override fun requestZonePreview(zoneId: String) = requests.request(zoneId)
    override fun notifyParentHelpSettled() = parentHelpPromptRepository.resetAfterParentHelpSettlement()
    override suspend fun resetProgress(skipOnboarding: Boolean) {
        firstRunGuide.reset(skipOnboarding)
        parentHelpPromptRepository.resetAfterParentHelpSettlement()
        repository.initialize()
    }
    override suspend fun purchaseSavingsGoal(goal: SavingsGoal): SavingsGoalPurchaseResult =
        purchaseSavingsGoal(goal)
    @Composable
    override fun InteriorStore(onBack: () -> Unit, modifier: Modifier) {
        FurnitureStoreScreen(furnitureCatalog, surfaceCatalog, furnitureStore, economyApi, onBack, modifier)
    }
    @Composable
    override fun Content(
        modifier: Modifier,
        petName: String,
        canShowDialogs: Boolean,
        petContent: @Composable (Modifier, RoomPetInteraction) -> Unit,
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
            wishArtwork = wishArtwork,
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
