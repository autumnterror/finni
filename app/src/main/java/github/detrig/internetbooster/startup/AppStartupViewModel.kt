package github.detrig.internetbooster.startup

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer

internal class AppStartupViewModel(
    private val preloader: AppAssetPreloader,
) : CoreViewModel<AppStartupViewState, AppStartupViewEvent>(
    initialState = AppStartupViewState.Loading(),
) {
    override fun perform(viewEvent: AppStartupViewEvent) {
        when (viewEvent) {
            AppStartupViewEvent.Load,
            AppStartupViewEvent.Retry,
            -> load()
        }
    }

    private fun load() = launchCoroutine(
        handleAction = ExceptionConsumer {
            updateState(AppStartupViewState.Error)
            true
        },
    ) {
        updateState(AppStartupViewState.Loading())
        preloader.preload { progress ->
            // preload runs from viewModelScope's main context, so keep progress
            // updates synchronous and do not let a queued postValue overwrite Ready.
            updateState(AppStartupViewState.Loading(progress))
        }
        updateState(AppStartupViewState.Ready)
    }
}
