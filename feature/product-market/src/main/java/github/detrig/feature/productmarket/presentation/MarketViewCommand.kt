package github.detrig.feature.productmarket.presentation

import github.detrig.core.mvvm.command.ViewCommand
import github.detrig.feature.productmarket.domain.MarketSlot

internal sealed interface MarketViewCommand : ViewCommand {
    data class Pickup(val sequence: Long, val slot: MarketSlot, val camera: Double) : MarketViewCommand
}
