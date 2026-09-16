package github.detrig.internetbooster

import github.detrig.core.CoreApplication
import github.detrig.core.di.CoreComponentDependencies
import github.detrig.core.exception.CoreErrorHandler
import github.detrig.internetbooster.di.AppComponent

class FinPetApplication : CoreApplication() {

    lateinit var appComponent: AppComponent
        private set

    override fun initCoreComponent() {
        super.initCoreComponent()
        appComponent = AppComponent(coreComponent)
        appComponent.initFeatures()
        CoreErrorHandler.init(
            handler = { throw it },
            exceptionMapper = coreComponent.coreExceptionMapper,
        )
    }

    override fun coreComponentDependencies(): CoreComponentDependencies {
        return object : CoreComponentDependencies {
            override val applicationContext = this@FinPetApplication
        }
    }
}
