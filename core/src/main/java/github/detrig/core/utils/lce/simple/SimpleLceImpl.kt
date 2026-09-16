package github.detrig.core.utils.lce.simple

internal class SimpleLceImpl<out T : Any>(
    private val result: Result<T?>,
) : SimpleLce<T> {

    companion object {

        fun <T : Any> loading(): SimpleLceImpl<T> = SimpleLceImpl(Result.success(null))

        fun <T : Any> content(value: T): SimpleLceImpl<T> = SimpleLceImpl(Result.success(value))

        fun <T : Any> error(error: Throwable): SimpleLceImpl<T> =
            SimpleLceImpl(Result.failure(SimpleLceException(null, error)))

        fun <T : Any> error(): SimpleLceImpl<T> =
            SimpleLceImpl(Result.failure(SimpleLceException("SimpleLceImpl error")))
    }

    override val isLoading: Boolean
        get() = result.isSuccess && result.getOrNull() == null

    override val isContent: Boolean
        get() = result.isSuccess && result.getOrNull() != null

    override val isError: Boolean
        get() = result.isFailure

    override fun <R : Any> mapContent(transform: (T) -> R): SimpleLce<R> {
        return fold(
            onLoading = Companion::loading,
            onContent = { value -> content(transform(value)) },
            onError = { error -> error.cause?.let { Companion.error(it) } ?: Companion.error() },
        )
    }

    override fun <R : Any> fold(
        onLoading: () -> R,
        onContent: (content: T) -> R,
        onError: (error: Throwable) -> R,
    ): R {
        return result.fold(
            onSuccess = { value ->
                value?.let(onContent) ?: onLoading()
            },
            onFailure = onError,
        )
    }

    override fun handle(
        onLoading: () -> Unit,
        onContent: (content: T) -> Unit,
        onError: (error: Throwable) -> Unit,
    ) {
        fold(onLoading, onContent, onError)
    }

    override fun getContentOrNull(): T? = result.getOrNull()

    override fun requireContent(): T = result.getOrNull()
        ?: throw IllegalStateException("No content available, current state = ${describeState()}")

    override fun getErrorOrNull(): Throwable? = result.exceptionOrNull()

    override fun requireError(): Throwable = result.exceptionOrNull()
        ?: throw IllegalStateException("No error available, current state = ${describeState()}")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleLceImpl<*>) return false
        return result == other.result
    }

    override fun hashCode(): Int {
        return result.fold(
            onSuccess = { value -> value.hashCode() },
            onFailure = { error -> error.hashCode() },
        )
    }

    override fun toString(): String {
        return result.fold(
            onSuccess = { value ->
                value?.let { "Content($it)" } ?: "Loading"
            },
            onFailure = { error -> "Error($error)" },
        )
    }

    private fun describeState(): String = result.fold(
        onSuccess = { data ->
            data?.let { "Content" } ?: "Loading"
        },
        onFailure = { "Error" },
    )
}
