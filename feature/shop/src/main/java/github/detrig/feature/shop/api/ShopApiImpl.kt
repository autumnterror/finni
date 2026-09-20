package github.detrig.feature.shop.api

import androidx.compose.runtime.Composable
import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.feature.shop.navigation.ShopRoute
import github.detrig.feature.shop.navigation.ShopRouter
import github.detrig.feature.shop.presentation.ShopCartScreen
import github.detrig.feature.shop.presentation.ShopScreen
import github.detrig.products.StoreId

internal class ShopApiImpl(
    private val router: ShopRouter,
) : ShopApi {
    override fun open(storeId: StoreId) = router.open(storeId)

    @Composable
    override fun Content(
        storeId: StoreId,
        onBack: () -> Unit,
        onOpenCart: () -> Unit,
        closeAfterReceipt: () -> Unit,
    ) = ShopScreen(
        storeId = storeId,
        onBack = onBack,
        onOpenCart = onOpenCart,
        closeAfterReceipt = closeAfterReceipt,
    )

    @Composable
    override fun CartContent(
        storeId: StoreId,
        onBack: () -> Unit,
        onCheckoutCompleted: () -> Unit,
    ) = ShopCartScreen(
        storeId = storeId,
        onBack = onBack,
        onCheckoutCompleted = onCheckoutCompleted,
    )

    override fun entries(): EntryHostProviderInstaller = {
        composable<ShopRoute.Catalog> { route -> ShopScreen(StoreId(route.storeId)) }
        composable<ShopRoute.Cart> { route -> ShopCartScreen(StoreId(route.storeId)) }
    }
}
