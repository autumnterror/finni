package github.detrig.core

import android.app.Application
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.CoreComponentDependencies

abstract class CoreApplication : Application() {

    companion object {
        lateinit var app: CoreApplication
    }

    lateinit var coreComponent: CoreComponent

    init {
        app = this
    }

    override fun onCreate() {
        initCoreComponent()
        super.onCreate()
    }

    open fun initCoreComponent() {
        if (::coreComponent.isInitialized) return

        coreComponent = CoreComponent(
            dependencies = coreComponentDependencies()
        )
    }

    protected abstract fun coreComponentDependencies(): CoreComponentDependencies
}