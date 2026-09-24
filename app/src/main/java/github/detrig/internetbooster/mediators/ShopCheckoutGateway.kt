package github.detrig.internetbooster.mediators

import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.FinancialOperation
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.shop.api.ShopCheckoutRejection
import github.detrig.feature.shop.api.ShopCheckoutRequest
import github.detrig.feature.shop.api.ShopCheckoutResult
import github.detrig.feature.shop.domain.ShopCatalogRegistry
import github.detrig.products.StoreCartLine
import github.detrig.products.StoreId
import github.detrig.products.ProductId
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * App-level bridge between the generic shop and the single economy transaction path.
 * Successful grocery purchases are handed to the inventory boundary after the economy debit.
 */
internal class ShopCheckoutGateway(
    private val catalogRegistry: ShopCatalogRegistry,
    private val currentBalanceRub: suspend () -> Long,
    private val debit: suspend (
        operationId: String,
        amountRub: Long,
        context: OperationContext,
    ) -> FinancialOperationResult,
    private val purchaseHistory: suspend () -> List<FinancialOperation> = { emptyList() },
    private val deliverFood: suspend (
        operationId: String,
        storeId: StoreId,
        lines: List<StoreCartLine>,
    ) -> Unit = { _, _, _ -> },
    private val recordPlanningActual: suspend (
        operationId: String,
        amountRub: Long,
    ) -> Unit = { _, _ -> },
) {
    private val checkoutMutex = Mutex()

    suspend fun checkout(
        request: ShopCheckoutRequest,
        lineTotalOverrides: Map<ProductId, Long> = emptyMap(),
        additionalMetadata: String? = null,
    ): ShopCheckoutResult = checkoutMutex.withLock {
        checkoutLocked(request, lineTotalOverrides, additionalMetadata)
    }

    private suspend fun checkoutLocked(
        request: ShopCheckoutRequest,
        lineTotalOverrides: Map<ProductId, Long>,
        additionalMetadata: String?,
    ): ShopCheckoutResult {
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
        if (lineTotalOverrides.any { (productId, totalRub) ->
                val line = quote.lines.firstOrNull { it.item.id == productId }
                val regularTotal = line?.let {
                    Math.multiplyExact(it.item.priceRub, it.quantity.toLong())
                }
                regularTotal == null || totalRub !in 1L..regularTotal
            }
        ) {
            return rejected(ShopCheckoutRejection.INVALID_CART)
        }
        val totalRub = try {
            quote.lines.fold(0L) { total, line ->
                val lineTotal = lineTotalOverrides[line.item.id]
                    ?: Math.multiplyExact(line.item.priceRub, line.quantity.toLong())
                Math.addExact(total, lineTotal)
            }
        } catch (_: ArithmeticException) {
            return rejected(ShopCheckoutRejection.INVALID_CART)
        }
        val receiptNumber = receiptNumberFor(request.operationId, catalog.storefront.receiptTypeCode.value)
            ?: return rejected(ShopCheckoutRejection.OPERATION_CONFLICT)
        val context = OperationContext(
            reasonId = "shop:${request.storeId.value}:purchase",
            metadata = RECEIPT_METADATA_PREFIX + receiptNumber + ";" + quote.lines
                .sortedBy { it.item.id.value }
                .joinToString(separator = ";") { "${it.item.id.value}=${it.quantity}" } +
                additionalMetadata?.let { ";$LEARNING_METADATA_PREFIX$it" }.orEmpty(),
        )
        return when (val result = debit(request.operationId, totalRub, context)) {
            is FinancialOperationResult.Applied -> {
                deliverFood(request.operationId, request.storeId, request.lines)
                recordPlanningActual(request.operationId, quote.totalRub)
                ShopCheckoutResult.Completed(
                    balanceRub = result.state.availableRub,
                    alreadyApplied = false,
                    receiptNumber = receiptNumber,
                )
            }
            is FinancialOperationResult.AlreadyApplied -> {
                deliverFood(request.operationId, request.storeId, request.lines)
                recordPlanningActual(request.operationId, quote.totalRub)
                ShopCheckoutResult.Completed(
                    balanceRub = result.state.availableRub,
                    alreadyApplied = true,
                    receiptNumber = receiptNumber,
                )
            }
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

    private suspend fun receiptNumberFor(operationId: String, receiptTypeCode: Int): String? {
        val history = purchaseHistory()
        val existingOperation = history.firstOrNull { it.id == operationId }
        if (existingOperation != null) {
            return existingOperation.context.receiptNumberOrNull()
        }

        val nextSequence = history.count { it.isShopPurchase() } + 1
        if (nextSequence > MAX_RECEIPT_SEQUENCE) return null
        return String.format(Locale.ROOT, "%02d%04d", receiptTypeCode, nextSequence)
    }

    private fun FinancialOperation.isShopPurchase(): Boolean {
        return context.reasonId?.let { it.startsWith(SHOP_REASON_PREFIX) && it.endsWith(PURCHASE_REASON_SUFFIX) } == true
    }

    private fun OperationContext.receiptNumberOrNull(): String? {
        return metadata
            ?.substringBefore(';')
            ?.takeIf { it.startsWith(RECEIPT_METADATA_PREFIX) }
            ?.removePrefix(RECEIPT_METADATA_PREFIX)
            ?.takeIf { it.length == RECEIPT_NUMBER_LENGTH && it.all(Char::isDigit) }
    }

    private companion object {
        const val SHOP_REASON_PREFIX = "shop:"
        const val PURCHASE_REASON_SUFFIX = ":purchase"
        const val RECEIPT_METADATA_PREFIX = "receipt="
        const val LEARNING_METADATA_PREFIX = "learning="
        const val RECEIPT_NUMBER_LENGTH = 6
        const val MAX_RECEIPT_SEQUENCE = 9_999
    }
}
