package github.detrig.feature.gamesession.presentation.animation

import github.detrig.feature.gamestate.domain.PetState

internal fun PetState.toPetAnimationState(): PetAnimationState {
    return when {
        health < CRITICAL_STAT_VALUE -> PetAnimationState.SICK
        hunger < CRITICAL_STAT_VALUE || thirst < CRITICAL_STAT_VALUE -> PetAnimationState.LOW_ENERGY
        happiness > HAPPY_STAT_VALUE -> PetAnimationState.HAPPY
        else -> PetAnimationState.CALM
    }
}

private const val CRITICAL_STAT_VALUE = 35
private const val HAPPY_STAT_VALUE = 85
