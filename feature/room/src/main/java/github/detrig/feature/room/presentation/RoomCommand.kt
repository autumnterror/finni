package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.command.ViewCommand

internal sealed interface RoomCommand : ViewCommand {
    data class ShowBuyConfirmation(val zoneId: String) : RoomCommand
    data class CloseBuyConfirmation(val zoneId: String) : RoomCommand
}
