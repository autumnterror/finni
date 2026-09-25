package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.shop.api.ShopCheckoutRejection
import github.detrig.products.ProductId
import github.detrig.products.SellableItem
import github.detrig.products.StoreCart
import github.detrig.products.StorefrontDefinition
import github.detrig.feature.shop.domain.ShopDecisionEvent
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.feature.shop.domain.ShopLinePrice
import github.detrig.feature.shop.domain.ShopPromotionKind
import github.detrig.feature.shop.domain.priceLine

internal enum class ShopCartError { LOAD }

internal data class ShopCartLineUi(
    val item: SellableItem,
    val quantity: Int,
    val unitPriceRub: Long,
    val totalRub: Long,
    val savingRub: Long,
    val freeQuantity: Int,
    val promotionKind: ShopPromotionKind?,
) {
    val hasAppliedPromotion: Boolean get() = savingRub > 0
}

internal data class ShopCartViewState(
    val storefront: StorefrontDefinition<SellableItem>? = null,
    val cart: StoreCart = StoreCart.Empty,
    val balanceRub: Long? = null,
    val loading: Boolean = true,
    val error: ShopCartError? = null,
    val paymentInProgress: Boolean = false,
    val checkoutRejection: ShopCheckoutRejection? = null,
    val decisionEvent: ShopDecisionEvent? = null,
) : CoreViewState {
    val lines: List<ShopCartLineUi>
        get() {
            val itemsById = storefront?.items?.associateBy { it.id }.orEmpty()
            return cart.lines.mapNotNull { line ->
                itemsById[line.itemId]?.let { item ->
                    val price = decisionEvent.priceLine(item.id, item.priceRub, line.quantity)
                    ShopCartLineUi(
                        item = item,
                        quantity = line.quantity,
                        unitPriceRub = effectiveUnitPrice(item.id),
                        totalRub = price.chargedTotalRub,
                        savingRub = price.savingRub,
                        freeQuantity = price.freeQuantity,
                        promotionKind = decisionEvent
                            ?.takeIf {
                                it.type == ShopDecisionEventType.PROMOTION && it.productId == item.id
                            }
                            ?.promotionKind,
                    )
                }
            }
        }

    val totalRub: Long
        get() = lines.fold(0L) { total, line -> Math.addExact(total, line.totalRub) }

    val canPay: Boolean
        get() = !cart.isEmpty && balanceRub?.let { it >= totalRub } == true

    val shortfallRub: Long
        get() = (totalRub - (balanceRub ?: 0L)).coerceAtLeast(0L)

    fun quantityInCart(productId: ProductId): Int = cart.quantityOf(productId)

    fun effectiveUnitPrice(productId: ProductId): Long {
        val item = storefront?.items?.firstOrNull { it.id == productId } ?: return 0
        val event = decisionEvent
        return if (
            event?.type == ShopDecisionEventType.PROMOTION &&
            event.productId == productId &&
            event.promotionKind == ShopPromotionKind.PERCENT_DISCOUNT
        ) event.offeredPriceRub else item.priceRub
    }

    fun priceLine(productId: ProductId, quantity: Int): ShopLinePrice? {
        val item = storefront?.items?.firstOrNull { it.id == productId } ?: return null
        return decisionEvent.priceLine(productId, item.priceRub, quantity)
    }
}
