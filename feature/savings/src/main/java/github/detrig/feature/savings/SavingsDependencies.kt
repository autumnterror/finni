package github.detrig.feature.savings

import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.savings.domain.SavingsConfiguration
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.pet.api.PetApi

interface SavingsDependencies {
    fun economyApi(): EconomyApi
    fun planningApi(): PlanningApi
    fun weekApi(): WeekApi
    fun petApi(): PetApi
    fun globalNavigator(): GlobalNavigator
    fun configuration(): SavingsConfiguration
}
