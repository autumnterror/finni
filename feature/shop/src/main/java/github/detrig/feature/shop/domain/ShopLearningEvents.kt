package github.detrig.feature.shop.domain

import github.detrig.products.ProductId
import github.detrig.products.SellableItem
import github.detrig.products.StoreId
import github.detrig.products.StorefrontDefinition
import kotlin.random.Random

enum class ShopDecisionEventType {
    PROMOTION,
    IMPULSE_WISH,
}

enum class ShopPromotionKind {
    PERCENT_DISCOUNT,
    BUY_TWO_GET_ONE_FREE,
}

data class ShopLinePrice(
    val regularUnitPriceRub: Long,
    val quantity: Int,
    val chargedTotalRub: Long,
    val freeQuantity: Int = 0,
) {
    val regularTotalRub: Long = Math.multiplyExact(regularUnitPriceRub, quantity.toLong())
    val savingRub: Long = regularTotalRub - chargedTotalRub

    init {
        require(regularUnitPriceRub > 0)
        require(quantity > 0)
        require(freeQuantity in 0..quantity)
        require(chargedTotalRub > 0)
        require(chargedTotalRub <= regularTotalRub)
    }
}

/** One deterministic event for a game day. It may be safely recreated after process death. */
data class ShopDecisionEvent(
    val eventId: String,
    val type: ShopDecisionEventType,
    val gamePeriod: Long,
    val eventPeriod: Long,
    val storeId: StoreId,
    val productId: ProductId,
    val productTitle: String,
    val regularPriceRub: Long,
    val offeredPriceRub: Long,
    val promotionKind: ShopPromotionKind = ShopPromotionKind.PERCENT_DISCOUNT,
) {
    init {
        require(eventId.isNotBlank())
        require(gamePeriod >= 1)
        require(eventPeriod >= 1)
        require(productTitle.isNotBlank())
        require(regularPriceRub > 0)
        require(offeredPriceRub > 0)
        require(offeredPriceRub <= regularPriceRub)
        if (type == ShopDecisionEventType.PROMOTION) {
            when (promotionKind) {
                ShopPromotionKind.PERCENT_DISCOUNT -> require(offeredPriceRub < regularPriceRub)
                ShopPromotionKind.BUY_TWO_GET_ONE_FREE -> require(offeredPriceRub == regularPriceRub)
            }
        }
    }

    val discountRub: Long get() = regularPriceRub - offeredPriceRub

    val minimumPromotionQuantity: Int
        get() = when (promotionKind) {
            ShopPromotionKind.PERCENT_DISCOUNT -> 1
            ShopPromotionKind.BUY_TWO_GET_ONE_FREE -> 3
        }
}

fun ShopDecisionEvent?.priceLine(
    productId: ProductId,
    regularUnitPriceRub: Long,
    quantity: Int,
): ShopLinePrice {
    require(regularUnitPriceRub > 0)
    require(quantity > 0)
    val regularTotal = Math.multiplyExact(regularUnitPriceRub, quantity.toLong())
    val promotion = this?.takeIf {
        it.type == ShopDecisionEventType.PROMOTION &&
            it.productId == productId &&
            it.regularPriceRub == regularUnitPriceRub
    }
    if (promotion == null) {
        return ShopLinePrice(
            regularUnitPriceRub = regularUnitPriceRub,
            quantity = quantity,
            chargedTotalRub = regularTotal,
        )
    }
    return when (promotion.promotionKind) {
        ShopPromotionKind.PERCENT_DISCOUNT -> ShopLinePrice(
            regularUnitPriceRub = regularUnitPriceRub,
            quantity = quantity,
            chargedTotalRub = Math.multiplyExact(promotion.offeredPriceRub, quantity.toLong()),
        )
        ShopPromotionKind.BUY_TWO_GET_ONE_FREE -> {
            val freeQuantity = quantity / 3
            ShopLinePrice(
                regularUnitPriceRub = regularUnitPriceRub,
                quantity = quantity,
                chargedTotalRub = Math.multiplyExact(
                    regularUnitPriceRub,
                    (quantity - freeQuantity).toLong(),
                ),
                freeQuantity = freeQuantity,
            )
        }
    }
}

/** Probabilities are intentionally independent so debug builds can force each scenario separately. */
data class ShopLearningEventConfig(
    val promotionEventProbability: Double = 0.25,
    val impulseWishEventProbability: Double = 0.25,
    /** Day 1 plus this delay is the first day on which a promotion may appear. */
    val firstPromotionDelayDays: Long = 3,
    /** Reserved until receipt checking has an actual player decision. */
    val receiptCheckEventProbability: Double = 0.0,
    val promotionDiscountPercent: Int = 30,
    /** Conditional probability of 2+1 after a promotion event has already been selected. */
    val buyTwoGetOnePromotionProbability: Double = 0.5,
    val randomSeed: Long = 6_202L,
    val forcedEventType: ShopDecisionEventType? = null,
    val forcedPromotionKind: ShopPromotionKind? = null,
) {
    init {
        require(promotionEventProbability in 0.0..1.0)
        require(impulseWishEventProbability in 0.0..1.0)
        require(firstPromotionDelayDays in 0 until Long.MAX_VALUE)
        require(receiptCheckEventProbability in 0.0..1.0)
        require(promotionDiscountPercent in 1..99)
        require(buyTwoGetOnePromotionProbability in 0.0..1.0)
    }
}

class ShopLearningEventGenerator(
    private val config: ShopLearningEventConfig,
) {
    fun eventFor(
        storefront: StorefrontDefinition<SellableItem>,
        gamePeriod: Long,
        eventPeriod: Long,
    ): ShopDecisionEvent? {
        if (storefront.items.isEmpty()) return null
        val random = Random(config.randomSeed xor eventPeriod xor storefront.storeId.value.hashCode().toLong())
        val firstPromotionDay = config.firstPromotionDelayDays + 1
        val promotionEligible = eventPeriod >= firstPromotionDay
        val guaranteedFirstPromotion = eventPeriod == firstPromotionDay
        if (config.forcedEventType == ShopDecisionEventType.PROMOTION && !promotionEligible) return null
        val type = config.forcedEventType ?: when {
            guaranteedFirstPromotion -> ShopDecisionEventType.PROMOTION
            promotionEligible && random.nextDouble() < config.promotionEventProbability ->
                ShopDecisionEventType.PROMOTION
            random.nextDouble() < config.impulseWishEventProbability -> ShopDecisionEventType.IMPULSE_WISH
            else -> return null
        }
        val promotionKind = if (type == ShopDecisionEventType.PROMOTION) {
            config.forcedPromotionKind ?: if (
                random.nextDouble() < config.buyTwoGetOnePromotionProbability
            ) {
                ShopPromotionKind.BUY_TWO_GET_ONE_FREE
            } else {
                ShopPromotionKind.PERCENT_DISCOUNT
            }
        } else {
            ShopPromotionKind.PERCENT_DISCOUNT
        }
        val candidates = storefront.items
            .filter {
                type != ShopDecisionEventType.PROMOTION ||
                    promotionKind != ShopPromotionKind.PERCENT_DISCOUNT ||
                    it.priceRub > 1
            }
            .sortedBy { it.id.value }
        if (candidates.isEmpty()) return null
        val item = candidates[random.nextInt(candidates.size)]
        val offeredPrice = when {
            type != ShopDecisionEventType.PROMOTION -> item.priceRub
            promotionKind == ShopPromotionKind.PERCENT_DISCOUNT -> discountedPrice(item.priceRub)
            else -> item.priceRub
        }
        val typeKey = type.name.lowercase()
        val offerKey = if (type == ShopDecisionEventType.PROMOTION) {
            ":${promotionKind.name.lowercase()}"
        } else {
            ""
        }
        return ShopDecisionEvent(
            eventId = "shop-event:$eventPeriod:${storefront.storeId.value}:$typeKey$offerKey:${item.id.value}",
            type = type,
            gamePeriod = gamePeriod,
            eventPeriod = eventPeriod,
            storeId = storefront.storeId,
            productId = item.id,
            productTitle = item.title,
            regularPriceRub = item.priceRub,
            offeredPriceRub = offeredPrice,
            promotionKind = promotionKind,
        )
    }

    private fun discountedPrice(regularPriceRub: Long): Long =
        (regularPriceRub * (100 - config.promotionDiscountPercent) / 100)
            .coerceIn(1, regularPriceRub - 1)
}
