package github.detrig.core.exception

import retrofit2.HttpException
import retrofit2.Response

/**
 * HttpException -> 300-500.
 */
open class ServerException(
    val code: Int,
    val body: String,
    inner: Throwable,
) : AppException(ExceptionType.Network, inner) {

    override val moduleCode: String
        get() = ""

    override val localCode: String
        get() = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ServerException

        if (code != other.code) return false
        if (body != other.body) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code
        result = 31 * result + body.hashCode()
        return result
    }
}

fun HttpException.toAppException(): AppException {
    return ServerException(
        code = code(),
        body = response()?.errorBody()?.string().orEmpty(),
        inner = this,
    )
}

fun <T> Response<T>.toAppException(): AppException {
    return HttpException(this).toAppException()
}
