package github.detrig.feature.shop.api

import github.detrig.products.StoreCartLine
import github.detrig.products.StoreId
import kotlinx.coroutines.flow.Flow

interface ShopHost {
    suspend fun preparePlayer()
    fun observeBalanceRub(): Flow<Long>
    suspend fun checkout(request: ShopCheckoutRequest): ShopCheckoutResult
}

/** A stable operation id makes a repeated checkout safe for the economy module. */
data class ShopCheckoutRequest(
    val operationId: String,
    val storeId: StoreId,
    val lines: List<StoreCartLine>,
)

sealed interface ShopCheckoutResult {
    val balanceRub: Long

    data class Completed(
        override val balanceRub: Long,
        val alreadyApplied: Boolean,
        /** Six-digit number persisted with the corresponding economy transaction. */
        val receiptNumber: String,
    ) : ShopCheckoutResult

    data class Rejected(
        val reason: ShopCheckoutRejection,
        override val balanceRub: Long,
    ) : ShopCheckoutResult
}

enum class ShopCheckoutRejection {
    EMPTY_CART,
    INVALID_CART,
    INSUFFICIENT_FUNDS,
    OPERATION_CONFLICT,
}
