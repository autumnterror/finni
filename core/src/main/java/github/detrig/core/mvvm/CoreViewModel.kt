package github.detrig.core.mvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import github.detrig.core.coroutines.CoroutinesDispatcherProvider
import github.detrig.core.exception.handler.CoroutineErrorHandler
import github.detrig.core.extensions.delegate
import github.detrig.core.extensions.onNext
import github.detrig.core.mvvm.command.CommandsQueue
import github.detrig.core.mvvm.command.ViewCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class CoreViewModel<S : CoreViewState, E : CoreViewEvent>(
    initialState: S? = null,
    mutableLiveData: MutableLiveData<S> = MutableLiveData<S>(),
    private val dispatcherProvider: CoroutinesDispatcherProvider = CoroutinesDispatcherProvider(),
) : ViewModel() {

    abstract fun perform(viewEvent: E)

    protected val state = mutableLiveData.apply {
        initialState?.let { onNext(it) }
    }

    protected var stateData: S by state.delegate()

    val commands = CommandsQueue<ViewCommand>()

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : ViewCommand> commands() = commands as CommandsQueue<T>

    fun state(): LiveData<S> = state

    fun launchCoroutine(
        handleAction: ExceptionConsumer = ExceptionConsumer { false },
        function: suspend CoroutineScope.() -> Unit,
    ): Job {
        return viewModelScope.launch(CoroutineErrorHandler(handleAction)) {
            function()
        }
    }

    @Deprecated(
        message = "Если нужно переключить dispatcher, используй withContext внутри launchCoroutine",
        replaceWith = ReplaceWith("launchCoroutine()")
    )
    fun launchIOCoroutine(
        handleAction: ExceptionConsumer = ExceptionConsumer { false },
        function: suspend CoroutineScope.() -> Unit,
    ): Job {
        return launchCoroutine(handleAction) {
            withContext(dispatcherProvider.io) {
                function()
            }
        }
    }

    protected fun updateState(newState: S) {
        stateData = newState
    }

    protected fun updateStateFromIo(newState: S) {
        state.postValue(newState)
    }

    protected fun updateState(block: S.() -> S) {
        updateState(block.invoke(stateData))
    }

    protected inline fun <reified T : S> updateRequiredState(block: T.() -> S) {
        updateState(block.invoke(requireState<T>()))
    }

    protected fun updateStateFromIo(block: S.() -> S) {
        state.postValue(block.invoke(stateData))
    }

    protected inline fun <reified T : S> requireState(): T {
        return stateData as T
    }

    protected inline fun <reified T : S> nullableState(): T? {
        return stateData as? T
    }
}
