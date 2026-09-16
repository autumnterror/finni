package github.detrig.feature.gamestate

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.gamestate.di.GameStateComponent
import github.detrig.feature.gamestate.di.GameStateModule

object GameStateFeature {

    var dependenciesProvider: ModuleDependenciesProvider<GameStateDependencies>? = null

    private var component: GameStateComponent? by diDemand {
        GameStateModule(
            dependencies = requireNotNull(dependenciesProvider?.getDependencies()),
        )
    }

    fun getApi(): GameStateApi = requireNotNull(component).api

    internal fun destroyComponent() {
        component = null
    }
}
