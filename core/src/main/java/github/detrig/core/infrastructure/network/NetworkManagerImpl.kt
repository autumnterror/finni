package github.detrig.core.infrastructure.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build

@Suppress("ReturnCount", "DEPRECATION")
internal class NetworkManagerImpl(private val context: Context) : NetworkManager {

    override fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val cap = cm.getNetworkCapabilities(cm.activeNetwork)
                return cap?.let { cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } ?: false
            }

            else -> {
                val networks: Array<Network> = cm.allNetworks
                for (n in networks) {
                    val info = cm.getNetworkInfo(n)
                    if (info != null && info.isConnected) return true
                }
                return false
            }
        }
    }
}
