package github.detrig.feature.pet.data

import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.repository.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class PetRepositoryImpl(
    private val storage: PetProfileStorage,
) : PetRepository {
    private val profile = MutableStateFlow(storage.readProfile())

    override fun observeProfile(): StateFlow<PetProfile?> = profile

    override fun saveProfile(value: PetProfile) {
        check(profile.value == null) { "Pet profile has already been created" }
        storage.saveProfile(value)
        profile.value = value
    }
}
