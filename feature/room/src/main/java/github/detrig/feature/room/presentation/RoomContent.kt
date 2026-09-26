package github.detrig.feature.room.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.room.presentation.component.HouseScene
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.component.RoomErrorState
import github.detrig.feature.room.api.RoomPetInteraction

@Composable
internal fun RoomContent(
    state: RoomViewState,
    onEvent: (RoomViewEvent) -> Unit,
    modifier: Modifier = Modifier,
    petContent: @Composable (Modifier, RoomPetInteraction) -> Unit = { _, _ -> },
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
                initialPosition = HouseLayout.initialPosition(),
                active = false,
                buyingZoneId = null,
                onZoneClick = {},
                onPhoneClick = {},
                onBedClick = {},
                onCalendarClick = {},
                onPiggyBankClick = {},
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
                tableFoodContent = tableFoodContent,
                petLookingAround = petLookingAround,
                phoneUnreadCount = phoneUnreadCount,
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
                    onCalendarClick = { onEvent(RoomViewEvent.CalendarClicked) },
                    onPiggyBankClick = { onEvent(RoomViewEvent.PiggyBankClicked) },
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
                    allowedObjectIds = allowedObjectIds,
                    onHighlightedObjectBoundsChanged = onHighlightedObjectBoundsChanged,
                    onPreviewReady = onPreviewReady,
                    modifier = Modifier.fillMaxSize(),
                    petContent = petContent,
                    petLookingAround = petLookingAround,
                    phoneUnreadCount = phoneUnreadCount,
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
