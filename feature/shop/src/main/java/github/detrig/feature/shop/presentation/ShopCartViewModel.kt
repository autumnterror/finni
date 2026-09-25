package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.core.audio.GameAudio
import github.detrig.core.audio.SilentGameAudio
import github.detrig.feature.shop.api.ShopCheckoutRejection
import github.detrig.feature.shop.api.ShopCheckoutRequest
import github.detrig.feature.shop.api.ShopCheckoutResult
import github.detrig.feature.shop.api.ShopHost
import github.detrig.feature.shop.domain.ShopCartStore
import github.detrig.feature.shop.domain.ShopCatalogRegistry
import github.detrig.feature.shop.domain.ShopDecisionEventStore
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.feature.shop.navigation.ShopRouter
import github.detrig.products.ProductId
import github.detrig.products.SellableCatalog
import github.detrig.products.SellableItem
import github.detrig.products.StoreCart
import github.detrig.products.StoreId
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine

internal class ShopCartViewModel(
    private val storeId: StoreId,
    catalogRegistry: ShopCatalogRegistry,
    private val host: ShopHost,
    private val cartStore: ShopCartStore,
    private val decisionEventStore: ShopDecisionEventStore,
    private val receiptStore: ShopReceiptStore,
    private val router: ShopRouter,
    private val useHostBack: Boolean = false,
    private val useHostCheckoutCompleted: Boolean = false,
    private val gameAudio: GameAudio = SilentGameAudio,
) : CoreViewModel<ShopCartViewState, ShopCartViewEvent>(ShopCartViewState()) {
    private val catalog: SellableCatalog<SellableItem>? = catalogRegistry.catalog(storeId)
    private var observationJob: Job? = null

    override fun perform(viewEvent: ShopCartViewEvent) {
        when (viewEvent) {
            ShopCartViewEvent.Load,
            ShopCartViewEvent.Retry -> load()
            ShopCartViewEvent.Back -> navigateBack()
            is ShopCartViewEvent.Increase -> add(viewEvent.productId)
            is ShopCartViewEvent.Decrease -> removeOne(viewEvent.productId)
            ShopCartViewEvent.PayClicked -> checkout()
            ShopCartViewEvent.CheckoutRejectionDismissed -> {
                updateState { copy(checkoutRejection = null) }
            }
        }
    }

    private fun load() {
        if (observationJob?.isActive == true) return
        val resolvedCatalog = catalog
        if (resolvedCatalog == null) {
            updateState { copy(loading = false, error = ShopCartError.LOAD) }
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
                updateState { copy(loading = false, error = ShopCartError.LOAD) }
                true
            },
        ) {
            host.preparePlayer()
            combine(
                host.observeBalanceRub(),
                cartStore.observe(storeId),
                decisionEventStore.observe(storeId),
            ) { balance, cart, decisionEvent -> Triple(balance, cart, decisionEvent) }
                .collect { (balance, cart, decisionEvent) ->
                    updateState {
                        copy(
                            storefront = resolvedCatalog.storefront,
                            cart = cart,
                            balanceRub = balance,
                            decisionEvent = decisionEvent,
                            loading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    private fun add(productId: ProductId) {
        if (stateData.storefront?.items?.none { it.id == productId } != false) return
        cartStore.add(storeId, productId)
        gameAudio.play(ShopAudioCues.Select)
    }

    private fun removeOne(productId: ProductId) {
        if (stateData.cart.quantityOf(productId) == 0) return
        cartStore.removeOne(storeId, productId)
        gameAudio.play(ShopAudioCues.Select)
    }

    private fun checkout() {
        val currentState = stateData
        val pendingCart = currentState.cart
        if (currentState.paymentInProgress || pendingCart.isEmpty) return
        if (!currentState.canPay) {
            updateState { copy(checkoutRejection = ShopCheckoutRejection.INSUFFICIENT_FUNDS) }
            gameAudio.play(ShopAudioCues.Rejected)
            return
        }
        val operationId = "shop-checkout:${storeId.value}:${UUID.randomUUID()}"

        updateState { copy(paymentInProgress = true, checkoutRejection = null) }
        launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState {
                    copy(
                        paymentInProgress = false,
                        checkoutRejection = ShopCheckoutRejection.OPERATION_CONFLICT,
                    )
                }
                true
            },
        ) {
            when (
                val result = host.checkout(
                    ShopCheckoutRequest(
                        operationId = operationId,
                        storeId = storeId,
                        lines = pendingCart.lines,
                        decisionEvent = currentState.decisionEvent,
                    ),
                )
            ) {
                is ShopCheckoutResult.Completed -> {
                    val receipt = stateData.createReceipt(
                        cart = pendingCart,
                        receiptNumber = result.receiptNumber,
                        feedback = result.feedback,
                    )
                    receiptStore.show(storeId, receipt)
                    cartStore.removePurchased(storeId, pendingCart)
                    if (currentState.decisionEvent?.type != ShopDecisionEventType.PROMOTION) {
                        decisionEventStore.clear(storeId)
                    }
                    updateState {
                        copy(
                            balanceRub = result.balanceRub,
                            paymentInProgress = false,
                            checkoutRejection = null,
                        )
                    }
                    finishCheckout(currentState.totalRub)
                }

                is ShopCheckoutResult.Rejected -> {
                    gameAudio.play(ShopAudioCues.Rejected)
                    updateState {
                        copy(
                            balanceRub = result.balanceRub,
                            paymentInProgress = false,
                            checkoutRejection = result.reason,
                        )
                    }
                }
            }
        }
    }

    private fun navigateBack() {
        if (useHostBack) {
            commands.onNext(ShopCartCommand.Back)
        } else {
            router.back()
        }
    }

    private fun finishCheckout(spentRub: Long) {
        if (useHostCheckoutCompleted) {
            commands.onNext(ShopCartCommand.CheckoutCompleted(spentRub))
        } else {
            router.back()
        }
    }

    private fun ShopCartViewState.createReceipt(
        cart: StoreCart,
        receiptNumber: String,
        feedback: github.detrig.feature.shop.api.ShopPurchaseFeedback?,
    ): ShopReceipt {
        val receiptStorefront = requireNotNull(storefront)
        val itemsById = receiptStorefront.items.associateBy { it.id }
        val receiptLines = cart.lines.map { line ->
            val item = requireNotNull(itemsById[line.itemId])
            val price = requireNotNull(priceLine(item.id, line.quantity))
            ShopReceiptLine(
                title = item.title,
                unitPriceRub = item.priceRub,
                quantity = line.quantity,
                totalRub = price.chargedTotalRub,
            )
        }
        return ShopReceipt(
            number = receiptNumber,
            storeTitle = receiptStorefront.title,
            lines = receiptLines,
            feedback = feedback,
        )
    }
}
