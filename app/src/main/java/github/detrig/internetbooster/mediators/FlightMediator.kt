package github.detrig.internetbooster.mediators

import android.content.Context
import android.content.SharedPreferences
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.gamestate.data.local.GameStateEntity
import github.detrig.feature.gamestate.domain.model.PetPlayCompletion
import github.detrig.feature.gamestate.domain.model.MiniGameAccess
import github.detrig.internetbooster.database.FlightDatabaseModule
import github.detrig.minigames.flight.FlightDependencies
import github.detrig.minigames.flight.FlightFeature
import github.detrig.minigames.flight.api.*
import kotlinx.coroutines.flow.first

internal class FlightMediator(
    private val core: CoreComponent,
    private val gameState: GameStateMediator,
    private val pet: PetMediator,
) {
    private val flightDatabase by lazy { FlightDatabaseModule(core.context) }
    private val feedback by lazy {
        FlightFeedbackPreferences(core.context.getSharedPreferences("finpet_feedback", Context.MODE_PRIVATE))
    }
    private val host by lazy {
        object : FlightHost {
            override suspend fun environment(): FlightEnvironment {
                val state = gameState.getApi().initialize()
                val profile = requireNotNull(pet.getApi().observeProfile().first())
                return FlightEnvironment(
                    GameStateEntity.CURRENT_STATE_ID,
                    MiniGameAccess.isOpen("flight", state.ownedZoneIds),
                    profile.appearanceId,
                )
            }
            override suspend fun applyPlayEffect(completion: FlightCompletion): Int =
                gameState.getApi().completePetPlay(PetPlayCompletion(
                    profileId = completion.profileId, sessionId = completion.sessionId, gameId = "flight",
                    completedNaturally = true, validActionCount = completion.flapCount,
                    activePlayMillis = completion.activeMillis,
                ))
            override fun feedbackSettings() = feedback.read()
            override fun saveFeedbackSettings(settings: FlightFeedbackSettings) = feedback.save(settings)
        }
    }
    fun init() {
        FlightFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : FlightDependencies {
                override fun host() = host
                override fun flightDao() = flightDatabase.dao
                override fun transactionRunner() = flightDatabase.transactionRunner
                override fun globalNavigator() = core.globalNavigator
                override fun configurationJson() = core.context.assets.open("flight_balance.json").bufferedReader().use { it.readText() }
                override fun currentTimeMillis() = System.currentTimeMillis()
                override fun petApi() = pet.getApi()
            }
        }
    }
}

/** Те же ключи общих настроек, что использует адаптер рыбалки. */
private class FlightFeedbackPreferences(preferences: SharedPreferences) : SharedStorage(preferences) {
    fun read() = FlightFeedbackSettings(readBoolean("sound", true), readBoolean("haptics", true),
        readBoolean("reduced_motion", false))
    fun save(settings: FlightFeedbackSettings) {
        putBoolean("sound", settings.sound)
        putBoolean("haptics", settings.haptics)
        putBoolean("reduced_motion", settings.reducedMotion)
    }
}
