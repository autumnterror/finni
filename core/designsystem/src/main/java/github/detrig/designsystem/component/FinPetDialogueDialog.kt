package github.detrig.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import github.detrig.designsystem.R
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import kotlin.math.min

/** Действие под репликой. Список из 0, 1, 2 или 3 элементов задаёт режим диалога. */
@Immutable
data class FinPetDialogueAction(
    val id: String,
    val label: String,
    val enabled: Boolean = true,
)

/** Общая карточка реплик. Источник текста, портрета и действий задаёт вызывающая фича. */
@Composable
fun FinPetDialogueDialog(
    speakerName: String,
    cards: List<String>,
    portrait: @Composable (Modifier) -> Unit,
    underlay: @Composable BoxScope.() -> Unit = {},
    additionalContent: @Composable ColumnScope.() -> Unit = {},
    onPageChanged: (Int) -> Unit = {},
    dismissOnBackPress: Boolean = true,
    advanceOnTap: Boolean = true,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    actions: List<FinPetDialogueAction> = emptyList(),
    onActionSelected: (FinPetDialogueAction) -> Unit = {},
    onFinished: () -> Unit,
) {
    require(cards.isNotEmpty()) { "Dialogue must contain at least one card" }
    require(actions.size <= MAX_DIALOGUE_ACTIONS) { "Dialogue supports up to three actions" }
    require(actions.all { it.id.isNotBlank() }) { "Dialogue action IDs must not be blank" }
    require(actions.map(FinPetDialogueAction::id).distinct().size == actions.size) {
        "Dialogue action IDs must be unique"
    }

    var pageIndex by rememberSaveable(cards) { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    LaunchedEffect(pageIndex) { scrollState.scrollTo(0) }
    val lastIndex = cards.lastIndex
    val maxCardHeight = LocalConfiguration.current.screenHeightDp.dp * 0.82f
    val interactionSource = remember { MutableInteractionSource() }
    val canAdvanceOnTap = advanceOnTap && actions.isEmpty()
    val tapHint = stringResource(
        if (pageIndex < lastIndex) R.string.dialogue_tap_next else R.string.dialogue_tap_finish,
    )

    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = onFinished,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            underlay()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (canAdvanceOnTap) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClickLabel = tapHint,
                                role = Role.Button,
                            ) {
                                if (pageIndex < lastIndex) {
                                    pageIndex++
                                    onPageChanged(pageIndex)
                                } else {
                                    onFinished()
                                }
                            }
                        } else {
                            Modifier
                        },
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(
                        start = AppTheme.spacing.md,
                        top = AppTheme.spacing.xl + topInset,
                        end = AppTheme.spacing.md,
                        bottom = AppTheme.spacing.xl,
                    )
                    .testTag("finpet_dialogue"),
                contentAlignment = Alignment.TopCenter,
            ) {
                DialogueBubble(
                    speakerName = speakerName,
                    text = cards[pageIndex],
                    pageIndex = pageIndex,
                    pageCount = cards.size,
                    portrait = portrait,
                    tapHint = tapHint,
                    showTapHint = canAdvanceOnTap,
                    actions = actions,
                    onActionSelected = onActionSelected,
                    scrollState = scrollState,
                    maxHeight = maxCardHeight,
                    additionalContent = additionalContent,
                )
            }
        }
    }
}

@Composable
private fun DialogueBubble(
    speakerName: String,
    text: String,
    pageIndex: Int,
    pageCount: Int,
    portrait: @Composable (Modifier) -> Unit,
    tapHint: String,
    showTapHint: Boolean,
    actions: List<FinPetDialogueAction>,
    onActionSelected: (FinPetDialogueAction) -> Unit,
    scrollState: ScrollState,
    maxHeight: androidx.compose.ui.unit.Dp,
    additionalContent: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors.dialogue
    FinPetCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = AppTheme.sizes.contentMaxWidth)
            .heightIn(max = maxHeight)
            .shadow(
                elevation = AppTheme.elevation.high,
                shape = DialogueBubbleShape,
                ambientColor = colors.shadow,
                spotColor = colors.shadow,
            ),
        shape = DialogueBubbleShape,
        containerColor = colors.panel,
        contentColor = colors.onPanel,
        borderColor = colors.outline,
        borderWidth = AppTheme.sizes.borderStrong * 2,
        elevation = AppTheme.elevation.none,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val portraitSize = (maxWidth * PORTRAIT_WIDTH_FRACTION).coerceIn(
                minimumValue = MIN_PORTRAIT_SIZE,
                maximumValue = MAX_PORTRAIT_SIZE,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(
                        start = AppTheme.spacing.lg,
                        top = AppTheme.spacing.lg,
                        end = AppTheme.spacing.lg,
                        bottom = DIALOGUE_TAIL_CONTENT_PADDING,
                    ),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(modifier = Modifier.size(portraitSize)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = AppTheme.sizes.borderStrong,
                                    color = colors.outline,
                                    shape = CircleShape,
                                )
                                .clip(CircleShape)
                                .background(AppTheme.colors.currencyContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            portrait(Modifier.fillMaxSize())
                        }
                        Image(
                            painter = painterResource(R.drawable.finpet_dialogue_accent_red),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = AppTheme.spacing.lg, y = -AppTheme.spacing.sm)
                                .size(width = 30.dp, height = 38.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = portraitSize),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = DIALOGUE_HEART_TEXT_INSET),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                        ) {
                            Text(
                                text = speakerName,
                                modifier = Modifier
                                    .background(
                                        color = colors.speakerContainer,
                                        shape = CircleShape,
                                    )
                                    .padding(
                                        horizontal = AppTheme.spacing.lg,
                                        vertical = AppTheme.spacing.xs,
                                    ),
                                style = AppTheme.typography.bodyStrong,
                                color = colors.onPanel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = text,
                                style = AppTheme.typography.sectionTitle,
                                color = colors.onPanel,
                            )
                        }
                        DialogueHeart(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(DIALOGUE_HEART_SIZE),
                        )
                    }
                }

                additionalContent()

                if (actions.isNotEmpty()) {
                    DialogueActions(actions = actions, onActionSelected = onActionSelected)
                } else if (showTapHint) {
                    DialogueFooter(
                        tapHint = tapHint,
                        pageIndex = pageIndex,
                        pageCount = pageCount,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogueActions(
    actions: List<FinPetDialogueAction>,
    onActionSelected: (FinPetDialogueAction) -> Unit,
) {
    val style = FinPetButtonDefaults.dialogueActionStyle()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DialogueRays(
            mirror = false,
            modifier = Modifier.size(width = DIALOGUE_RAYS_WIDTH, height = DIALOGUE_RAYS_HEIGHT),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            actions.forEach { action ->
                DialogueActionButton(
                    action = action,
                    onClick = { onActionSelected(action) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finpet_dialogue_action_${action.id}"),
                    style = style,
                )
            }
        }
        DialogueRays(
            mirror = true,
            modifier = Modifier.size(width = DIALOGUE_RAYS_WIDTH, height = DIALOGUE_RAYS_HEIGHT),
        )
        DialoguePaw(modifier = Modifier.size(DIALOGUE_ACTION_PAW_SIZE))
    }
}

@Composable
private fun DialogueActionButton(
    action: FinPetDialogueAction,
    onClick: () -> Unit,
    modifier: Modifier,
    style: FinPetButtonStyle,
) {
    FinPetButton(
        onClick = onClick,
        enabled = action.enabled,
        modifier = modifier,
        style = style,
    ) {
        Text(
            text = action.label,
            modifier = Modifier.weight(1f),
            style = style.textStyle,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DialogueFooter(
    tapHint: String,
    pageIndex: Int,
    pageCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tapHint,
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(Modifier.weight(1f))
        if (pageCount > 1) {
            Text(
                text = stringResource(R.string.dialogue_progress, pageIndex + 1, pageCount),
                style = AppTheme.typography.label,
                color = AppTheme.colors.textSecondary,
            )
        }
        DialogueRays(
            mirror = true,
            modifier = Modifier
                .padding(start = AppTheme.spacing.sm)
                .size(width = 22.dp, height = 30.dp),
        )
        DialoguePaw(modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun DialogueHeart(modifier: Modifier = Modifier) {
    val color = AppTheme.colors.statusCritical.accent
    val highlight = AppTheme.colors.statusCritical.container
    Canvas(modifier) {
        val heart = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.92f)
            cubicTo(
                size.width * 0.42f,
                size.height * 0.78f,
                size.width * 0.08f,
                size.height * 0.58f,
                size.width * 0.08f,
                size.height * 0.3f,
            )
            cubicTo(
                size.width * 0.08f,
                size.height * 0.04f,
                size.width * 0.4f,
                0f,
                size.width * 0.5f,
                size.height * 0.22f,
            )
            cubicTo(
                size.width * 0.6f,
                0f,
                size.width * 0.92f,
                size.height * 0.04f,
                size.width * 0.92f,
                size.height * 0.3f,
            )
            cubicTo(
                size.width * 0.92f,
                size.height * 0.58f,
                size.width * 0.58f,
                size.height * 0.78f,
                size.width * 0.5f,
                size.height * 0.92f,
            )
            close()
        }
        drawPath(heart, color)
        drawCircle(
            color = highlight,
            radius = size.minDimension * 0.09f,
            center = Offset(size.width * 0.3f, size.height * 0.27f),
        )
    }
}

@Composable
private fun DialogueRays(
    mirror: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = AppTheme.colors.currencyAccent
    val borderWidth = AppTheme.sizes.borderStrong
    Canvas(modifier) {
        val outerX = if (mirror) size.width else 0f
        val innerX = if (mirror) 0f else size.width
        val stroke = borderWidth.toPx() * 2.5f
        listOf(-0.18f, 0f, 0.18f).forEachIndexed { index, offset ->
            val outerY = size.height * (0.28f + index * 0.22f)
            val innerY = outerY - size.height * offset
            drawLine(
                color = color,
                start = Offset(outerX, outerY),
                end = Offset(innerX, innerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun DialoguePaw(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.finpet_dialogue_paw),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

private object DialogueBubbleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val tailHeight = min(size.height * 0.12f, with(density) { 30.dp.toPx() })
        val bodyBottom = size.height - tailHeight
        val cornerRadius = min(
            min(size.width, bodyBottom) * 0.18f,
            with(density) { 56.dp.toPx() },
        )
        val tailHalfWidth = min(size.width * 0.09f, with(density) { 42.dp.toPx() })
        val centerX = size.width / 2f
        val path = Path().apply {
            moveTo(cornerRadius, 0f)
            lineTo(size.width - cornerRadius, 0f)
            quadraticTo(size.width, 0f, size.width, cornerRadius)
            lineTo(size.width, bodyBottom - cornerRadius)
            quadraticTo(size.width, bodyBottom, size.width - cornerRadius, bodyBottom)
            lineTo(centerX + tailHalfWidth, bodyBottom)
            lineTo(centerX, size.height)
            lineTo(centerX - tailHalfWidth, bodyBottom)
            lineTo(cornerRadius, bodyBottom)
            quadraticTo(0f, bodyBottom, 0f, bodyBottom - cornerRadius)
            lineTo(0f, cornerRadius)
            quadraticTo(0f, 0f, cornerRadius, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

private const val MAX_DIALOGUE_ACTIONS = 3
private const val PORTRAIT_WIDTH_FRACTION = 0.22f
private val MIN_PORTRAIT_SIZE = 72.dp
private val MAX_PORTRAIT_SIZE = 112.dp
private val DIALOGUE_TAIL_CONTENT_PADDING = 42.dp
private val DIALOGUE_HEART_TEXT_INSET = 44.dp
private val DIALOGUE_HEART_SIZE = 38.dp
private val DIALOGUE_RAYS_WIDTH = 24.dp
private val DIALOGUE_RAYS_HEIGHT = 44.dp
private val DIALOGUE_ACTION_PAW_SIZE = 42.dp

private data class DialoguePreviewState(
    val text: String,
    val actions: List<FinPetDialogueAction>,
)

private class DialoguePreviewProvider : PreviewParameterProvider<DialoguePreviewState> {
    override val values = sequenceOf(
        DialoguePreviewState(
            text = "Обязательное — еда и другие важные вещи.",
            actions = emptyList(),
        ),
        DialoguePreviewState(
            text = "Отлично! Теперь у нас есть еда. Давай меня покормим!",
            actions = listOf(FinPetDialogueAction("understood", "Понятно")),
        ),
        DialoguePreviewState(
            text = "Как лучше поступить с оставшимися деньгами?",
            actions = listOf(
                FinPetDialogueAction("save", "Отложить в копилку"),
                FinPetDialogueAction("food", "Купить запас еды"),
                FinPetDialogueAction("later", "Решить позже"),
            ),
        ),
    )
}

@Preview(name = "Диалог питомца", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun FinPetDialogueDialogPreview(
    @PreviewParameter(DialoguePreviewProvider::class) state: DialoguePreviewState,
) {
    FinPetTheme {
        FinPetDialogueDialog(
            speakerName = "Финни",
            cards = listOf(state.text),
            portrait = { modifier ->
                Box(modifier.background(AppTheme.colors.currencyContainer))
            },
            actions = state.actions,
            onActionSelected = {},
            onFinished = {},
        )
    }
}
