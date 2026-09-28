package github.detrig.internetbooster.di

import github.detrig.core.di.CoreComponent
import github.detrig.core.audio.GameAudio
import github.detrig.internetbooster.startup.AppExperienceMode
import kotlinx.coroutines.flow.StateFlow

interface AppComponent {

    val coreComponent: CoreComponent
    val gameAudio: GameAudio
    val selectedExperienceMode: StateFlow<AppExperienceMode?>

    fun initFeatures()
    fun selectExperienceMode(mode: AppExperienceMode)
    suspend fun reconcileTimedEvents()
    suspend fun hasPetProfile(): Boolean
    suspend fun resetDemoProgress(skipOnboarding: Boolean)
}

fun AppComponent(
    coreComponent: CoreComponent,
): AppComponent {
    val appModule = AppModuleImpl(coreComponent)

    return object : AppComponent,
        AppModule by appModule {

        override val coreComponent: CoreComponent = coreComponent
    }
}
