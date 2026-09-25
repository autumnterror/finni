package github.detrig.feature.savings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.core.audio.GameAudio
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
    fun roomBackdrop(): SavingsRoomBackdrop
    fun globalNavigator(): GlobalNavigator
    fun configuration(): SavingsConfiguration
    fun gameAudio(): GameAudio
}

fun interface SavingsRoomBackdrop {
    @Composable
    fun Content(
        modifier: Modifier,
        petContent: @Composable (Modifier) -> Unit,
    )
}
