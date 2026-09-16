package github.detrig.feature.pet.domain.repository

import github.detrig.feature.pet.domain.model.PetProfile
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    fun observeProfile(): Flow<PetProfile?>
    fun saveProfile(value: PetProfile)
}
