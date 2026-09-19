package github.detrig.feature.shop.domain

import github.detrig.products.SellableCatalog
import github.detrig.products.SellableItem
import github.detrig.products.StoreId

fun interface ShopCatalogRegistry {
    fun catalog(storeId: StoreId): SellableCatalog<SellableItem>?
}
