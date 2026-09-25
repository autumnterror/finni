package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.command.ViewCommand

internal sealed interface ShopCommand : ViewCommand {
    data object Back : ShopCommand
    data object OpenCart : ShopCommand
    data object CloseAfterReceipt : ShopCommand
}
