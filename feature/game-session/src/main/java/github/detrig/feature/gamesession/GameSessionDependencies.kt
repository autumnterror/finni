package github.detrig.feature.gamesession

import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.room.api.RoomApi

interface GameSessionDependencies {

    fun globalNavigator(): GlobalNavigator

    fun globalMessageController(): GlobalMessageController

    fun gameStateApi(): GameStateApi
    fun economyApi(): EconomyApi

    fun petApi(): PetApi

    fun roomApi(): RoomApi
}
