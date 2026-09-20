package github.detrig.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme

/**
 * Общая icon-only кнопка возврата. [contentDescription] должен описывать
 * действие на языке текущего экрана, например «Назад».
 */
@Composable
fun FinPetBackButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = AppTheme.sizes.preferredTouchTarget,
) {
    val style = FinPetButtonDefaults.storefrontOutlinedStyle().copy(
        containerColor = AppTheme.colors.currencyContainer,
        contentColor = AppTheme.colors.storefront.outline,
        contentPadding = PaddingValues(AppTheme.spacing.none),
        minHeight = size,
    )

    FinPetButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription },
        enabled = enabled,
        style = style,
    ) {
        FinPetBackArrow(buttonSize = size)
    }
}

@Composable
private fun FinPetBackArrow(buttonSize: Dp) {
    val color = LocalContentColor.current
    val strokeWidth = AppTheme.sizes.borderStrong * 2

    Canvas(modifier = Modifier.size(buttonSize * (AppTheme.sizes.iconLarge.value / AppTheme.sizes.preferredTouchTarget.value))) {
        val tail = Offset(x = size.width * 0.8f, y = size.height * 0.5f)
        val tip = Offset(x = size.width * 0.2f, y = size.height * 0.5f)
        val upperArm = Offset(x = size.width * 0.45f, y = size.height * 0.24f)
        val lowerArm = Offset(x = size.width * 0.45f, y = size.height * 0.76f)

        drawLine(
            color = color,
            start = tail,
            end = tip,
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = tip,
            end = upperArm,
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = tip,
            end = lowerArm,
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Preview(name = "Back button", widthDp = 120, heightDp = 120)
@Composable
private fun FinPetBackButtonPreview() {
    FinPetTheme {
        Box(
            modifier = Modifier
                .background(AppTheme.colors.storefront.background)
                .padding(AppTheme.spacing.xl),
        ) {
            FinPetBackButton(
                onClick = {},
                contentDescription = "Назад",
            )
        }
    }
}
