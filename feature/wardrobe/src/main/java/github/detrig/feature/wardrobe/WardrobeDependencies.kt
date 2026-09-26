package github.detrig.feature.wardrobe

import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.week.api.WeekApi

interface WardrobeDependencies {
    fun globalNavigator(): GlobalNavigator
    fun petApi(): PetApi
    fun economyApi(): EconomyApi
    fun planningApi(): PlanningApi
    fun weekApi(): WeekApi
}
