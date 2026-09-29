package github.detrig.feature.room.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.domain.model.HousePosition
import github.detrig.feature.room.presentation.component.HouseScene
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.component.RoomErrorState
import github.detrig.feature.room.api.RoomPetInteraction
import github.detrig.feature.room.domain.furniture.FurnitureVariant
import github.detrig.feature.room.presentation.model.HouseSurfaceTextures

@Composable
internal fun RoomContent(
    state: RoomViewState,
    onEvent: (RoomViewEvent) -> Unit,
    loadingPosition: HousePosition = HouseLayout.initialPosition(),
    furnitureByPlacement: Map<String, FurnitureVariant> = emptyMap(),
    bathroomFurnitureByPlacement: Map<String, FurnitureVariant> = emptyMap(),
    surfaces: HouseSurfaceTextures = HouseSurfaceTextures.EMPTY,
    modifier: Modifier = Modifier,
    petContent: @Composable (Modifier, RoomPetInteraction) -> Unit = { _, _ -> },
    onPetTap: () -> Unit = {},
    petLookingAround: Boolean = false,
    onMirrorClick: () -> Unit = {},
    onPhoneClick: () -> Unit = {},
    phoneUnreadCount: Int = 0,
    onFoodClick: () -> Unit = {},
    onFeedingClick: () -> Unit = {},
    tableFoodContent: @Composable (Modifier) -> Unit = {},
    active: Boolean = true,
    previewZoneId: String? = null,
    focusObjectId: String? = null,
    isFeedingScene: Boolean = focusObjectId == "dining_table",
    petAnchorObjectId: String? = null,
    petZIndex: Float = 3f,
    petBaselineFraction: Float? = null,
    highlightedObjectIds: Set<String> = emptySet(),
    allowedObjectIds: Set<String> = emptySet(),
    onHighlightedObjectBoundsChanged: (Rect?) -> Unit = {},
    onPreviewReady: (String) -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(AppTheme.colors.house.floor), contentAlignment = Alignment.Center) {
        when (state) {
            RoomViewState.Loading -> HouseScene(
                zones = emptyList(),
                initialPosition = loadingPosition,
                active = false,
                buyingZoneId = null,
                onZoneClick = {},
                onPhoneClick = {},
                onBedClick = {},
                onBathtubClick = {},
                onCalendarClick = {},
                onPiggyBankClick = {},
                onWishBoardClick = {},
                onTestsClick = {},
                onWardrobeClick = {},
                onMirrorClick = {},
                onFoodClick = {},
                onDishesClick = {},
                onFeedingClick = {},
                onSavePosition = {},
                previewZoneId = null,
                focusObjectId = focusObjectId,
                isFeedingScene = isFeedingScene,
                petAnchorObjectId = petAnchorObjectId,
                petZIndex = petZIndex,
                petBaselineFraction = petBaselineFraction,
                highlightedObjectIds = emptySet(),
                allowedObjectIds = emptySet(),
                onHighlightedObjectBoundsChanged = {},
                onPreviewReady = {},
                modifier = Modifier.fillMaxSize(),
                petContent = petContent,
                onPetTap = onPetTap,
                tableFoodContent = tableFoodContent,
                petLookingAround = petLookingAround,
                phoneUnreadCount = phoneUnreadCount,
                furnitureByPlacement = furnitureByPlacement,
                bathroomFurnitureByPlacement = bathroomFurnitureByPlacement,
                surfaces = surfaces,
            )
            RoomViewState.Error -> RoomErrorState(
                onRetry = { onEvent(RoomViewEvent.RetryClicked) }, modifier = Modifier.fillMaxSize())
            is RoomViewState.Content -> {
                HouseScene(
                    state.zones, state.initialPosition, active && !state.sleeping, state.buyingZoneId,
                    nightMode = state.sleeping || state.onboarding?.step in bedtimeSteps,
                    onZoneClick = { onEvent(RoomViewEvent.ZoneClicked(it)) },
                    onPhoneClick = onPhoneClick,
                    onFoodClick = onFoodClick,
                    tableFoodContent = tableFoodContent,
                    onBedClick = { onEvent(RoomViewEvent.BedClicked) },
                    onBathtubClick = { onEvent(RoomViewEvent.BathtubClicked) },
                    onCalendarClick = { onEvent(RoomViewEvent.CalendarClicked) },
                    onPiggyBankClick = { onEvent(RoomViewEvent.PiggyBankClicked) },
                    onWishBoardClick = { onEvent(RoomViewEvent.WishBoardClicked) },
                    onTestsClick = { onEvent(RoomViewEvent.TestsClicked) },
                    onWardrobeClick = { onEvent(RoomViewEvent.WardrobeClicked) },
                    onMirrorClick = onMirrorClick,
                    onDishesClick = { onEvent(RoomViewEvent.DishesClicked) },
                    onFeedingClick = onFeedingClick,
                    onSavePosition = { onEvent(RoomViewEvent.SavePosition(it)) },
                    previewZoneId = previewZoneId,
                    focusObjectId = focusObjectId,
                    isFeedingScene = isFeedingScene,
                    petAnchorObjectId = petAnchorObjectId,
                    petZIndex = petZIndex,
                    petBaselineFraction = petBaselineFraction,
                    highlightedObjectIds = highlightedObjectIds,
                    showInteractiveObjectOutlines = state.showInteractiveObjectOutlines,
                    onPetTap = onPetTap,
                    allowedObjectIds = allowedObjectIds,
                    onHighlightedObjectBoundsChanged = onHighlightedObjectBoundsChanged,
                    onPreviewReady = onPreviewReady,
                    modifier = Modifier.fillMaxSize(),
                    petContent = petContent,
                    petLookingAround = petLookingAround,
                    phoneUnreadCount = phoneUnreadCount,
                    furnitureByPlacement = furnitureByPlacement,
                    bathroomFurnitureByPlacement = bathroomFurnitureByPlacement,
                    surfaces = surfaces,
                )
                if (state.sleeping) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(AppTheme.colors.roomBackground.copy(alpha = 0.55f)),
                    )
                }
            }
        }
    }
}

private val bedtimeSteps = setOf(
    github.detrig.feature.room.api.FirstRunOnboardingStep.BEDTIME_LATE,
    github.detrig.feature.room.api.FirstRunOnboardingStep.BEDTIME_GUIDANCE,
    github.detrig.feature.room.api.FirstRunOnboardingStep.WAITING_FOR_BED,
)

@Preview(name = "Room backdrop", widthDp = 360, heightDp = 760)
@Composable
private fun RoomContentPreview() {
    FinPetTheme {
        RoomContent(
            state = github.detrig.feature.room.presentation.preview.RoomPreviewData.state,
            onEvent = {},
            loadingPosition = HouseLayout.initialPosition(),
        )
    }
}
