package github.detrig.feature.shop.di

import github.detrig.feature.shop.api.ShopApi
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.feature.shop.api.ShopItemDetailsResolver
import github.detrig.feature.shop.presentation.ShopViewModel
import github.detrig.feature.shop.presentation.ShopCartViewModel
import github.detrig.products.StoreId

internal interface ShopComponent {
    val api: ShopApi
    val artworkResolver: ShopArtworkResolver
    val itemDetailsResolver: ShopItemDetailsResolver
    fun viewModel(storeId: StoreId): ShopViewModel
    fun cartViewModel(storeId: StoreId): ShopCartViewModel
}
