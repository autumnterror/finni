package github.detrig.internetbooster.mediators

import github.detrig.feature.learning.domain.PurchaseDecision
import github.detrig.feature.learning.domain.PurchaseDecisionContext
import github.detrig.feature.learning.domain.PurchaseScenario
import java.nio.charset.StandardCharsets
import java.util.Base64

internal data class ShopPurchaseLearningPayload(
    val gamePeriod: Long,
    val sourceOperationId: String,
    val standardPurchase: PurchaseDecisionContext,
    val eventDecision: PurchaseDecisionContext?,
    val mandatoryPlanRub: Long,
    val wantsPlanRub: Long,
)

internal object ShopPurchaseLearningPayloadCodec {
    private const val VERSION = "v2"
    private const val LEGACY_VERSION = "v1"

    fun encode(payload: ShopPurchaseLearningPayload): String {
        val raw = buildString {
            append(VERSION)
            append('\t')
            append(payload.gamePeriod)
            append('\t')
            append(payload.sourceOperationId)
            append('\t')
            append(payload.mandatoryPlanRub)
            append('\t')
            append(payload.wantsPlanRub)
            append('\n')
            append(payload.standardPurchase.encode())
            payload.eventDecision?.let {
                append('\n')
                append(it.encode())
            }
        }
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(encoded: String): ShopPurchaseLearningPayload? = runCatching {
        val raw = String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
        val lines = raw.lines()
        val header = lines.first().split('\t')
        require(
            (header[0] == VERSION && header.size == 5) ||
                (header[0] == LEGACY_VERSION && header.size == 3),
        )
        require(lines.size in 2..3)
        val standard = lines[1].decodeContext()
        ShopPurchaseLearningPayload(
            gamePeriod = header[1].toLong(),
            sourceOperationId = header[2],
            standardPurchase = standard,
            eventDecision = lines.getOrNull(2)?.decodeContext(),
            mandatoryPlanRub = header.getOrNull(3)?.toLong() ?: standard.requiredFoodCostRub,
            wantsPlanRub = header.getOrNull(4)?.toLong() ?: standard.optionalPurchaseRub,
        )
    }.getOrNull()

    private fun PurchaseDecisionContext.encode(): String = listOf(
        scenario.name,
        decision.name,
        balanceBeforeRub,
        balanceAfterPurchaseRub,
        purchaseTotalRub,
        futureMandatoryRub,
        discretionaryRub,
        mandatoryFoodNeeded,
        foodUnitsPurchased,
        requiredFoodCostRub,
        extraUnitsPurchased,
        optionalPurchaseRub,
        eventTargetPurchased,
        eventTargetNeeded,
        eventTargetPriceRub,
        promotionSavingRub,
        eventTargetQuantity,
        eventTargetMinimumQuantity,
    ).joinToString("\t")

    private fun String.decodeContext(): PurchaseDecisionContext {
        val values = split('\t')
        require(values.size == 18)
        return PurchaseDecisionContext(
            scenario = PurchaseScenario.valueOf(values[0]),
            decision = PurchaseDecision.valueOf(values[1]),
            balanceBeforeRub = values[2].toLong(),
            balanceAfterPurchaseRub = values[3].toLong(),
            purchaseTotalRub = values[4].toLong(),
            futureMandatoryRub = values[5].toLong(),
            discretionaryRub = values[6].toLong(),
            mandatoryFoodNeeded = values[7].toBooleanStrict(),
            foodUnitsPurchased = values[8].toInt(),
            requiredFoodCostRub = values[9].toLong(),
            extraUnitsPurchased = values[10].toInt(),
            optionalPurchaseRub = values[11].toLong(),
            eventTargetPurchased = values[12].toBooleanStrict(),
            eventTargetNeeded = values[13].toBooleanStrict(),
            eventTargetPriceRub = values[14].toLong(),
            promotionSavingRub = values[15].toLong(),
            eventTargetQuantity = values[16].toInt(),
            eventTargetMinimumQuantity = values[17].toInt(),
        )
    }
}
