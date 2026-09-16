package github.detrig.minigames.fishing.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller

interface FishingApi {
    fun open()
    fun entries(): EntryHostProviderInstaller
}
