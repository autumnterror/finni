package github.detrig.internetbooster

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import github.detrig.core.view.Nav3Activity
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.internetbooster.navigation.appGraph
import github.detrig.internetbooster.startup.AppStartupGate

class MainActivity : Nav3Activity(
    navHostSpec = appGraph(),
) {
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
