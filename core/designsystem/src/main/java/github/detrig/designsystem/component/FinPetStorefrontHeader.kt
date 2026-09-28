package github.detrig.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme

/** Keeps the complete store name within the space left by the back button and balance. */
@Composable
fun FinPetStorefrontHeader(
    title: String,
    balanceRub: Long?,
    onBack: () -> Unit,
    backContentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FinPetBackButton(onClick = onBack, contentDescription = backContentDescription)
        BoxWithConstraints(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val measurer = rememberTextMeasurer()
            val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
            val titleStyle = AppTheme.typography.screenTitle
            val fittedStyle = remember(title, titleStyle, widthPx, measurer) {
                val naturalWidth = measurer.measure(
                    text = title,
                    style = titleStyle,
                    softWrap = false,
                    maxLines = 1,
                ).size.width
                if (naturalWidth <= widthPx || naturalWidth == 0) titleStyle else {
                    // Android's large-font scaling is nonlinear. Measure each candidate
                    // instead of assuming that pixel width scales with the sp value.
                    var lower = 0.01f
                    var upper = 1f
                    repeat(12) {
                        val candidate = (lower + upper) / 2f
                        val candidateWidth = measurer.measure(
                            text = title,
                            style = titleStyle.copy(fontSize = titleStyle.fontSize * candidate),
                            softWrap = false,
                            maxLines = 1,
                        ).size.width
                        if (candidateWidth <= widthPx) lower = candidate else upper = candidate
                    }
                    titleStyle.copy(
                        fontSize = titleStyle.fontSize * lower,
                        lineHeight = titleStyle.lineHeight * lower,
                    )
                }
            }
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = fittedStyle,
                color = AppTheme.colors.storefront.onSurface,
                textAlign = TextAlign.Center,
                softWrap = false,
                maxLines = 1,
            )
        }
        FinPetStorefrontBalanceBadge(balanceRub = balanceRub)
    }
}

private class StorefrontTitlePreviews : PreviewParameterProvider<String> {
    override val values = sequenceOf("Продуктовый", "Одежда", "Интерьер", "Корзина")
}

@Preview(name = "Store headers", widthDp = 360, showBackground = true)
@Preview(name = "Narrow screen, large font", widthDp = 320, fontScale = 1.5f, showBackground = true)
@Composable
private fun FinPetStorefrontHeaderPreview(@PreviewParameter(StorefrontTitlePreviews::class) title: String) {
    FinPetTheme {
        FinPetStorefrontHeader(title, 12_500, {}, "Назад")
    }
}
