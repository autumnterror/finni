package github.detrig.feature.shop.presentation

import github.detrig.core.mvvm.command.ViewCommand

internal sealed interface ShopCartCommand : ViewCommand {
    data object Back : ShopCartCommand
    data class CheckoutCompleted(val spentRub: Long) : ShopCartCommand
}
