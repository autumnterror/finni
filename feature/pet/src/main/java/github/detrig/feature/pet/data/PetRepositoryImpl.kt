package github.detrig.feature.pet.data

import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.model.ClothingState
import github.detrig.feature.pet.domain.repository.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class PetRepositoryImpl(
    private val storage: PetProfileStorage,
) : PetRepository {
    private val profile = MutableStateFlow(storage.readProfile())

    override fun observeProfile(): StateFlow<PetProfile?> = profile

    override fun currentProfile(): PetProfile? = profile.value

    @Synchronized
    override fun saveProfile(value: PetProfile) {
        storage.saveProfile(value)
        profile.value = value
    }

    @Synchronized
    override fun updateClothing(change: (ClothingState) -> ClothingState): PetProfile {
        val current = requireNotNull(profile.value)
        return current.copy(clothing = change(current.clothing)).also(::saveProfile)
    }
}
