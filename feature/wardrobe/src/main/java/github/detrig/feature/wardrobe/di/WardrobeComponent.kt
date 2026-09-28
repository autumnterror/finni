package github.detrig.feature.wardrobe.di

import github.detrig.feature.wardrobe.api.WardrobeApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.wardrobe.presentation.WardrobeViewModel
import github.detrig.feature.wardrobe.presentation.WardrobeMode

internal interface WardrobeComponent {
    val api: WardrobeApi
    val petApi: PetApi
    fun viewModel(mode: WardrobeMode): WardrobeViewModel
}
