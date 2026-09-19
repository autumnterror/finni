package github.detrig.feature.shop.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.products.StoreId

interface ShopApi {
    fun open(storeId: StoreId)
    fun entries(): EntryHostProviderInstaller
}
