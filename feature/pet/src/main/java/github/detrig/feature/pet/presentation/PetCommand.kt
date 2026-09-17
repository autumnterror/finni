package github.detrig.feature.pet.presentation

import github.detrig.core.mvvm.command.ViewCommand

internal sealed interface PetCommand : ViewCommand {
    data object ShowGreeting : PetCommand
}
