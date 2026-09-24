package github.detrig.feature.shop.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetLazyColumn
import github.detrig.designsystem.component.FinPetOutlinedButton
import github.detrig.designsystem.component.FinPetQuantityStepper
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.shop.R
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.feature.shop.api.ShopItemDetail
import github.detrig.feature.shop.api.ShopItemDetailIcon
import github.detrig.feature.shop.api.ShopItemDetailsResolver
import github.detrig.feature.shop.domain.ShopPromotionKind
import github.detrig.products.FoodItem
import github.detrig.products.GroceryCatalog
import github.detrig.products.StoreCart

private val CartItemCardHeight = 116.dp
private val CartArtworkSize = 72.dp

@Composable
internal fun ShopCartContent(
    state: ShopCartViewState,
    artworkResolver: ShopArtworkResolver,
    itemDetailsResolver: ShopItemDetailsResolver,
    onEvent: (ShopCartViewEvent) -> Unit,
    onBack: () -> Unit = { onEvent(ShopCartViewEvent.Back) },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        val storefront = state.storefront
        if (storefront != null) {
            ShopHeader(
                title = stringResource(R.string.shop_cart_title),
                balanceRub = state.balanceRub,
                onBack = onBack,
            )
            if (state.lines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.shop_cart_empty),
                        style = AppTheme.typography.sectionTitle,
                        color = AppTheme.colors.storefront.onSurface,
                    )
                }
            } else {
                FinPetLazyColumn(
                    items = state.lines,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = AppTheme.spacing.lg,
                        vertical = AppTheme.spacing.md,
                    ),
                    itemSpacing = AppTheme.spacing.md,
                    key = { it.item.id.value },
                ) { line ->
                    ShopCartLineCard(
                        line = line,
                        artworkResolver = artworkResolver,
                        itemDetailsResolver = itemDetailsResolver,
                        enabled = !state.paymentInProgress,
                        onDecrease = { onEvent(ShopCartViewEvent.Decrease(line.item.id)) },
                        onIncrease = { onEvent(ShopCartViewEvent.Increase(line.item.id)) },
                    )
                }
                ShopCartFooter(
                    totalRub = state.totalRub,
                    canPay = state.canPay,
                    isPaymentInProgress = state.paymentInProgress,
                    shortfallRub = state.shortfallRub,
                    checkoutRejection = state.checkoutRejection,
                    onPay = { onEvent(ShopCartViewEvent.PayClicked) },
                )
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

    if (state.error == ShopCartError.LOAD) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.shop_error_title)) },
            text = { Text(stringResource(R.string.shop_error_body)) },
            confirmButton = {
                FinPetButton(
                    text = stringResource(R.string.shop_retry),
                    onClick = { onEvent(ShopCartViewEvent.Retry) },
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

}

@Composable
private fun ShopCartLineCard(
    line: ShopCartLineUi,
    artworkResolver: ShopArtworkResolver,
    itemDetailsResolver: ShopItemDetailsResolver,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    FinPetCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(CartItemCardHeight),
        shape = AppTheme.shapes.storefrontControl,
        containerColor = AppTheme.colors.storefront.surface,
        borderColor = AppTheme.colors.storefront.outline,
        borderWidth = AppTheme.sizes.borderStrong,
        elevation = AppTheme.elevation.low,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShopProductArtwork(
                item = line.item,
                artworkResolver = artworkResolver,
                artworkSize = CartArtworkSize,
                modifier = Modifier.size(CartArtworkSize),
            )
            Spacer(modifier = Modifier.width(AppTheme.spacing.sm))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                Text(
                    text = line.item.title,
                    style = AppTheme.typography.screenTitle,
                    color = AppTheme.colors.storefront.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val detail = itemDetailsResolver.details(line.item).firstOrNull()
                if (detail != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ShopDetailIcon(detail.icon)
                        Text(
                            text = detail.text,
                            style = AppTheme.typography.bodyStrong,
                            color = AppTheme.colors.actionPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(AppTheme.spacing.sm))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                FinPetQuantityStepper(
                    quantity = line.quantity,
                    onDecrease = onDecrease,
                    onIncrease = onIncrease,
                    enabled = enabled,
                    minWidth = 112.dp,
                    decreaseContentDescription = stringResource(
                        R.string.shop_decrease_quantity,
                        line.item.title,
                    ),
                    increaseContentDescription = stringResource(
                        R.string.shop_increase_quantity,
                        line.item.title,
                    ),
                )
                ShopCurrencyAmount(
                    amountRub = line.totalRub,
                    style = AppTheme.typography.bodyStrong,
                    coinSize = 24.dp,
                )
                val promotionText = when {
                    line.freeQuantity > 0 -> stringResource(
                        R.string.shop_cart_free_items,
                        line.freeQuantity,
                    )
                    line.promotionKind == ShopPromotionKind.BUY_TWO_GET_ONE_FREE -> stringResource(
                        R.string.shop_cart_items_until_free,
                        3 - line.quantity % 3,
                    )
                    line.savingRub > 0 -> stringResource(
                        R.string.shop_cart_discount,
                        line.savingRub,
                    )
                    else -> null
                }
                if (promotionText != null) {
                    Text(
                        text = promotionText,
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.actionPrimary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopCartFooter(
    totalRub: Long,
    canPay: Boolean,
    isPaymentInProgress: Boolean,
    shortfallRub: Long,
    checkoutRejection: github.detrig.feature.shop.api.ShopCheckoutRejection?,
    onPay: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppTheme.spacing.lg,
                end = AppTheme.spacing.lg,
                top = AppTheme.spacing.md,
                bottom = AppTheme.spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        ShopDashedDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.shop_cart_total),
                style = AppTheme.typography.brand,
                color = AppTheme.colors.storefront.onSurface,
            )
            ShopCurrencyAmount(
                amountRub = totalRub,
                style = AppTheme.typography.brand,
            )
        }
        if (!canPay && shortfallRub > 0) {
            Text(
                text = stringResource(R.string.shop_not_enough_money, shortfallRub),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.statusCritical.accent,
            )
        } else if (checkoutRejection != null) {
            Text(
                text = checkoutRejection.message(),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.statusCritical.accent,
            )
        }
        FinPetButton(
            text = stringResource(R.string.shop_pay),
            onClick = onPay,
            modifier = Modifier.fillMaxWidth(),
            enabled = canPay && !isPaymentInProgress,
            style = FinPetButtonDefaults.storefrontPrimaryStyle(),
        )
    }
}

@Composable
internal fun ShopCurrencyAmount(
    amountRub: Long,
    style: androidx.compose.ui.text.TextStyle,
    coinSize: androidx.compose.ui.unit.Dp = 32.dp,
) {
    val description = stringResource(R.string.shop_total_accessibility, amountRub)
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Surface(
            modifier = Modifier.size(coinSize),
            shape = CircleShape,
            color = AppTheme.colors.currencyContainer,
            contentColor = AppTheme.colors.currencyAccent,
            border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.currencyAccent),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "₽",
                    style = if (coinSize <= 24.dp) AppTheme.typography.label else AppTheme.typography.metricValue,
                )
            }
        }
        Text(
            text = amountRub.toString(),
            style = style,
            color = AppTheme.colors.storefront.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun ShopDashedDivider() {
    val color = AppTheme.colors.currencyAccent
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx())),
        )
    }
}

@Composable
private fun github.detrig.feature.shop.api.ShopCheckoutRejection.message(): String = when (this) {
    github.detrig.feature.shop.api.ShopCheckoutRejection.EMPTY_CART ->
        stringResource(R.string.shop_cart_empty)
    github.detrig.feature.shop.api.ShopCheckoutRejection.INVALID_CART ->
        stringResource(R.string.shop_payment_error)
    github.detrig.feature.shop.api.ShopCheckoutRejection.INSUFFICIENT_FUNDS ->
        stringResource(R.string.shop_payment_insufficient_funds)
    github.detrig.feature.shop.api.ShopCheckoutRejection.OPERATION_CONFLICT ->
        stringResource(R.string.shop_payment_error)
}

private val cartPreviewDetails = ShopItemDetailsResolver { item ->
    val food = item as? FoodItem ?: return@ShopItemDetailsResolver emptyList()
    listOf(ShopItemDetail("+${food.effects.satietyPercent}%", ShopItemDetailIcon.SATIETY))
}

@Preview(name = "Cart content", widthDp = 432, heightDp = 920, showBackground = true)
@Composable
private fun ShopCartContentPreview() {
    val catalog = GroceryCatalog().storefront
    val cart = StoreCart.Empty
        .add(catalog.items[0].id, quantity = 2)
        .add(catalog.items.first { it.title == "Салат" }.id)
        .add(catalog.items.first { it.title == "Молоко" }.id)
    FinPetTheme {
        ShopCartContent(
            state = ShopCartViewState(
                storefront = catalog,
                cart = cart,
                balanceRub = 480,
                loading = false,
            ),
            artworkResolver = ShopArtworkResolver.Empty,
            itemDetailsResolver = cartPreviewDetails,
            onEvent = {},
        )
    }
}
