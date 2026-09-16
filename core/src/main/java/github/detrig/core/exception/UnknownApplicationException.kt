package github.detrig.core.exception

data class UnknownApplicationException(
    val inner: Throwable? = null,
) : AppException(ExceptionType.Application, inner) {

    override val moduleCode: String
        get() = ""

    override val localCode: String
        get() = ""
}
