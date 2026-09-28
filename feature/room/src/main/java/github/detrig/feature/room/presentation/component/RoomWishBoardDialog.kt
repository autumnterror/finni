package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetCoinText as Text
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.component.FinPetStorefrontProgressIndicator
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.economy.domain.SavingsGoalProgress
import github.detrig.feature.gamestate.domain.model.PetHappinessRules
import github.detrig.feature.gamestate.domain.model.PetWishHappinessRewards
import github.detrig.feature.room.R
import github.detrig.feature.room.api.RoomWishArtwork
import github.detrig.feature.room.domain.model.RoomImpulseWish

@Composable
internal fun RoomWishBoardDialog(
    wishes: List<RoomImpulseWish>,
    absoluteDay: Long,
    savingsGoal: SavingsGoalProgress?,
    wishArtwork: RoomWishArtwork?,
    onDismiss: () -> Unit,
) {
    FinPetModalDialog(
        title = stringResource(R.string.wish_board_title),
        onDismissRequest = onDismiss,
        actions = {},
    ) {
        if (wishes.isEmpty() && savingsGoal == null) {
            FinPetModalSection(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.wish_board_empty),
                    modifier = Modifier.padding(AppTheme.spacing.md),
                    style = AppTheme.typography.body,
                )
            }
        }
        wishes.forEach { WishCard(it, absoluteDay, wishArtwork) }
        savingsGoal?.let { SavingsWishCard(it) }
    }
}

@Composable
private fun WishCard(wish: RoomImpulseWish, absoluteDay: Long, wishArtwork: RoomWishArtwork?) {
    val emoji = when (wish.kind) {
        RoomImpulseWish.Kind.GROCERY -> "🍎"
        RoomImpulseWish.Kind.MINI_GAME -> "🎮"
        RoomImpulseWish.Kind.CLOTHING -> "👕"
        RoomImpulseWish.Kind.TOY -> "🧸"
        RoomImpulseWish.Kind.SAVINGS_TOP_UP -> "🐷"
        RoomImpulseWish.Kind.FREE -> "✨"
    }
    val title = when {
        wish.isPurchasable -> stringResource(R.string.wish_board_buy_title, wish.productTitle)
        wish.kind == RoomImpulseWish.Kind.MINI_GAME -> stringResource(R.string.wish_board_play_title, wish.productTitle)
        else -> wish.productTitle
    }
    val duration = wish.daysRemaining(absoluteDay)?.let { days ->
        if (days == 1) stringResource(R.string.wish_board_expires_today)
        else pluralStringResource(R.plurals.wish_board_remaining_days, days, days)
    } ?: stringResource(R.string.wish_board_goal_duration)
    val bonus = when (wish.kind) {
        RoomImpulseWish.Kind.SAVINGS_TOP_UP -> stringResource(
            R.string.wish_board_protection_bonus,
            PetHappinessRules.SAVINGS_PROTECTED_SLEEP_COST,
            PetHappinessRules.SLEEP_COST,
            PetHappinessRules.SAVINGS_PROTECTION_SLEEP_COUNT,
        )
        RoomImpulseWish.Kind.MINI_GAME -> stringResource(
            if (wish.productId == "flight") R.string.wish_board_round_bonus
            else R.string.wish_board_launch_bonus,
            wish.happinessBonus,
        )
        else -> stringResource(R.string.wish_board_happiness_bonus, wish.happinessBonus)
    }
    FinPetModalSection(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (wish.kind == RoomImpulseWish.Kind.GROCERY || wish.kind == RoomImpulseWish.Kind.CLOTHING) {
                    wishArtwork?.Content(wish, Modifier.fillMaxWidth().height(46.dp))
                        ?: Text(emoji, style = AppTheme.typography.screenTitle)
                } else Text(emoji, style = AppTheme.typography.screenTitle)
            }
            Spacer(Modifier.width(AppTheme.spacing.md))
            WishDetails(title, duration, bonus, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WishDetails(title: String, duration: String, bonus: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        Text(title, style = AppTheme.typography.bodyStrong)
        Text(duration, style = AppTheme.typography.caption, color = AppTheme.colors.textSecondary)
        Text(bonus, style = AppTheme.typography.caption)
    }
}

@Composable
private fun SavingsWishCard(progress: SavingsGoalProgress) {
    val isGameGoal = progress.goal.id.startsWith("room-zone:")
    val readyToBuy = progress.isReached && isGameGoal
    val target = progress.goal.targetRub.coerceAtLeast(1L)
    FinPetModalSection(
        modifier = Modifier.fillMaxWidth(),
        tone = if (progress.isReached) FinPetModalSectionTone.Highlighted else FinPetModalSectionTone.Neutral,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            WishDetails(
                title = stringResource(
                    if (readyToBuy) R.string.wish_board_buy_title else R.string.wish_board_save_title,
                    progress.goal.title,
                ),
                duration = stringResource(
                    if (readyToBuy) R.string.wish_board_purchase_duration else R.string.wish_board_goal_duration,
                ),
                bonus = if (readyToBuy) stringResource(
                    R.string.wish_board_unlock_bonus, PetWishHappinessRewards.UNLOCKED_GAME,
                ) else stringResource(
                    R.string.wish_board_milestones_bonus,
                    PetWishHappinessRewards.SAVINGS_HALF_WAY,
                    PetWishHappinessRewards.SAVINGS_GOAL_REACHED,
                ),
            )
            Text(
                stringResource(R.string.wish_board_goal_progress, progress.savedRub, target),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            FinPetStorefrontProgressIndicator(
                progress = (progress.savedRub.toFloat() / target).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Список желаний", widthDp = 380, heightDp = 780, showBackground = true)
@Composable
private fun RoomWishBoardDialogPreview() {
    FinPetTheme {
        RoomWishBoardDialog(
            wishes = listOf(
                RoomImpulseWish("food-preview", "Пирожное", 1, false, expiresOnAbsoluteDayExclusive = 2),
                RoomImpulseWish("game-preview", "Рыбалка", 0, false,
                    kind = RoomImpulseWish.Kind.MINI_GAME, productId = "fishing", expiresOnAbsoluteDayExclusive = 2),
                RoomImpulseWish("saving-preview", "Пополнить копилку", 0, false,
                    kind = RoomImpulseWish.Kind.SAVINGS_TOP_UP, expiresOnAbsoluteDayExclusive = 3),
            ),
            absoluteDay = 1,
            savingsGoal = SavingsGoalProgress(
                goal = SavingsGoal("room-zone:fishing", "Рыбалка", 600),
                savedRub = 300, remainingRub = 300, isReached = false,
                nextPeriodicIncome = github.detrig.feature.economy.domain.PeriodicIncome(0, 0, 0),
            ),
            wishArtwork = null,
            onDismiss = {},
        )
    }
}
