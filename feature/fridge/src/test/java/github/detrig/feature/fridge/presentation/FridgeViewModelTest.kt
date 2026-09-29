package github.detrig.feature.fridge.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import github.detrig.feature.fridge.navigation.FridgeRouter
import github.detrig.feature.inventory.api.DeliveryResult
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.inventory.domain.InventoryStageResult
import github.detrig.feature.inventory.domain.StagedFoodItem
import github.detrig.feature.inventory.domain.StockItem
import github.detrig.feature.inventory.domain.TableFoodConsumptionResult
import github.detrig.feature.room.api.FirstRunGuideApi
import github.detrig.feature.room.api.FirstRunOnboardingStep
import github.detrig.products.ProductId
import github.detrig.products.ProductQuantity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FridgeViewModelTest {
    @get:Rule val liveData = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()
    private val product = ProductId("apple")
    private lateinit var inventory: FakeInventory
    private lateinit var guide: FakeGuide
    private lateinit var viewModel: FridgeViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        inventory = FakeInventory(product, 3)
        guide = FakeGuide()
        viewModel = FridgeViewModel(inventory, object : FridgeRouter {
            override fun open() = Unit
            override fun openFeeding() = Unit
            override fun back() = Unit
        }, guide)
        viewModel.perform(FridgeViewEvent.Load)
        dispatcher.scheduler.runCurrent()
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun clickTransfersOnePortionWithoutWaitingForAnimation() {
        viewModel.perform(FridgeViewEvent.ProductClicked(product))
        dispatcher.scheduler.runCurrent()

        assertEquals(1, inventory.table.value.size)
        assertEquals(2, viewModel.state().value!!.stock.single().quantity)
        assertEquals(listOf(product), viewModel.state().value!!.sessionSlots)
        assertEquals(product, viewModel.state().value!!.flights.single().productId)
        assertEquals(FirstRunOnboardingStep.WAITING_FOR_FRIDGE_CLOSE, guide.step.value)
    }

    @Test fun rapidClicksTransferMultiplePortionsOfTheSameProduct() {
        repeat(2) { viewModel.perform(FridgeViewEvent.ProductClicked(product)) }
        dispatcher.scheduler.runCurrent()

        assertEquals(2, inventory.table.value.size)
        assertEquals(1, viewModel.state().value!!.stock.single().quantity)
        assertEquals(listOf(product), viewModel.state().value!!.sessionSlots)
        val flights = viewModel.state().value!!.flights
        assertEquals(2, flights.size)
        assertEquals(2, flights.map { it.id }.distinct().size)
        assertEquals(listOf(0, 0), flights.map { it.slotIndex })
        assertNull(viewModel.state().value!!.message)
    }

    @Test fun rapidClicksCannotTransferMoreThanTheRemainingStock() {
        repeat(6) { viewModel.perform(FridgeViewEvent.ProductClicked(product)) }
        dispatcher.scheduler.runCurrent()

        assertEquals(3, inventory.table.value.size)
        assertEquals(emptyList<StockItem>(), viewModel.state().value!!.stock)
        assertEquals(listOf<ProductId?>(null), viewModel.state().value!!.sessionSlots)
        val flights = viewModel.state().value!!.flights
        assertEquals(3, flights.size)
        assertEquals(3, flights.map { it.id }.distinct().size)
        assertEquals(listOf(product, product, product), flights.map { it.productId })
        assertEquals(listOf(0, 0, 0), flights.map { it.slotIndex })
    }

    @Test fun finishingOneFlightKeepsOtherFlightsAndDoesNotTransferAnotherPortion() {
        repeat(2) { viewModel.perform(FridgeViewEvent.ProductClicked(product)) }
        dispatcher.scheduler.runCurrent()
        val flights = viewModel.state().value!!.flights

        repeat(2) { viewModel.perform(FridgeViewEvent.FlightAnimationFinished(flights.first().id)) }
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(flights.last()), viewModel.state().value!!.flights)
        assertEquals(2, inventory.table.value.size)
        assertEquals(1, viewModel.state().value!!.stock.single().quantity)
    }

    @Test fun closingFridgeClearsAllFlyingCopies() {
        repeat(2) { viewModel.perform(FridgeViewEvent.ProductClicked(product)) }
        dispatcher.scheduler.runCurrent()

        viewModel.perform(FridgeViewEvent.Back)

        assertEquals(emptyList<FridgeFoodFlight>(), viewModel.state().value!!.flights)
        assertEquals(2, inventory.table.value.size)
    }

    @Test fun pendingTransferDoesNotStartFlightAfterClosingFridge() {
        viewModel.perform(FridgeViewEvent.ProductClicked(product))
        viewModel.perform(FridgeViewEvent.Back)
        dispatcher.scheduler.runCurrent()

        assertEquals(emptyList<FridgeFoodFlight>(), viewModel.state().value!!.flights)
        assertEquals(1, inventory.table.value.size)
    }

    private class FakeInventory(private val product: ProductId, quantity: Int) : InventoryApi {
        val stock = MutableStateFlow(listOf(StockItem(product, quantity)))
        val table = MutableStateFlow(emptyList<StagedFoodItem>())
        private val mutex = Mutex()
        override fun observeStock() = stock
        override fun observeTable() = table
        override suspend fun stageForTable(productId: ProductId): InventoryStageResult = mutex.withLock {
            yield() // Simulate a suspended persistence operation while more taps arrive.
            val item = stock.value.firstOrNull { it.productId == productId }
                ?: return@withLock InventoryStageResult.InsufficientStock
            stock.value = if (item.quantity == 1) emptyList() else listOf(item.copy(quantity = item.quantity - 1))
            table.value += StagedFoodItem("portion:${table.value.size}", product)
            InventoryStageResult.Staged
        }
        override suspend fun resetProgress() = Unit
        override suspend fun deliver(operationId: String, items: List<ProductQuantity>): DeliveryResult = error("Unused")
        override suspend fun reconcileTableExpiry() = false
        override suspend fun acknowledgeSpoiledTableFood() = Unit
        override suspend fun consumeTableItem(itemId: String, onConsume: suspend (StagedFoodItem) -> Unit): TableFoodConsumptionResult = error("Unused")
    }

    private class FakeGuide : FirstRunGuideApi {
        override val step = MutableStateFlow(FirstRunOnboardingStep.FRIDGE_PICK_FOOD)
        override val resetVersion = MutableStateFlow(0)
        override fun moveTo(step: FirstRunOnboardingStep) { this.step.value = step }
        override fun completeFirstNeed() = Unit
        override fun reset(skipOnboarding: Boolean) = Unit
    }
}
