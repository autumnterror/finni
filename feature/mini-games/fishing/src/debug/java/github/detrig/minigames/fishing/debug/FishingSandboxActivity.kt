package github.detrig.minigames.fishing.debug

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.core.audio.AndroidGameAudio
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.minigames.fishing.api.*
import github.detrig.minigames.fishing.data.*
import github.detrig.minigames.fishing.domain.*
import github.detrig.minigames.fishing.navigation.FishingRouter
import github.detrig.minigames.fishing.presentation.FishingScreen
import github.detrig.minigames.fishing.presentation.FishingViewModel
import kotlinx.serialization.json.Json

/** Только debug: полная игра в отдельном сохранении, без покупки и изменения общего профиля. */
class FishingSandboxActivity : ComponentActivity() {
    private val dependencies by lazy { SandboxDependencies(applicationContext) }

    override fun onStart() {
        super.onStart()
        dependencies.gameAudio.setForeground(true)
    }

    override fun onStop() {
        dependencies.gameAudio.setForeground(false)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinPetTheme {
                FishingScreen(vm = viewModel {
                    dependencies.viewModel(object : FishingRouter {
                        override fun open() = Unit
                        override fun back() = finish()
                    })
                }, gameAudio = dependencies.gameAudio)
            }
        }
    }
}

@Database(entities = [FishingProgressEntity::class], version = 1, exportSchema = false)
abstract class FishingSandboxDatabase : RoomDatabase() { abstract fun fishingDao(): FishingDao }

private class SandboxDependencies(context: Context) {
    val gameAudio = AndroidGameAudio(context)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val config = json.decodeFromString<FishingConfig>(context.assets.open("fishing_balance.json").bufferedReader().use { it.readText() }).validate()
    private val engine = FishingEngine(config)
    private val preferences = SandboxPreferences(context)
    private val host = object : FishingHost {
        override suspend fun environment() = FishingEnvironment("fishing-sandbox", true)
        override fun showUnlockPreview() = Unit
        // Стенд показывает результат эффекта, не изменяя реального питомца.
        override suspend fun applyPlayEffect(profileId: String, sessionId: String) = 3
        override fun feedbackSettings() = preferences.read()
        override fun saveFeedbackSettings(settings: FishingFeedbackSettings) = preferences.save(settings)
    }
    private val database = database(context)
    private val repository = FishingRepositoryImpl(database.fishingDao(), RoomTransactionRunner(database), json, config.rulesVersion)
    fun viewModel(router: FishingRouter) = FishingViewModel(
        FishingInteractor(repository, host, engine, System::currentTimeMillis), router, System::currentTimeMillis,
    )
    companion object {
        @Volatile private var instance: FishingSandboxDatabase? = null
        private fun database(context: Context): FishingSandboxDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, FishingSandboxDatabase::class.java,
                "fishing_sandbox.db").build().also { instance = it }
        }
    }
}

private class SandboxPreferences(context: Context) : SharedStorage(context.getSharedPreferences("fishing_sandbox", Context.MODE_PRIVATE)) {
    fun read() = FishingFeedbackSettings(readBoolean("sound", true), readBoolean("haptics", true), readBoolean("reduced_motion", false))
    fun save(settings: FishingFeedbackSettings) {
        putBoolean("sound", settings.sound)
        putBoolean("haptics", settings.haptics)
        putBoolean("reduced_motion", settings.reducedMotion)
    }
}
