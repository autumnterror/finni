package github.detrig.feature.gamesession.presentation

import github.detrig.core.mvvm.command.ViewCommand

internal sealed interface GameSessionCommand : ViewCommand {

    data class ShowMessage(
        val message: String,
    ) : GameSessionCommand
}
