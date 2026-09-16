package github.detrig.feature.gamesession.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetProgressIndicator
import github.detrig.designsystem.theme.AppTheme

@Composable
internal fun StatPanel(
    modifier: Modifier = Modifier,
    title: String,
    valueText: String,
    progress: Int?,
    @DrawableRes drawableRes: Int,
    iconDescription: String,
    accentColor: Color,
) {
    FinPetCard(
        modifier = modifier.height(112.dp),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelImage(
                    drawableRes = drawableRes,
                    contentDescription = iconDescription,
                    size = AppTheme.sizes.iconMedium,
                )
                Spacer(modifier = Modifier.width(AppTheme.spacing.sm))
                Text(
                    text = title,
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                )
            }
            Text(
                text = valueText,
                style = AppTheme.typography.metricValue,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
            )
            if (progress != null) {
                FinPetProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = accentColor,
                )
            }
        }
    }
}
