package github.detrig.internetbooster

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import github.detrig.core.view.Nav3Activity
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.internetbooster.navigation.appGraph
import github.detrig.internetbooster.startup.AppStartupGate
import github.detrig.internetbooster.time.NotificationPermissionStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : Nav3Activity(
    navHostSpec = appGraph(),
) {
    private var timeReconciliationJob: Job? = null
    private val notificationPermissionStorage by lazy { NotificationPermissionStorage(this) }
    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) lifecycleScope.launch {
            (application as FinPetApplication).appComponent.reconcileTimedEvents()
        }
    }

    override fun onStart() {
        super.onStart()
        (application as FinPetApplication).appComponent.gameAudio.setForeground(true)
        timeReconciliationJob = lifecycleScope.launch {
            while (isActive) {
                (application as FinPetApplication).appComponent.reconcileTimedEvents()
                requestPetNotificationPermissionIfNeeded()
                delay(60_000)
            }
        }
    }

    private suspend fun requestPetNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33 || notificationPermissionStorage.wasRequested()) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) return
        if (!(application as FinPetApplication).appComponent.hasPetProfile()) return
        notificationPermissionStorage.markRequested()
        notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onStop() {
        timeReconciliationJob?.cancel()
        timeReconciliationJob = null
        (application as FinPetApplication).appComponent.gameAudio.setForeground(false)
        super.onStop()
    }

    @Composable
    override fun ProvideAppContent(content: @Composable () -> Unit) {
        FinPetTheme {
            AppStartupGate {
                AchievementNotificationHost(content)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
