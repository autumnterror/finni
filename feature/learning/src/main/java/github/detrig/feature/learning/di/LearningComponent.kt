package github.detrig.feature.learning.di

import github.detrig.feature.learning.api.LearningApi

internal interface LearningComponent {
    val api: LearningApi
}
