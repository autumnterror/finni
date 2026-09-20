package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.component.FinPetProgressIndicator
import github.detrig.designsystem.component.FinPetStorefrontSlider
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.planning.domain.CategoryPlanProgress
import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.feature.planning.domain.PlanProgressTone
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.room.R
import github.detrig.feature.room.presentation.PlanEditorState
import github.detrig.feature.room.presentation.PlanTutorialStep
import kotlin.math.roundToInt

@Composable
internal fun WeeklyPlanEditorDialog(
    editor: PlanEditorState,
    availableRub: Long,
    isSaving: Boolean,
    petName: String,
    petPortrait: @Composable (Modifier) -> Unit,
    tutorialStep: PlanTutorialStep?,
    onTutorialNext: () -> Unit,
    onPercentChanged: (PlanCategory, Int) -> Unit,
    onSave: () -> Unit,
) {
    FinPetModalDialog(
        title = stringResource(R.string.plan_title),
        onDismissRequest = null,
        modifier = Modifier.testTag("weekly_plan_editor"),
        actions = {
            FinPetButton(
                text = if (isSaving) stringResource(R.string.plan_saving) else stringResource(R.string.plan_save),
                onClick = onSave,
                enabled = tutorialStep == null && !isSaving && editor.total <= 100,
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
                text = stringResource(R.string.plan_description, availableRub),
                modifier = Modifier.padding(AppTheme.spacing.md),
                style = AppTheme.typography.body,
                color = AppTheme.colors.storefront.onSurface,
            )
        }
        FinPetModalSection(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.plan_distribution, editor.total),
                modifier = Modifier.padding(AppTheme.spacing.md),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.storefront.onSurface,
            )
        }
        FinPetModalSection(
            modifier = Modifier.fillMaxWidth(),
            tone = if (tutorialStep == PlanTutorialStep.RESERVE) {
                FinPetModalSectionTone.Highlighted
            } else {
                FinPetModalSectionTone.Neutral
            },
        ) {
            Column(Modifier.padding(AppTheme.spacing.md)) {
                if (tutorialStep == PlanTutorialStep.RESERVE) TutorialHighlightLabel()
                Text(
                    text = stringResource(
                        R.string.plan_reserve,
                        editor.reserve,
                        availableRub * editor.reserve / 100,
                    ),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.storefront.onSurface,
                )
            }
        }
        PercentageSlider(
            category = PlanCategory.MANDATORY,
            value = editor.mandatory,
            highlighted = tutorialStep == PlanTutorialStep.MANDATORY,
            enabled = tutorialStep == null,
            onPercentChanged = onPercentChanged,
        )
        PercentageSlider(
            category = PlanCategory.WANTS,
            value = editor.wants,
            highlighted = tutorialStep == PlanTutorialStep.WANTS,
            enabled = tutorialStep == null,
            onPercentChanged = onPercentChanged,
        )
        PercentageSlider(
            category = PlanCategory.SAVINGS,
            value = editor.savings,
            highlighted = tutorialStep == PlanTutorialStep.SAVINGS,
            enabled = tutorialStep == null,
            onPercentChanged = onPercentChanged,
        )
        if (tutorialStep != null) {
            FinPetDialogueDialog(
                speakerName = petName,
                cards = PlanTutorialStep.entries.map { it.message() },
                portrait = petPortrait,
                onPageChanged = { onTutorialNext() },
                dismissOnBackPress = false,
                onFinished = onTutorialNext,
            )
        }
    }
}

@Composable
private fun PlanTutorialStep.message(): String = stringResource(when (this) {
    PlanTutorialStep.INTRODUCTION -> R.string.plan_tutorial_intro
    PlanTutorialStep.MANDATORY -> R.string.plan_tutorial_mandatory
    PlanTutorialStep.WANTS -> R.string.plan_tutorial_wants
    PlanTutorialStep.SAVINGS -> R.string.plan_tutorial_savings
    PlanTutorialStep.RESERVE -> R.string.plan_tutorial_reserve
    PlanTutorialStep.PRACTICE -> R.string.plan_tutorial_try
})

@Composable
private fun TutorialHighlightLabel() {
    Text(
        text = stringResource(R.string.plan_tutorial_highlight),
        style = AppTheme.typography.caption,
        color = AppTheme.colors.actionPrimary,
    )
}

@Composable
private fun PercentageSlider(
    category: PlanCategory,
    value: Int,
    highlighted: Boolean,
    enabled: Boolean,
    onPercentChanged: (PlanCategory, Int) -> Unit,
) {
    FinPetModalSection(
        modifier = Modifier.fillMaxWidth(),
        tone = if (highlighted) FinPetModalSectionTone.Highlighted else FinPetModalSectionTone.Neutral,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            if (highlighted) TutorialHighlightLabel()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(category.title(), style = AppTheme.typography.bodyStrong)
                Text(
                    stringResource(R.string.plan_percent, value),
                    style = AppTheme.typography.metricValue,
                    color = AppTheme.colors.actionPrimary,
                )
            }
            FinPetStorefrontSlider(
                value = value.toFloat(),
                onValueChange = { onPercentChanged(category, it.roundToInt()) },
                valueRange = 0f..100f,
                steps = 99,
                enabled = enabled,
                modifier = Modifier
                    .heightIn(min = AppTheme.sizes.minimumTouchTarget)
                    .testTag("weekly_plan_${category.code}"),
            )
        }
    }
}

@Composable
internal fun WeeklyPlanProgressDialog(
    progress: WeeklyPlanProgress,
    onDismiss: () -> Unit,
) {
    FinPetModalDialog(
        title = stringResource(R.string.plan_progress_title, progress.plan.weekNumber),
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("weekly_plan_progress"),
        actions = {
            FinPetButton(
                text = stringResource(R.string.plan_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
        },
    ) {
        Text(stringResource(R.string.plan_progress_description), style = AppTheme.typography.body)
        progress.categories.forEach { CategoryProgressRow(it) }
        FinPetModalSection(
            modifier = Modifier.fillMaxWidth(),
            tone = FinPetModalSectionTone.Highlighted,
        ) {
            Text(
                text = stringResource(R.string.plan_progress_reserve, progress.plan.reserveRub),
                modifier = Modifier.padding(AppTheme.spacing.md),
                style = AppTheme.typography.bodyStrong,
            )
        }
    }
}

@Composable
private fun CategoryProgressRow(progress: CategoryPlanProgress) {
    val color = progress.tone.color()
    FinPetModalSection(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(progress.category.title(), style = AppTheme.typography.bodyStrong)
                Text(
                    stringResource(R.string.plan_actual_of_planned, progress.actualRub, progress.plannedRub),
                    style = AppTheme.typography.caption,
                )
            }
            FinPetProgressIndicator(
                progress = progress.progress,
                color = color,
                modifier = Modifier.fillMaxWidth().testTag("plan_progress_${progress.category.code}"),
            )
            Text(progress.tone.label(), style = AppTheme.typography.caption, color = color)
        }
    }
}

@Composable
private fun PlanCategory.title(): String = stringResource(when (this) {
    PlanCategory.MANDATORY -> R.string.plan_mandatory
    PlanCategory.WANTS -> R.string.plan_wants
    PlanCategory.SAVINGS -> R.string.plan_savings
})

@Composable
private fun PlanProgressTone.label(): String = stringResource(when (this) {
    PlanProgressTone.ON_TRACK -> R.string.plan_progress_on_track
    PlanProgressTone.WARNING -> R.string.plan_progress_warning
    PlanProgressTone.OVER_LIMIT -> R.string.plan_progress_over_limit
})

@Composable
private fun PlanProgressTone.color(): Color = when (this) {
    PlanProgressTone.ON_TRACK -> AppTheme.colors.statusPositive.accent
    PlanProgressTone.WARNING -> AppTheme.colors.statusWarning.accent
    PlanProgressTone.OVER_LIMIT -> AppTheme.colors.statusCritical.accent
}

@Preview(name = "План на неделю", widthDp = 360, heightDp = 760, showBackground = true)
@Composable
private fun WeeklyPlanDialogPreview() {
    FinPetTheme {
        WeeklyPlanEditorDialog(
            editor = PlanEditorState(mandatory = 50, wants = 30, savings = 20),
            availableRub = 500,
            isSaving = false,
            petName = "Барсик",
            petPortrait = { modifier ->
                Box(modifier.background(AppTheme.colors.actionSecondary))
            },
            tutorialStep = PlanTutorialStep.MANDATORY,
            onTutorialNext = {},
            onPercentChanged = { _, _ -> },
            onSave = {},
        )
    }
}
