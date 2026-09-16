package github.detrig.feature.productmarket.api

import github.detrig.products.ProductQuantity

/** Итог сверки, а не оплаченный инвентарь. tripId позволяет распознать повторную доставку. */
data class MarketTripResult(
    val tripId: String,
    val items: List<ProductQuantity>,
    val missing: List<ProductQuantity>,
)
