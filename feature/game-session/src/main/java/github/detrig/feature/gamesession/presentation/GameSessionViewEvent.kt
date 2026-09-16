package github.detrig.feature.gamesession.presentation

import github.detrig.core.mvvm.CoreViewEvent

internal sealed interface GameSessionViewEvent : CoreViewEvent {

    data object Load : GameSessionViewEvent

    data object ShopClicked : GameSessionViewEvent

    data object WorkClicked : GameSessionViewEvent

    data object TasksClicked : GameSessionViewEvent

    data object BankClicked : GameSessionViewEvent

    data object CareClicked : GameSessionViewEvent

    data object HomeClicked : GameSessionViewEvent
}
