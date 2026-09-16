package github.detrig.core.exception

/**
 * Базовая ошибка приложения.
 */
abstract class AppException(
    val type: ExceptionType,
    val innerException: Throwable? = null,
) : RuntimeException(innerException) {

    abstract val moduleCode: String
    abstract val localCode: String

    override fun toString(): String {
        return "${javaClass.simpleName}[$moduleCode,$localCode,$type] --> ${innerException?.toString()}"
    }
}
