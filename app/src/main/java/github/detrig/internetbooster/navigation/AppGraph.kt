package github.detrig.internetbooster.navigation

import github.detrig.core.presentation.navigation.v3.Nav3HostGraphSpec
import github.detrig.feature.gamesession.navigation.gameSessionGraph

fun appGraph(): Nav3HostGraphSpec {
    val home = gameSessionGraph()
    return home.copy(builder = {
        home.builder(this)
        github.detrig.feature.phone.PhoneFeature.getApi().entries()(this)
        github.detrig.feature.fridge.FridgeFeature.getApi().entries()(this)
        github.detrig.feature.savings.SavingsFeature.getApi().entries()(this)
        github.detrig.feature.shop.ShopFeature.getApi().entries()(this)
        github.detrig.minigames.fishing.FishingFeature.getApi().entries()(this)
        github.detrig.minigames.flight.FlightFeature.getApi().installEntries(this)
    })
}
