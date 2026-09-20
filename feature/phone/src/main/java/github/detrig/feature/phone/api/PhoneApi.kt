package github.detrig.feature.phone.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller

interface PhoneApi {
    fun open()
    fun entries(): EntryHostProviderInstaller
}
