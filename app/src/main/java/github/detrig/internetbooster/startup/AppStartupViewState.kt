package github.detrig.internetbooster.startup

import github.detrig.core.mvvm.CoreViewState

internal sealed interface AppStartupViewState : CoreViewState {
    data class Loading(val progress: Float = 0f) : AppStartupViewState
    data object Ready : AppStartupViewState
    data object Error : AppStartupViewState
}
