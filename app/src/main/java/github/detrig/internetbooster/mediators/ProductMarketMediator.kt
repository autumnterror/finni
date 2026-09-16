package github.detrig.internetbooster.mediators

import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.productmarket.ProductMarketDependencies
import github.detrig.feature.productmarket.ProductMarketFeature
import github.detrig.feature.productmarket.api.ProductMarketHost
import github.detrig.feature.productmarket.domain.MarketConfiguration
import github.detrig.internetbooster.database.ProductMarketDatabaseModule
import github.detrig.products.DefaultProductCatalog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
internal class ProductMarketMediator(
    private val core: CoreComponent,
    private val database: ProductMarketDatabaseModule,
    private val economy: EconomyMediator,
) {
    private val host = object : ProductMarketHost {
        override suspend fun preparePlayer() { economy.getApi().initialize() }
        override fun observeBalanceRub() = economy.getApi().observeState()
            .map { Math.toIntExact(it.availableRub) }.distinctUntilChanged()
    }
    fun init() {
        ProductMarketFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : ProductMarketDependencies {
                override fun host() = host
                override fun tripDao() = database.tripDao
                override fun productCatalog() = DefaultProductCatalog()
                override fun configuration() = MarketConfiguration()
                override fun applicationScope() = core.applicationScope
                override fun globalNavigator() = core.globalNavigator
            }
        }
    }
}
