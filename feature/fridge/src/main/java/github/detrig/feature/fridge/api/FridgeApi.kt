package github.detrig.feature.fridge.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller

interface FridgeApi {
    fun open()

    /** Opens the feeding scene for the portions already placed on the table. */
    fun openFeeding()

    fun entries(): EntryHostProviderInstaller

    /** Places up to three staged products on the room table. */
    @Composable
    fun TableContent(modifier: Modifier = Modifier)
}
