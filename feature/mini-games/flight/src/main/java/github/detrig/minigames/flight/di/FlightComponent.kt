package github.detrig.minigames.flight.di

import github.detrig.feature.pet.api.PetApi
import github.detrig.core.audio.GameAudio
import github.detrig.minigames.flight.api.FlightApi
import github.detrig.minigames.flight.presentation.FlightViewModel

internal interface FlightComponent {
    val api: FlightApi
    val petApi: PetApi
    val gameAudio: GameAudio
    fun viewModel(): FlightViewModel
}
