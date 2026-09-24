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
import github.detrig.feature.shop.domain.ShopDecisionEventStore
import github.detrig.feature.shop.domain.ShopLearningEventConfig
import github.detrig.feature.shop.domain.ShopLearningEventGenerator
import github.detrig.feature.shop.domain.ShopDecisionEventType
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
    fun hostBackIsDeliveredAsScreenCommand() {
        viewModel = createViewModel(
            storeId = GroceryStoreIds.Store,
            useHostBack = true,
        )
        start()

        viewModel.perform(ShopViewEvent.Back)

        assertEquals(
            listOf(ShopCommand.Back),
            viewModel.commands<ShopCommand>().value?.toList(),
        )
        assertEquals(0, router.backCount)
    }

    @Test
    fun trackingFailureDoesNotBlockHostBack() {
        viewModel = createViewModel(
            storeId = GroceryStoreIds.Store,
            eventConfig = ShopLearningEventConfig(
                forcedEventType = ShopDecisionEventType.IMPULSE_WISH,
            ),
            useHostBack = true,
        )
        host.declineFailure = IllegalStateException("tracking failed")
        start()

        viewModel.perform(ShopViewEvent.Back)
        dispatcher.scheduler.runCurrent()

        assertEquals(
            listOf(ShopCommand.Back),
            viewModel.commands<ShopCommand>().value?.toList(),
        )
        assertEquals(null, state().decisionEvent)
        assertEquals(0, router.backCount)
    }

    @Test
    fun promotionPersistsAcrossReentryAndRefreshesWithGameDay() {
        viewModel = createViewModel(
            storeId = GroceryStoreIds.Store,
            eventConfig = ShopLearningEventConfig(
                forcedEventType = ShopDecisionEventType.PROMOTION,
                firstPromotionDelayDays = 0,
                randomSeed = 31,
            ),
            useHostBack = true,
        )
        start()
        val firstDayPromotion = requireNotNull(state().decisionEvent)

        viewModel.perform(ShopViewEvent.EventDialogueFinished)
        viewModel.perform(ShopViewEvent.Back)

        assertEquals(firstDayPromotion, state().decisionEvent)
        assertEquals(0, host.recordDeclinedCalls)

        viewModel.perform(ShopViewEvent.Load)
        dispatcher.scheduler.runCurrent()

        assertEquals(firstDayPromotion, state().decisionEvent)
        assertEquals(1, host.promotionIntroductionClaims)

        val nextDayPromotion = firstDayPromotion.copy(
            eventId = "${firstDayPromotion.eventId}:next-day",
            eventPeriod = firstDayPromotion.eventPeriod + 1,
        )
        host.event = nextDayPromotion
        viewModel.perform(ShopViewEvent.Load)
        dispatcher.scheduler.runCurrent()

        assertEquals(nextDayPromotion, state().decisionEvent)

        host.event = null
        viewModel.perform(ShopViewEvent.Load)
        dispatcher.scheduler.runCurrent()

        assertEquals(null, state().decisionEvent)
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

    @Test
    fun forcedImpulseWishHighlightsTargetWithoutActionDialog() {
        viewModel = createViewModel(
            storeId = GroceryStoreIds.Store,
            eventConfig = ShopLearningEventConfig(
                forcedEventType = ShopDecisionEventType.IMPULSE_WISH,
                randomSeed = 17,
            ),
        )
        start()
        val event = requireNotNull(state().decisionEvent)
        assertFalse(state().eventDialogueVisible)

        viewModel.perform(ShopViewEvent.ProductClicked(event.productId))
        dispatcher.scheduler.runCurrent()

        assertEquals(1, state().quantityInCart(event.productId))
    }

    @Test
    fun firstPromotionUsesInformationalDialogueWithoutDecisionButtons() {
        viewModel = createViewModel(
            storeId = GroceryStoreIds.Store,
            eventConfig = ShopLearningEventConfig(
                forcedEventType = ShopDecisionEventType.PROMOTION,
                firstPromotionDelayDays = 0,
                randomSeed = 21,
            ),
        )

        start()

        assertEquals(ShopDecisionEventType.PROMOTION, state().decisionEvent?.type)
        assertTrue(state().eventDialogueVisible)
        viewModel.perform(ShopViewEvent.EventDialogueFinished)
        assertFalse(state().eventDialogueVisible)
    }

    @Test
    fun badPurchaseFeedbackAppearsAfterReceiptAndBeforeClosingShop() {
        start()
        val item = catalog.storefront.items.first()
        receiptStore.show(
            GroceryStoreIds.Store,
            ShopReceipt(
                number = "123456",
                storeTitle = catalog.storefront.title,
                lines = listOf(ShopReceiptLine(item.title, item.priceRub, 1)),
                feedback = github.detrig.feature.shop.api.ShopPurchaseFeedback.REQUIRED_FOOD_MISSING,
            ),
        )
        dispatcher.scheduler.runCurrent()

        viewModel.perform(ShopViewEvent.ReceiptDismissed)
        dispatcher.scheduler.runCurrent()

        assertEquals(
            github.detrig.feature.shop.api.ShopPurchaseFeedback.REQUIRED_FOOD_MISSING,
            state().purchaseFeedback,
        )
        assertEquals(0, router.closeToRoomCount)

        viewModel.perform(ShopViewEvent.PurchaseFeedbackFinished)
        assertEquals(1, router.closeToRoomCount)
    }

    private fun createViewModel(
        storeId: StoreId,
        eventConfig: ShopLearningEventConfig = ShopLearningEventConfig(
            promotionEventProbability = 0.0,
            impulseWishEventProbability = 0.0,
        ),
        useHostBack: Boolean = false,
    ): ShopViewModel {
        host.event = ShopLearningEventGenerator(eventConfig).eventFor(
            storefront = catalog.storefront,
            gamePeriod = 1,
            eventPeriod = 1,
        )
        return ShopViewModel(
            storeId = storeId,
            catalogRegistry = ShopCatalogRegistry { requestedId ->
                catalog.takeIf { it.storefront.storeId == requestedId }
            },
            host = host,
            cartStore = cartStore,
            decisionEventStore = ShopDecisionEventStore(),
            receiptStore = receiptStore,
            router = router,
            useHostBack = useHostBack,
        )
    }

    private fun start() {
        viewModel.perform(ShopViewEvent.Load)
        dispatcher.scheduler.runCurrent()
    }

    private fun state(): ShopViewState = requireNotNull(viewModel.state().value)

    private class FakeHost : ShopHost {
        val balance = MutableStateFlow(480L)
        var event: github.detrig.feature.shop.domain.ShopDecisionEvent? = null
        var declineFailure: Throwable? = null
        var promotionIntroductionClaims = 0
        var recordDeclinedCalls = 0

        override suspend fun preparePlayer() = Unit

        override fun observeBalanceRub(): Flow<Long> = balance

        override fun observePetName(): Flow<String> = MutableStateFlow("Пончик")

        override suspend fun currentDecisionEvent(storeId: StoreId) = event

        override suspend fun claimPromotionIntroduction(): Boolean {
            promotionIntroductionClaims++
            return true
        }

        override suspend fun recordEventDeclined(
            event: github.detrig.feature.shop.domain.ShopDecisionEvent,
        ) {
            recordDeclinedCalls++
            declineFailure?.let { throw it }
        }

        override suspend fun checkout(request: ShopCheckoutRequest): ShopCheckoutResult =
            error("Checkout is not used by the catalog screen")
    }

    private class FakeRouter : ShopRouter {
        var backCount = 0
        var closeToRoomCount = 0
        var openedCartStoreId: StoreId? = null

        override fun open(storeId: StoreId) = Unit

        override fun openCart(storeId: StoreId) {
            openedCartStoreId = storeId
        }

        override fun back() {
            backCount++
        }

        override fun closeToRoom() {
            closeToRoomCount++
        }
    }
}
