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

@Composable
internal fun RoomBuyDialog(
    zone: RoomZoneUiModel,
    progress: RoomProgress,
    isBuying: Boolean,
    onConfirm: () -> Unit,
    isSavingGoal: Boolean,
    onSaveAsGoal: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val access = zone.access as? RoomZoneAccess.Buyable ?: return
    val title = stringResource(zone.appearance.titleRes)
    AlertDialog(
        onDismissRequest = { if (!isBuying) onDismiss() },
        title = { Text(stringResource(R.string.room_buy_title, title)) },
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
                        progress.daysUntilAllowance,
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
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                TextButton(
                    onClick = { onSaveAsGoal(title) },
                    enabled = !isBuying && !isSavingGoal,
                    modifier = Modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget),
                ) {
                    Text(if (isSavingGoal) stringResource(R.string.room_goal_saving)
                        else stringResource(R.string.room_save_as_goal, access.priceRub))
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !isBuying && !isSavingGoal,
                    modifier = Modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget),
                ) { Text(stringResource(R.string.room_cancel)) }
            }
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
