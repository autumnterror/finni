package github.detrig.designsystem.component

import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import github.detrig.designsystem.theme.AppTheme

@Composable
fun FinPetProgressIndicator(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.height(AppTheme.sizes.progressIndicator),
        color = color,
        trackColor = AppTheme.colors.progressTrack,
        strokeCap = StrokeCap.Round,
        gapSize = AppTheme.spacing.none,
        drawStopIndicator = {},
    )
}
