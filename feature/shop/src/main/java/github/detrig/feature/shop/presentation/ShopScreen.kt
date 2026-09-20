package github.detrig.feature.shop.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.shop.ShopFeature
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.feature.shop.api.ShopItemDetailsResolver
import github.detrig.products.GroceryCatalog
import github.detrig.products.StoreId

@Composable
internal fun ShopScreen(
    storeId: StoreId,
    onBack: (() -> Unit)? = null,
    onOpenCart: (() -> Unit)? = null,
    closeAfterReceipt: (() -> Unit)? = null,
) {
    val component = ShopFeature.component()
    val viewModel: ShopViewModel = viewModel(key = "${storeId.value}:${onBack != null}") {
        component.viewModel(
            storeId = storeId,
            onOpenCart = onOpenCart,
            closeAfterReceipt = closeAfterReceipt,
        )
    }
    val state by viewModel.state().observeAsState(ShopViewState())

    LaunchedEffect(viewModel) { viewModel.perform(ShopViewEvent.Load) }
    val handleBack = {
        if (state.receipt == null) onBack?.invoke() ?: viewModel.perform(ShopViewEvent.Back)
        else viewModel.perform(ShopViewEvent.Back)
    }
    BackHandler { handleBack() }

    Scaffold(
        containerColor = AppTheme.colors.storefront.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        ShopContent(
            state = state,
            artworkResolver = component.artworkResolver,
            itemDetailsResolver = component.itemDetailsResolver,
            onEvent = viewModel::perform,
            onBack = handleBack,
            modifier = if (onBack == null) Modifier.shopSafeDrawingPadding() else Modifier,
            contentPadding = padding,
        )
    }
}

@Preview(name = "Shop screen", widthDp = 360, heightDp = 760, showBackground = true)
@Composable
private fun ShopScreenPreview() {
    FinPetTheme {
        ShopContent(
            state = ShopViewState(
                storefront = GroceryCatalog().storefront,
                balanceRub = 25_000,
                loading = false,
            ),
            artworkResolver = ShopArtworkResolver.Empty,
            itemDetailsResolver = ShopItemDetailsResolver.Empty,
            onEvent = {},
        )
    }
}
