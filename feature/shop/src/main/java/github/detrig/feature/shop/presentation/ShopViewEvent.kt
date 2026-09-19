package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.products.ProductId
import github.detrig.products.StoreCategoryId

internal sealed interface ShopViewEvent : CoreViewEvent {
    data object Load : ShopViewEvent
    data object Retry : ShopViewEvent
    data object Back : ShopViewEvent
    data object OpenCart : ShopViewEvent
    data object ReceiptDismissed : ShopViewEvent
    data class CategorySelected(val categoryId: StoreCategoryId?) : ShopViewEvent
    data class ProductClicked(val productId: ProductId) : ShopViewEvent
}
