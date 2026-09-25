package github.detrig.feature.gamestate.di

import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.gamestate.api.ProgressionApi

internal interface GameStateComponent {

    val api: GameStateApi
    val progressionApi: ProgressionApi
}
