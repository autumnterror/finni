package github.detrig.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf

private val IgnoreModalVisibility: (Boolean) -> Unit = {}

/** Reports blocking modal windows to the app-level notification host. */
val LocalFinPetModalVisibilityReporter = compositionLocalOf { IgnoreModalVisibility }

/**
 * Registers the lifetime of a modal window.
 *
 * Achievement notifications remain queued while at least one registered modal is visible, so
 * platform dialogs and app-level popups never compete for the same screen space.
 */
@Composable
fun FinPetModalVisibilityEffect() {
    val reportVisibility = LocalFinPetModalVisibilityReporter.current
    DisposableEffect(reportVisibility) {
        reportVisibility(true)
        onDispose { reportVisibility(false) }
    }
}
