package github.detrig.minigames.common

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.minigames.common.api.MiniGamesCommonApi
import github.detrig.minigames.common.data.database.api.MiniGameQuestionsDao
import github.detrig.minigames.common.data.network.RestApi
import github.detrig.minigames.common.di.MiniGamesCommonComponent
import github.detrig.minigames.common.di.MiniGamesCommonModule

object MiniGamesCommonFeature {

    var dependenciesProvider: ModuleDependenciesProvider<MiniGamesCommonDependencies>? = null

    private var component: MiniGamesCommonComponent? by diDemand {
        MiniGamesCommonModule(
            dependencies = requireNotNull(dependenciesProvider?.getDependencies()),
        )
    }

    fun getApi(): MiniGamesCommonApi = requireNotNull(component).api

    internal fun component(): MiniGamesCommonComponent = requireNotNull(component)

    internal fun destroyComponent() {
        component = null
    }
}

interface MiniGamesCommonDependencies {

    fun restApi(): RestApi

    fun miniGameQuestionsDao(): MiniGameQuestionsDao
}