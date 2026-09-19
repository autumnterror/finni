package github.detrig.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import github.detrig.designsystem.theme.AppTheme

@Composable
fun FinPetCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.card,
    containerColor: Color = AppTheme.colors.surfaceElevated,
    contentColor: Color = AppTheme.colors.textPrimary,
    borderColor: Color? = AppTheme.colors.borderDefault,
    borderWidth: Dp = AppTheme.sizes.borderThin,
    elevation: Dp = AppTheme.elevation.none,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = borderColor?.let { color -> BorderStroke(borderWidth, color) },
        shadowElevation = elevation,
        content = content,
    )
}

/** Карточка в общей геометрии и палитре игровых каталогов. */
@Composable
fun FinPetStorefrontCard(
    modifier: Modifier = Modifier,
    containerColor: Color = AppTheme.colors.storefront.surface,
    content: @Composable () -> Unit,
) {
    FinPetCard(
        modifier = modifier.shadow(
            elevation = AppTheme.elevation.low,
            shape = AppTheme.shapes.storefrontControl,
            ambientColor = AppTheme.colors.storefront.shadow,
            spotColor = AppTheme.colors.storefront.shadow,
        ),
        shape = AppTheme.shapes.storefrontControl,
        containerColor = containerColor,
        contentColor = AppTheme.colors.storefront.onSurface,
        borderColor = AppTheme.colors.storefront.outline,
        borderWidth = AppTheme.sizes.borderStrong,
        content = content,
    )
}
