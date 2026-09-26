package github.detrig.internetbooster.mediators

internal data class ShopPlanLine(
    val restoresSatiety: Boolean,
    val totalRub: Long,
)

internal data class ShopPlanActualAmounts(
    val mandatoryRub: Long,
    val wantsRub: Long,
)

internal fun classifyShopPlanActuals(
    lines: List<ShopPlanLine>,
): ShopPlanActualAmounts {
    val mandatoryRub = lines
        .filter(ShopPlanLine::restoresSatiety)
        .sumOf(ShopPlanLine::totalRub)
    return ShopPlanActualAmounts(
        mandatoryRub = mandatoryRub,
        wantsRub = (lines.sumOf(ShopPlanLine::totalRub) - mandatoryRub).coerceAtLeast(0),
    )
}
