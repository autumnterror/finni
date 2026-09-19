package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.component.FinPetMoneyAmount
import github.detrig.designsystem.component.FinPetOutlinedButton
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
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
    FinPetModalDialog(
        title = stringResource(R.string.parent_help_title),
        onDismissRequest = onDismiss,
        dismissEnabled = !isRequesting,
        modifier = Modifier.testTag("parent_help_dialog"),
        actions = {
            FinPetOutlinedButton(
                text = stringResource(R.string.parent_help_close),
                onClick = onDismiss,
                enabled = !isRequesting,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontOutlinedStyle(),
            )
        },
    ) {
        val active = state.activeHelp
        if (active == null) {
            FinPetModalSection(
                modifier = Modifier.fillMaxWidth(),
                tone = FinPetModalSectionTone.Highlighted,
            ) {
                Text(
                    text = stringResource(R.string.parent_help_description),
                    modifier = Modifier.padding(AppTheme.spacing.md),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.storefront.onSurface,
                )
            }
            state.offers.forEach { offer ->
                ParentHelpOfferCard(offer, isRequesting, onOfferSelected)
            }
        } else {
            ActiveParentHelpCard(active)
        }
    }
}

@Composable
private fun ParentHelpOfferCard(
    offer: ParentHelpOffer,
    isRequesting: Boolean,
    onOfferSelected: (String) -> Unit,
) {
    FinPetModalSection(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Text(
                stringResource(R.string.parent_help_offer_weeks, offer.repaymentWeeks),
                style = AppTheme.typography.sectionTitle,
                color = AppTheme.colors.storefront.onSurface,
            )
            ParentHelpMoneyRow(R.string.parent_help_get_now, offer.receivedRub)
            ParentHelpMoneyRow(R.string.parent_help_return_total, offer.totalRepaymentRub)
            Text(stringResource(R.string.parent_help_extra, offer.extraRub), style = AppTheme.typography.caption)
            Text(stringResource(R.string.parent_help_weekly, offer.weeklyRepaymentRub), style = AppTheme.typography.caption)
            FinPetButton(
                text = stringResource(R.string.parent_help_choose, offer.receivedRub),
                onClick = { onOfferSelected(offer.id) },
                enabled = !isRequesting,
                modifier = Modifier.fillMaxWidth().testTag("parent_help_offer_${offer.id}"),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
        }
    }
}

@Composable
private fun ActiveParentHelpCard(help: ParentHelpState) {
    FinPetModalSection(
        modifier = Modifier.fillMaxWidth(),
        tone = FinPetModalSectionTone.Highlighted,
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Text(stringResource(R.string.parent_help_active), style = AppTheme.typography.body)
            ParentHelpMoneyRow(R.string.parent_help_remaining, help.remainingRub)
            Text(
                stringResource(R.string.parent_help_active_schedule, help.nextPaymentRub, help.paymentsRemaining),
                style = AppTheme.typography.bodyStrong,
            )
        }
    }
}

@Composable
private fun ParentHelpMoneyRow(labelRes: Int, amountRub: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(labelRes), modifier = Modifier.weight(1f), style = AppTheme.typography.body)
        Spacer(Modifier.width(AppTheme.spacing.md))
        FinPetMoneyAmount(amountRub.toString())
    }
}

@Composable
internal fun AllowanceReceiptDialog(notice: AllowanceNoticeState, onDismiss: () -> Unit) {
    FinPetModalDialog(
        title = stringResource(R.string.allowance_notice_title),
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("weekly_allowance_notice"),
        actions = {
            FinPetButton(
                text = stringResource(R.string.allowance_notice_continue),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
        },
    ) {
        FinPetMoneyAmount(notice.grossRub.toString())
        Text(stringResource(R.string.allowance_notice_received, notice.grossRub), style = AppTheme.typography.body)
        if (notice.parentHelpRepaidRub > 0) {
            FinPetModalSection(
                modifier = Modifier.fillMaxWidth(),
                tone = FinPetModalSectionTone.Warning,
            ) {
                Column(
                    modifier = Modifier.padding(AppTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                ) {
                    Text(
                        stringResource(R.string.allowance_notice_parent_help, notice.parentHelpRepaidRub),
                    )
                    Text(
                        stringResource(R.string.allowance_notice_after_help, notice.receivedRub),
                        style = AppTheme.typography.bodyStrong,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ZeroBalanceHelpDialog(notice: ZeroBalanceHelpNoticeState, onDismiss: () -> Unit) {
    FinPetModalDialog(
        title = stringResource(R.string.zero_balance_help_title),
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("zero_balance_help_notice"),
        actions = {
            FinPetButton(
                text = stringResource(R.string.zero_balance_help_continue),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
        },
    ) {
        FinPetModalSection(
            modifier = Modifier.fillMaxWidth(),
            tone = FinPetModalSectionTone.Highlighted,
        ) {
            Column(
                modifier = Modifier.padding(AppTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                FinPetMoneyAmount(notice.amountRub.toString())
                Text(
                    stringResource(R.string.zero_balance_help_description, notice.amountRub),
                    style = AppTheme.typography.body,
                )
            }
        }
    }
}

@Preview(name = "Помощь от родителей", widthDp = 360, heightDp = 760, showBackground = true)
@Composable
private fun ParentHelpDialogPreview() {
    FinPetTheme {
        ParentHelpDialog(
            state = ParentHelpDialogState(
                offers = listOf(
                    ParentHelpOffer("two-weeks", 600, 2, 720),
                    ParentHelpOffer("three-weeks", 600, 3, 690),
                ),
                activeHelp = null,
            ),
            isRequesting = false,
            onOfferSelected = {},
            onDismiss = {},
        )
    }
}
