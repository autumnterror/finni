package github.detrig.feature.productmarket.presentation

import android.app.Activity
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.core.mvvm.command.CommandsQueueEffect
import github.detrig.core.mvvm.command.ImmutableCommandsQueue
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.designsystem.theme.FinPetThemePacks
import github.detrig.feature.productmarket.ProductMarketFeature
import kotlinx.coroutines.isActive

@Composable
@SuppressLint("SourceLockedOrientationActivity") // Портретный экран — явное требование этой фичи.
internal fun MarketScreen() {
    val viewModel: MarketViewModel = viewModel { ProductMarketFeature.component().viewModel() }
    val state by viewModel.state().observeAsState(MarketViewState())
    var pickup by remember { mutableStateOf<MarketViewCommand.Pickup?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val focused = LocalWindowInfo.current.isWindowFocused
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context.activity()
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            if (previous != null) activity.requestedOrientation = previous
            viewModel.perform(MarketViewEvent.Foreground(false))
        }
    }
    LaunchedEffect(viewModel) { viewModel.perform(MarketViewEvent.Load) }
    LaunchedEffect(lifecycle, focused) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.perform(MarketViewEvent.Foreground(focused))
            try {
                if (focused) {
                    var last = withFrameNanos { it }
                    while (isActive) {
                        val now = withFrameNanos { it }
                        viewModel.perform(MarketViewEvent.Frame((now - last) / 1_000_000_000.0))
                        last = now
                    }
                }
            } finally { viewModel.perform(MarketViewEvent.Foreground(false)) }
        }
    }
    BackHandler { viewModel.perform(MarketViewEvent.Back) }
    CommandsQueueEffect(remember(viewModel) { ImmutableCommandsQueue(viewModel.commands<MarketViewCommand>()) }) {
        when (it) { is MarketViewCommand.Pickup -> pickup = it }
    }
    FinPetTheme(FinPetThemePacks.wireframe) {
        MarketContent(state, viewModel.rules.config, viewModel.catalog, pickup, viewModel::perform)
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
