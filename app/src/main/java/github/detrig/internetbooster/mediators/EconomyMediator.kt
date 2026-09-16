package github.detrig.internetbooster.mediators

import androidx.annotation.MainThread
import github.detrig.core.Mediator
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.economy.EconomyDependencies
import github.detrig.feature.economy.EconomyFeature
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.data.local.EconomyDao
import github.detrig.feature.economy.domain.EconomyConfig
import github.detrig.internetbooster.database.AppDatabaseModule

internal class EconomyMediator(private val databaseModule: AppDatabaseModule) : Mediator<EconomyApi> {
    @MainThread
    fun init() {
        EconomyFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : EconomyDependencies {
                override fun economyDao(): EconomyDao = databaseModule.economyDao
                override fun transactionRunner(): RoomTransactionRunner = databaseModule.transactionRunner
                override fun config() = EconomyConfig()
                override fun currentTimeMillis() = System.currentTimeMillis()
            }
        }
    }

    @MainThread
    override fun getApi(): EconomyApi = EconomyFeature.getApi()
}
