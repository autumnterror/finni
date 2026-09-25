package github.detrig.internetbooster.di

import github.detrig.core.di.CoreComponent
import github.detrig.core.audio.GameAudio

interface AppComponent {

    val coreComponent: CoreComponent
    val gameAudio: GameAudio

    fun initFeatures()
    suspend fun reconcileTimedEvents()
    suspend fun hasPetProfile(): Boolean
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
