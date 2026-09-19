package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.shop.api.ShopCheckoutRejection
import github.detrig.feature.shop.api.ShopCheckoutRequest
import github.detrig.feature.shop.api.ShopCheckoutResult
import github.detrig.feature.shop.api.ShopHost
import github.detrig.feature.shop.domain.ShopCartStore
import github.detrig.feature.shop.domain.ShopCatalogRegistry
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
    private val receiptStore: ShopReceiptStore,
    private val router: ShopRouter,
) : CoreViewModel<ShopCartViewState, ShopCartViewEvent>(ShopCartViewState()) {
    private val catalog: SellableCatalog<SellableItem>? = catalogRegistry.catalog(storeId)
    private var observationJob: Job? = null

    override fun perform(viewEvent: ShopCartViewEvent) {
        when (viewEvent) {
            ShopCartViewEvent.Load,
            ShopCartViewEvent.Retry -> load()
            ShopCartViewEvent.Back -> router.back()
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
            ) { balance, cart -> balance to cart }.collect { (balance, cart) ->
                updateState {
                    copy(
                        storefront = resolvedCatalog.storefront,
                        cart = cart,
                        balanceRub = balance,
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
    }

    private fun removeOne(productId: ProductId) {
        if (stateData.cart.quantityOf(productId) == 0) return
        cartStore.removeOne(storeId, productId)
    }

    private fun checkout() {
        val currentState = stateData
        val pendingCart = currentState.cart
        if (currentState.paymentInProgress || pendingCart.isEmpty) return
        if (!currentState.canPay) {
            updateState { copy(checkoutRejection = ShopCheckoutRejection.INSUFFICIENT_FUNDS) }
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
                    ),
                )
            ) {
                is ShopCheckoutResult.Completed -> {
                    val receipt = stateData.createReceipt(
                        cart = pendingCart,
                        receiptNumber = result.receiptNumber,
                    )
                    receiptStore.show(storeId, receipt)
                    cartStore.removePurchased(storeId, pendingCart)
                    updateState {
                        copy(
                            balanceRub = result.balanceRub,
                            paymentInProgress = false,
                            checkoutRejection = null,
                        )
                    }
                    router.back()
                }

                is ShopCheckoutResult.Rejected -> {
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

    private fun ShopCartViewState.createReceipt(
        cart: StoreCart,
        receiptNumber: String,
    ): ShopReceipt {
        val receiptStorefront = requireNotNull(storefront)
        val itemsById = receiptStorefront.items.associateBy { it.id }
        val receiptLines = cart.lines.map { line ->
            val item = requireNotNull(itemsById[line.itemId])
            ShopReceiptLine(
                title = item.title,
                unitPriceRub = item.priceRub,
                quantity = line.quantity,
            )
        }
        return ShopReceipt(
            number = receiptNumber,
            storeTitle = receiptStorefront.title,
            lines = receiptLines,
        )
    }
}
