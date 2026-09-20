package github.detrig.feature.gamesession.di

import github.detrig.feature.gamesession.api.GameSessionApi
import github.detrig.feature.gamesession.navigation.GameSessionRouter
import github.detrig.feature.gamesession.presentation.GameSessionViewModel
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.phone.api.PhoneApi

internal interface GameSessionComponent {

    val api: GameSessionApi
    val petApi: PetApi
    val roomApi: RoomApi
    val phoneApi: PhoneApi
    val router: GameSessionRouter

    fun getGameSessionViewModel(): GameSessionViewModel
}
