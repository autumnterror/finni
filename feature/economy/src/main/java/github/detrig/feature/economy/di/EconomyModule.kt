package github.detrig.feature.economy.di

import github.detrig.feature.economy.EconomyDependencies
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.api.EconomyApiImpl
import github.detrig.feature.economy.data.EconomyRepositoryImpl

internal class EconomyModule(private val dependencies: EconomyDependencies) : EconomyComponent {
    private val repository by lazy {
        EconomyRepositoryImpl(
            dao = dependencies.economyDao(),
            transactionRunner = dependencies.transactionRunner(),
            config = dependencies.config(),
            currentTimeMillis = dependencies::currentTimeMillis,
        )
    }
    override val api: EconomyApi by lazy { EconomyApiImpl(repository) }
}
