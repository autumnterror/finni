package github.detrig.internetbooster.mediators

import androidx.annotation.MainThread
import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.learningtests.LearningTestsDependencies
import github.detrig.feature.learningtests.LearningTestsFeature
import github.detrig.feature.learningtests.api.LearningTestsApi
import github.detrig.internetbooster.database.AppDatabaseModule

internal class LearningTestsMediator(
    private val coreComponent: CoreComponent,
    private val databaseModule: AppDatabaseModule,
    private val economyMediator: EconomyMediator,
    private val weekMediator: WeekMediator,
    private val gameStateMediator: GameStateMediator,
    private val petMediator: PetMediator,
) : Mediator<LearningTestsApi> {
    @MainThread
    fun init() {
        LearningTestsFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : LearningTestsDependencies {
                override fun learningTestsDao() = databaseModule.learningTestsDao
                override fun tutorialPreferences() = coreComponent.context.getSharedPreferences(
                    "finpet_learning_tests", android.content.Context.MODE_PRIVATE,
                )
                override fun transactionRunner() = databaseModule.transactionRunner
                override fun economyApi() = economyMediator.getApi()
                override fun progressionApi() = gameStateMediator.getProgressionApi()
                override fun weekApi() = weekMediator.getApi()
                override fun petApi() = petMediator.getApi()
                override fun globalNavigator() = coreComponent.globalNavigator
            }
        }
        LearningTestsFeature.getApi()
    }

    @MainThread
    override fun getApi(): LearningTestsApi = LearningTestsFeature.getApi()
}
