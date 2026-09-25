package github.detrig.internetbooster.mediators

import github.detrig.products.ProductId

internal data class ShopPlanLine(
    val productId: ProductId,
    val isFood: Boolean,
    val totalRub: Long,
)

internal data class ShopPlanActualAmounts(
    val mandatoryRub: Long,
    val wantsRub: Long,
)

internal fun classifyShopPlanActuals(
    lines: List<ShopPlanLine>,
    impulseWishProductId: ProductId?,
): ShopPlanActualAmounts {
    val mandatoryRub = lines
        .filter { it.isFood && it.productId != impulseWishProductId }
        .sumOf(ShopPlanLine::totalRub)
    return ShopPlanActualAmounts(
        mandatoryRub = mandatoryRub,
        wantsRub = (lines.sumOf(ShopPlanLine::totalRub) - mandatoryRub).coerceAtLeast(0),
    )
}
