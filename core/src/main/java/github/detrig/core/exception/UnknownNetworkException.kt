package github.detrig.core.exception

/**
 * Неизвестная сетевая ошибка.
 */
data class UnknownNetworkException(
    val inner: Throwable? = null,
) : AppException(ExceptionType.Network, inner) {

    override val moduleCode: String
        get() = ""

    override val localCode: String
        get() = ""
}
