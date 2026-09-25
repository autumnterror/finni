package github.detrig.feature.room.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import github.detrig.feature.room.R
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.presentation.model.RoomZoneAppearance
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
    val controlsEnabled = !isBuying && !isSavingGoal

    FinPetModalDialog(
        title = stringResource(R.string.room_buy_title, title),
        onDismissRequest = onDismiss,
        dismissEnabled = !isBuying,
        actions = {
            FinPetButton(
                text = if (isBuying) {
                    stringResource(R.string.room_buying)
                } else {
                    stringResource(R.string.room_confirm)
                },
                onClick = onConfirm,
                enabled = zone.canAfford && !isBuying,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
            FinPetOutlinedButton(
                text = if (isSavingGoal) {
                    stringResource(R.string.room_goal_saving)
                } else {
                    stringResource(R.string.room_save_as_goal)
                },
                onClick = { onSaveAsGoal(title) },
                enabled = controlsEnabled,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontOutlinedStyle(),
            )
            FinPetOutlinedButton(
                text = stringResource(R.string.room_cancel),
                onClick = onDismiss,
                enabled = controlsEnabled,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontOutlinedStyle(),
            )
        },
    ) {
        MiniGameGameplayPreview(zone.appearance)
        Text(stringResource(R.string.room_buy_description), style = AppTheme.typography.body)
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            MoneyRow(stringResource(R.string.room_price), access.priceRub)
        }
        if (!zone.canAfford) {
            FinPetModalSection(
                modifier = Modifier.fillMaxWidth(),
                tone = FinPetModalSectionTone.Warning,
            ) {
                Text(
                    text = stringResource(R.string.room_missing_money, access.priceRub - progress.balanceRub),
                    modifier = Modifier.padding(AppTheme.spacing.md),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.storefront.onSurface,
                )
            }
        }
        Text(
            stringResource(R.string.room_next_allowance, progress.daysUntilAllowance),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textSecondary,
        )
    }
}

@Composable
internal fun MiniGameGameplayPreview(
    appearance: RoomZoneAppearance,
    modifier: Modifier = Modifier,
) {
    val imageRes = appearance.gameplayPreviewRes() ?: return
    Image(
        painter = painterResource(imageRes),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(AppTheme.shapes.storefrontControl),
        contentScale = ContentScale.Crop,
    )
}

@DrawableRes
private fun RoomZoneAppearance.gameplayPreviewRes(): Int? = when (this) {
    RoomZoneAppearance.DRAWING -> R.drawable.minigame_preview_drawing
    RoomZoneAppearance.MUSIC -> R.drawable.minigame_preview_music
    RoomZoneAppearance.FISHING -> R.drawable.minigame_preview_fishing
    else -> null
}

@Preview(name = "Покупка игровой зоны", widthDp = 360, heightDp = 680, showBackground = true)
@Composable
private fun RoomBuyDialogPreview() {
    FinPetTheme {
        RoomBuyDialog(
            zone = RoomZoneUiModel(
                id = "fishing",
                gameId = "fishing",
                appearance = github.detrig.feature.room.presentation.model.RoomZoneAppearance.FISHING,
                access = RoomZoneAccess.Buyable(priceRub = 450),
                canAfford = true,
            ),
            progress = RoomProgress(
                balanceRub = 800,
                playerLevel = 2,
                ownedZoneIds = emptySet(),
                absoluteDay = 8,
                weekNumber = 2,
                dayOfWeek = 1,
                daysUntilAllowance = 6,
            ),
            isBuying = false,
            onConfirm = {},
            isSavingGoal = false,
            onSaveAsGoal = {},
            onDismiss = {},
        )
    }
}

@Composable
private fun MoneyRow(label: String, amount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = AppTheme.typography.bodyStrong)
        Spacer(Modifier.width(AppTheme.spacing.md))
        FinPetMoneyAmount(amount.toString())
    }
}
