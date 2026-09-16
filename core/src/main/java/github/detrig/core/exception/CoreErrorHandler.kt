package github.detrig.core.exception

import github.detrig.core.exception.mapper.CoreExceptionMapper

/**
 * Глобальная точка обработки ошибок приложения.
 *
 * Должна быть проинициализирована при старте приложения через [init].
 *
 * [mapToAppException] преобразует [Throwable] в [AppException].
 * [handleException] выполняет дефолтную обработку ошибки.
 * [recordException] отправляет информацию об обработанной ошибке в recorder.
 */
object CoreErrorHandler {

    private lateinit var exceptionHandler: (exception: Throwable) -> Unit
    private lateinit var coreExceptionMapper: CoreExceptionMapper
    private lateinit var exceptionRecorder: (exception: AppException) -> Unit

    fun init(
        handler: (exception: Throwable) -> Unit,
        exceptionMapper: CoreExceptionMapper,
        recorder: (exception: AppException) -> Unit = {},
    ) {
        exceptionHandler = handler
        coreExceptionMapper = exceptionMapper
        exceptionRecorder = recorder
    }

    fun mapToAppException(exception: Throwable): AppException {
        return coreExceptionMapper.map(exception)
    }

    fun handleException(exception: Throwable) {
        exceptionHandler.invoke(exception)
    }

    fun recordException(exception: AppException) {
        exceptionRecorder.invoke(exception)
    }
}
