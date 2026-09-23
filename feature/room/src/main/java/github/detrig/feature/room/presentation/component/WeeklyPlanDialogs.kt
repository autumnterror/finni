package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetDialogueAction
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.component.FinPetProgressIndicator
import github.detrig.designsystem.component.FinPetStorefrontSlider
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.planning.domain.CategoryPlanProgress
import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.feature.planning.domain.PlanProgressTone
import github.detrig.feature.planning.domain.PlanPercentages
import github.detrig.feature.planning.domain.WeeklyPlan
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.room.R
import github.detrig.feature.room.presentation.PlanEditorState
import github.detrig.feature.room.presentation.PlanTutorialStep
import github.detrig.feature.room.presentation.WeekPlanAssessment
import github.detrig.feature.room.presentation.WeekPlanItem
import github.detrig.feature.room.presentation.WeekPlanOutcome
import github.detrig.feature.room.presentation.assessWeek
import kotlin.math.roundToInt

@Composable
internal fun WeeklyPlanEditorDialog(
    editor: PlanEditorState,
    weekNumber: Long,
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
    var totalBounds by remember { mutableStateOf<Rect?>(null) }

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

    WeeklyPlanNotebookDialog(
        title = stringResource(R.string.plan_title, weekNumber),
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
        NotebookSection(
            modifier = Modifier.fillMaxWidth()
                .onGloballyPositioned { totalBounds = it.boundsInWindow() },
        ) {
            Text(
                text = stringResource(R.string.plan_description, availableRub),
                modifier = Modifier.padding(AppTheme.spacing.md),
                style = AppTheme.typography.body,
                color = AppTheme.colors.storefront.onSurface,
            )
        }
        NotebookSection(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.plan_distribution, editor.total),
                modifier = Modifier.padding(AppTheme.spacing.md),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.storefront.onSurface,
            )
        }
        NotebookSection(
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
                cards = listOf(tutorialStep.message()),
                portrait = petPortrait,
                underlay = {
                    TutorialSpotlight(
                        targetBounds = when (tutorialStep) {
                            PlanTutorialStep.MANDATORY -> mandatoryBounds
                            PlanTutorialStep.WANTS -> wantsBounds
                            PlanTutorialStep.SAVINGS -> savingsBounds
                            PlanTutorialStep.RESERVE -> reserveBounds
                            PlanTutorialStep.INTRODUCTION -> totalBounds
                            PlanTutorialStep.PRACTICE -> null
                        },
                    )
                },
                advanceOnTap = false,
                dismissOnBackPress = false,
                topInset = dialogueTopInset,
                actions = listOf(FinPetDialogueAction(
                    id = "next",
                    label = stringResource(if (tutorialStep == PlanTutorialStep.RESERVE) {
                        R.string.plan_tutorial_practice
                    } else {
                        R.string.plan_tutorial_next
                    }),
                )),
                onActionSelected = { onTutorialNext() },
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
                actions = listOf(FinPetDialogueAction(
                    id = PLAN_EDIT_ACTION_ID,
                    label = stringResource(R.string.plan_feedback_edit),
                )),
                onActionSelected = { onFeedbackEdit() },
                onFinished = onFeedbackEdit,
            )
        }
    }
}

private const val PLAN_EDIT_ACTION_ID = "edit_plan"

@Composable
private fun WeeklyPlanNotebookDialog(
    title: String,
    onDismissRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val canDismiss = onDismissRequest != null
    Dialog(
        onDismissRequest = { if (canDismiss) onDismissRequest() },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
            usePlatformDefaultWidth = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(
                modifier = modifier
                    .width(maxWidth.coerceAtMost(430.dp))
                    .fillMaxHeight(),
            ) {
                val horizontalPadding = maxWidth * 0.085f
                val topPadding = maxHeight * 0.12f
                val bottomPadding = maxHeight * 0.055f

                NotebookFrame(Modifier.fillMaxSize())
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = horizontalPadding,
                            top = topPadding,
                            end = horizontalPadding,
                            bottom = bottomPadding,
                        ),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        style = AppTheme.typography.screenTitle,
                        color = AppTheme.colors.storefront.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                        content = content,
                    )
                    actions()
                }
            }
        }
    }
}

/** Keeps the rings and page edge in proportion while the paper grows with the screen. */
@Composable
private fun NotebookFrame(modifier: Modifier = Modifier) {
    val frame = ImageBitmap.imageResource(R.drawable.planning_notebook_frame)
    Canvas(modifier) {
        val targetWidth = size.width.roundToInt()
        val targetHeight = size.height.roundToInt()
        val sourceLeft = (frame.width * 0.078f).roundToInt()
        val sourceWidth = frame.width - sourceLeft * 2
        val topEnd = (frame.height * 0.14f).roundToInt()
        val bottomStart = (frame.height * 0.84f).roundToInt()
        val topHeight = (topEnd * targetWidth.toFloat() / sourceWidth).roundToInt()
        val bottomHeight = ((frame.height - bottomStart) * targetWidth.toFloat() / sourceWidth).roundToInt()
        val middleHeight = (targetHeight - topHeight - bottomHeight).coerceAtLeast(1)

        drawNotebookSlice(frame, sourceLeft, 0, sourceWidth, topEnd, 0, topHeight, targetWidth)
        drawNotebookSlice(
            frame, sourceLeft, topEnd, sourceWidth, bottomStart - topEnd,
            topHeight, middleHeight, targetWidth,
        )
        drawNotebookSlice(
            frame, sourceLeft, bottomStart, sourceWidth, frame.height - bottomStart,
            topHeight + middleHeight, bottomHeight, targetWidth,
        )
    }
}

private fun DrawScope.drawNotebookSlice(
    frame: ImageBitmap,
    sourceLeft: Int,
    sourceTop: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    targetTop: Int,
    targetHeight: Int,
    targetWidth: Int,
) {
    drawImage(
        image = frame,
        srcOffset = IntOffset(sourceLeft, sourceTop),
        srcSize = IntSize(sourceWidth, sourceHeight),
        dstOffset = IntOffset(0, targetTop),
        dstSize = IntSize(targetWidth, targetHeight),
        filterQuality = FilterQuality.Medium,
    )
}

@Composable
private fun NotebookSection(
    modifier: Modifier = Modifier,
    tone: FinPetModalSectionTone = FinPetModalSectionTone.Neutral,
    content: @Composable () -> Unit,
) {
    val container = when (tone) {
        FinPetModalSectionTone.Neutral -> AppTheme.colors.surfaceElevated.copy(alpha = 0.9f)
        FinPetModalSectionTone.Highlighted -> AppTheme.colors.currencyContainer.copy(alpha = 0.88f)
        FinPetModalSectionTone.Warning -> AppTheme.colors.statusWarning.container.copy(alpha = 0.9f)
    }
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.storefrontControl,
        color = container,
        contentColor = AppTheme.colors.storefront.onSurface,
        border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
        shadowElevation = AppTheme.elevation.low,
        content = content,
    )
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
    NotebookSection(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(category.artwork()),
                    contentDescription = null,
                    modifier = Modifier.size(AppTheme.sizes.iconLarge),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    category.title(),
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.bodyStrong,
                )
                Surface(
                    shape = CircleShape,
                    color = AppTheme.colors.statusPositive.container,
                    contentColor = AppTheme.colors.statusPositive.onContainer,
                    border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
                ) {
                    Text(
                        text = stringResource(R.string.plan_percent, value),
                        modifier = Modifier.padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
                        style = AppTheme.typography.bodyStrong,
                    )
                }
            }
            FinPetStorefrontSlider(
                value = value.toFloat(),
                onValueChange = { onPercentChanged(category, it.roundToInt()) },
                valueRange = 0f..100f,
                steps = 99,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppTheme.sizes.minimumTouchTarget)
                    .testTag("weekly_plan_${category.code}"),
            )
        }
    }
}

@Composable
internal fun WeeklyPlanProgressDialog(
    progress: WeeklyPlanProgress,
    isWeekResult: Boolean = false,
    onDismiss: () -> Unit,
) {
    val assessment = remember(progress, isWeekResult) {
        if (isWeekResult) progress.assessWeek() else null
    }
    WeeklyPlanNotebookDialog(
        title = stringResource(
            if (isWeekResult) R.string.plan_result_title else R.string.plan_progress_title,
            progress.plan.weekNumber,
        ),
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("weekly_plan_progress"),
        actions = {
            FinPetButton(
                text = stringResource(
                    if (isWeekResult) R.string.plan_result_continue else R.string.plan_close,
                ),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
        },
    ) {
        if (assessment != null) {
            WeekResultFeedback(assessment)
        }
        NotebookSection(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.plan_progress_description),
                modifier = Modifier.padding(AppTheme.spacing.md),
                style = AppTheme.typography.body,
                color = AppTheme.colors.storefront.onSurface,
            )
        }
        progress.categories.forEach { category ->
            CategoryProgressRow(
                progress = category,
                matched = assessment?.matches(category.category),
            )
        }
        NotebookSection(
            modifier = Modifier.fillMaxWidth(),
            tone = if (assessment?.matchedItems?.contains(WeekPlanItem.RESERVE) == false) {
                FinPetModalSectionTone.Warning
            } else {
                FinPetModalSectionTone.Highlighted
            },
        ) {
            Text(
                text = if (assessment == null) {
                    stringResource(R.string.plan_progress_reserve, progress.plan.reserveRub)
                } else {
                    stringResource(
                        R.string.plan_result_reserve,
                        assessment.actualReserveRub,
                        progress.plan.reserveRub,
                    )
                },
                modifier = Modifier.padding(AppTheme.spacing.md),
                style = AppTheme.typography.bodyStrong,
            )
        }
    }
}

@Composable
private fun WeekResultFeedback(assessment: WeekPlanAssessment) {
    val missedItems = listOfNotNull(
        stringResource(R.string.plan_mandatory)
            .takeIf { WeekPlanItem.MANDATORY in assessment.missedItems },
        stringResource(R.string.plan_wants)
            .takeIf { WeekPlanItem.WANTS in assessment.missedItems },
        stringResource(R.string.plan_savings)
            .takeIf { WeekPlanItem.SAVINGS in assessment.missedItems },
        stringResource(R.string.plan_result_reserve_name)
            .takeIf { WeekPlanItem.RESERVE in assessment.missedItems },
    ).joinToString(", ")
    NotebookSection(
        modifier = Modifier.fillMaxWidth(),
        tone = when (assessment.outcome) {
            WeekPlanOutcome.ALL_MATCHED -> FinPetModalSectionTone.Highlighted
            WeekPlanOutcome.PARTIALLY_MATCHED,
            WeekPlanOutcome.TRY_AGAIN,
            -> FinPetModalSectionTone.Warning
        },
    ) {
        Text(
            text = when (assessment.outcome) {
                WeekPlanOutcome.ALL_MATCHED -> stringResource(R.string.plan_result_all_matched)
                WeekPlanOutcome.PARTIALLY_MATCHED -> stringResource(
                    R.string.plan_result_partially_matched,
                    missedItems,
                )
                WeekPlanOutcome.TRY_AGAIN -> stringResource(R.string.plan_result_try_again)
            },
            modifier = Modifier.padding(AppTheme.spacing.md),
            style = AppTheme.typography.bodyStrong,
        )
    }
}

@Composable
private fun CategoryProgressRow(
    progress: CategoryPlanProgress,
    matched: Boolean? = null,
) {
    val color = when (matched) {
        true -> AppTheme.colors.statusPositive.accent
        false -> AppTheme.colors.statusWarning.accent
        null -> progress.tone.color()
    }
    NotebookSection(modifier = Modifier.fillMaxWidth()) {
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
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
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
                Text(
                    text = when (matched) {
                        true -> stringResource(R.string.plan_result_matched)
                        false -> stringResource(R.string.plan_result_needs_attention)
                        null -> progress.tone.label()
                    },
                    style = AppTheme.typography.caption,
                    color = color,
                )
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
            weekNumber = 2,
            availableRub = 500,
            isSaving = false,
            petName = "Барсик",
            petPortrait = { modifier ->
                Box(modifier.background(AppTheme.colors.actionSecondary))
            },
            tutorialStep = null,
            feedbackCards = null,
            onTutorialNext = {},
            onFeedbackEdit = {},
            onFeedbackFinished = {},
            onPercentChanged = { _, _ -> },
            onSave = {},
        )
    }
}

@Preview(name = "Итоги недели", widthDp = 360, heightDp = 760, showBackground = true)
@Composable
private fun WeeklyPlanResultPreview() {
    FinPetTheme {
        WeeklyPlanProgressDialog(
            progress = WeeklyPlanProgress(
                plan = WeeklyPlan(
                    weekNumber = 2,
                    availableRub = 500,
                    percentages = PlanPercentages(mandatory = 40, wants = 25, savings = 20),
                ),
                categories = listOf(
                    CategoryPlanProgress(PlanCategory.MANDATORY, 200, 190, PlanProgressTone.ON_TRACK),
                    CategoryPlanProgress(PlanCategory.WANTS, 125, 150, PlanProgressTone.WARNING),
                    CategoryPlanProgress(PlanCategory.SAVINGS, 100, 80, PlanProgressTone.ON_TRACK),
                ),
            ),
            isWeekResult = true,
            onDismiss = {},
        )
    }
}
