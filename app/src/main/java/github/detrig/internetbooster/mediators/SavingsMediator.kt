package github.detrig.internetbooster.mediators

import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.audio.GameAudio
import github.detrig.feature.savings.SavingsDependencies
import github.detrig.feature.savings.SavingsFeature
import github.detrig.feature.savings.SavingsRoomBackdrop
import github.detrig.feature.savings.api.SavingsApi
import github.detrig.feature.savings.domain.SavingsConfiguration
import github.detrig.feature.savings.api.SavingsGoalPurchaser

internal class SavingsMediator(
    private val core: CoreComponent,
    private val economy: EconomyMediator,
    private val planning: PlanningMediator,
    private val week: WeekMediator,
    private val pet: PetMediator,
    private val roomApiProvider: () -> github.detrig.feature.room.api.RoomApi,
    private val gameAudio: GameAudio,
    private val learning: LearningMediator,
) : Mediator<SavingsApi> {
    fun init() {
        SavingsFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : SavingsDependencies {
                override fun economyApi() = economy.getApi()
                override fun planningApi() = planning.getApi()
                override fun weekApi() = week.getApi()
                override fun petApi() = pet.getApi()
                override fun roomBackdrop() = SavingsRoomBackdrop { modifier, petContent ->
                    roomApiProvider().Content(
                        modifier = modifier,
                        canShowDialogs = false,
                        petContent = petContent,
                        active = false,
                    )
                }
                override fun learningApi() = learning.getApi()
                override fun globalNavigator() = core.globalNavigator
                override fun configuration() = SavingsConfiguration()
                override fun gameAudio() = gameAudio
                override fun goalPurchaser() = SavingsGoalPurchaser { goal ->
                    roomApiProvider().purchaseSavingsGoal(goal)
                }
                override fun transactionRunner() = economy.transactionRunner()
            }
        }
    }

    override fun getApi(): SavingsApi = SavingsFeature.getApi()
}
