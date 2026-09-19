package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.HouseObjectArt
import github.detrig.feature.room.presentation.model.RoomZoneUiModel

@Composable
internal fun RoomZonePlace(
    zone: RoomZoneUiModel,
    art: HouseObjectArt,
    artWidth: Dp,
    artHeight: Dp,
    isBuying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    renderArtwork: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val description = when (val access = zone.access) {
        RoomZoneAccess.Open -> stringResource(R.string.room_open)
        is RoomZoneAccess.Unavailable -> stringResource(R.string.room_from_level, access.requiredLevel)
        is RoomZoneAccess.Buyable -> stringResource(R.string.room_buy_price, access.priceRub)
    }
    val title = stringResource(zone.appearance.titleRes)
    Box(modifier.testTag("room_zone_${zone.id}")
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) {
            contentDescription = title
            stateDescription = description
        }, contentAlignment = Alignment.Center,
    ) {
        if (renderArtwork) {
            when (zone.access) {
                is RoomZoneAccess.Unavailable -> RoomZoneFog(Modifier.size(artWidth, artHeight))
                RoomZoneAccess.Open, is RoomZoneAccess.Buyable -> {
                    HouseObjectArtwork(art, Modifier.size(artWidth, artHeight))
                    if (zone.access is RoomZoneAccess.Buyable) {
                        RoomZoneLock(Modifier.size(maxOf(artWidth * HouseLayout.LOCK_SIZE_FRACTION, AppTheme.sizes.iconMedium)))
                    }
                }
            }
        }
        if (isBuying) {
            CircularProgressIndicator(color = AppTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.BottomCenter).padding(AppTheme.spacing.sm).size(AppTheme.sizes.iconMedium))
        }
    }
}
