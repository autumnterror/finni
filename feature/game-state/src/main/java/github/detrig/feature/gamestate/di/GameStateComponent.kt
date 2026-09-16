package github.detrig.feature.gamestate.di

import github.detrig.feature.gamestate.api.GameStateApi

internal interface GameStateComponent {

    val api: GameStateApi
}
