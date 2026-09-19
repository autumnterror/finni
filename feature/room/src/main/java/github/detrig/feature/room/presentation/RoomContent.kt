package github.detrig.feature.room.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.presentation.component.HouseScene
import github.detrig.designsystem.component.FinPetCard
import androidx.compose.ui.platform.testTag
import github.detrig.feature.room.presentation.component.RoomErrorState

@Composable
internal fun RoomContent(
    state: RoomViewState,
    onEvent: (RoomViewEvent) -> Unit,
    modifier: Modifier = Modifier,
    petContent: @Composable (Modifier) -> Unit = {},
    onMirrorClick: () -> Unit = {},
    active: Boolean = true,
    previewZoneId: String? = null,
    onPreviewReady: (String) -> Unit = {},
) {
    Box(modifier.fillMaxSize().background(AppTheme.colors.house.floor), contentAlignment = Alignment.Center) {
        when (state) {
            RoomViewState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(AppTheme.spacing.md))
                Text(stringResource(R.string.room_loading), color = AppTheme.colors.textPrimary)
            }
            RoomViewState.Error -> RoomErrorState(
                onRetry = { onEvent(RoomViewEvent.RetryClicked) }, modifier = Modifier.fillMaxSize())
            is RoomViewState.Content -> {
                HouseScene(
                    state.zones, state.initialPosition, active && !state.sleeping, state.buyingZoneId,
                    onZoneClick = { onEvent(RoomViewEvent.ZoneClicked(it)) },
                    onMarketClick = { onEvent(RoomViewEvent.MarketClicked) },
                    onBedClick = { onEvent(RoomViewEvent.BedClicked) },
                    onCalendarClick = { onEvent(RoomViewEvent.CalendarClicked) },
                    onPiggyBankClick = { onEvent(RoomViewEvent.PiggyBankClicked) },
                    onTestsClick = { onEvent(RoomViewEvent.TestsClicked) },
                    onWardrobeClick = { onEvent(RoomViewEvent.WardrobeClicked) },
                    onMirrorClick = onMirrorClick,
                    onFoodClick = { onEvent(RoomViewEvent.FoodClicked) },
                    onDishesClick = { onEvent(RoomViewEvent.DishesClicked) },
                    onFeedingClick = { onEvent(RoomViewEvent.FeedingClicked) },
                    onSavePosition = { onEvent(RoomViewEvent.SavePosition(it)) },
                    previewZoneId = previewZoneId,
                    onPreviewReady = onPreviewReady,
                    modifier = Modifier.fillMaxSize(),
                    petContent = petContent,
                )
                if (state.sleeping) {
                    FinPetCard(Modifier.align(Alignment.Center).testTag("room_sleeping")) {
                        Text(stringResource(R.string.room_sleeping),
                            modifier = Modifier.padding(AppTheme.spacing.lg),
                            style = AppTheme.typography.bodyStrong)
                    }
                }
            }
        }
    }
}
