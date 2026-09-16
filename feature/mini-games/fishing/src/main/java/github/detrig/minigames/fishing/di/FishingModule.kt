package github.detrig.minigames.fishing.di

import github.detrig.minigames.fishing.FishingDependencies
import github.detrig.minigames.fishing.api.FishingApi
import github.detrig.minigames.fishing.api.FishingApiImpl
import github.detrig.minigames.fishing.data.FishingRepositoryImpl
import github.detrig.minigames.fishing.domain.FishingConfig
import github.detrig.minigames.fishing.domain.FishingEngine
import github.detrig.minigames.fishing.domain.FishingInteractor
import github.detrig.minigames.fishing.navigation.FishingRouterImpl
import github.detrig.minigames.fishing.presentation.FishingViewModel
import kotlinx.serialization.json.Json

internal class FishingModule(private val dependencies: FishingDependencies) : FishingComponent {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val config by lazy { json.decodeFromString<FishingConfig>(dependencies.configurationJson()).validate() }
    private val engine by lazy { FishingEngine(config) }
    private val repository by lazy {
        FishingRepositoryImpl(dependencies.fishingDao(), dependencies.transactionRunner(), json, config.rulesVersion)
    }
    private val interactor by lazy { FishingInteractor(repository, dependencies.host(), engine, dependencies::currentTimeMillis) }
    private val router by lazy { FishingRouterImpl(dependencies.globalNavigator()) }
    override val api: FishingApi by lazy { FishingApiImpl(router) }
    override val petApi by lazy { dependencies.petApi() }
    override fun viewModel() = FishingViewModel(interactor, router, dependencies::currentTimeMillis)
}
