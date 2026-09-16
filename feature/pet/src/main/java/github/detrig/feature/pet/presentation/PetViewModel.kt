package github.detrig.feature.pet.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.feature.pet.domain.interactor.CreatePetInteractor
import github.detrig.feature.pet.domain.model.PetNameRules
import github.detrig.feature.pet.domain.repository.PetRepository
import kotlinx.coroutines.Job

internal class PetViewModel(
    private val repository: PetRepository,
    private val createPet: CreatePetInteractor,
) : CoreViewModel<PetViewState, PetViewEvent>(PetViewState.Loading) {
    private var observationJob: Job? = null

    override fun perform(viewEvent: PetViewEvent) {
        when (viewEvent) {
            PetViewEvent.Load -> observeProfile()
            is PetViewEvent.NameChanged -> updateCreating {
                val limited = PetNameRules.limit(viewEvent.value)
                copy(name = limited, nameError = PetNameRules.validate(limited))
            }
            is PetViewEvent.SpeciesSelected -> updateCreating { copy(species = viewEvent.value) }
            is PetViewEvent.ColorSelected -> updateCreating { copy(color = viewEvent.value) }
            PetViewEvent.CreateClicked -> create()
        }
    }

    private fun observeProfile() {
        if (observationJob?.isActive == true) return
        observationJob = launchCoroutine {
            repository.observeProfile().collect { profile ->
                if (profile != null) {
                    updateState(PetViewState.Ready(profile))
                } else if (stateData !is PetViewState.Creating) {
                    updateState(PetViewState.Creating())
                }
            }
        }
    }

    private fun create() {
        val current = nullableState<PetViewState.Creating>() ?: return
        val error = PetNameRules.validate(current.name)
        if (error != null) {
            updateState(current.copy(nameError = error))
            return
        }
        createPet(current.name, current.species, current.color)
    }

    private fun updateCreating(block: PetViewState.Creating.() -> PetViewState.Creating) {
        nullableState<PetViewState.Creating>()?.let { updateState(it.block()) }
    }
}
