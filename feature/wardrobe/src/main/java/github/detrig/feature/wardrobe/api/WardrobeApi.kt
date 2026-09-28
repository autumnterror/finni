package github.detrig.feature.wardrobe.api

import androidx.compose.runtime.Composable
import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller

interface WardrobeApi {
    fun open()

    /** Renders the clothing shop inside the phone. */
    @Composable
    fun StoreContent(onBack: () -> Unit)

    fun entries(): EntryHostProviderInstaller
}
