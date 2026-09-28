package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetCoinText as Text
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.component.FinPetStorefrontProgressIndicator
import github.detrig.designsystem.component.FinPetSunIcon
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
    val displayedGoal = savingsGoal?.takeUnless {
        it.isReached && !it.goal.id.startsWith("room-zone:")
    }
    FinPetModalDialog(
        title = stringResource(R.string.wish_board_title),
        onDismissRequest = onDismiss,
        actions = {},
    ) {
        if (wishes.isEmpty() && displayedGoal == null) {
            FinPetModalSection(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.wish_board_empty),
                    modifier = Modifier.padding(AppTheme.spacing.md),
                    style = AppTheme.typography.body,
                )
            }
        }
        wishes.forEach { WishCard(it, absoluteDay, wishArtwork) }
        displayedGoal?.let { SavingsWishCard(it) }
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
        RoomImpulseWish.Kind.SAVINGS_GOAL -> "🎯"
        RoomImpulseWish.Kind.SAVED_GAME -> "🎮"
        RoomImpulseWish.Kind.FREE -> "✨"
    }
    val title = when {
        wish.isPurchasable -> stringResource(R.string.wish_board_buy_title, wish.productTitle)
        wish.kind == RoomImpulseWish.Kind.MINI_GAME -> stringResource(R.string.wish_board_play_title, wish.productTitle)
        wish.kind == RoomImpulseWish.Kind.SAVINGS_GOAL -> stringResource(R.string.wish_board_save_title, wish.productTitle)
        wish.kind == RoomImpulseWish.Kind.SAVED_GAME -> stringResource(R.string.wish_board_buy_title, wish.productTitle)
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
    WishRow(
        title = title,
        duration = duration,
        emoji = emoji,
        isCompleted = wish.isCompleted,
        bonusDescription = bonus,
        artwork = if (wish.kind == RoomImpulseWish.Kind.GROCERY || wish.kind == RoomImpulseWish.Kind.CLOTHING) {
            wishArtwork?.let { artwork -> { modifier -> artwork.Content(wish, modifier) } }
        } else null,
        bonus = {
            if (wish.kind == RoomImpulseWish.Kind.SAVINGS_TOP_UP) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                    Text(PetHappinessRules.SAVINGS_PROTECTION_SLEEP_COUNT.toString(), style = AppTheme.typography.caption)
                    Text("🛡", style = AppTheme.typography.body)
                }
            } else HappinessBonus("+${wish.happinessBonus}")
        },
    )
}

@Composable
private fun WishRow(
    title: String,
    duration: String,
    emoji: String,
    isCompleted: Boolean,
    bonusDescription: String,
    artwork: (@Composable (Modifier) -> Unit)? = null,
    bonus: @Composable () -> Unit,
    details: (@Composable () -> Unit)? = null,
) {
    val completedDescription = stringResource(R.string.wish_board_completed)
    val iconSize = 32.dp
    val contentGap = AppTheme.spacing.sm
    FinPetModalSection(
        modifier = Modifier.fillMaxWidth(),
        tone = if (isCompleted) FinPetModalSectionTone.Positive else FinPetModalSectionTone.Neutral,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(contentGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.minimumTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(contentGap),
            ) {
                Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                    artwork?.invoke(Modifier.fillMaxWidth().height(iconSize))
                        ?: Text(emoji, style = AppTheme.typography.body)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                ) {
                    Text(title, style = AppTheme.typography.bodyStrong,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!isCompleted) {
                        Text(duration, style = AppTheme.typography.caption,
                            color = AppTheme.colors.textSecondary)
                    }
                }
                Box(
                    modifier = Modifier.widthIn(min = AppTheme.sizes.preferredTouchTarget),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (isCompleted) {
                        Text("✓", style = AppTheme.typography.metricValue,
                            color = AppTheme.colors.statusPositive.accent,
                            modifier = Modifier.semantics { contentDescription = completedDescription })
                    } else Box(Modifier.semantics { contentDescription = bonusDescription }) { bonus() }
                }
            }
            details?.let { content ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = iconSize + contentGap),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun HappinessBonus(points: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        Text(points, style = AppTheme.typography.caption, maxLines = 1)
        FinPetSunIcon(Modifier.size(18.dp))
    }
}

@Composable
private fun SavingsWishCard(progress: SavingsGoalProgress) {
    val isGameGoal = progress.goal.id.startsWith("room-zone:")
    val readyToBuy = progress.isReached && isGameGoal
    val target = progress.goal.targetRub.coerceAtLeast(1L)
    WishRow(
        title = stringResource(
            if (readyToBuy) R.string.wish_board_buy_title else R.string.wish_board_save_title,
            progress.goal.title,
        ),
        duration = stringResource(
            if (readyToBuy) R.string.wish_board_purchase_duration else R.string.wish_board_goal_duration,
        ),
        emoji = if (readyToBuy) "🎮" else "🎯",
        isCompleted = progress.isReached && !isGameGoal,
        bonusDescription = if (readyToBuy) stringResource(
            R.string.wish_board_unlock_bonus, PetWishHappinessRewards.UNLOCKED_GAME,
        ) else stringResource(
            R.string.wish_board_milestones_bonus,
            PetWishHappinessRewards.SAVINGS_HALF_WAY,
            PetWishHappinessRewards.SAVINGS_GOAL_REACHED,
        ),
        bonus = {
            if (readyToBuy) HappinessBonus("+${PetWishHappinessRewards.UNLOCKED_GAME}")
            else HappinessBonus("+${PetWishHappinessRewards.SAVINGS_HALF_WAY}/+${PetWishHappinessRewards.SAVINGS_GOAL_REACHED}")
        },
        details = {
            Text(
                stringResource(R.string.wish_board_goal_progress, progress.savedRub, target),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            FinPetStorefrontProgressIndicator(
                progress = (progress.savedRub.toFloat() / target).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
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
                RoomImpulseWish("clothes-preview", "Синяя кепка", 1, false,
                    kind = RoomImpulseWish.Kind.CLOTHING, expiresOnAbsoluteDayExclusive = 3,
                    completedOnAbsoluteDay = 1),
                RoomImpulseWish("completed-game-preview", "Рыбалка", 0, false,
                    kind = RoomImpulseWish.Kind.SAVED_GAME, completedOnAbsoluteDay = 1),
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
