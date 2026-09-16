package github.detrig.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.designsystem.theme.AppTheme

@Composable
fun FinPetIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(AppTheme.sizes.minimumTouchTarget),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = AppTheme.colors.actionPrimary,
            disabledContentColor = AppTheme.colors.textSecondary,
        ),
        content = content,
    )
}
