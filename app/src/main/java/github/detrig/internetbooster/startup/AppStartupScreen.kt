package github.detrig.internetbooster.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
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
            .background(AppTheme.colors.storefront.background)
            .clipToBounds(),
    ) {
        val artwork = painterResource(R.drawable.finni_loading_splash)
        val aspectRatio = artwork.intrinsicSize.width / artwork.intrinsicSize.height
        val artworkWidth = maxOf(maxWidth, maxHeight * aspectRatio)
        val artworkHeight = artworkWidth / aspectRatio
        val artworkLeft = (maxWidth - artworkWidth) / 2
        val artworkTop = (maxHeight - artworkHeight) / 2
        Image(
            painter = artwork,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        FinPetStorefrontProgressIndicator(
            progress = progress,
            modifier = Modifier
                .offset(
                    x = artworkLeft + artworkWidth * 0.129f,
                    y = artworkTop + artworkHeight * 0.820f,
                )
                .width(artworkWidth * 0.742f),
            height = artworkHeight * 0.052f,
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
@Preview(name = "Загрузка на высоком экране", widthDp = 360, heightDp = 800)
@Composable
private fun AppStartupLoadingPreview() {
    FinPetTheme {
        AppStartupLoadingContent(progress = 0.55f)
    }
}
