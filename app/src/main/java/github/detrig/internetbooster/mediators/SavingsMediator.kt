package github.detrig.internetbooster.mediators

import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.savings.SavingsDependencies
import github.detrig.feature.savings.SavingsFeature
import github.detrig.feature.savings.api.SavingsApi
import github.detrig.feature.savings.domain.SavingsConfiguration

internal class SavingsMediator(
    private val core: CoreComponent,
    private val economy: EconomyMediator,
    private val planning: PlanningMediator,
    private val week: WeekMediator,
) : Mediator<SavingsApi> {
    fun init() {
        SavingsFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : SavingsDependencies {
                override fun economyApi() = economy.getApi()
                override fun planningApi() = planning.getApi()
                override fun weekApi() = week.getApi()
                override fun globalNavigator() = core.globalNavigator
                override fun configuration() = SavingsConfiguration()
            }
        }
    }

    override fun getApi(): SavingsApi = SavingsFeature.getApi()
}
