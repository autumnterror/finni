package github.detrig.core.exception.mapper

import android.system.ErrnoException
import github.detrig.core.exception.AppException
import github.detrig.core.exception.NetworkConnectionException
import github.detrig.core.exception.UnknownApplicationException
import github.detrig.core.exception.UnknownNetworkException
import github.detrig.core.exception.toAppException
import github.detrig.core.infrastructure.network.NetworkManager
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import kotlin.reflect.KClass

/**
 * Маппер исключений core.
 *
 * Преобразует обычный [Throwable] в [AppException], чтобы вся обработка ошибок
 * в приложении работала с одной иерархией ошибок.
 */
open class CoreExceptionMapper(
    private val networkManager: NetworkManager,
) {

    companion object {

        @JvmStatic
        val NETWORK_EXCEPTIONS: Array<KClass<out Throwable>> = arrayOf(
            HttpException::class,
            SSLHandshakeException::class,
            UnknownHostException::class,
            ConnectException::class,
            ErrnoException::class,
            SocketTimeoutException::class,
        )
    }

    open fun map(from: Throwable): AppException {
        return when (from) {
            is AppException -> from
            is HttpException -> from.toAppException()
            is SSLException, is SocketTimeoutException -> UnknownNetworkException(from)
            is UnknownHostException, is ConnectException, is ErrnoException -> checkInternetConnection(from)
            else -> UnknownApplicationException(from)
        }
    }

    private fun checkInternetConnection(from: Throwable): AppException {
        if (networkManager.isNetworkAvailable().not()) {
            return NetworkConnectionException(from)
        }
        return UnknownNetworkException(from)
    }
}
