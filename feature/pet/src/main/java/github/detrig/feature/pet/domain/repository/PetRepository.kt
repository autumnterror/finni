package github.detrig.feature.pet.domain.repository

import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.model.ClothingState
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    fun observeProfile(): Flow<PetProfile?>
    fun currentProfile(): PetProfile?
    fun saveProfile(value: PetProfile)
    fun updateClothing(change: (ClothingState) -> ClothingState): PetProfile
}
