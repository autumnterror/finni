package github.detrig.feature.savings.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface SavingsRoute : NavKey {
    @Serializable
    data class Home(
        val firstRunOnboarding: Boolean = false,
        val suggestedGoalId: String? = null,
    ) : SavingsRoute
}
