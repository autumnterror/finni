package github.detrig.feature.pet.domain.interactor

import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.HamsterAppearance
import github.detrig.feature.pet.domain.model.PetNameRules
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.model.PetSpecies
import github.detrig.feature.pet.domain.repository.PetRepository

internal class CreatePetInteractor(
    private val repository: PetRepository,
) {
    operator fun invoke(
        name: String,
        species: PetSpecies,
        color: PetColor,
        hamsterAppearance: HamsterAppearance = HamsterAppearance(),
    ): PetProfile {
        val normalizedName = PetNameRules.normalize(name)
        require(PetNameRules.validate(normalizedName) == null) { "Invalid pet name" }
        return PetProfile(normalizedName, species, color, hamsterAppearance).also(repository::saveProfile)
    }
}
