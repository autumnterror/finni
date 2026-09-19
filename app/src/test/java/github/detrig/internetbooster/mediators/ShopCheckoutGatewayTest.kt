package github.detrig.internetbooster.mediators

import github.detrig.feature.economy.domain.EconomyState
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.PeriodicIncome
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.shop.api.ShopCheckoutRejection
import github.detrig.feature.shop.api.ShopCheckoutRequest
import github.detrig.feature.shop.api.ShopCheckoutResult
import github.detrig.feature.shop.domain.ShopCatalogRegistry
import github.detrig.products.GroceryCatalog
import github.detrig.products.GroceryItemIds
import github.detrig.products.GroceryStoreIds
import github.detrig.products.StoreCartLine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopCheckoutGatewayTest {
    private val catalog = GroceryCatalog()
    private val registry = ShopCatalogRegistry { storeId ->
        catalog.takeIf { storeId == GroceryStoreIds.Store }
    }
    private val economyState = EconomyState(
        availableRub = 50,
        savingsRub = 0,
        debtRub = 0,
        periodicIncome = PeriodicIncome(100, 1, 1),
    )

    @Test
    fun checkoutUsesCatalogPriceAndEconomyDebit() = runBlocking {
        var chargedAmount = 0L
        var chargedContext: OperationContext? = null
        val gateway = ShopCheckoutGateway(
            catalogRegistry = registry,
            currentBalanceRub = { economyState.availableRub },
            debit = { _, amount, context ->
                chargedAmount = amount
                chargedContext = context
                FinancialOperationResult.Rejected(
                    RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS,
                    economyState,
                )
            },
        )

        val result = gateway.checkout(
            ShopCheckoutRequest(
                operationId = "shop-checkout-1",
                storeId = GroceryStoreIds.Store,
                lines = listOf(
                    StoreCartLine(GroceryItemIds.Apple, 2),
                    StoreCartLine(GroceryItemIds.Soup, 1),
                ),
            ),
        )

        assertEquals(75L, chargedAmount)
        assertEquals("shop:${GroceryStoreIds.Store.value}:purchase", chargedContext?.reasonId)
        assertTrue(chargedContext?.metadata.orEmpty().contains("${GroceryItemIds.Apple.value}=2"))
        assertEquals(
            ShopCheckoutRejection.INSUFFICIENT_FUNDS,
            (result as ShopCheckoutResult.Rejected).reason,
        )
    }

    @Test
    fun emptyCartDoesNotCallEconomyDebit() = runBlocking {
        var debitCalls = 0
        val gateway = ShopCheckoutGateway(
            catalogRegistry = registry,
            currentBalanceRub = { economyState.availableRub },
            debit = { _, _, _ ->
                debitCalls++
                error("Economy must not be called for an empty cart")
            },
        )

        val result = gateway.checkout(
            ShopCheckoutRequest(
                operationId = "shop-checkout-empty",
                storeId = GroceryStoreIds.Store,
                lines = emptyList(),
            ),
        )

        assertEquals(0, debitCalls)
        assertEquals(ShopCheckoutRejection.EMPTY_CART, (result as ShopCheckoutResult.Rejected).reason)
        assertEquals(50L, result.balanceRub)
    }
}
