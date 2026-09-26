package github.detrig.feature.wardrobe.di

import github.detrig.feature.wardrobe.api.WardrobeApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.wardrobe.presentation.WardrobeViewModel

internal interface WardrobeComponent {
    val api: WardrobeApi
    val petApi: PetApi
    fun viewModel(): WardrobeViewModel
}
