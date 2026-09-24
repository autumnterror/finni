package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.shop.api.ShopHost
import github.detrig.feature.shop.domain.ShopCatalogRegistry
import github.detrig.feature.shop.domain.ShopCartStore
import github.detrig.feature.shop.domain.ShopDecisionEventStore
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.feature.shop.navigation.ShopRouter
import github.detrig.products.ProductId
import github.detrig.products.SellableCatalog
import github.detrig.products.SellableItem
import github.detrig.products.StoreCategoryId
import github.detrig.products.StoreId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine

internal class ShopViewModel(
    private val storeId: StoreId,
    catalogRegistry: ShopCatalogRegistry,
    private val host: ShopHost,
    private val cartStore: ShopCartStore,
    private val decisionEventStore: ShopDecisionEventStore,
    private val receiptStore: ShopReceiptStore,
    private val router: ShopRouter,
    private val useHostBack: Boolean = false,
    private val useHostCart: Boolean = false,
    private val useHostCloseAfterReceipt: Boolean = false,
) : CoreViewModel<ShopViewState, ShopViewEvent>(ShopViewState()) {
    private val catalog: SellableCatalog<SellableItem>? = catalogRegistry.catalog(storeId)
    private var observationJob: Job? = null
    private var eventRefreshJob: Job? = null

    override fun perform(viewEvent: ShopViewEvent) {
        when (viewEvent) {
            ShopViewEvent.Load,
            ShopViewEvent.Retry -> load()
            ShopViewEvent.Back -> when {
                stateData.receipt != null -> dismissReceipt()
                stateData.purchaseFeedback != null -> finishPurchaseFeedback()
                else -> leaveShop()
            }
            ShopViewEvent.OpenCart -> openCart()
            ShopViewEvent.ReceiptDismissed -> dismissReceipt()
            ShopViewEvent.EventDialogueFinished -> updateState { copy(eventDialogueVisible = false) }
            ShopViewEvent.PurchaseFeedbackFinished -> finishPurchaseFeedback()
            is ShopViewEvent.CategorySelected -> selectCategory(viewEvent.categoryId)
            is ShopViewEvent.ProductClicked -> addProductToCart(viewEvent.productId)
        }
    }

    private fun load() {
        val resolvedCatalog = catalog
        if (resolvedCatalog == null) {
            updateState { copy(loading = false, error = ShopError.LOAD) }
            return
        }
        if (observationJob?.isActive == true) {
            refreshDecisionEvent()
            return
        }
        updateState {
            copy(
                storefront = resolvedCatalog.storefront,
                loading = true,
                error = null,
            )
        }
        observationJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState {
                    copy(
                        storefront = null,
                        loading = false,
                        error = ShopError.LOAD,
                    )
                }
                true
            },
        ) {
            host.preparePlayer()
            refreshDecisionEventNow()
            combine(
                host.observeBalanceRub(),
                host.observePetName(),
                cartStore.observe(storeId),
                receiptStore.observe(storeId),
                decisionEventStore.observe(storeId),
            ) { balance, petName, cart, receipt, currentEvent ->
                ShopObservation(balance, petName, cart, receipt, currentEvent)
            }.collect { observation ->
                updateState {
                    copy(
                        storefront = resolvedCatalog.storefront,
                        balanceRub = observation.balanceRub,
                        cart = observation.cart,
                        receipt = observation.receipt,
                        decisionEvent = observation.decisionEvent,
                        petName = observation.petName,
                        loading = false,
                        error = null,
                    )
                }
            }
        }
    }

    private fun selectCategory(categoryId: StoreCategoryId?) {
        val storefront = stateData.storefront ?: return
        if (categoryId != null && storefront.categories.none { it.id == categoryId }) return
        updateState { copy(selectedCategoryId = categoryId) }
    }

    private fun addProductToCart(productId: ProductId) {
        val storefront = stateData.storefront ?: return
        if (storefront.items.none { it.id == productId }) return
        cartStore.add(storeId, productId)
    }

    private fun declineDecisionEvent(navigateAfter: Boolean = false) {
        val event = stateData.decisionEvent
        if (event == null) {
            if (navigateAfter) navigateBack()
            return
        }
        launchCoroutine(
            handleAction = ExceptionConsumer {
                finishDecliningEvent(navigateAfter)
                true
            },
        ) {
            host.recordEventDeclined(event)
            finishDecliningEvent(navigateAfter)
        }
    }

    private fun refreshDecisionEvent() {
        if (eventRefreshJob?.isActive == true) return
        eventRefreshJob = launchCoroutine(
            handleAction = ExceptionConsumer { true },
        ) {
            host.preparePlayer()
            refreshDecisionEventNow()
        }
    }

    private suspend fun refreshDecisionEventNow() {
        val currentEvent = decisionEventStore.current(storeId)
        val refreshedEvent = host.currentDecisionEvent(storeId)
        val showPromotionIntroduction = refreshedEvent?.type == ShopDecisionEventType.PROMOTION &&
            refreshedEvent.eventId != currentEvent?.eventId &&
            host.claimPromotionIntroduction()
        decisionEventStore.replace(storeId, refreshedEvent)
        updateState { copy(eventDialogueVisible = showPromotionIntroduction) }
    }

    private fun finishDecliningEvent(navigateAfter: Boolean) {
        decisionEventStore.clear(storeId)
        if (navigateAfter) navigateBack()
    }

    private fun leaveShop() {
        when (stateData.decisionEvent?.type) {
            ShopDecisionEventType.IMPULSE_WISH -> declineDecisionEvent(navigateAfter = true)
            ShopDecisionEventType.PROMOTION,
            null,
            -> navigateBack()
        }
    }

    private fun dismissReceipt() {
        val receipt = stateData.receipt ?: return
        receiptStore.clear(storeId)
        if (receipt.feedback != null) {
            updateState { copy(purchaseFeedback = receipt.feedback) }
        } else {
            closeShopAfterReceipt()
        }
    }

    private fun finishPurchaseFeedback() {
        if (stateData.purchaseFeedback == null) return
        updateState { copy(purchaseFeedback = null) }
        closeShopAfterReceipt()
    }

    private fun navigateBack() {
        if (useHostBack) {
            commands.onNext(ShopCommand.Back)
        } else {
            router.back()
        }
    }

    private fun openCart() {
        if (useHostCart) {
            commands.onNext(ShopCommand.OpenCart)
        } else {
            router.openCart(storeId)
        }
    }

    private fun closeShopAfterReceipt() {
        if (useHostCloseAfterReceipt) {
            commands.onNext(ShopCommand.CloseAfterReceipt)
        } else {
            router.closeToRoom()
        }
    }

    private data class ShopObservation(
        val balanceRub: Long,
        val petName: String,
        val cart: github.detrig.products.StoreCart,
        val receipt: ShopReceipt?,
        val decisionEvent: github.detrig.feature.shop.domain.ShopDecisionEvent?,
    )
}
