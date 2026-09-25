package github.detrig.feature.shop.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import github.detrig.core.exception.CoreErrorHandler
import github.detrig.core.exception.mapper.CoreExceptionMapper
import github.detrig.core.infrastructure.network.NetworkManager
import github.detrig.feature.shop.api.ShopCheckoutRejection
import github.detrig.feature.shop.api.ShopCheckoutRequest
import github.detrig.feature.shop.api.ShopCheckoutResult
import github.detrig.feature.shop.api.ShopHost
import github.detrig.feature.shop.domain.ShopCartStore
import github.detrig.feature.shop.domain.ShopCatalogRegistry
import github.detrig.feature.shop.domain.ShopDecisionEvent
import github.detrig.feature.shop.domain.ShopDecisionEventStore
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.feature.shop.domain.ShopPromotionKind
import github.detrig.feature.shop.navigation.ShopRouter
import github.detrig.products.GroceryCatalog
import github.detrig.products.GroceryStoreIds
import github.detrig.products.StoreId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopCartViewModelTest {
    @get:Rule
    val liveData = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val catalog = GroceryCatalog()
    private lateinit var host: FakeHost
    private lateinit var router: FakeRouter
    private lateinit var cartStore: ShopCartStore
    private lateinit var decisionEventStore: ShopDecisionEventStore
    private lateinit var receiptStore: ShopReceiptStore
    private lateinit var viewModel: ShopCartViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        CoreErrorHandler.init(
            handler = { throw it },
            exceptionMapper = CoreExceptionMapper(
                object : NetworkManager {
                    override fun isNetworkAvailable() = true
                },
            ),
        )
        host = FakeHost()
        router = FakeRouter()
        cartStore = ShopCartStore()
        decisionEventStore = ShopDecisionEventStore()
        receiptStore = ShopReceiptStore()
        viewModel = createViewModel()
    }

    private fun createViewModel(
        useHostBack: Boolean = false,
        useHostCheckoutCompleted: Boolean = false,
    ) = ShopCartViewModel(
        storeId = GroceryStoreIds.Store,
        catalogRegistry = ShopCatalogRegistry { requestedId ->
            catalog.takeIf { it.storefront.storeId == requestedId }
        },
        host = host,
        cartStore = cartStore,
        decisionEventStore = decisionEventStore,
        receiptStore = receiptStore,
        router = router,
        useHostBack = useHostBack,
        useHostCheckoutCompleted = useHostCheckoutCompleted,
    )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun checkoutUsesSingleEconomyOperationAndClearsOnlyPurchasedLines() {
        val apple = catalog.storefront.items.first()
        cartStore.add(GroceryStoreIds.Store, apple.id)
        cartStore.add(GroceryStoreIds.Store, apple.id)
        start()

        assertEquals(30L, state().totalRub)
        assertTrue(state().canPay)

        viewModel.perform(ShopCartViewEvent.PayClicked)
        viewModel.perform(ShopCartViewEvent.PayClicked)
        dispatcher.scheduler.runCurrent()

        assertEquals(1, host.checkoutCalls)
        assertTrue(host.lastRequest?.operationId?.startsWith("shop-checkout:") == true)
        assertEquals(2, host.lastRequest?.lines?.single()?.quantity)
        assertTrue(state().cart.isEmpty)
        assertEquals(450L, state().balanceRub)
        assertEquals(1, router.backCount)
        assertEquals(0, router.closeToRoomCount)
    }

    @Test
    fun insufficientFundsStayInCartAndExplainTheShortfall() {
        val apple = catalog.storefront.items.first()
        host.balance.value = 10L
        cartStore.add(GroceryStoreIds.Store, apple.id)
        start()

        viewModel.perform(ShopCartViewEvent.PayClicked)

        assertEquals(ShopCheckoutRejection.INSUFFICIENT_FUNDS, state().checkoutRejection)
        assertEquals(5L, state().shortfallRub)
        assertEquals(1, state().cart.quantityOf(apple.id))
        assertEquals(0, host.checkoutCalls)
    }

    @Test
    fun promotionRemainsAvailableAfterSuccessfulCheckout() {
        val apple = catalog.storefront.items.first()
        val promotion = ShopDecisionEvent(
            eventId = "promotion:day-1:${apple.id.value}",
            type = ShopDecisionEventType.PROMOTION,
            gamePeriod = 1,
            eventPeriod = 1,
            storeId = GroceryStoreIds.Store,
            productId = apple.id,
            productTitle = apple.title,
            regularPriceRub = apple.priceRub,
            offeredPriceRub = apple.priceRub - 1,
            promotionKind = ShopPromotionKind.PERCENT_DISCOUNT,
        )
        decisionEventStore.set(promotion)
        cartStore.add(GroceryStoreIds.Store, apple.id)
        start()

        viewModel.perform(ShopCartViewEvent.PayClicked)
        dispatcher.scheduler.runCurrent()

        assertEquals(promotion, state().decisionEvent)
        assertEquals(promotion, host.lastRequest?.decisionEvent)
    }

    @Test
    fun embeddedCheckoutRequestsImmediateReturnToReceipt() {
        val apple = catalog.storefront.items.first()
        cartStore.add(GroceryStoreIds.Store, apple.id)
        viewModel = createViewModel(useHostCheckoutCompleted = true)
        start()

        viewModel.perform(ShopCartViewEvent.PayClicked)
        dispatcher.scheduler.runCurrent()

        assertEquals(
            listOf(ShopCartCommand.CheckoutCompleted),
            viewModel.commands<ShopCartCommand>().value?.toList(),
        )
        assertEquals(0, router.backCount)
        assertTrue(state().cart.isEmpty)
    }

    @Test
    fun decrementRemovesAProductWhenItsQuantityReachesZero() {
        val apple = catalog.storefront.items.first()
        cartStore.add(GroceryStoreIds.Store, apple.id)
        start()

        viewModel.perform(ShopCartViewEvent.Decrease(apple.id))
        dispatcher.scheduler.runCurrent()

        assertTrue(state().cart.isEmpty)
        assertTrue(state().lines.isEmpty())
    }

    private fun start() {
        viewModel.perform(ShopCartViewEvent.Load)
        dispatcher.scheduler.runCurrent()
    }

    private fun state(): ShopCartViewState = requireNotNull(viewModel.state().value)

    private class FakeHost : ShopHost {
        val balance = MutableStateFlow(480L)
        var checkoutCalls = 0
        var lastRequest: ShopCheckoutRequest? = null

        override suspend fun preparePlayer() = Unit

        override fun observeBalanceRub(): Flow<Long> = balance

        override fun observePetName(): Flow<String> = MutableStateFlow("Пончик")

        override suspend fun currentDecisionEvent(storeId: StoreId) = null

        override suspend fun claimPromotionIntroduction() = false

        override suspend fun recordEventDeclined(
            event: github.detrig.feature.shop.domain.ShopDecisionEvent,
        ) = Unit

        override suspend fun checkout(request: ShopCheckoutRequest): ShopCheckoutResult {
            checkoutCalls++
            lastRequest = request
            balance.value = 450L
            return ShopCheckoutResult.Completed(
                balanceRub = 450L,
                alreadyApplied = false,
                receiptNumber = "010001",
            )
        }
    }

    private class FakeRouter : ShopRouter {
        var backCount = 0
        var closeToRoomCount = 0

        override fun open(storeId: StoreId) = Unit

        override fun openCart(storeId: StoreId) = Unit

        override fun back() {
            backCount++
        }

        override fun closeToRoom() {
            closeToRoomCount++
        }
    }
}
