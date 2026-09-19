package github.detrig.feature.pet.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.feature.pet.domain.model.PetColor
import github.detrig.feature.pet.domain.model.HamsterAppearance
import github.detrig.feature.pet.domain.model.PetSpecies

internal sealed interface PetViewEvent : CoreViewEvent {
    data object Load : PetViewEvent
    data class NameChanged(val value: String) : PetViewEvent
    data class SpeciesSelected(val value: PetSpecies) : PetViewEvent
    data class ColorSelected(val value: PetColor) : PetViewEvent
    data class HamsterAppearanceChanged(val value: HamsterAppearance) : PetViewEvent
    data object CustomizeClicked : PetViewEvent
    data object CancelCustomization : PetViewEvent
    data object CreateClicked : PetViewEvent
    data object PetClicked : PetViewEvent
}
