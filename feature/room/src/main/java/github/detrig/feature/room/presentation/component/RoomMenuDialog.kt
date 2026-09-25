package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.component.FinPetProgressIndicator
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.gamestate.domain.progression.PetGrowthStage
import github.detrig.feature.room.R
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.presentation.PlanAchievementFeedback

@Composable
internal fun RoomMenuDialog(
    progress: RoomProgress,
    achievements: List<PlanAchievementFeedback>,
    showAllUnlocked: Boolean,
    onToggleUnlocked: () -> Unit,
    onShowAllAchievements: () -> Unit,
    onParentCabinet: () -> Unit,
    onDismiss: () -> Unit,
) {
    val unlocked = achievements.filter { it.isUnlocked }
        .sortedByDescending { it.unlockOrder ?: 0L }
    FinPetModalDialog(
        title = stringResource(R.string.room_menu_title),
        onDismissRequest = onDismiss,
        actions = {
            FinPetButton(
                text = stringResource(R.string.parent_cabinet_title),
                onClick = onParentCabinet,
                modifier = Modifier.fillMaxWidth().testTag("menu_parent_cabinet"),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
        },
    ) {
        FinPetModalSection(
            modifier = Modifier.fillMaxWidth(),
            tone = FinPetModalSectionTone.Highlighted,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                Text(
                    text = stringResource(when (progress.petGrowthStage) {
                        PetGrowthStage.BABY -> R.string.pet_stage_baby
                        PetGrowthStage.EXPLORER -> R.string.pet_stage_explorer
                        PetGrowthStage.COMPANION -> R.string.pet_stage_companion
                    }),
                    style = AppTheme.typography.metricValue,
                    color = AppTheme.colors.storefront.onSurface,
                )
                FinPetProgressIndicator(
                    progress = progress.experienceProgress,
                    color = AppTheme.colors.currencyAccent,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = progress.xpUntilNextLevel?.let { remaining ->
                        stringResource(R.string.hud_experience_remaining, remaining)
                    } ?: stringResource(R.string.hud_experience_max_level),
                    style = AppTheme.typography.bodyStrong,
                )
            }
        }

        Text(
            text = stringResource(R.string.menu_unlocked_achievements, unlocked.size),
            style = AppTheme.typography.sectionTitle,
        )
        if (unlocked.isEmpty()) {
            FinPetModalSection(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.menu_no_achievements),
                    modifier = Modifier.padding(AppTheme.spacing.md),
                    style = AppTheme.typography.body,
                )
            }
        } else {
            (if (showAllUnlocked) unlocked else unlocked.take(4)).forEach { achievement ->
                AchievementRow(achievement)
            }
            if (unlocked.size > 4) {
                FinPetButton(
                    text = if (showAllUnlocked) stringResource(R.string.menu_collapse_achievements)
                    else stringResource(R.string.menu_expand_achievements, unlocked.size),
                    onClick = onToggleUnlocked,
                    modifier = Modifier.fillMaxWidth().testTag("menu_achievements_accordion"),
                    style = FinPetButtonDefaults.storefrontOutlinedStyle(),
                )
            }
        }
        FinPetButton(
            text = stringResource(R.string.menu_all_achievements),
            onClick = onShowAllAchievements,
            modifier = Modifier.fillMaxWidth().testTag("menu_all_achievements"),
            style = FinPetButtonDefaults.storefrontOutlinedStyle(),
        )
    }
}

@Preview(name = "Меню игрока", widthDp = 360, heightDp = 740, showBackground = true)
@Preview(name = "Меню, крупный шрифт", widthDp = 320, heightDp = 740, fontScale = 1.3f)
@Composable
private fun RoomMenuDialogPreview() {
    FinPetTheme {
        RoomMenuDialog(
            progress = RoomProgress(
                balanceRub = 240,
                playerLevel = 3,
                ownedZoneIds = emptySet(),
                absoluteDay = 9,
                weekNumber = 2,
                dayOfWeek = 2,
                daysUntilAllowance = 5,
                totalXp = 310,
                currentLevelXp = 60,
                nextLevelXp = 200,
                experienceProgress = .3f,
                petGrowthStage = PetGrowthStage.EXPLORER,
            ),
            achievements = (1..6).map { index ->
                PlanAchievementFeedback(
                    id = "achievement-$index",
                    title = "Достижение $index",
                    description = "Новое финансовое умение питомца и игрока.",
                    xpReward = if (index % 2 == 0) 20 else 50,
                    unlockOrder = index.toLong(),
                )
            },
            showAllUnlocked = false,
            onToggleUnlocked = {},
            onShowAllAchievements = {},
            onParentCabinet = {},
            onDismiss = {},
        )
    }
}
