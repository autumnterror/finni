package github.detrig.internetbooster

import github.detrig.core.CoreApplication
import github.detrig.core.di.CoreComponentDependencies
import github.detrig.core.exception.CoreErrorHandler
import github.detrig.internetbooster.di.AppComponent
import github.detrig.internetbooster.time.TimeWorkScheduler
import github.detrig.internetbooster.time.HungerNotificationDispatcher

class FinPetApplication : CoreApplication() {

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        HungerNotificationDispatcher.createChannel(this)
        TimeWorkScheduler.schedule(this)
    }

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
