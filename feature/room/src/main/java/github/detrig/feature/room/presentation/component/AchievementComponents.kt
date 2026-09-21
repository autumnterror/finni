package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetFeedbackSurface
import github.detrig.designsystem.component.FinPetFeedbackTone
import github.detrig.designsystem.component.FinPetIconButton
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.presentation.PlanAchievementFeedback

@Composable
internal fun AchievementMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.achievements_open)
    FinPetCard(
        modifier = modifier,
        shape = AppTheme.shapes.button,
        borderColor = AppTheme.colors.actionPrimary,
        borderWidth = AppTheme.sizes.borderStrong,
        elevation = AppTheme.elevation.medium,
    ) {
        FinPetIconButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = label },
        ) {
            val color = AppTheme.colors.actionPrimary
            val strokeWidth = AppTheme.sizes.borderStrong
            Canvas(Modifier.size(AppTheme.sizes.iconMedium)) {
                val stroke = strokeWidth.toPx()
                val left = size.width * 0.12f
                val right = size.width * 0.88f
                listOf(0.22f, 0.5f, 0.78f).forEach { fraction ->
                    val y = size.height * fraction
                    drawLine(
                        color = color,
                        start = Offset(left, y),
                        end = Offset(right, y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AchievementUnlockedBanner(
    achievement: PlanAchievementFeedback,
    onDismiss: () -> Unit,
) {
    val closeLabel = stringResource(R.string.achievement_banner_close)
    Popup(
        alignment = Alignment.TopCenter,
        properties = PopupProperties(focusable = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = AppTheme.spacing.xl, vertical = AppTheme.spacing.md),
            contentAlignment = Alignment.TopCenter,
        ) {
            FinPetFeedbackSurface(
                tone = FinPetFeedbackTone.Positive,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = AppTheme.sizes.contentMaxWidth),
            ) { style ->
                Row(
                    modifier = Modifier.padding(AppTheme.spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "★",
                        style = AppTheme.typography.screenTitle,
                        color = style.accent,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.achievement_banner_title),
                            style = AppTheme.typography.label,
                            color = style.accent,
                        )
                        Text(
                            text = achievement.title,
                            style = AppTheme.typography.bodyStrong,
                            color = style.content,
                        )
                        Text(
                            text = achievement.description,
                            style = AppTheme.typography.caption,
                            color = style.content,
                        )
                    }
                    FinPetIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.semantics { contentDescription = closeLabel },
                    ) {
                        val strokeWidth = AppTheme.sizes.borderStrong
                        Canvas(Modifier.size(AppTheme.sizes.iconMedium)) {
                            val stroke = strokeWidth.toPx()
                            val inset = size.minDimension * 0.22f
                            drawLine(
                                color = style.accent,
                                start = Offset(inset, inset),
                                end = Offset(size.width - inset, size.height - inset),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = style.accent,
                                start = Offset(size.width - inset, inset),
                                end = Offset(inset, size.height - inset),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AchievementsDialog(
    achievements: List<PlanAchievementFeedback>,
    onDismiss: () -> Unit,
) {
    val unlockedCount = achievements.count { it.isUnlocked }
    FinPetModalDialog(
        title = stringResource(R.string.achievements_title),
        onDismissRequest = onDismiss,
        actions = {
            FinPetButton(
                text = stringResource(R.string.achievements_close),
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
            Text(
                text = stringResource(
                    R.string.achievements_progress,
                    unlockedCount,
                    achievements.size,
                ),
                modifier = Modifier.padding(AppTheme.spacing.md),
                style = AppTheme.typography.bodyStrong,
            )
        }
        achievements.forEach { achievement ->
            AchievementRow(achievement)
        }
    }
}

@Composable
private fun AchievementRow(achievement: PlanAchievementFeedback) {
    FinPetModalSection(
        modifier = Modifier.fillMaxWidth(),
        tone = if (achievement.isUnlocked) {
            FinPetModalSectionTone.Highlighted
        } else {
            FinPetModalSectionTone.Neutral
        },
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = if (achievement.isUnlocked) "✓" else "○",
                style = AppTheme.typography.metricValue,
                color = if (achievement.isUnlocked) {
                    AppTheme.colors.statusPositive.accent
                } else {
                    AppTheme.colors.textSecondary
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                Text(
                    text = achievement.title,
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.storefront.onSurface,
                )
                Text(
                    text = achievement.description,
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
                Text(
                    text = stringResource(
                        if (achievement.isUnlocked) {
                            R.string.achievement_unlocked
                        } else {
                            R.string.achievement_locked
                        },
                    ),
                    style = AppTheme.typography.label,
                    color = if (achievement.isUnlocked) {
                        AppTheme.colors.statusPositive.accent
                    } else {
                        AppTheme.colors.textSecondary
                    },
                )
            }
        }
    }
}

@Preview(name = "Достижения", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun AchievementsPreview() {
    FinPetTheme {
        AchievementsDialog(
            achievements = listOf(
                PlanAchievementFeedback(
                    id = "first-plan",
                    title = "Первый план",
                    description = "Ты познакомился с распределением денег на неделю.",
                ),
                PlanAchievementFeedback(
                    id = "thoughtful-plan",
                    title = "Продуманный план",
                    description = "В твоём плане хватает денег на обязательные расходы.",
                    isUnlocked = false,
                ),
            ),
            onDismiss = {},
        )
    }
}
