package github.detrig.feature.shop.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetOutlinedButton
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.shop.R
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.feature.shop.api.ShopItemDetail
import github.detrig.feature.shop.api.ShopItemDetailIcon
import github.detrig.feature.shop.api.ShopItemDetailsResolver
import github.detrig.feature.shop.api.ShopPetPortrait
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.products.FoodItem
import github.detrig.products.GroceryCatalog

@Composable
internal fun ShopContent(
    state: ShopViewState,
    artworkResolver: ShopArtworkResolver,
    itemDetailsResolver: ShopItemDetailsResolver,
    petPortrait: ShopPetPortrait = ShopPetPortrait.Empty,
    onEvent: (ShopViewEvent) -> Unit,
    onBack: () -> Unit = { onEvent(ShopViewEvent.Back) },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    highlightedProductId: github.detrig.products.ProductId? = null,
    onProductSelected: (github.detrig.products.ProductId) -> Unit = {},
    tutorialMessage: String? = null,
) {
    val gridState = rememberLazyGridState()
    val promotionIntro = state.decisionEvent?.takeIf {
        state.eventDialogueVisible && it.type == ShopDecisionEventType.PROMOTION
    }
    var promotionInView by rememberSaveable(promotionIntro?.eventId) { mutableStateOf(false) }
    val visibleItems = state.visibleItems
    LaunchedEffect(promotionIntro?.eventId, visibleItems, state.selectedCategoryId) {
        val promotion = promotionIntro ?: return@LaunchedEffect
        val index = visibleItems.indexOfFirst { it.id == promotion.productId }
        if (index < 0) {
            onEvent(ShopViewEvent.CategorySelected(null))
        } else {
            gridState.animateScrollToItem(index)
            promotionInView = true
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .testTag("shop_screen"),
    ) {
        val storefront = state.storefront
        if (storefront != null) {
            ShopHeader(
                title = storefront.title,
                balanceRub = state.balanceRub,
                onBack = onBack,
            )
            ShopCategoryRow(
                storefront = storefront,
                selectedCategoryId = state.selectedCategoryId,
                onSelected = { onEvent(ShopViewEvent.CategorySelected(it)) },
            )
            tutorialMessage?.let { message ->
                FinPetModalSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.xs),
                    tone = FinPetModalSectionTone.Highlighted,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(AppTheme.spacing.md),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.storefront.onSurface,
                    )
                }
            }
            if (state.visibleItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.shop_empty_category),
                        style = AppTheme.typography.body,
                    )
                }
            } else {
                ShopProductGrid(
                    items = state.visibleItems,
                    columns = storefront.gridLayout.columns,
                    gridState = gridState,
                    quantityInCart = state::quantityInCart,
                    onItemClick = {
                        onEvent(ShopViewEvent.ProductClicked(it))
                        onProductSelected(it)
                    },
                    artworkResolver = artworkResolver,
                    itemDetailsResolver = itemDetailsResolver,
                    unitPriceRub = state::effectiveUnitPrice,
                    decisionEvent = state.decisionEvent,
                    highlightedProductId = highlightedProductId,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!state.cart.isEmpty) {
                FinPetButton(
                    onClick = { onEvent(ShopViewEvent.OpenCart) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = AppTheme.spacing.lg,
                            end = AppTheme.spacing.lg,
                            top = AppTheme.spacing.md,
                            bottom = AppTheme.spacing.xl,
                        ),
                    style = FinPetButtonDefaults.storefrontPrimaryStyle(),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ShopCartIcon(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterStart),
                        )
                        Text(
                            text = stringResource(R.string.shop_open_cart),
                            modifier = Modifier.align(Alignment.Center),
                            style = AppTheme.typography.button,
                            maxLines = 1,
                        )
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.loading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.shop_loading),
                            modifier = Modifier.padding(top = AppTheme.spacing.md),
                            style = AppTheme.typography.body,
                        )
                    }
                }
            }
        }
    }

    if (state.error == ShopError.LOAD) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.shop_error_title)) },
            text = { Text(stringResource(R.string.shop_error_body)) },
            confirmButton = {
                FinPetButton(
                    text = stringResource(R.string.shop_retry),
                    onClick = { onEvent(ShopViewEvent.Retry) },
                    style = FinPetButtonDefaults.storefrontPrimaryStyle(),
                )
            },
            dismissButton = {
                FinPetOutlinedButton(
                    text = stringResource(R.string.shop_back),
                    onClick = onBack,
                    style = FinPetButtonDefaults.storefrontOutlinedStyle(),
                )
            },
        )
    }

    state.receipt?.let { receipt ->
        ShopReceiptDialog(
            receipt = receipt,
            onDismiss = { onEvent(ShopViewEvent.ReceiptDismissed) },
        )
    }

    if (state.receipt == null && state.purchaseFeedback != null) {
        ShopPurchaseFeedbackDialog(
            feedback = state.purchaseFeedback,
            speakerName = state.petName,
            portrait = petPortrait,
            onFinished = { onEvent(ShopViewEvent.PurchaseFeedbackFinished) },
        )
    }

    if (state.receipt == null && state.purchaseFeedback == null && state.eventDialogueVisible &&
        (promotionIntro == null || promotionInView)
    ) {
        val event = state.decisionEvent
        if (event != null) {
            ShopDecisionEventDialog(
                event = event,
                speakerName = state.petName,
                portrait = petPortrait,
                onFinished = { onEvent(ShopViewEvent.EventDialogueFinished) },
            )
        }
    }
}

private val shopContentPreviewDetails = ShopItemDetailsResolver { item ->
    val food = item as? FoodItem ?: return@ShopItemDetailsResolver emptyList()
    listOf(ShopItemDetail("+${food.effects.satietyPercent}%", ShopItemDetailIcon.SATIETY))
}

@Preview(name = "Product catalog", widthDp = 432, heightDp = 920, showBackground = true)
@Composable
private fun ShopContentPreview() {
    FinPetTheme {
        ShopContent(
            state = ShopViewState(
                storefront = GroceryCatalog().storefront,
                cart = github.detrig.products.StoreCart.Empty
                    .add(GroceryCatalog().storefront.items.first().id, quantity = 2),
                balanceRub = 12_500,
                loading = false,
            ),
            artworkResolver = ShopArtworkResolver.Empty,
            itemDetailsResolver = shopContentPreviewDetails,
            petPortrait = ShopPetPortrait.Empty,
            onEvent = {},
        )
    }
}
