package github.detrig.internetbooster.network

import github.detrig.internetbooster.BuildConfig
import github.detrig.minigames.common.data.network.RestApi
import retrofit2.Retrofit

internal class AppNetworkModule {

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.MINI_GAMES_BASE_URL)
            .build()
    }

    val miniGamesRestApi: RestApi by lazy {
        retrofit.create(RestApi::class.java)
    }
}
