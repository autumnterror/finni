package github.detrig.feature.shop.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.core.mvvm.command.CommandsQueueEffect
import github.detrig.core.mvvm.command.ImmutableCommandsQueue
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.shop.ShopFeature
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.feature.shop.api.ShopItemDetailsResolver
import github.detrig.products.GroceryCatalog
import github.detrig.products.StoreId

@Composable
internal fun ShopScreen(
    storeId: StoreId,
    onBack: (() -> Unit)? = null,
    onOpenCart: (() -> Unit)? = null,
    closeAfterReceipt: (() -> Unit)? = null,
    highlightedProductId: github.detrig.products.ProductId? = null,
    onProductSelected: (github.detrig.products.ProductId) -> Unit = {},
    tutorialMessage: String? = null,
) {
    val component = ShopFeature.component()
    val viewModel: ShopViewModel = viewModel(key = "${storeId.value}:${onBack != null}") {
        component.viewModel(
            storeId = storeId,
            useHostBack = onBack != null,
            useHostCart = onOpenCart != null,
            useHostCloseAfterReceipt = closeAfterReceipt != null,
        )
    }
    val state by viewModel.state().observeAsState(ShopViewState())
    val commands = remember(viewModel) { ImmutableCommandsQueue(viewModel.commands<ShopCommand>()) }
    CommandsQueueEffect(commands) { command ->
        when (command) {
            ShopCommand.Back -> onBack?.invoke()
            ShopCommand.OpenCart -> onOpenCart?.invoke()
            ShopCommand.CloseAfterReceipt -> closeAfterReceipt?.invoke()
        }
    }

    LaunchedEffect(viewModel) { viewModel.perform(ShopViewEvent.Load) }
    val handleBack = { viewModel.perform(ShopViewEvent.Back) }
    BackHandler { handleBack() }

    Scaffold(
        containerColor = AppTheme.colors.storefront.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        ShopContent(
            state = state,
            artworkResolver = component.artworkResolver,
            itemDetailsResolver = component.itemDetailsResolver,
            petPortrait = component.petPortrait,
            onEvent = viewModel::perform,
            onBack = handleBack,
            modifier = if (onBack == null) Modifier.shopSafeDrawingPadding() else Modifier,
            contentPadding = padding,
            highlightedProductId = highlightedProductId,
            onProductSelected = onProductSelected,
            tutorialMessage = tutorialMessage,
        )
    }
}

@Preview(name = "Shop screen", widthDp = 360, heightDp = 760, showBackground = true)
@Composable
private fun ShopScreenPreview() {
    FinPetTheme {
        ShopContent(
            state = ShopViewState(
                storefront = GroceryCatalog().storefront,
                balanceRub = 25_000,
                loading = false,
            ),
            artworkResolver = ShopArtworkResolver.Empty,
            itemDetailsResolver = ShopItemDetailsResolver.Empty,
            onEvent = {},
        )
    }
}
