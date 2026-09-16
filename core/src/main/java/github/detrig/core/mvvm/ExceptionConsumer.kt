package github.detrig.core.mvvm

import github.detrig.core.exception.AppException

fun interface ExceptionConsumer {
    fun consume(appException: AppException): Boolean
}
