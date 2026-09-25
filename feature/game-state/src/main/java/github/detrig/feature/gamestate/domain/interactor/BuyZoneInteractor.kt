package github.detrig.feature.gamestate.domain.interactor

import github.detrig.feature.gamestate.domain.GameStateRepository
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.gamestate.domain.model.ZoneOffer

internal class BuyZoneInteractor(
    private val repository: GameStateRepository,
) {
    suspend operator fun invoke(offer: ZoneOffer, useSavings: Boolean = false): ZoneBuyResult =
        repository.buyZone(offer, useSavings)
}
