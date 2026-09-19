package github.detrig.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import github.detrig.designsystem.theme.AppTheme

@Immutable
data class FinPetFilterChipStyle(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val borderColor: Color,
    val disabledBorderColor: Color,
    val borderWidth: Dp,
    val shape: Shape,
    val minHeight: Dp,
    val contentPadding: PaddingValues,
    val selectedElevation: Dp,
    val elevation: Dp,
    val iconSpacing: Dp,
    val textStyle: TextStyle,
)

object FinPetFilterChipDefaults {

    @Composable
    fun storefrontStyle(): FinPetFilterChipStyle {
        val colors = AppTheme.colors
        return FinPetFilterChipStyle(
            selectedContainerColor = colors.storefront.primaryAction,
            selectedContentColor = colors.storefront.onPrimaryAction,
            containerColor = colors.storefront.surface,
            contentColor = colors.storefront.onSurface,
            disabledContainerColor = colors.surfaceInteractive,
            disabledContentColor = colors.textSecondary,
            borderColor = colors.storefront.outline,
            disabledBorderColor = colors.borderDefault,
            borderWidth = AppTheme.sizes.borderStrong,
            shape = AppTheme.shapes.storefrontControl,
            minHeight = AppTheme.sizes.minimumTouchTarget,
            contentPadding = PaddingValues(
                horizontal = AppTheme.spacing.lg,
                vertical = AppTheme.spacing.sm,
            ),
            selectedElevation = AppTheme.elevation.low,
            elevation = AppTheme.elevation.low,
            iconSpacing = AppTheme.spacing.sm,
            textStyle = AppTheme.typography.button,
        )
    }
}

/** Selectable chip с явным состоянием и touch target не меньше 48 dp. */
@Composable
fun FinPetFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: FinPetFilterChipStyle = FinPetFilterChipDefaults.storefrontStyle(),
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val containerColor = when {
        !enabled -> style.disabledContainerColor
        selected -> style.selectedContainerColor
        else -> style.containerColor
    }
    val contentColor = when {
        !enabled -> style.disabledContentColor
        selected -> style.selectedContentColor
        else -> style.contentColor
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = style.minHeight)
            .semantics { this.selected = selected },
        enabled = enabled,
        shape = style.shape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(
            width = style.borderWidth,
            color = if (enabled) style.borderColor else style.disabledBorderColor,
        ),
        shadowElevation = if (selected) style.selectedElevation else style.elevation,
    ) {
        Row(
            modifier = Modifier.padding(style.contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(style.iconSpacing))
            }
            Text(
                text = text,
                style = style.textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Готовая data-driven строка chips с произвольной моделью категории. */
@Composable
fun <T> FinPetFilterChipRow(
    items: List<T>,
    isSelected: (item: T) -> Boolean,
    onItemSelected: (item: T) -> Unit,
    text: (item: T) -> String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemSpacing: Dp = AppTheme.spacing.sm,
    enabled: (item: T) -> Boolean = { true },
    key: ((item: T) -> Any)? = null,
    style: FinPetFilterChipStyle = FinPetFilterChipDefaults.storefrontStyle(),
) {
    FinPetLazyRow(
        items = items,
        modifier = modifier,
        contentPadding = contentPadding,
        itemSpacing = itemSpacing,
        key = key,
    ) { item ->
        FinPetFilterChip(
            text = text(item),
            selected = isSelected(item),
            onClick = { onItemSelected(item) },
            enabled = enabled(item),
            style = style,
        )
    }
}
