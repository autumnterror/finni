package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.shop.api.ShopCheckoutRejection
import github.detrig.products.ProductId
import github.detrig.products.SellableItem
import github.detrig.products.StoreCart
import github.detrig.products.StorefrontDefinition

internal enum class ShopCartError { LOAD }

internal data class ShopCartLineUi(
    val item: SellableItem,
    val quantity: Int,
) {
    val totalRub: Long = Math.multiplyExact(item.priceRub, quantity.toLong())
}

internal data class ShopCartViewState(
    val storefront: StorefrontDefinition<SellableItem>? = null,
    val cart: StoreCart = StoreCart.Empty,
    val balanceRub: Long? = null,
    val loading: Boolean = true,
    val error: ShopCartError? = null,
    val paymentInProgress: Boolean = false,
    val checkoutRejection: ShopCheckoutRejection? = null,
) : CoreViewState {
    val lines: List<ShopCartLineUi>
        get() {
            val itemsById = storefront?.items?.associateBy { it.id }.orEmpty()
            return cart.lines.mapNotNull { line ->
                itemsById[line.itemId]?.let { ShopCartLineUi(it, line.quantity) }
            }
        }

    val totalRub: Long
        get() = lines.fold(0L) { total, line -> Math.addExact(total, line.totalRub) }

    val canPay: Boolean
        get() = !cart.isEmpty && balanceRub?.let { it >= totalRub } == true

    val shortfallRub: Long
        get() = (totalRub - (balanceRub ?: 0L)).coerceAtLeast(0L)

    fun quantityInCart(productId: ProductId): Int = cart.quantityOf(productId)
}
