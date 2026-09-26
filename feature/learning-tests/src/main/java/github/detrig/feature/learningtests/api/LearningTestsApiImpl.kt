package github.detrig.feature.learningtests.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.feature.learningtests.navigation.LearningTestsRoute
import github.detrig.feature.learningtests.navigation.LearningTestsRouter
import github.detrig.feature.learningtests.presentation.LearningTestsHomeScreen
import github.detrig.feature.learningtests.presentation.LearningTestQuizScreen

internal class LearningTestsApiImpl(
    private val router: LearningTestsRouter,
) : LearningTestsApi {
    override fun open() = router.open()

    override fun entries(): EntryHostProviderInstaller = {
        composable<LearningTestsRoute.Home> { LearningTestsHomeScreen() }
        composable<LearningTestsRoute.Quiz> { route ->
            LearningTestQuizScreen(testId = route.testId)
        }
    }
}
