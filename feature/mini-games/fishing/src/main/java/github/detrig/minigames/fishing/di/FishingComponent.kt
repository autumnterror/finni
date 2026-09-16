package github.detrig.minigames.fishing.di

import github.detrig.feature.pet.api.PetApi
import github.detrig.minigames.fishing.api.FishingApi
import github.detrig.minigames.fishing.presentation.FishingViewModel

internal interface FishingComponent {
    val api: FishingApi
    val petApi: PetApi
    fun viewModel(): FishingViewModel
}
