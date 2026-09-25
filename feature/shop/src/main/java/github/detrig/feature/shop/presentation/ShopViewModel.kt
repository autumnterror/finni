package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.core.audio.GameAudio
import github.detrig.core.audio.SilentGameAudio
import github.detrig.feature.shop.api.ShopHost
import github.detrig.feature.shop.domain.ShopCatalogRegistry
import github.detrig.feature.shop.domain.ShopCartStore
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
    private val receiptStore: ShopReceiptStore,
    private val router: ShopRouter,
    private val onOpenCart: (() -> Unit)? = null,
    private val closeAfterReceipt: (() -> Unit)? = null,
    private val gameAudio: GameAudio = SilentGameAudio,
) : CoreViewModel<ShopViewState, ShopViewEvent>(ShopViewState()) {
    private val catalog: SellableCatalog<SellableItem>? = catalogRegistry.catalog(storeId)
    private var observationJob: Job? = null

    override fun perform(viewEvent: ShopViewEvent) {
        when (viewEvent) {
            ShopViewEvent.Load,
            ShopViewEvent.Retry -> load()
            ShopViewEvent.Back -> if (stateData.receipt == null) router.back() else dismissReceipt()
            ShopViewEvent.OpenCart -> onOpenCart?.invoke() ?: router.openCart(storeId)
            ShopViewEvent.ReceiptDismissed -> dismissReceipt()
            is ShopViewEvent.CategorySelected -> selectCategory(viewEvent.categoryId)
            is ShopViewEvent.ProductClicked -> addProductToCart(viewEvent.productId)
        }
    }

    private fun load() {
        if (observationJob?.isActive == true) return
        val resolvedCatalog = catalog
        if (resolvedCatalog == null) {
            updateState { copy(loading = false, error = ShopError.LOAD) }
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
            combine(
                host.observeBalanceRub(),
                cartStore.observe(storeId),
                receiptStore.observe(storeId),
            ) { balance, cart, receipt -> Triple(balance, cart, receipt) }.collect { (balance, cart, receipt) ->
                updateState {
                    copy(
                        storefront = resolvedCatalog.storefront,
                        balanceRub = balance,
                        cart = cart,
                        receipt = receipt,
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
        gameAudio.play(ShopAudioCues.Select)
    }

    private fun addProductToCart(productId: ProductId) {
        val storefront = stateData.storefront ?: return
        if (storefront.items.none { it.id == productId }) return
        cartStore.add(storeId, productId)
        gameAudio.play(ShopAudioCues.Select)
    }

    private fun dismissReceipt() {
        if (stateData.receipt == null) return
        receiptStore.clear(storeId)
        closeAfterReceipt?.invoke() ?: router.closeToRoom()
    }
}
