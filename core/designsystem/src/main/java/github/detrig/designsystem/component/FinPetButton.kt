package github.detrig.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.theme.AppTheme

/**
 * Полный визуальный контракт кнопки. Feature-код может выбрать готовый вариант
 * из [FinPetButtonDefaults] или изменить только нужные роли через [copy].
 */
@Immutable
data class FinPetButtonStyle(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val borderColor: Color?,
    val disabledBorderColor: Color?,
    val borderWidth: Dp,
    val shape: Shape,
    val minHeight: Dp,
    val contentPadding: PaddingValues,
    val shadowElevation: Dp,
    val disabledShadowElevation: Dp,
    val shadowColor: Color,
    val iconSpacing: Dp,
    val textStyle: TextStyle,
)

object FinPetButtonDefaults {

    @Composable
    fun primaryStyle(): FinPetButtonStyle {
        val colors = AppTheme.colors
        return FinPetButtonStyle(
            containerColor = colors.actionPrimary,
            contentColor = colors.onActionPrimary,
            disabledContainerColor = colors.surfaceInteractive,
            disabledContentColor = colors.textSecondary,
            borderColor = null,
            disabledBorderColor = null,
            borderWidth = AppTheme.sizes.borderThin,
            shape = AppTheme.shapes.button,
            minHeight = AppTheme.sizes.minimumTouchTarget,
            contentPadding = PaddingValues(
                horizontal = AppTheme.spacing.lg,
                vertical = AppTheme.spacing.sm,
            ),
            shadowElevation = AppTheme.elevation.none,
            disabledShadowElevation = AppTheme.elevation.none,
            shadowColor = Color.Black,
            iconSpacing = AppTheme.spacing.sm,
            textStyle = AppTheme.typography.button,
        )
    }

    @Composable
    fun outlinedStyle(): FinPetButtonStyle {
        val colors = AppTheme.colors
        return primaryStyle().copy(
            containerColor = Color.Transparent,
            contentColor = colors.actionPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.textSecondary,
            borderColor = colors.actionPrimary,
            disabledBorderColor = colors.borderDefault,
        )
    }

    /** Вариант основной кнопки каталога по текущему storefront-направлению. */
    @Composable
    fun storefrontPrimaryStyle(): FinPetButtonStyle {
        val colors = AppTheme.colors
        return primaryStyle().copy(
            containerColor = colors.storefront.primaryAction,
            contentColor = colors.storefront.onPrimaryAction,
            borderColor = colors.storefront.outline,
            disabledBorderColor = colors.borderDefault,
            borderWidth = AppTheme.sizes.borderStrong,
            shape = AppTheme.shapes.storefrontControl,
            minHeight = AppTheme.sizes.preferredTouchTarget,
            shadowElevation = AppTheme.elevation.low,
            shadowColor = colors.storefront.shadow,
        )
    }

    /** Светлый вариант кнопки каталога с тем же контуром и геометрией. */
    @Composable
    fun storefrontOutlinedStyle(): FinPetButtonStyle {
        val colors = AppTheme.colors
        return storefrontPrimaryStyle().copy(
            containerColor = colors.storefront.surface,
            contentColor = colors.storefront.onSurface,
            disabledContainerColor = colors.surfaceInteractive,
        )
    }
}

/**
 * Текстовая кнопка. Первые четыре параметра сохранены в прежнем порядке, поэтому
 * существующие позиционные вызовы остаются source-compatible.
 */
@Composable
fun FinPetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: FinPetButtonStyle = FinPetButtonDefaults.primaryStyle(),
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    FinPetButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = style,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(style.iconSpacing))
        }
        Text(
            text = text,
            style = style.textStyle,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Кнопка с полностью произвольным содержимым в [RowScope]. */
@Composable
fun FinPetButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: FinPetButtonStyle = FinPetButtonDefaults.primaryStyle(),
    content: @Composable RowScope.() -> Unit,
) {
    FinPetButtonContainer(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = style,
        content = content,
    )
}

/**
 * Светлая текстовая кнопка. Первые четыре параметра совпадают со старым API.
 */
@Composable
fun FinPetOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: FinPetButtonStyle = FinPetButtonDefaults.outlinedStyle(),
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    FinPetOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = style,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(style.iconSpacing))
        }
        Text(
            text = text,
            style = style.textStyle,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Светлая кнопка с полностью произвольным содержимым в [RowScope]. */
@Composable
fun FinPetOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: FinPetButtonStyle = FinPetButtonDefaults.outlinedStyle(),
    content: @Composable RowScope.() -> Unit,
) {
    FinPetButtonContainer(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = style,
        content = content,
    )
}

@Composable
private fun FinPetButtonContainer(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    style: FinPetButtonStyle,
    content: @Composable RowScope.() -> Unit,
) {
    val elevation = if (enabled) style.shadowElevation else style.disabledShadowElevation
    val buttonModifier = modifier
        .then(
            if (elevation > 0.dp) {
                Modifier.shadow(
                    elevation = elevation,
                    shape = style.shape,
                    clip = false,
                    ambientColor = style.shadowColor,
                    spotColor = style.shadowColor,
                )
            } else {
                Modifier
            },
        )
        .heightIn(min = style.minHeight)

    Button(
        onClick = onClick,
        modifier = buttonModifier,
        enabled = enabled,
        shape = style.shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = style.containerColor,
            contentColor = style.contentColor,
            disabledContainerColor = style.disabledContainerColor,
            disabledContentColor = style.disabledContentColor,
        ),
        border = (if (enabled) style.borderColor else style.disabledBorderColor)?.let { color ->
            BorderStroke(width = style.borderWidth, color = color)
        },
        contentPadding = style.contentPadding,
        content = content,
    )
}
