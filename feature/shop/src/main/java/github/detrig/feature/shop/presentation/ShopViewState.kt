package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.products.ProductId
import github.detrig.products.SellableItem
import github.detrig.products.StoreCart
import github.detrig.products.StoreCategoryId
import github.detrig.products.StorefrontDefinition
import github.detrig.feature.shop.domain.ShopDecisionEvent
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.feature.shop.domain.ShopPromotionKind
import github.detrig.feature.shop.domain.priceLine
import github.detrig.feature.shop.api.ShopPurchaseFeedback

internal enum class ShopError { LOAD }

internal data class ShopViewState(
    val storefront: StorefrontDefinition<SellableItem>? = null,
    val selectedCategoryId: StoreCategoryId? = null,
    val cart: StoreCart = StoreCart.Empty,
    val balanceRub: Long? = null,
    val loading: Boolean = true,
    val error: ShopError? = null,
    val receipt: ShopReceipt? = null,
    val decisionEvent: ShopDecisionEvent? = null,
    val eventDialogueVisible: Boolean = false,
    val purchaseFeedback: ShopPurchaseFeedback? = null,
    val petName: String = "Питомец",
) : CoreViewState {
    val visibleItems: List<SellableItem>
        get() = storefront?.itemsIn(selectedCategoryId).orEmpty()

    val cartTotalRub: Long
        get() {
            val itemsById = storefront?.items?.associateBy { it.id }.orEmpty()
            return cart.lines.fold(0L) { total, line ->
                val item = itemsById[line.itemId] ?: return@fold total
                val linePrice = decisionEvent.priceLine(item.id, item.priceRub, line.quantity)
                Math.addExact(total, linePrice.chargedTotalRub)
            }
        }

    val canAffordCart: Boolean
        get() = balanceRub?.let { it >= cartTotalRub } == true

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
}
