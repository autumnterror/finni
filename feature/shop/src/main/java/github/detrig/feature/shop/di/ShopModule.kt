package github.detrig.feature.shop.di

import github.detrig.feature.shop.ShopDependencies
import github.detrig.feature.shop.api.ShopApi
import github.detrig.feature.shop.api.ShopApiImpl
import github.detrig.feature.shop.domain.ShopCartStore
import github.detrig.feature.shop.navigation.ShopRouterImpl
import github.detrig.feature.shop.presentation.ShopCartViewModel
import github.detrig.feature.shop.presentation.ShopReceiptStore
import github.detrig.feature.shop.presentation.ShopViewModel
import github.detrig.products.StoreId

internal class ShopModule(
    private val dependencies: ShopDependencies,
) : ShopComponent {
    private val router by lazy { ShopRouterImpl(dependencies.globalNavigator()) }
    private val cartStore = ShopCartStore()
    private val receiptStore = ShopReceiptStore()

    override val artworkResolver by lazy { dependencies.artworkResolver() }
    override val itemDetailsResolver by lazy { dependencies.itemDetailsResolver() }

    override val api: ShopApi by lazy { ShopApiImpl(router) }

    override fun viewModel(
        storeId: StoreId,
        onOpenCart: (() -> Unit)?,
        closeAfterReceipt: (() -> Unit)?,
    ) = ShopViewModel(
        storeId = storeId,
        catalogRegistry = dependencies.catalogRegistry(),
        host = dependencies.host(),
        cartStore = cartStore,
        receiptStore = receiptStore,
        router = router,
        onOpenCart = onOpenCart,
        closeAfterReceipt = closeAfterReceipt,
        gameAudio = dependencies.gameAudio(),
    )

    override fun cartViewModel(
        storeId: StoreId,
        onBack: (() -> Unit)?,
        onCheckoutCompleted: (() -> Unit)?,
    ) = ShopCartViewModel(
        storeId = storeId,
        catalogRegistry = dependencies.catalogRegistry(),
        host = dependencies.host(),
        cartStore = cartStore,
        receiptStore = receiptStore,
        router = router,
        onBack = onBack,
        onCheckoutCompleted = onCheckoutCompleted,
        gameAudio = dependencies.gameAudio(),
    )
}
