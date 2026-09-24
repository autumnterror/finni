package github.detrig.feature.shop.di

import github.detrig.feature.shop.api.ShopApi
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.feature.shop.api.ShopItemDetailsResolver
import github.detrig.feature.shop.api.ShopPetPortrait
import github.detrig.feature.shop.presentation.ShopViewModel
import github.detrig.feature.shop.presentation.ShopCartViewModel
import github.detrig.products.StoreId

internal interface ShopComponent {
    val api: ShopApi
    val artworkResolver: ShopArtworkResolver
    val itemDetailsResolver: ShopItemDetailsResolver
    val petPortrait: ShopPetPortrait
    fun viewModel(
        storeId: StoreId,
        useHostBack: Boolean = false,
        useHostCart: Boolean = false,
        useHostCloseAfterReceipt: Boolean = false,
    ): ShopViewModel
    fun cartViewModel(
        storeId: StoreId,
        useHostBack: Boolean = false,
        useHostCheckoutCompleted: Boolean = false,
    ): ShopCartViewModel
}
