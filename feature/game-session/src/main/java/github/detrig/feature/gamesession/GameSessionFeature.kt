package github.detrig.feature.gamesession

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.gamesession.api.GameSessionApi
import github.detrig.feature.gamesession.di.GameSessionComponent
import github.detrig.feature.gamesession.di.GameSessionModule

object GameSessionFeature {

    var dependenciesProvider: ModuleDependenciesProvider<GameSessionDependencies>? = null

    private var component: GameSessionComponent? by diDemand {
        GameSessionModule(
            dependencies = requireNotNull(dependenciesProvider?.getDependencies()),
        )
    }

    fun getApi(): GameSessionApi = requireNotNull(component).api

    internal fun component(): GameSessionComponent = requireNotNull(component)

    internal fun destroyComponent() {
        component = null
    }
}
