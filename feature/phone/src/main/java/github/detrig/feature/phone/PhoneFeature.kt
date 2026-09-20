package github.detrig.feature.phone

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.phone.api.PhoneApi
import github.detrig.feature.phone.di.PhoneComponent
import github.detrig.feature.phone.di.PhoneModule

object PhoneFeature {
    var dependenciesProvider: ModuleDependenciesProvider<PhoneDependencies>? = null

    private var component: PhoneComponent? by diDemand {
        PhoneModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): PhoneApi = requireNotNull(component).api

    internal fun component(): PhoneComponent = requireNotNull(component)

    internal fun destroyComponent() {
        component = null
    }
}
