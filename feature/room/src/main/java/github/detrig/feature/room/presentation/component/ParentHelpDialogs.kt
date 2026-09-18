package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpState
import github.detrig.feature.room.R
import github.detrig.feature.room.presentation.AllowanceNoticeState
import github.detrig.feature.room.presentation.ParentHelpDialogState
import github.detrig.feature.room.presentation.ZeroBalanceHelpNoticeState

@Composable
internal fun ParentHelpDialog(
    state: ParentHelpDialogState,
    isRequesting: Boolean,
    onOfferSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isRequesting) onDismiss() },
        modifier = Modifier.testTag("parent_help_dialog"),
        title = { Text(stringResource(R.string.parent_help_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                val active = state.activeHelp
                if (active == null) {
                    Text(stringResource(R.string.parent_help_description), style = AppTheme.typography.body)
                    state.offers.forEach { offer ->
                        ParentHelpOfferCard(offer, isRequesting, onOfferSelected)
                    }
                } else {
                    ActiveParentHelpCard(active)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRequesting,
                modifier = Modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget),
            ) { Text(stringResource(R.string.parent_help_close)) }
        },
    )
}

@Composable
private fun ParentHelpOfferCard(
    offer: ParentHelpOffer,
    isRequesting: Boolean,
    onOfferSelected: (String) -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            Text(stringResource(R.string.parent_help_offer_weeks, offer.repaymentWeeks), style = AppTheme.typography.bodyStrong)
            ParentHelpMoneyRow(R.string.parent_help_get_now, offer.receivedRub)
            ParentHelpMoneyRow(R.string.parent_help_return_total, offer.totalRepaymentRub)
            Text(stringResource(R.string.parent_help_extra, offer.extraRub), style = AppTheme.typography.caption)
            Text(stringResource(R.string.parent_help_weekly, offer.weeklyRepaymentRub), style = AppTheme.typography.caption)
            Button(
                onClick = { onOfferSelected(offer.id) },
                enabled = !isRequesting,
                modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.minimumTouchTarget)
                    .testTag("parent_help_offer_${offer.id}"),
            ) { Text(stringResource(R.string.parent_help_choose, offer.receivedRub)) }
        }
    }
}

@Composable
private fun ActiveParentHelpCard(help: ParentHelpState) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Text(stringResource(R.string.parent_help_active), style = AppTheme.typography.body)
        ParentHelpMoneyRow(R.string.parent_help_remaining, help.remainingRub)
        Text(
            stringResource(R.string.parent_help_active_schedule, help.nextPaymentRub, help.paymentsRemaining),
            style = AppTheme.typography.bodyStrong,
        )
    }
}

@Composable
private fun ParentHelpMoneyRow(labelRes: Int, amountRub: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(labelRes), modifier = Modifier.weight(1f))
        Spacer(Modifier.width(AppTheme.spacing.md))
        Text(stringResource(R.string.parent_help_money, amountRub), style = AppTheme.typography.bodyStrong)
    }
}

@Composable
internal fun AllowanceReceiptDialog(notice: AllowanceNoticeState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("weekly_allowance_notice"),
        title = { Text(stringResource(R.string.allowance_notice_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                Text(stringResource(R.string.allowance_notice_received, notice.grossRub))
                if (notice.parentHelpRepaidRub > 0) {
                    Text(stringResource(R.string.allowance_notice_parent_help, notice.parentHelpRepaidRub))
                    Text(stringResource(R.string.allowance_notice_after_help, notice.receivedRub),
                        style = AppTheme.typography.bodyStrong)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget)) {
                Text(stringResource(R.string.allowance_notice_continue))
            }
        },
    )
}

@Composable
internal fun ZeroBalanceHelpDialog(notice: ZeroBalanceHelpNoticeState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("zero_balance_help_notice"),
        title = { Text(stringResource(R.string.zero_balance_help_title)) },
        text = { Text(stringResource(R.string.zero_balance_help_description, notice.amountRub)) },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget)) {
                Text(stringResource(R.string.zero_balance_help_continue))
            }
        },
    )
}
