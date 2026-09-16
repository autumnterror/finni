package github.detrig.feature.pet.di

import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.pet.presentation.PetViewModel

internal interface PetComponent {
    val api: PetApi
    fun getPetViewModel(): PetViewModel
}
