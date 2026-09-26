package github.detrig.feature.learningtests.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface LearningTestsRoute : NavKey {
    @Serializable
    data object Home : LearningTestsRoute

    @Serializable
    data class Quiz(val testId: String) : LearningTestsRoute
}
