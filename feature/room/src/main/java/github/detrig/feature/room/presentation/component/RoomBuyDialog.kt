package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.presentation.model.RoomZoneUiModel
import kotlinx.coroutines.delay

@Composable
internal fun RoomBuyDialog(
    zone: RoomZoneUiModel,
    progress: RoomProgress,
    isBuying: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val access = zone.access as? RoomZoneAccess.Buyable ?: return
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(10_000)
        }
    }
    AlertDialog(
        onDismissRequest = { if (!isBuying) onDismiss() },
        title = { Text(stringResource(R.string.room_buy_title, stringResource(zone.appearance.titleRes))) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                Text(stringResource(R.string.room_buy_description))
                MoneyRow(stringResource(R.string.room_price), access.priceRub)
                MoneyRow(stringResource(R.string.room_balance), progress.balanceRub)
                if (zone.canAfford) {
                    MoneyRow(stringResource(R.string.room_balance_after), progress.balanceRub - access.priceRub)
                } else {
                    Text(stringResource(R.string.room_missing_money, access.priceRub - progress.balanceRub))
                }
                Text(
                    stringResource(
                        R.string.room_next_allowance,
                        progress.nextAllowanceAmountRub,
                        allowanceTime(progress.nextAllowanceAtMillis, now),
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = zone.canAfford && !isBuying,
                modifier = Modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget),
            ) {
                Text(
                    if (isBuying) stringResource(R.string.room_buying)
                    else stringResource(R.string.room_confirm, access.priceRub),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isBuying,
                modifier = Modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget),
            ) { Text(stringResource(R.string.room_cancel)) }
        },
    )
}

@Composable
private fun MoneyRow(label: String, amount: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(AppTheme.spacing.md))
        Text(stringResource(R.string.room_money, amount), style = AppTheme.typography.bodyStrong)
    }
}

@Composable
internal fun allowanceTime(atMillis: Long, nowMillis: Long): String {
    val remaining = (atMillis - nowMillis).coerceAtLeast(0L)
    return when {
        remaining == 0L -> stringResource(R.string.room_allowance_due)
        remaining < 60_000L -> stringResource(R.string.room_allowance_soon)
        remaining < 3_600_000L -> stringResource(R.string.room_allowance_minutes, (remaining + 59_999L) / 60_000L)
        remaining < 86_400_000L -> stringResource(R.string.room_allowance_hours, (remaining + 3_599_999L) / 3_600_000L)
        else -> stringResource(R.string.room_allowance_days, (remaining + 86_399_999L) / 86_400_000L)
    }
}
