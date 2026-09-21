package github.detrig.feature.savings.navigation

import github.detrig.core.presentation.navigation.GlobalNavigator

internal interface SavingsRouter {
    fun open(firstRunOnboarding: Boolean = false, suggestedGoalId: String? = null)
    fun back()
}

internal class SavingsRouterImpl(private val navigator: GlobalNavigator) : SavingsRouter {
    override fun open(firstRunOnboarding: Boolean, suggestedGoalId: String?) {
        navigator.navigate(
            SavingsRoute.Home(firstRunOnboarding, suggestedGoalId),
            launchSingleTop = true,
        )
    }
    override fun back() { navigator.back() }
}
