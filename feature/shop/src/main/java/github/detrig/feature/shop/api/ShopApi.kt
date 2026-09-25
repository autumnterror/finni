package github.detrig.feature.shop.api

import androidx.compose.runtime.Composable
import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.products.StoreId
import github.detrig.products.ProductId

interface ShopApi {
    fun open(storeId: StoreId)
    fun entries(): EntryHostProviderInstaller

    /** Renders the catalog inside another bounded surface, such as the phone display. */
    @Composable
    fun Content(
        storeId: StoreId,
        onBack: () -> Unit,
        onOpenCart: () -> Unit,
        closeAfterReceipt: () -> Unit,
        highlightedProductId: ProductId? = null,
        onProductSelected: (ProductId) -> Unit = {},
        tutorialMessage: String? = null,
    )

    /** Renders the cart without taking over the host navigation stack. */
    @Composable
    fun CartContent(
        storeId: StoreId,
        onBack: () -> Unit,
        onCheckoutCompleted: (spentRub: Long) -> Unit,
    )
}
