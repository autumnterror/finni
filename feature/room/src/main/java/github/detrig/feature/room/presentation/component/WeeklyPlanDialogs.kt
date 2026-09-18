package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetProgressIndicator
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.planning.domain.CategoryPlanProgress
import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.feature.planning.domain.PlanProgressTone
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.room.R
import github.detrig.feature.room.presentation.PlanEditorState
import kotlin.math.roundToInt

@Composable
internal fun WeeklyPlanEditorDialog(
    editor: PlanEditorState,
    isSaving: Boolean,
    onPercentChanged: (PlanCategory, Int) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier.testTag("weekly_plan_editor"),
        title = { Text(stringResource(R.string.plan_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                Text(stringResource(R.string.plan_description), style = AppTheme.typography.body)
                Text(stringResource(R.string.plan_total, editor.total), style = AppTheme.typography.bodyStrong)
                PercentageSlider(PlanCategory.MANDATORY, editor.mandatory, onPercentChanged)
                PercentageSlider(PlanCategory.WANTS, editor.wants, onPercentChanged)
                PercentageSlider(PlanCategory.SAVINGS, editor.savings, onPercentChanged)
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving && editor.total == 100,
                modifier = Modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget),
            ) { Text(if (isSaving) stringResource(R.string.plan_saving) else stringResource(R.string.plan_save)) }
        },
    )
}

@Composable
private fun PercentageSlider(
    category: PlanCategory,
    value: Int,
    onPercentChanged: (PlanCategory, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category.title(), style = AppTheme.typography.bodyStrong)
            Text(stringResource(R.string.plan_percent, value), style = AppTheme.typography.bodyStrong)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onPercentChanged(category, it.roundToInt()) },
            valueRange = 0f..100f,
            steps = 99,
            modifier = Modifier.testTag("weekly_plan_${category.code}"),
        )
    }
}

@Composable
internal fun WeeklyPlanProgressDialog(
    progress: WeeklyPlanProgress,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("weekly_plan_progress"),
        title = { Text(stringResource(R.string.plan_progress_title, progress.plan.weekNumber)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            ) {
                Text(stringResource(R.string.plan_progress_description), style = AppTheme.typography.body)
                progress.categories.forEach { CategoryProgressRow(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget)) {
                Text(stringResource(R.string.plan_close))
            }
        },
    )
}

@Composable
private fun CategoryProgressRow(progress: CategoryPlanProgress) {
    val color = progress.tone.color()
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(progress.category.title(), style = AppTheme.typography.bodyStrong)
            Text(stringResource(R.string.plan_actual_of_planned, progress.actualRub, progress.plannedRub),
                style = AppTheme.typography.caption)
        }
        FinPetProgressIndicator(
            progress = progress.progress,
            color = color,
            modifier = Modifier.fillMaxWidth().testTag("plan_progress_${progress.category.code}"),
        )
        Text(progress.tone.label(), style = AppTheme.typography.caption, color = color)
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
