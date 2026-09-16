package github.detrig.feature.gamesession.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.gamesession.presentation.animation.PetAnimationState

internal sealed interface GameSessionViewState : CoreViewState {

    data object Loading : GameSessionViewState

    data class Content(
        val balanceText: String,
        val nextAllowanceText: String,
        val levelText: String,
        val hunger: Int,
        val thirst: Int,
        val happiness: Int,
        val health: Int,
        val petAnimationState: PetAnimationState = PetAnimationState.CALM,
    ) : GameSessionViewState

    data object Error : GameSessionViewState
}
