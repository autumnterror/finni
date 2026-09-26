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
            is PetViewEvent.ColorSelected -> updateCreating { copy(color = viewEvent.value) }
            is PetViewEvent.HamsterAppearanceChanged -> updateCreating {
                copy(hamsterAppearance = viewEvent.value)
            }
            PetViewEvent.CustomizeClicked -> customize()
            PetViewEvent.CancelCustomization -> cancelCustomization()
            PetViewEvent.CreateClicked -> create()
            PetViewEvent.PetClicked -> if (stateData is PetViewState.Ready) {
                commands.onNext(PetCommand.ShowGreeting)
            }
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
        createPet(
            name = current.name,
            color = current.color,
            hamsterAppearance = current.hamsterAppearance,
            existingProfile = current.existingProfile,
        )
    }

    private fun customize() {
        val current = nullableState<PetViewState.Ready>() ?: return
        val profile = current.profile
        updateState(
            PetViewState.Creating(
                name = profile.name,
                color = profile.color,
                hamsterAppearance = profile.hamsterAppearance,
                existingProfile = profile,
            ),
        )
    }

    private fun cancelCustomization() {
        val current = nullableState<PetViewState.Creating>() ?: return
        current.existingProfile?.let { updateState(PetViewState.Ready(it)) }
    }

    private fun updateCreating(block: PetViewState.Creating.() -> PetViewState.Creating) {
        nullableState<PetViewState.Creating>()?.let { updateState(it.block()) }
    }
}
