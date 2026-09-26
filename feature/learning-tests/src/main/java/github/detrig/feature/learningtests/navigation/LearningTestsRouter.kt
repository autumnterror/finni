package github.detrig.feature.learningtests.navigation

import github.detrig.core.presentation.navigation.GlobalNavigator

internal interface LearningTestsRouter {
    fun open()
    fun openQuiz(testId: String)
    fun back()
}

internal class LearningTestsRouterImpl(
    private val navigator: GlobalNavigator,
) : LearningTestsRouter {
    override fun open() = navigator.navigate(LearningTestsRoute.Home, launchSingleTop = true)
    override fun openQuiz(testId: String) = navigator.navigate(LearningTestsRoute.Quiz(testId))
    override fun back() = navigator.back()
}
