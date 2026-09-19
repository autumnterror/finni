package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.products.ProductId

internal sealed interface ShopCartViewEvent : CoreViewEvent {
    data object Load : ShopCartViewEvent
    data object Retry : ShopCartViewEvent
    data object Back : ShopCartViewEvent
    data class Increase(val productId: ProductId) : ShopCartViewEvent
    data class Decrease(val productId: ProductId) : ShopCartViewEvent
    data object PayClicked : ShopCartViewEvent
    data object CheckoutRejectionDismissed : ShopCartViewEvent
}
