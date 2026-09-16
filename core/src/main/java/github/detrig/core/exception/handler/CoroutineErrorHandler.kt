package github.detrig.core.exception.handler

import github.detrig.core.exception.AppException
import github.detrig.core.exception.CoreErrorHandler
import github.detrig.core.mvvm.ExceptionConsumer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Обработчик исключений для coroutine.
 *
 * [handleAction] дает экрану возможность обработать ошибку локально.
 * true - ошибка обработана, дефолтный обработчик не нужен.
 * false - ошибка уходит в общий CoreErrorHandler.
 */
class CoroutineErrorHandler(
    private val handleAction: ExceptionConsumer = ExceptionConsumer { false },
) : AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        val appException = CoreErrorHandler.mapToAppException(exception)

        runCatching {
            handleAction.consume(appException)
        }.onFailure { consumerException ->
            processException(handled = false, throwable = consumerException)
        }.onSuccess { handled ->
            processException(handled = handled, throwable = appException)
        }
    }

    private fun processException(handled: Boolean, throwable: Throwable) {
        if (throwable !is AppException) throw throwable
        if (handled) {
            CoreErrorHandler.recordException(throwable)
        } else {
            CoreErrorHandler.handleException(throwable)
        }
    }
}
