package github.detrig.internetbooster.mediators

import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.shop.api.ShopCheckoutRejection
import github.detrig.feature.shop.api.ShopCheckoutRequest
import github.detrig.feature.shop.api.ShopCheckoutResult
import github.detrig.feature.shop.domain.ShopCatalogRegistry

/**
 * App-level bridge between the generic shop and the single economy transaction path.
 * Inventory delivery will be coordinated here when checkout UI is introduced.
 */
internal class ShopCheckoutGateway(
    private val catalogRegistry: ShopCatalogRegistry,
    private val currentBalanceRub: suspend () -> Long,
    private val debit: suspend (
        operationId: String,
        amountRub: Long,
        context: OperationContext,
    ) -> FinancialOperationResult,
) {
    suspend fun checkout(request: ShopCheckoutRequest): ShopCheckoutResult {
        val catalog = catalogRegistry.catalog(request.storeId)
            ?: return rejected(ShopCheckoutRejection.INVALID_CART)
        if (request.lines.isEmpty()) {
            return rejected(ShopCheckoutRejection.EMPTY_CART)
        }
        val quote = try {
            catalog.quote(request.lines)
        } catch (_: IllegalArgumentException) {
            return rejected(ShopCheckoutRejection.INVALID_CART)
        } catch (_: ArithmeticException) {
            return rejected(ShopCheckoutRejection.INVALID_CART)
        }
        val context = OperationContext(
            reasonId = "shop:${request.storeId.value}:purchase",
            metadata = quote.lines
                .sortedBy { it.item.id.value }
                .joinToString(separator = ";") { "${it.item.id.value}=${it.quantity}" },
        )
        return when (val result = debit(request.operationId, quote.totalRub, context)) {
            is FinancialOperationResult.Applied -> ShopCheckoutResult.Completed(
                balanceRub = result.state.availableRub,
                alreadyApplied = false,
            )
            is FinancialOperationResult.AlreadyApplied -> ShopCheckoutResult.Completed(
                balanceRub = result.state.availableRub,
                alreadyApplied = true,
            )
            is FinancialOperationResult.Rejected -> ShopCheckoutResult.Rejected(
                reason = when (result.reason) {
                    RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS ->
                        ShopCheckoutRejection.INSUFFICIENT_FUNDS
                    RejectionReason.OPERATION_ID_CONFLICT,
                    RejectionReason.INVALID_OPERATION_ID ->
                        ShopCheckoutRejection.OPERATION_CONFLICT
                    else -> ShopCheckoutRejection.INVALID_CART
                },
                balanceRub = result.state.availableRub,
            )
        }
    }

    private suspend fun rejected(
        reason: ShopCheckoutRejection,
    ): ShopCheckoutResult.Rejected {
        return ShopCheckoutResult.Rejected(reason, currentBalanceRub())
    }
}
