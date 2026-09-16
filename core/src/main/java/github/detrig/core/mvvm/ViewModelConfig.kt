package github.detrig.core.mvvm

import androidx.lifecycle.MutableLiveData
import github.detrig.core.coroutines.CoroutinesDispatcherProvider

interface ViewModelConfig<S> {
    val dispatcherProvider: CoroutinesDispatcherProvider
    val mutableLiveData: MutableLiveData<S>
}

class DefaultViewModelConfig<S : CoreViewState> : ViewModelConfig<S> {
    override val dispatcherProvider = CoroutinesDispatcherProvider()
    override val mutableLiveData = MutableLiveData<S>()
}
