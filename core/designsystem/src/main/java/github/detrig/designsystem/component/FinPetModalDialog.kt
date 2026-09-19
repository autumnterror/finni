package github.detrig.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import github.detrig.designsystem.R
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme

/** Общий каркас модалок в том же визуальном языке, что и каталог магазина. */
@Composable
fun FinPetModalDialog(
    title: String,
    onDismissRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
    dismissEnabled: Boolean = true,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val canDismiss = onDismissRequest != null && dismissEnabled
    Dialog(
        onDismissRequest = { if (canDismiss) onDismissRequest() },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(AppTheme.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            FinPetModalSurface(
                title = title,
                onDismissRequest = onDismissRequest,
                dismissEnabled = dismissEnabled,
                modifier = modifier,
                actions = actions,
                content = content,
            )
        }
    }
}

@Composable
private fun FinPetModalSurface(
    title: String,
    onDismissRequest: (() -> Unit)?,
    dismissEnabled: Boolean,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxDialogHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = AppTheme.sizes.contentMaxWidth)
            .heightIn(max = maxDialogHeight)
            .shadow(
                elevation = AppTheme.elevation.medium,
                shape = AppTheme.shapes.storefrontControl,
                ambientColor = AppTheme.colors.storefront.shadow,
                spotColor = AppTheme.colors.storefront.shadow,
            ),
        shape = AppTheme.shapes.storefrontControl,
        color = AppTheme.colors.storefront.surface,
        contentColor = AppTheme.colors.storefront.onSurface,
        border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = AppTheme.sizes.preferredTouchTarget),
                    style = AppTheme.typography.screenTitle,
                    color = AppTheme.colors.storefront.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (onDismissRequest != null) {
                    ModalCloseButton(
                        onClick = onDismissRequest,
                        enabled = dismissEnabled,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                content = content,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                content = actions,
            )
        }
    }
}

@Composable
private fun ModalCloseButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.modal_close)
    val strokeWidth = AppTheme.sizes.borderStrong * 1.75f
    val style = FinPetButtonDefaults.storefrontOutlinedStyle().copy(
        containerColor = Color.Transparent,
        contentColor = AppTheme.colors.storefront.outline,
        disabledContainerColor = Color.Transparent,
        borderColor = null,
        disabledBorderColor = null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(AppTheme.spacing.none),
        shadowElevation = AppTheme.elevation.none,
    )
    FinPetButton(
        onClick = onClick,
        modifier = modifier
            .size(AppTheme.sizes.minimumTouchTarget)
            .semantics { contentDescription = label },
        enabled = enabled,
        style = style,
    ) {
        val color = androidx.compose.material3.LocalContentColor.current
        Canvas(Modifier.size(AppTheme.sizes.iconMedium)) {
            val inset = size.minDimension * 0.2f
            val stroke = strokeWidth.toPx()
            drawLine(color, Offset(inset, inset), Offset(size.width - inset, size.height - inset), stroke, StrokeCap.Round)
            drawLine(color, Offset(size.width - inset, inset), Offset(inset, size.height - inset), stroke, StrokeCap.Round)
        }
    }
}

enum class FinPetModalSectionTone { Neutral, Highlighted, Warning }

/** Округлая storefront-карточка для смысловых блоков внутри модалки. */
@Composable
fun FinPetModalSection(
    modifier: Modifier = Modifier,
    tone: FinPetModalSectionTone = FinPetModalSectionTone.Neutral,
    content: @Composable () -> Unit,
) {
    val container = when (tone) {
        FinPetModalSectionTone.Neutral -> AppTheme.colors.storefront.surface
        FinPetModalSectionTone.Highlighted -> AppTheme.colors.storefront.selectedSurface
        FinPetModalSectionTone.Warning -> AppTheme.colors.currencyContainer
    }
    FinPetCard(
        modifier = modifier.shadow(
            elevation = AppTheme.elevation.low,
            shape = AppTheme.shapes.storefrontControl,
            ambientColor = AppTheme.colors.storefront.shadow,
            spotColor = AppTheme.colors.storefront.shadow,
        ),
        shape = AppTheme.shapes.storefrontControl,
        containerColor = container,
        contentColor = AppTheme.colors.storefront.onSurface,
        borderColor = AppTheme.colors.storefront.outline,
        borderWidth = AppTheme.sizes.borderStrong,
        content = content,
    )
}

/** Денежный бейдж, повторяющий баланс и ценники магазина. */
@Composable
fun FinPetMoneyAmount(
    amount: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = AppTheme.sizes.minimumTouchTarget)
            .shadow(
                elevation = AppTheme.elevation.low,
                shape = AppTheme.shapes.storefrontControl,
                ambientColor = AppTheme.colors.storefront.shadow,
                spotColor = AppTheme.colors.storefront.shadow,
            ),
        shape = AppTheme.shapes.storefrontControl,
        color = AppTheme.colors.currencyContainer,
        contentColor = AppTheme.colors.storefront.onSurface,
        border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(AppTheme.sizes.iconMedium),
                shape = CircleShape,
                color = AppTheme.colors.currencyContainer,
                contentColor = AppTheme.colors.storefront.onSurface,
                border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.currencyAccent),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "₽", style = AppTheme.typography.label)
                }
            }
            Text(text = amount, style = AppTheme.typography.currency, maxLines = 1)
        }
    }
}

/** Числовое поле без Material-label/notch, оформленное как контрол магазина. */
@Composable
fun FinPetAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    suffix: String = "₽",
) {
    val textColor = if (enabled) AppTheme.colors.storefront.onSurface else AppTheme.colors.textSecondary
    Surface(
        modifier = modifier
            .heightIn(min = AppTheme.sizes.preferredTouchTarget)
            .shadow(
                elevation = if (enabled) AppTheme.elevation.low else AppTheme.elevation.none,
                shape = AppTheme.shapes.storefrontControl,
                ambientColor = AppTheme.colors.storefront.shadow,
                spotColor = AppTheme.colors.storefront.shadow,
            ),
        shape = AppTheme.shapes.storefrontControl,
        color = AppTheme.colors.storefront.surface,
        contentColor = textColor,
        border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                textStyle = AppTheme.typography.currency.copy(color = textColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(AppTheme.colors.storefront.outline),
                singleLine = true,
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text("0", style = AppTheme.typography.currency, color = AppTheme.colors.textSecondary)
                        }
                        inner()
                    }
                },
            )
            Text(suffix, style = AppTheme.typography.currency)
        }
    }
}

/** Ползунок с круглым маркером и цельной дорожкой в стиле игровых контролов магазина. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FinPetStorefrontSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val trackColor = AppTheme.colors.currencyContainer
    val activeColor = AppTheme.colors.storefront.primaryAction
    val outlineColor = AppTheme.colors.storefront.outline
    val outlineWidth = AppTheme.sizes.borderStrong
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        interactionSource = interactionSource,
        thumb = {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = AppTheme.colors.storefront.primaryAction,
                border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
                shadowElevation = AppTheme.elevation.low,
                content = {},
            )
        },
        track = { state ->
            val span = valueRange.endInclusive - valueRange.start
            val fraction = if (span == 0f) 0f else ((state.value - valueRange.start) / span).coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxWidth().heightIn(min = 12.dp)) {
                val trackHeight = 12.dp.toPx()
                val top = (size.height - trackHeight) / 2f
                val radius = CornerRadius(trackHeight / 2f)
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trackHeight),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, top),
                    size = Size(size.width * fraction, trackHeight),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trackHeight),
                    cornerRadius = radius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(outlineWidth.toPx()),
                )
            }
        },
    )
}

@Preview(name = "Storefront modal", widthDp = 360, heightDp = 700, showBackground = true)
@Composable
private fun FinPetModalPreview() {
    FinPetTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(AppTheme.colors.storefront.background)
                .padding(AppTheme.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            FinPetModalSurface(
                title = "План на неделю",
                onDismissRequest = {},
                dismissEnabled = true,
                actions = {
                    FinPetButton(
                        text = "Сохранить",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        style = FinPetButtonDefaults.storefrontPrimaryStyle(),
                    )
                },
            ) {
                FinPetModalSection(Modifier.fillMaxWidth(), FinPetModalSectionTone.Highlighted) {
                    Text(
                        text = "Распредели деньги между важным, желаниями и накоплениями",
                        modifier = Modifier.padding(AppTheme.spacing.md),
                        style = AppTheme.typography.body,
                    )
                }
                FinPetMoneyAmount("480")
            }
        }
    }
}
