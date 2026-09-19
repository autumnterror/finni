package github.detrig.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme

@Preview(name = "Storefront controls", widthDp = 360, heightDp = 280)
@Preview(name = "Storefront controls · large font", widthDp = 360, heightDp = 320, fontScale = 1.3f)
@Composable
private fun StorefrontControlsPreview() {
    FinPetTheme {
        var selectedCategory by remember { mutableStateOf("Все") }
        val categories = listOf("Все", "Продукты", "Блюда", "Напитки")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.storefront.background)
                .padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
        ) {
            FinPetFilterChipRow(
                items = categories,
                isSelected = { category -> category == selectedCategory },
                onItemSelected = { category -> selectedCategory = category },
                text = { category -> category },
                key = { category -> category },
                modifier = Modifier.fillMaxWidth(),
            )
            FinPetButton(
                text = "В корзину",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
                leadingIcon = { Text(text = "▣") },
            )
            FinPetOutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontOutlinedStyle(),
            ) {
                Text(text = "−")
                Text(
                    text = "  2 в корзине  ",
                    style = AppTheme.typography.button,
                )
                Text(text = "+")
            }
        }
    }
}

@Preview(name = "Reusable storefront grid", widthDp = 360, heightDp = 520)
@Composable
private fun StorefrontGridPreview() {
    FinPetTheme {
        val products = listOf(
            PreviewProduct("Яблоко", "15 ₽"),
            PreviewProduct("Банан", "20 ₽"),
            PreviewProduct("Морковь", "10 ₽"),
            PreviewProduct("Салат", "35 ₽"),
            PreviewProduct("Суп", "45 ₽"),
            PreviewProduct("Молоко", "25 ₽"),
        )

        FinPetLazyGrid(
            items = products,
            columns = FinPetGridColumns.Fixed(3),
            contentPadding = PaddingValues(AppTheme.spacing.lg),
            key = { product -> product.name },
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.storefront.background),
        ) { product ->
            FinPetCard(
                modifier = Modifier.aspectRatio(0.72f),
                shape = AppTheme.shapes.storefrontControl,
                containerColor = AppTheme.colors.storefront.surface,
                contentColor = AppTheme.colors.storefront.onSurface,
                borderColor = AppTheme.colors.storefront.outline,
                borderWidth = AppTheme.sizes.borderStrong,
                elevation = AppTheme.elevation.low,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppTheme.spacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "◯", style = AppTheme.typography.screenTitle)
                        Text(text = product.name, style = AppTheme.typography.bodyStrong)
                        Text(text = product.price, style = AppTheme.typography.currency)
                    }
                }
            }
        }
    }
}

private data class PreviewProduct(
    val name: String,
    val price: String,
)
