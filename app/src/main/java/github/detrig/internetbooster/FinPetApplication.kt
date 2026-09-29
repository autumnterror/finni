package github.detrig.internetbooster

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import github.detrig.core.CoreApplication
import github.detrig.core.di.CoreComponentDependencies
import github.detrig.core.exception.CoreErrorHandler
import github.detrig.internetbooster.di.AppComponent
import github.detrig.internetbooster.time.TimeWorkScheduler
import github.detrig.internetbooster.time.HungerNotificationDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FinPetApplication : CoreApplication() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        HungerNotificationDispatcher.createChannel(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = reconcileNotifications()
            override fun onStop(owner: LifecycleOwner) = reconcileNotifications()
        })
        TimeWorkScheduler.schedule(this)
    }

    private fun reconcileNotifications() {
        applicationScope.launch { appComponent.reconcileTimedEvents() }
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
