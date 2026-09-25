package github.detrig.minigames.fishing.di

import github.detrig.feature.pet.api.PetApi
import github.detrig.core.audio.GameAudio
import github.detrig.minigames.fishing.api.FishingApi
import github.detrig.minigames.fishing.presentation.FishingViewModel

internal interface FishingComponent {
    val api: FishingApi
    val petApi: PetApi
    val gameAudio: GameAudio
    fun viewModel(): FishingViewModel
}
