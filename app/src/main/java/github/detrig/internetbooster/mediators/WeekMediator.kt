package github.detrig.internetbooster.mediators

import github.detrig.core.Mediator
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.week.WeekDependencies
import github.detrig.feature.week.WeekFeature
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.week.data.local.WeekDao
import github.detrig.feature.week.domain.PetDayEffects
import github.detrig.internetbooster.database.AppDatabaseModule

internal class WeekMediator(
    private val databaseModule: AppDatabaseModule,
    private val economyMediator: EconomyMediator,
    private val gameStateMediator: GameStateMediator,
    private val planningMediator: PlanningMediator,
) : Mediator<WeekApi> {
    fun init() {
        WeekFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : WeekDependencies {
                override fun weekDao(): WeekDao = databaseModule.weekDao
                override fun economyApi(): EconomyApi = economyMediator.getApi()
                override fun petDayEffects(): PetDayEffects = PetDayEffects {
                    gameStateMediator.getApi().consumeHungerForSleep()
                }
                override fun transactionRunner(): RoomTransactionRunner = databaseModule.transactionRunner
                override fun progressionApi() = gameStateMediator.getProgressionApi()
                override fun planningApi() = planningMediator.getApi()
            }
        }
    }

    override fun getApi(): WeekApi = WeekFeature.getApi()
}
