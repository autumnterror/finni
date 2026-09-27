package github.detrig.feature.learningtests.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller

interface LearningTestsApi {
    fun open()
    fun entries(): EntryHostProviderInstaller
}
