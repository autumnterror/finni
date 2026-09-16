package github.detrig.core

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import github.detrig.core.exception.handler.CoroutineErrorHandler
import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.core.mvvm.CoreViewState
import github.detrig.core.mvvm.ExceptionConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class CoreViewModel<S: CoreViewState, E : CoreViewEvent>(
    initialState: S? = null,
    mutableLiveData: MutableLiveData<S> = MutableLiveData<S>()
) : ViewModel() {
    abstract fun perform(viewEvent: E)

    protected val state = mutableLiveData.apply {
        initialState?.let { onNext(it) }
    }
    fun state(): LiveData<S> = state

    protected fun launchCoroutine(
        handleAction: ExceptionConsumer = ExceptionConsumer { false },
        function: suspend CoroutineScope.() -> Unit,
    ) : Job {
        return viewModelScope.launch(CoroutineErrorHandler(handleAction)) {
            function
        }
    }
}

fun <T> MutableLiveData<T>.onNext(next: T) {
    this.value = next
}