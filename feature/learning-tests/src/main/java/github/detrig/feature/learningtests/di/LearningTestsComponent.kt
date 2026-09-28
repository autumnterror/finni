package github.detrig.feature.learningtests.di

import github.detrig.feature.learningtests.api.LearningTestsApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.learningtests.presentation.LearningTestQuizViewModel
import github.detrig.feature.learningtests.presentation.LearningTestsHomeViewModel

internal interface LearningTestsComponent {
    val api: LearningTestsApi
    val petApi: PetApi
    fun homeViewModel(): LearningTestsHomeViewModel
    fun quizViewModel(testId: String): LearningTestQuizViewModel
}
