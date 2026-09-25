package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import github.detrig.designsystem.component.FinPetCoinText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.component.FinPetMoneyAmount
import github.detrig.designsystem.component.FinPetCoinIcon
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpState
import github.detrig.feature.room.R
import github.detrig.feature.room.presentation.AllowanceNoticeState
import github.detrig.feature.room.presentation.ParentHelpDialogState

@Composable
fun ParentHelpDialog(
    state: ParentHelpDialogState,
    isRequesting: Boolean,
    onOfferSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onPayOffNow: (() -> Unit)? = null,
) {
    val canDismiss = !isRequesting
    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val maxDialogHeight = LocalConfiguration.current.screenHeightDp.dp * 0.94f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
                    .heightIn(max = maxDialogHeight)
                    .testTag("parent_help_dialog"),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp)
                        .heightIn(max = maxDialogHeight - 44.dp)
                        .shadow(
                            elevation = AppTheme.elevation.medium,
                            shape = AppTheme.shapes.storefrontControl,
                            ambientColor = AppTheme.colors.storefront.shadow,
                            spotColor = AppTheme.colors.storefront.shadow,
                        ),
                    shape = AppTheme.shapes.storefrontControl,
                    color = AppTheme.colors.storefront.surface,
                    contentColor = AppTheme.colors.storefront.onSurface,
                    border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(AppTheme.spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                    ) {
                        ParentHelpHeader(
                            dismissEnabled = canDismiss,
                            onDismiss = onDismiss,
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                        ) {
                            val active = state.activeHelp
                            if (active == null) {
                                ParentHelpDescription()
                                state.offers.forEach { offer ->
                                    ParentHelpOfferCard(offer, isRequesting, onOfferSelected)
                                }
                            } else {
                                ActiveParentHelpCard(
                                    help = active,
                                    availableRub = state.availableRub,
                                    isRequesting = isRequesting,
                                    onPayOffNow = onPayOffNow,
                                )
                            }
                        }
                    }
                }
                Image(
                    painter = painterResource(R.drawable.parent_help_hamster),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = AppTheme.spacing.lg)
                        .size(width = 126.dp, height = 92.dp)
                        .zIndex(1f),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun ParentHelpHeader(
    dismissEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 34.dp),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.parent_help_title),
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.screenTitle,
            color = AppTheme.colors.storefront.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        val closeLabel = stringResource(R.string.parent_help_close)
        FinPetButton(
            text = "×",
            onClick = onDismiss,
            enabled = dismissEnabled,
            modifier = Modifier
                .size(AppTheme.sizes.minimumTouchTarget)
                .semantics { contentDescription = closeLabel },
            style = FinPetButtonDefaults.storefrontOutlinedStyle().copy(
                minHeight = AppTheme.sizes.minimumTouchTarget,
                contentPadding = PaddingValues(AppTheme.spacing.none),
                textStyle = AppTheme.typography.screenTitle,
            ),
        )
    }
}

@Composable
private fun ParentHelpDescription() {
    FinPetModalSection(
        modifier = Modifier.fillMaxWidth(),
        tone = FinPetModalSectionTone.Highlighted,
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.parent_help_heart_hands),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(R.string.parent_help_description),
                modifier = Modifier.weight(1f),
                style = AppTheme.typography.body,
                color = AppTheme.colors.storefront.onSurface,
            )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.parent_help_offer_weeks, offer.repaymentWeeks),
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.sectionTitle,
                    color = AppTheme.colors.storefront.onSurface,
                )
                Image(
                    painter = painterResource(R.drawable.parent_help_calendar),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            ParentHelpMoneyRow(R.string.parent_help_get_now, offer.receivedRub)
            ParentHelpMoneyRow(R.string.parent_help_return_total, offer.totalRepaymentRub)
            Text(stringResource(R.string.parent_help_extra, offer.extraRub), style = AppTheme.typography.caption)
            Text(stringResource(R.string.parent_help_rate, offer.ratePercent), style = AppTheme.typography.caption)
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
private fun ActiveParentHelpCard(
    help: ParentHelpState,
    availableRub: Long,
    isRequesting: Boolean,
    onPayOffNow: (() -> Unit)?,
) {
    FinPetModalSection(
        modifier = Modifier.fillMaxWidth(),
        tone = FinPetModalSectionTone.Highlighted,
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FinPetCoinIcon(size = 72.dp)
                Text(
                    text = stringResource(R.string.parent_help_active),
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.sectionTitle,
                )
            }
            ParentHelpMoneyRow(R.string.parent_help_remaining, help.remainingRub)
            Text(
                stringResource(R.string.parent_help_active_schedule, help.nextPaymentRub, help.paymentsRemaining),
                style = AppTheme.typography.bodyStrong,
            )
            if (onPayOffNow != null) {
                if (availableRub < help.remainingRub) {
                    Text(
                        stringResource(
                            R.string.parent_help_payoff_unavailable,
                            help.remainingRub,
                            availableRub,
                        ),
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.textSecondary,
                    )
                }
                FinPetButton(
                    text = stringResource(R.string.parent_help_payoff_now, help.remainingRub),
                    onClick = onPayOffNow,
                    enabled = !isRequesting && availableRub >= help.remainingRub,
                    modifier = Modifier.fillMaxWidth().testTag("parent_help_payoff_now"),
                    style = FinPetButtonDefaults.storefrontPrimaryStyle(),
                )
            }
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
internal fun AllowanceReceiptDialog(
    notice: AllowanceNoticeState,
    firstRun: Boolean = false,
    onDismiss: () -> Unit,
) {
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
        Text(
            stringResource(
                if (firstRun) R.string.allowance_notice_first_received else R.string.allowance_notice_received,
                notice.grossRub,
            ),
            style = AppTheme.typography.body,
        )
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
internal fun EarlyWeekParentHelpDialog(onDismiss: () -> Unit) {
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
                Text(
                    stringResource(R.string.zero_balance_help_description),
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
