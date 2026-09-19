package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.products.ProductId
import github.detrig.products.SellableItem
import github.detrig.products.StoreCart
import github.detrig.products.StoreCategoryId
import github.detrig.products.StorefrontDefinition

internal enum class ShopError { LOAD }

internal data class ShopViewState(
    val storefront: StorefrontDefinition<SellableItem>? = null,
    val selectedCategoryId: StoreCategoryId? = null,
    val cart: StoreCart = StoreCart.Empty,
    val balanceRub: Long? = null,
    val loading: Boolean = true,
    val error: ShopError? = null,
) : CoreViewState {
    val visibleItems: List<SellableItem>
        get() = storefront?.itemsIn(selectedCategoryId).orEmpty()

    val cartTotalRub: Long
        get() {
            val itemsById = storefront?.items?.associateBy { it.id }.orEmpty()
            return cart.lines.fold(0L) { total, line ->
                val item = itemsById[line.itemId] ?: return@fold total
                Math.addExact(total, Math.multiplyExact(item.priceRub, line.quantity.toLong()))
            }
        }

    val canAffordCart: Boolean
        get() = balanceRub?.let { it >= cartTotalRub } == true

    fun quantityInCart(productId: ProductId): Int = cart.quantityOf(productId)
}
