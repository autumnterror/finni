package github.detrig.feature.shop.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import github.detrig.core.exception.CoreErrorHandler
import github.detrig.core.exception.mapper.CoreExceptionMapper
import github.detrig.core.infrastructure.network.NetworkManager
import github.detrig.feature.shop.api.ShopHost
import github.detrig.feature.shop.api.ShopCheckoutRequest
import github.detrig.feature.shop.api.ShopCheckoutResult
import github.detrig.feature.shop.domain.ShopCatalogRegistry
import github.detrig.feature.shop.domain.ShopCartStore
import github.detrig.feature.shop.navigation.ShopRouter
import github.detrig.products.GroceryCatalog
import github.detrig.products.GroceryCategoryIds
import github.detrig.products.GroceryStoreIds
import github.detrig.products.StoreCategoryId
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {
    @get:Rule
    val liveData = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val catalog = GroceryCatalog()
    private lateinit var host: FakeHost
    private lateinit var router: FakeRouter
    private lateinit var cartStore: ShopCartStore
    private lateinit var receiptStore: ShopReceiptStore
    private lateinit var viewModel: ShopViewModel

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
        receiptStore = ShopReceiptStore()
        viewModel = createViewModel(GroceryStoreIds.Store)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAndCategorySelectionUseDataDrivenCatalog() {
        start()

        assertEquals(39, state().visibleItems.size)
        assertEquals(480L, state().balanceRub)
        assertFalse(state().loading)

        viewModel.perform(ShopViewEvent.CategorySelected(GroceryCategoryIds.Meals))
        assertEquals(10, state().visibleItems.size)
        assertTrue(state().visibleItems.all { it.categoryId == GroceryCategoryIds.Meals })

        viewModel.perform(
            ShopViewEvent.CategorySelected(StoreCategoryId("store.grocery.category.unknown")),
        )
        assertEquals(GroceryCategoryIds.Meals, state().selectedCategoryId)
    }

    @Test
    fun balanceUpdatesWithoutAnyCartAction() {
        start()

        host.balance.value = 12_500L
        dispatcher.scheduler.runCurrent()

        assertEquals(12_500L, state().balanceRub)
    }

    @Test
    fun productClickAddsOneMoreUnitToSharedCart() {
        start()
        val appleId = catalog.storefront.items.first().id

        viewModel.perform(ShopViewEvent.ProductClicked(appleId))
        dispatcher.scheduler.runCurrent()
        assertEquals(1, state().quantityInCart(appleId))
        assertEquals(15L, state().cartTotalRub)
        assertTrue(state().canAffordCart)

        viewModel.perform(ShopViewEvent.ProductClicked(appleId))
        dispatcher.scheduler.runCurrent()
        assertEquals(2, state().quantityInCart(appleId))
        assertEquals(30L, state().cartTotalRub)

        viewModel.perform(
            ShopViewEvent.ProductClicked(github.detrig.products.ProductId("unknown.product")),
        )
        assertEquals(2, state().cart.totalQuantity)
    }

    @Test
    fun backDelegatesToFeatureRouter() {
        start()

        viewModel.perform(ShopViewEvent.Back)

        assertEquals(1, router.backCount)
    }

    @Test
    fun openCartDelegatesToFeatureRouter() {
        start()

        viewModel.perform(ShopViewEvent.OpenCart)

        assertEquals(GroceryStoreIds.Store, router.openedCartStoreId)
    }

    @Test
    fun unknownStoreShowsLoadError() {
        viewModel = createViewModel(StoreId("store.unknown"))

        start()

        assertFalse(state().loading)
        assertEquals(ShopError.LOAD, state().error)
    }

    private fun createViewModel(storeId: StoreId) = ShopViewModel(
        storeId = storeId,
        catalogRegistry = ShopCatalogRegistry { requestedId ->
            catalog.takeIf { it.storefront.storeId == requestedId }
        },
        host = host,
        cartStore = cartStore,
        receiptStore = receiptStore,
        router = router,
    )

    private fun start() {
        viewModel.perform(ShopViewEvent.Load)
        dispatcher.scheduler.runCurrent()
    }

    private fun state(): ShopViewState = requireNotNull(viewModel.state().value)

    private class FakeHost : ShopHost {
        val balance = MutableStateFlow(480L)

        override suspend fun preparePlayer() = Unit

        override fun observeBalanceRub(): Flow<Long> = balance

        override suspend fun checkout(request: ShopCheckoutRequest): ShopCheckoutResult =
            error("Checkout is not used by the catalog screen")
    }

    private class FakeRouter : ShopRouter {
        var backCount = 0
        var openedCartStoreId: StoreId? = null

        override fun open(storeId: StoreId) = Unit

        override fun openCart(storeId: StoreId) {
            openedCartStoreId = storeId
        }

        override fun back() {
            backCount++
        }

        override fun closeToRoom() = Unit
    }
}
