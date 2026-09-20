package github.detrig.internetbooster.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetStorefrontProgressIndicator
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.internetbooster.R

@Composable
internal fun AppStartupGate(content: @Composable () -> Unit) {
    val viewModel: AppStartupViewModel = viewModel { AppStartupViewModel(AppAssetPreloader()) }
    val state by viewModel.state().observeAsState(AppStartupViewState.Loading())
    LaunchedEffect(viewModel) { viewModel.perform(AppStartupViewEvent.Load) }

    when (val current = state) {
        AppStartupViewState.Ready -> content()
        is AppStartupViewState.Loading -> AppStartupLoadingContent(current.progress)
        AppStartupViewState.Error -> AppStartupErrorContent {
            viewModel.perform(AppStartupViewEvent.Retry)
        }
    }
}

@Composable
private fun AppStartupLoadingContent(progress: Float) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.storefront.background),
    ) {
        Image(
            painter = painterResource(R.drawable.finni_loading_splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        FinPetStorefrontProgressIndicator(
            progress = progress,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = maxHeight * 0.814f)
                .fillMaxWidth(0.742f),
            height = maxHeight * 0.052f,
        )
    }
}

@Composable
private fun AppStartupErrorContent(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.storefront.background),
        contentAlignment = Alignment.Center,
    ) {
        FinPetButton(
            text = "Повторить",
            onClick = onRetry,
            style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            modifier = Modifier.padding(AppTheme.spacing.xl),
        )
    }
}

@Preview(name = "Загрузка приложения", widthDp = 360, heightDp = 640)
@Composable
private fun AppStartupLoadingPreview() {
    FinPetTheme {
        AppStartupLoadingContent(progress = 0.55f)
    }
}
