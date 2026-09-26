package github.detrig.feature.wardrobe.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller

interface WardrobeApi {
    fun open()
    fun entries(): EntryHostProviderInstaller
}
