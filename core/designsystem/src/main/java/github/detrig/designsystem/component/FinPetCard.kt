package github.detrig.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
