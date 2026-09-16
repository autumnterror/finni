package github.detrig.internetbooster.mediators

import android.content.Context
import android.content.SharedPreferences
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.gamestate.data.local.GameStateEntity
import github.detrig.feature.gamestate.domain.model.PetPlayCompletion
import github.detrig.internetbooster.database.AppDatabaseModule
import github.detrig.minigames.fishing.FishingDependencies
import github.detrig.minigames.fishing.FishingFeature
import github.detrig.minigames.fishing.api.*

internal class FishingMediator(
    private val core: CoreComponent,
    private val database: AppDatabaseModule,
    private val gameState: GameStateMediator,
    private val pet: PetMediator,
) {
    private val feedback by lazy {
        GameFeedbackPreferences(core.context.getSharedPreferences("finpet_feedback", Context.MODE_PRIVATE))
    }
    private val host by lazy {
        object : FishingHost {
            override suspend fun environment(): FishingEnvironment {
                val state = gameState.getApi().initialize()
                return FishingEnvironment(
                    GameStateEntity.CURRENT_STATE_ID,
                    github.detrig.feature.gamestate.domain.model.MiniGameAccess.isOpen("fishing", state.ownedZoneIds),
                )
            }
            override suspend fun applyPlayEffect(profileId: String, sessionId: String): Int =
                gameState.getApi().completePetPlay(PetPlayCompletion(profileId, sessionId, "fishing", true, 1))
            override fun showUnlockPreview() {
                github.detrig.feature.room.RoomFeature.getApi().requestZonePreview("fishing")
                core.globalNavigator.back()
            }
            override fun feedbackSettings() = feedback.read()
            override fun saveFeedbackSettings(settings: FishingFeedbackSettings) = feedback.save(settings)
        }
    }
    fun init() {
        FishingFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : FishingDependencies {
                override fun host() = host
                override fun fishingDao() = database.fishingDao
                override fun transactionRunner() = database.transactionRunner
                override fun globalNavigator() = core.globalNavigator
                override fun configurationJson() = core.context.assets.open("fishing_balance.json").bufferedReader().use { it.readText() }
                override fun currentTimeMillis() = System.currentTimeMillis()
                override fun petApi() = pet.getApi()
            }
        }
    }
}

/** Общие настройки обратной связи; следующие мини-игры используют тот же файл. */
internal class GameFeedbackPreferences(preferences: SharedPreferences) : SharedStorage(preferences) {
    fun read() = FishingFeedbackSettings(readBoolean("sound", true), readBoolean("haptics", true), readBoolean("reduced_motion", false))
    fun save(settings: FishingFeedbackSettings) {
        putBoolean("sound", settings.sound)
        putBoolean("haptics", settings.haptics)
        putBoolean("reduced_motion", settings.reducedMotion)
    }
}
