package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetDialogueAction
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
    feedbackCards: List<String>?,
    dialogueTopInset: Dp = 0.dp,
    onTutorialNext: () -> Unit,
    onFeedbackEdit: () -> Unit,
    onFeedbackFinished: () -> Unit,
    onPercentChanged: (PlanCategory, Int) -> Unit,
    onSave: () -> Unit,
) {
    val mandatoryRequester = remember { BringIntoViewRequester() }
    val wantsRequester = remember { BringIntoViewRequester() }
    val savingsRequester = remember { BringIntoViewRequester() }
    val reserveRequester = remember { BringIntoViewRequester() }
    var mandatoryBounds by remember { mutableStateOf<Rect?>(null) }
    var wantsBounds by remember { mutableStateOf<Rect?>(null) }
    var savingsBounds by remember { mutableStateOf<Rect?>(null) }
    var reserveBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(tutorialStep) {
        val requester = when (tutorialStep) {
            PlanTutorialStep.MANDATORY -> mandatoryRequester
            PlanTutorialStep.WANTS -> wantsRequester
            PlanTutorialStep.SAVINGS -> savingsRequester
            PlanTutorialStep.RESERVE -> reserveRequester
            PlanTutorialStep.INTRODUCTION,
            PlanTutorialStep.PRACTICE,
            null,
            -> null
        }
        if (requester != null) {
            withFrameNanos { }
            requester.bringIntoView()
        }
    }

    FinPetModalDialog(
        title = stringResource(R.string.plan_title),
        onDismissRequest = null,
        modifier = Modifier.testTag("weekly_plan_editor"),
        actions = {
            FinPetButton(
                text = if (isSaving) stringResource(R.string.plan_saving) else stringResource(R.string.plan_save),
                onClick = onSave,
                enabled = tutorialStep == null && feedbackCards == null && !isSaving && editor.total <= 100,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
        },
    ) {
        FinPetModalSection(
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(reserveRequester)
                .onGloballyPositioned { reserveBounds = it.boundsInWindow() },
        ) {
            Column(Modifier.padding(AppTheme.spacing.md)) {
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
            enabled = tutorialStep == null && feedbackCards == null,
            modifier = Modifier
                .bringIntoViewRequester(mandatoryRequester)
                .onGloballyPositioned { mandatoryBounds = it.boundsInWindow() },
            onPercentChanged = onPercentChanged,
        )
        PercentageSlider(
            category = PlanCategory.WANTS,
            value = editor.wants,
            enabled = tutorialStep == null && feedbackCards == null,
            modifier = Modifier
                .bringIntoViewRequester(wantsRequester)
                .onGloballyPositioned { wantsBounds = it.boundsInWindow() },
            onPercentChanged = onPercentChanged,
        )
        PercentageSlider(
            category = PlanCategory.SAVINGS,
            value = editor.savings,
            enabled = tutorialStep == null && feedbackCards == null,
            modifier = Modifier
                .bringIntoViewRequester(savingsRequester)
                .onGloballyPositioned { savingsBounds = it.boundsInWindow() },
            onPercentChanged = onPercentChanged,
        )
        if (tutorialStep != null) {
            FinPetDialogueDialog(
                speakerName = petName,
                cards = PlanTutorialStep.entries.map { it.message() },
                portrait = petPortrait,
                underlay = {
                    TutorialSpotlight(
                        targetBounds = when (tutorialStep) {
                            PlanTutorialStep.MANDATORY -> mandatoryBounds
                            PlanTutorialStep.WANTS -> wantsBounds
                            PlanTutorialStep.SAVINGS -> savingsBounds
                            PlanTutorialStep.RESERVE -> reserveBounds
                            PlanTutorialStep.INTRODUCTION,
                            PlanTutorialStep.PRACTICE,
                            -> null
                        },
                    )
                },
                onPageChanged = { onTutorialNext() },
                dismissOnBackPress = false,
                topInset = dialogueTopInset,
                onFinished = onTutorialNext,
            )
        } else if (feedbackCards != null) {
            FinPetDialogueDialog(
                speakerName = petName,
                cards = feedbackCards,
                portrait = petPortrait,
                dismissOnBackPress = false,
                advanceOnTap = false,
                topInset = dialogueTopInset,
                actions = listOf(
                    FinPetDialogueAction(
                        id = PLAN_EDIT_ACTION_ID,
                        label = stringResource(R.string.plan_feedback_edit),
                    ),
                    FinPetDialogueAction(
                        id = PLAN_SAVE_ACTION_ID,
                        label = stringResource(R.string.plan_feedback_save_anyway),
                    ),
                ),
                onActionSelected = { action ->
                    if (action.id == PLAN_EDIT_ACTION_ID) onFeedbackEdit() else onFeedbackFinished()
                },
                onFinished = onFeedbackFinished,
            )
        }
    }
}

private const val PLAN_EDIT_ACTION_ID = "edit_plan"
private const val PLAN_SAVE_ACTION_ID = "save_plan"

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
internal fun TutorialSpotlight(targetBounds: Rect?) {
    if (targetBounds == null) return
    val density = LocalDensity.current
    val paddingPx = with(density) { AppTheme.spacing.sm.toPx() }
    val cornerRadiusPx = with(density) { AppTheme.spacing.md.toPx() }
    val scrimColor = AppTheme.colors.sceneShadow.copy(alpha = 0.72f)
    val clearColor = AppTheme.colors.storefront.surface

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val left = (targetBounds.left - paddingPx).coerceAtLeast(0f)
        val top = (targetBounds.top - paddingPx).coerceAtLeast(0f)
        val right = (targetBounds.right + paddingPx).coerceAtMost(size.width)
        val bottom = (targetBounds.bottom + paddingPx).coerceAtMost(size.height)
        val spotlightSize = Size(
            width = (right - left).coerceAtLeast(0f),
            height = (bottom - top).coerceAtLeast(0f),
        )
        val cornerRadius = CornerRadius(cornerRadiusPx)

        drawRect(scrimColor)
        drawRoundRect(
            color = clearColor,
            topLeft = Offset(left, top),
            size = spotlightSize,
            cornerRadius = cornerRadius,
            blendMode = BlendMode.Clear,
        )
    }
}

@Composable
private fun PercentageSlider(
    category: PlanCategory,
    value: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onPercentChanged: (PlanCategory, Int) -> Unit,
) {
    FinPetModalSection(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
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
        FinPetModalSection(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.plan_progress_description),
                modifier = Modifier.padding(AppTheme.spacing.md),
                style = AppTheme.typography.body,
                color = AppTheme.colors.storefront.onSurface,
            )
        }
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
        Row(
            modifier = Modifier.padding(AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(progress.category.artwork()),
                contentDescription = null,
                modifier = Modifier.size(AppTheme.sizes.iconLarge),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(AppTheme.spacing.xs))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(progress.category.title(), style = AppTheme.typography.bodyStrong)
                    Text(
                        stringResource(
                            R.string.plan_actual_of_planned,
                            progress.actualRub,
                            progress.plannedRub,
                        ),
                        style = AppTheme.typography.caption,
                    )
                }
                FinPetProgressIndicator(
                    progress = progress.progress,
                    color = color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_progress_${progress.category.code}"),
                )
                Text(progress.tone.label(), style = AppTheme.typography.caption, color = color)
            }
        }
    }
}

private fun PlanCategory.artwork(): Int = when (this) {
    PlanCategory.MANDATORY -> R.drawable.planning_icon_mandatory
    PlanCategory.WANTS -> R.drawable.planning_icon_wants
    PlanCategory.SAVINGS -> R.drawable.planning_icon_savings
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
            feedbackCards = null,
            onTutorialNext = {},
            onFeedbackEdit = {},
            onFeedbackFinished = {},
            onPercentChanged = { _, _ -> },
            onSave = {},
        )
    }
}
