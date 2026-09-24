package github.detrig.feature.shop.api

import github.detrig.products.StoreCartLine
import github.detrig.products.StoreId
import github.detrig.feature.shop.domain.ShopDecisionEvent
import kotlinx.coroutines.flow.Flow

interface ShopHost {
    suspend fun preparePlayer()
    fun observeBalanceRub(): Flow<Long>
    fun observePetName(): Flow<String>
    suspend fun currentDecisionEvent(storeId: StoreId): ShopDecisionEvent?
    suspend fun claimPromotionIntroduction(): Boolean
    suspend fun recordEventDeclined(event: ShopDecisionEvent)
    suspend fun checkout(request: ShopCheckoutRequest): ShopCheckoutResult
}

/** A stable operation id makes a repeated checkout safe for the economy module. */
data class ShopCheckoutRequest(
    val operationId: String,
    val storeId: StoreId,
    val lines: List<StoreCartLine>,
    val decisionEvent: ShopDecisionEvent? = null,
)

sealed interface ShopCheckoutResult {
    val balanceRub: Long

    data class Completed(
        override val balanceRub: Long,
        val alreadyApplied: Boolean,
        /** Six-digit number persisted with the corresponding economy transaction. */
        val receiptNumber: String,
        val feedback: ShopPurchaseFeedback? = null,
    ) : ShopCheckoutResult

    data class Rejected(
        val reason: ShopCheckoutRejection,
        override val balanceRub: Long,
    ) : ShopCheckoutResult
}

enum class ShopPurchaseFeedback {
    REQUIRED_FOOD_MISSING,
    MANDATORY_MONEY_AT_RISK,
    PROMOTION_OVERBUY,
    TOO_MANY_EXTRAS,
}

enum class ShopCheckoutRejection {
    EMPTY_CART,
    INVALID_CART,
    INSUFFICIENT_FUNDS,
    OPERATION_CONFLICT,
}
