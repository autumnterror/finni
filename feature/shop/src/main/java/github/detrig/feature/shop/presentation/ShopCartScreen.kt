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
import github.detrig.products.StoreCart
import github.detrig.products.StoreId

@Composable
internal fun ShopCartScreen(storeId: StoreId) {
    val component = ShopFeature.component()
    val viewModel: ShopCartViewModel = viewModel(key = "cart:${storeId.value}") {
        component.cartViewModel(storeId)
    }
    val state by viewModel.state().observeAsState(ShopCartViewState())

    LaunchedEffect(viewModel) { viewModel.perform(ShopCartViewEvent.Load) }
    BackHandler { viewModel.perform(ShopCartViewEvent.Back) }

    Scaffold(
        containerColor = AppTheme.colors.storefront.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        ShopCartContent(
            state = state,
            artworkResolver = component.artworkResolver,
            itemDetailsResolver = component.itemDetailsResolver,
            onEvent = viewModel::perform,
            modifier = Modifier.shopSafeDrawingPadding(),
            contentPadding = padding,
        )
    }
}

@Preview(name = "Cart screen", widthDp = 432, heightDp = 920, showBackground = true)
@Composable
private fun ShopCartScreenPreview() {
    val catalog = GroceryCatalog().storefront
    val cart = StoreCart.Empty
        .add(catalog.items[0].id, quantity = 2)
        .add(catalog.items.first { it.title == "Салат" }.id)
        .add(catalog.items.first { it.title == "Молоко" }.id)
    FinPetTheme {
        ShopCartContent(
            state = ShopCartViewState(
                storefront = catalog,
                cart = cart,
                balanceRub = 480,
                loading = false,
            ),
            artworkResolver = ShopArtworkResolver.Empty,
            itemDetailsResolver = ShopItemDetailsResolver.Empty,
            onEvent = {},
        )
    }
}
