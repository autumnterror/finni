package github.detrig.feature.learningtests.di

import github.detrig.feature.learningtests.LearningTestsDependencies
import github.detrig.feature.learningtests.api.LearningTestsApi
import github.detrig.feature.learningtests.api.LearningTestsApiImpl
import github.detrig.feature.learningtests.data.LearningTestsRepository
import github.detrig.feature.learningtests.data.local.LearningTestsTutorialStorage
import github.detrig.feature.learningtests.domain.LearningTestsInteractor
import github.detrig.feature.learningtests.navigation.LearningTestsRouter
import github.detrig.feature.learningtests.navigation.LearningTestsRouterImpl
import github.detrig.feature.learningtests.presentation.LearningTestQuizViewModel
import github.detrig.feature.learningtests.presentation.LearningTestsHomeViewModel
import github.detrig.feature.pet.api.PetApi

internal class LearningTestsModule(
    private val dependencies: LearningTestsDependencies,
) : LearningTestsComponent {
    private val repository by lazy {
        LearningTestsRepository(dependencies.learningTestsDao(), dependencies.transactionRunner())
    }
    private val tutorialStorage by lazy {
        LearningTestsTutorialStorage(dependencies.tutorialPreferences())
    }
    private val interactor by lazy {
        LearningTestsInteractor(
            repository = repository,
            economyApi = dependencies.economyApi(),
            progressionApi = dependencies.progressionApi(),
            weekApi = dependencies.weekApi(),
        )
    }
    private val router: LearningTestsRouter by lazy { LearningTestsRouterImpl(dependencies.globalNavigator()) }

    override val api: LearningTestsApi by lazy { LearningTestsApiImpl(router) }
    override val petApi: PetApi by lazy { dependencies.petApi() }

    override fun homeViewModel() = LearningTestsHomeViewModel(interactor, router, tutorialStorage)

    override fun quizViewModel(testId: String) = LearningTestQuizViewModel(testId, interactor, router)
}
