package github.detrig.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import github.detrig.designsystem.theme.AppTheme

@Composable
fun FinPetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget),
        enabled = enabled,
        shape = AppTheme.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.actionPrimary,
            contentColor = colors.onActionPrimary,
            disabledContainerColor = colors.surfaceInteractive,
            disabledContentColor = colors.textSecondary,
        ),
        contentPadding = PaddingValues(
            horizontal = AppTheme.spacing.lg,
            vertical = AppTheme.spacing.sm,
        ),
    ) {
        Text(
            text = text,
            style = AppTheme.typography.button,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun FinPetOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = AppTheme.sizes.minimumTouchTarget),
        enabled = enabled,
        shape = AppTheme.shapes.button,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.actionPrimary,
            disabledContentColor = colors.textSecondary,
        ),
        border = BorderStroke(
            width = AppTheme.sizes.borderThin,
            color = if (enabled) colors.actionPrimary else colors.borderDefault,
        ),
        contentPadding = PaddingValues(
            horizontal = AppTheme.spacing.lg,
            vertical = AppTheme.spacing.sm,
        ),
    ) {
        Text(
            text = text,
            style = AppTheme.typography.button,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
