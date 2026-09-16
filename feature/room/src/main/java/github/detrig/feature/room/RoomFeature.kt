package github.detrig.feature.room

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.room.di.RoomComponent
import github.detrig.feature.room.di.RoomModule

object RoomFeature {
    var dependenciesProvider: ModuleDependenciesProvider<RoomDependencies>? = null

    private var component: RoomComponent? by diDemand {
        RoomModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): RoomApi = requireNotNull(component).api
    internal fun component(): RoomComponent = requireNotNull(component)
    internal fun destroyComponent() { component = null }
}
