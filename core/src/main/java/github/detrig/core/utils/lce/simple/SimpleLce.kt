package github.detrig.core.utils.lce.simple

/**
 * LCE == Loading | Content | Error.
 *
 * Контекст для данных, которые загружаются из внешнего источника
 * и могут завершиться ошибкой.
 */
interface SimpleLce<out T : Any> {

    companion object {
        fun <T : Any> loading(): SimpleLce<T> = SimpleLceImpl.loading()

        fun <T : Any> content(value: T): SimpleLce<T> = SimpleLceImpl.content(value)

        fun <T : Any> error(error: Throwable): SimpleLce<T> = SimpleLceImpl.error(error)

        fun <T : Any> error(): SimpleLce<T> = SimpleLceImpl.error()
    }

    val isLoading: Boolean
    val isContent: Boolean
    val isError: Boolean

    fun <R : Any> mapContent(transform: (T) -> R): SimpleLce<R>

    fun <R : Any> fold(
        onLoading: () -> R,
        onContent: (content: T) -> R,
        onError: (error: Throwable) -> R,
    ): R

    fun handle(
        onLoading: () -> Unit,
        onContent: (content: T) -> Unit,
        onError: (error: Throwable) -> Unit,
    )

    fun getContentOrNull(): T?

    fun requireContent(): T

    fun getErrorOrNull(): Throwable?

    fun requireError(): Throwable
}
