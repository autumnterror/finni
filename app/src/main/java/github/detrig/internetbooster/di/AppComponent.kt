package github.detrig.internetbooster.di

import github.detrig.core.di.CoreComponent

interface AppComponent {

    val coreComponent: CoreComponent

    fun initFeatures()
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
