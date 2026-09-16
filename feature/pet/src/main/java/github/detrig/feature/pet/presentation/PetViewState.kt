package github.detrig.feature.pet.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.PetNameRules
import github.detrig.feature.pet.domain.model.PetNameValidationError
import github.detrig.feature.pet.domain.model.PetProfile
import github.detrig.feature.pet.domain.model.PetSpecies

internal sealed interface PetViewState : CoreViewState {
    data object Loading : PetViewState

    data class Creating(
        val name: String = "",
        val species: PetSpecies = PetSpecies.Cat,
        val color: PetColor = PetColor.Sunny,
        val nameError: PetNameValidationError? = null,
    ) : PetViewState {
        val canCreate: Boolean get() = PetNameRules.validate(name) == null
    }

    data class Ready(val profile: PetProfile) : PetViewState
}
