package github.detrig.feature.wardrobe

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.wardrobe.api.WardrobeApi
import github.detrig.feature.wardrobe.di.WardrobeComponent
import github.detrig.feature.wardrobe.di.WardrobeModule

object WardrobeFeature {
    var dependenciesProvider: ModuleDependenciesProvider<WardrobeDependencies>? = null
    private var component: WardrobeComponent? by diDemand {
        WardrobeModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): WardrobeApi = component().api
    internal fun component(): WardrobeComponent = requireNotNull(component)
}
