package github.detrig.feature.pet.di

import github.detrig.feature.pet.PetDependencies
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.pet.api.PetApiImpl
import github.detrig.feature.pet.data.PetProfileStorage
import github.detrig.feature.pet.data.PetRepositoryImpl
import github.detrig.feature.pet.domain.interactor.CreatePetInteractor
import github.detrig.feature.pet.presentation.PetViewModel

internal class PetModule(
    private val dependencies: PetDependencies,
) : PetComponent {
    private val storage by lazy { PetProfileStorage(dependencies.profilePreferences()) }
    private val repository by lazy { PetRepositoryImpl(storage) }
    private val createPet by lazy { CreatePetInteractor(repository) }

    override val api: PetApi by lazy {
        PetApiImpl(repository, dependencies.assets())
    }

    override fun getPetViewModel(): PetViewModel = PetViewModel(repository, createPet)
}
