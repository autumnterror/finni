package github.detrig.internetbooster.mediators

import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.internetbooster.R
import github.detrig.feature.shop.ShopDependencies
import github.detrig.feature.shop.ShopFeature
import github.detrig.feature.shop.api.ShopApi
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.feature.shop.api.ShopHost
import github.detrig.feature.shop.api.ShopItemDetail
import github.detrig.feature.shop.api.ShopItemDetailIcon
import github.detrig.feature.shop.api.ShopItemDetailsResolver
import github.detrig.feature.shop.domain.ShopCatalogRegistry
import github.detrig.products.FoodItem
import github.detrig.products.GroceryCatalog
import github.detrig.products.GroceryStoreIds
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Wires the reusable product catalog screen to application state. */
internal class ShopMediator(
    private val coreComponent: CoreComponent,
    private val economyMediator: EconomyMediator,
    private val weekMediator: WeekMediator,
) : Mediator<ShopApi> {
    private val groceryCatalog = GroceryCatalog()
    private val artworkResolver = GroceryArtworkResolver(R.drawable.grocery_product_atlas)
    private val catalogRegistry = ShopCatalogRegistry { storeId ->
        groceryCatalog.takeIf { storeId == GroceryStoreIds.Store }
    }
    private val checkoutGateway = ShopCheckoutGateway(
        catalogRegistry = catalogRegistry,
        currentBalanceRub = { economyMediator.getApi().getState().availableRub },
        debit = { operationId, amountRub, context ->
            economyMediator.getApi().debit(operationId, amountRub, context)
        },
        purchaseHistory = { economyMediator.getApi().getExpenseHistory() },
    )

    private val detailsResolver = ShopItemDetailsResolver { item ->
        val food = item as? FoodItem ?: return@ShopItemDetailsResolver emptyList()
        listOf(
            ShopItemDetail(
                text = "+${food.effects.satietyPercent}%",
                icon = ShopItemDetailIcon.SATIETY,
            ),
        )
    }

    private val host = object : ShopHost {
        override suspend fun preparePlayer() {
            economyMediator.getApi().initialize()
            weekMediator.getApi().initialize()
        }

        override fun observeBalanceRub() = economyMediator.getApi()
            .observeState()
            .map { it.availableRub }
            .distinctUntilChanged()

        override suspend fun checkout(request: github.detrig.feature.shop.api.ShopCheckoutRequest) =
            checkoutGateway.checkout(request)
    }

    fun init() {
        ShopFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : ShopDependencies {
                override fun host(): ShopHost = host
                override fun catalogRegistry(): ShopCatalogRegistry = catalogRegistry
                override fun artworkResolver(): ShopArtworkResolver = artworkResolver
                override fun itemDetailsResolver(): ShopItemDetailsResolver = detailsResolver
                override fun globalNavigator() = coreComponent.globalNavigator
            }
        }
    }

    override fun getApi(): ShopApi = ShopFeature.getApi()
}
