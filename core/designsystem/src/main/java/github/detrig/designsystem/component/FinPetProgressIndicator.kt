package github.detrig.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme

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

/** Толстый контурный прогресс из визуального языка магазина. */
@Composable
fun FinPetStorefrontProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.storefront.primaryAction,
    height: androidx.compose.ui.unit.Dp = 16.dp,
) {
    val trackColor = AppTheme.colors.currencyContainer
    val outlineColor = AppTheme.colors.storefront.outline
    val outlineWidth = AppTheme.sizes.borderStrong
    Canvas(modifier.height(height)) {
        val stroke = outlineWidth.toPx()
        val inset = stroke / 2f
        val barHeight = size.height - stroke
        val radius = CornerRadius(barHeight / 2f)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, barHeight),
            cornerRadius = radius,
        )
        val fraction = progress.coerceIn(0f, 1f)
        if (fraction > 0f) {
            drawRoundRect(
                color = color,
                topLeft = Offset(inset, inset),
                size = Size((size.width - stroke) * fraction, barHeight),
                cornerRadius = radius,
            )
        }
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, barHeight),
            cornerRadius = radius,
            style = Stroke(stroke),
        )
    }
}

@Preview(name = "Storefront progress", widthDp = 320, heightDp = 64, showBackground = true)
@Composable
private fun FinPetStorefrontProgressPreview() {
    FinPetTheme {
        Box(
            modifier = Modifier
                .background(AppTheme.colors.storefront.background)
                .padding(AppTheme.spacing.lg),
        ) {
            FinPetStorefrontProgressIndicator(progress = 0.62f)
        }
    }
}
