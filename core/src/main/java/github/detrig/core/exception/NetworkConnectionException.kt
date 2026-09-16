package github.detrig.core.exception

/**
 * Нет подключения к сети.
 */
class NetworkConnectionException(
    inner: Throwable? = null,
) : AppException(ExceptionType.Network, inner) {

    override val moduleCode: String
        get() = ""

    override val localCode: String
        get() = ""
}
