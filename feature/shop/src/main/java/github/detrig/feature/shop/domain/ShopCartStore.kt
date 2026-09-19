package github.detrig.feature.shop.domain

import github.detrig.products.ProductId
import github.detrig.products.StoreCart
import github.detrig.products.StoreId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory cart session owned by the shop feature graph.
 *
 * A cart is scoped to a storefront, allowing food, clothes and later storefronts
 * to share the same checkout UI without mixing their products.
 */
internal class ShopCartStore {
    private val carts = MutableStateFlow<Map<StoreId, StoreCart>>(emptyMap())

    fun observe(storeId: StoreId): Flow<StoreCart> = carts
        .map { it[storeId] ?: StoreCart.Empty }
        .distinctUntilChanged()

    fun add(storeId: StoreId, productId: ProductId) {
        carts.update { current ->
            current + (storeId to (current[storeId] ?: StoreCart.Empty).add(productId))
        }
    }

    fun removeOne(storeId: StoreId, productId: ProductId) {
        updateCart(storeId) { it.removeOne(productId) }
    }

    fun removePurchased(storeId: StoreId, cart: StoreCart) {
        updateCart(storeId) { current ->
            cart.lines.fold(current) { updated, line ->
                updated.remove(line.itemId, line.quantity)
            }
        }
    }

    private fun updateCart(storeId: StoreId, transform: (StoreCart) -> StoreCart) {
        carts.update { current ->
            val updated = transform(current[storeId] ?: StoreCart.Empty)
            if (updated.isEmpty) current - storeId else current + (storeId to updated)
        }
    }
}
