package github.detrig.internetbooster.mediators

import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.wardrobe.WardrobeDependencies
import github.detrig.feature.wardrobe.WardrobeFeature
import github.detrig.feature.wardrobe.api.WardrobeApi

internal class WardrobeMediator(
    private val core: CoreComponent,
    private val pet: PetMediator,
    private val economy: EconomyMediator,
    private val planning: PlanningMediator,
    private val week: WeekMediator,
) : Mediator<WardrobeApi> {
    fun init() {
        WardrobeFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : WardrobeDependencies {
                override fun globalNavigator() = core.globalNavigator
                override fun petApi() = pet.getApi()
                override fun economyApi() = economy.getApi()
                override fun planningApi() = planning.getApi()
                override fun weekApi() = week.getApi()
            }
        }
    }

    override fun getApi(): WardrobeApi = WardrobeFeature.getApi()
}
