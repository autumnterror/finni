package github.detrig.internetbooster.mediators

import androidx.annotation.MainThread
import github.detrig.core.Mediator
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.internetbooster.database.MiniGamesCommonDatabaseModule
import github.detrig.internetbooster.network.AppNetworkModule
import github.detrig.minigames.common.MiniGamesCommonDependencies
import github.detrig.minigames.common.MiniGamesCommonFeature
import github.detrig.minigames.common.api.MiniGamesCommonApi
import github.detrig.minigames.common.data.database.api.MiniGameQuestionsDao
import github.detrig.minigames.common.data.network.RestApi

internal class MiniGamesCommonMediator(
    private val networkModule: AppNetworkModule,
    private val databaseModule: MiniGamesCommonDatabaseModule,
) : Mediator<MiniGamesCommonApi> {

    @MainThread
    fun init() {
        MiniGamesCommonFeature.dependenciesProvider = ModuleDependenciesProvider {
            MiniGamesCommonDependenciesImpl(
                networkModule = networkModule,
                databaseModule = databaseModule,
            )
        }
    }

    @MainThread
    override fun getApi(): MiniGamesCommonApi {
        return MiniGamesCommonFeature.getApi()
    }
}

private class MiniGamesCommonDependenciesImpl(
    private val networkModule: AppNetworkModule,
    private val databaseModule: MiniGamesCommonDatabaseModule,
) : MiniGamesCommonDependencies {

    override fun restApi(): RestApi {
        return networkModule.miniGamesRestApi
    }

    override fun miniGameQuestionsDao(): MiniGameQuestionsDao {
        return databaseModule.miniGameQuestionsDao
    }
}
