package github.detrig.internetbooster.startup

import github.detrig.core.mvvm.CoreViewEvent

internal sealed interface AppStartupViewEvent : CoreViewEvent {
    data object Load : AppStartupViewEvent
    data object Retry : AppStartupViewEvent
}
