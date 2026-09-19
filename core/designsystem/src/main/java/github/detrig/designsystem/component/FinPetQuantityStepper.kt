package github.detrig.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme

/**
 * Reusable storefront quantity control. It keeps the decrement/increment actions
 * separate so a feature can apply its own stock, cart, or inventory rules.
 */
@Composable
fun FinPetQuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canDecrease: Boolean = quantity > 0,
    canIncrease: Boolean = true,
    minWidth: Dp = 112.dp,
    minHeight: Dp = AppTheme.sizes.minimumTouchTarget,
    decreaseContentDescription: String = "Уменьшить количество",
    increaseContentDescription: String = "Увеличить количество",
) {
    require(quantity >= 0) { "Quantity must not be negative" }
    require(minWidth > 0.dp) { "Minimum width must be positive" }
    require(minHeight > 0.dp) { "Minimum height must be positive" }

    Surface(
        modifier = modifier
            .defaultMinSize(minWidth = minWidth)
            .heightIn(min = minHeight),
        shape = AppTheme.shapes.storefrontControl,
        color = AppTheme.colors.currencyContainer,
        contentColor = AppTheme.colors.storefront.onSurface,
        border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
    ) {
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDecrease,
                enabled = enabled && canDecrease,
                modifier = Modifier.semantics { contentDescription = decreaseContentDescription },
            ) {
                Text(
                    text = "−",
                    style = AppTheme.typography.bodyStrong,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = quantity.toString(),
                style = AppTheme.typography.bodyStrong,
                textAlign = TextAlign.Center,
            )
            IconButton(
                onClick = onIncrease,
                enabled = enabled && canIncrease,
                modifier = Modifier.semantics { contentDescription = increaseContentDescription },
            ) {
                Text(
                    text = "+",
                    style = AppTheme.typography.bodyStrong,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(name = "Quantity stepper", showBackground = true)
@Composable
private fun FinPetQuantityStepperPreview() {
    FinPetTheme {
        FinPetQuantityStepper(
            quantity = 2,
            onDecrease = {},
            onIncrease = {},
            modifier = Modifier.padding(AppTheme.spacing.md),
        )
    }
}
