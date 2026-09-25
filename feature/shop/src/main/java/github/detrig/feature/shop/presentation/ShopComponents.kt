package github.detrig.feature.shop.presentation

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetBackButton
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetCoinIcon
import github.detrig.designsystem.component.FinPetFilterChip
import github.detrig.designsystem.component.FinPetFilterChipDefaults
import github.detrig.designsystem.component.FinPetGridColumns
import github.detrig.designsystem.component.FinPetLazyGrid
import github.detrig.designsystem.component.FinPetLazyRow
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.shop.R
import github.detrig.feature.shop.api.ShopArtwork
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.feature.shop.api.ShopItemDetailIcon
import github.detrig.feature.shop.api.ShopItemDetailsResolver
import github.detrig.feature.shop.domain.ShopDecisionEvent
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.feature.shop.domain.ShopPromotionKind
import github.detrig.products.FoodItem
import github.detrig.products.GroceryCatalog
import github.detrig.products.ProductId
import github.detrig.products.SellableItem
import github.detrig.products.StoreCategory
import github.detrig.products.StoreCategoryId
import github.detrig.products.StorefrontDefinition

private val ProductCardHeight = 198.dp
private val ProductArtworkHeight = 82.dp
private val ProductTitleHeight = 36.dp
private val ProductDetailHeight = 22.dp
private val ProductPriceHeight = 34.dp
private val PromotionBadgeSize = 64.dp

/** Applies Android display-cutout and navigation safe areas only to storefront screens. */
@Composable
internal fun Modifier.shopSafeDrawingPadding(): Modifier = windowInsetsPadding(
    insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
)

@Composable
internal fun ShopHeader(
    title: String,
    balanceRub: Long?,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md),
    ) {
        FinPetBackButton(
            onClick = onBack,
            contentDescription = stringResource(R.string.shop_back),
            modifier = Modifier.align(Alignment.CenterStart),
            size = AppTheme.sizes.preferredTouchTarget - 4.dp,
        )
        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 96.dp),
            style = AppTheme.typography.screenTitle,
            color = AppTheme.colors.storefront.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ShopBalanceBadge(
            balanceRub = balanceRub,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun ShopBalanceBadge(
    balanceRub: Long?,
    modifier: Modifier = Modifier,
) {
    val value = balanceRub?.toString() ?: "—"
    val valueStyle = compactCurrencyStyle(value.length)

    Surface(
        modifier = modifier.heightIn(min = AppTheme.sizes.preferredTouchTarget - 4.dp),
        shape = AppTheme.shapes.storefrontControl,
        color = AppTheme.colors.storefront.surface,
        contentColor = AppTheme.colors.storefront.onSurface,
        border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
        shadowElevation = AppTheme.elevation.low,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = valueStyle,
                maxLines = 1,
            )
            FinPetCoinIcon()
        }
    }
}

@Composable
private fun compactCurrencyStyle(characterCount: Int): TextStyle = when {
    characterCount >= 7 -> AppTheme.typography.caption
    characterCount >= 5 -> AppTheme.typography.bodyStrong
    else -> AppTheme.typography.currency
}

private sealed interface ShopFilterOption {
    val key: String
    val title: String

    data class All(override val title: String) : ShopFilterOption {
        override val key = "filter:all"
    }

    data class Category(val category: StoreCategory) : ShopFilterOption {
        override val key = "filter:category:${category.id.value}"
        override val title = category.title
    }
}

@Composable
internal fun ShopCategoryRow(
    storefront: StorefrontDefinition<SellableItem>,
    selectedCategoryId: StoreCategoryId?,
    onSelected: (StoreCategoryId?) -> Unit,
) {
    val options = listOf(ShopFilterOption.All(storefront.allItemsLabel)) +
        storefront.categories.map(ShopFilterOption::Category)
    FinPetLazyRow(
        items = options,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.spacing.xs, bottom = AppTheme.spacing.sm),
        contentPadding = PaddingValues(horizontal = AppTheme.spacing.lg),
        itemSpacing = AppTheme.spacing.sm,
        key = ShopFilterOption::key,
    ) { option ->
        val categoryId = (option as? ShopFilterOption.Category)?.category?.id
        FinPetFilterChip(
            text = option.title,
            selected = selectedCategoryId == categoryId,
            onClick = { onSelected(categoryId) },
            modifier = Modifier.testTag("shop_category_${option.key}"),
            style = FinPetFilterChipDefaults.storefrontCompactStyle(),
        )
    }
}

@Composable
internal fun ShopProductGrid(
    items: List<SellableItem>,
    columns: Int,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    quantityInCart: (ProductId) -> Int,
    onItemClick: (ProductId) -> Unit,
    artworkResolver: ShopArtworkResolver,
    itemDetailsResolver: ShopItemDetailsResolver,
    unitPriceRub: (ProductId) -> Long = { productId ->
        items.first { it.id == productId }.priceRub
    },
    decisionEvent: ShopDecisionEvent? = null,
    highlightedProductId: ProductId? = null,
    modifier: Modifier = Modifier,
) {
    FinPetLazyGrid(
        items = items,
        state = gridState,
        modifier = modifier.fillMaxWidth(),
        columns = FinPetGridColumns.Fixed(columns),
        contentPadding = PaddingValues(AppTheme.spacing.lg),
        horizontalSpacing = AppTheme.spacing.md,
        verticalSpacing = AppTheme.spacing.md,
        key = { it.id.value },
    ) { item ->
        ShopProductCard(
            item = item,
            quantity = quantityInCart(item.id),
            onClick = { onItemClick(item.id) },
            artworkResolver = artworkResolver,
            itemDetailsResolver = itemDetailsResolver,
            unitPriceRub = unitPriceRub(item.id),
            decisionEvent = decisionEvent?.takeIf { it.productId == item.id },
            tutorialHighlighted = item.id == highlightedProductId,
        )
    }
}

@Composable
private fun ShopProductCard(
    item: SellableItem,
    quantity: Int,
    onClick: () -> Unit,
    artworkResolver: ShopArtworkResolver,
    itemDetailsResolver: ShopItemDetailsResolver,
    unitPriceRub: Long,
    decisionEvent: ShopDecisionEvent?,
    tutorialHighlighted: Boolean = false,
) {
    val itemDescription = stringResource(
        R.string.shop_item_accessibility,
        item.title,
        unitPriceRub,
    )
    val quantityDescription = stringResource(R.string.shop_item_cart_quantity, quantity)
    val isInCart = quantity > 0
    val promotionEvent = decisionEvent?.takeIf { it.type == ShopDecisionEventType.PROMOTION }
    FinPetCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProductCardHeight)
            .shadow(
                elevation = AppTheme.elevation.low,
                shape = AppTheme.shapes.storefrontControl,
                ambientColor = AppTheme.colors.storefront.shadow,
                spotColor = AppTheme.colors.storefront.shadow,
            )
            .clickable(
                onClick = onClick,
                role = Role.Button,
            )
            .semantics {
                contentDescription = itemDescription
                stateDescription = quantityDescription
            }
            .testTag("shop_item_${item.id.value}"),
        shape = AppTheme.shapes.storefrontControl,
        containerColor = if (isInCart) {
            AppTheme.colors.storefront.selectedSurface
        } else {
            AppTheme.colors.storefront.surface
        },
        borderColor = when {
            tutorialHighlighted -> AppTheme.colors.actionPrimary
            promotionEvent != null -> AppTheme.colors.currencyAccent
            isInCart -> AppTheme.colors.actionPrimary
            else -> AppTheme.colors.storefront.outline
        },
        borderWidth = if (tutorialHighlighted) AppTheme.sizes.borderStrong * 2 else AppTheme.sizes.borderStrong,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (promotionEvent != null) {
                ShopPromotionBackground(modifier = Modifier.fillMaxSize())
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppTheme.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShopProductArtwork(
                    item = item,
                    artworkResolver = artworkResolver,
                    blendWithBackground = promotionEvent != null,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ProductTitleHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.title,
                        modifier = Modifier.fillMaxWidth(),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.storefront.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ProductDetailHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    val detail = itemDetailsResolver.details(item).firstOrNull()
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTheme.spacing.xs)
                        .height(ProductPriceHeight),
                    shape = AppTheme.shapes.badge,
                    color = AppTheme.colors.currencyContainer,
                    contentColor = AppTheme.colors.storefront.onSurface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (unitPriceRub != item.priceRub) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = item.priceRub.toString(),
                                        style = AppTheme.typography.caption,
                                        color = AppTheme.colors.textSecondary,
                                        textDecoration = TextDecoration.LineThrough,
                                        maxLines = 1,
                                    )
                                    FinPetCoinIcon(size = AppTheme.sizes.iconSmall)
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = unitPriceRub.toString(),
                                    style = AppTheme.typography.metricValue,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                                FinPetCoinIcon(size = AppTheme.sizes.iconSmall)
                            }
                        }
                    }
                }
            }
            if (isInCart) {
                ShopCartQuantityBadge(
                    quantity = quantity,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AppTheme.spacing.xs)
                        .testTag("shop_item_quantity_${item.id.value}"),
                )
            }
            if (promotionEvent != null) {
                ShopPromotionBadge(
                    event = promotionEvent,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(AppTheme.spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun ShopPromotionBadge(
    event: ShopDecisionEvent,
    modifier: Modifier = Modifier,
) {
    val percent = (event.discountRub * 100 / event.regularPriceRub).toInt()
    val region = when (event.promotionKind) {
        ShopPromotionKind.BUY_TWO_GET_ONE_FREE -> PromotionAtlasRegion.TWO_PLUS_ONE
        ShopPromotionKind.PERCENT_DISCOUNT -> when (percent) {
            30 -> PromotionAtlasRegion.DISCOUNT_30
            50 -> PromotionAtlasRegion.DISCOUNT_50
            else -> null
        }
    }
    val description = when (event.promotionKind) {
        ShopPromotionKind.BUY_TWO_GET_ONE_FREE -> stringResource(R.string.shop_promotion_badge_two_plus_one)
        ShopPromotionKind.PERCENT_DISCOUNT -> stringResource(R.string.shop_promotion_badge, percent)
    }
    if (region != null) {
        Image(
            painter = promotionPainter(region),
            contentDescription = description,
            modifier = modifier.size(PromotionBadgeSize),
            contentScale = ContentScale.Fit,
        )
        return
    }
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.badge,
        color = AppTheme.colors.storefront.primaryAction,
        contentColor = AppTheme.colors.storefront.onSurface,
        border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
    ) {
        Text(
            text = description,
            modifier = Modifier.padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
            style = AppTheme.typography.caption,
            maxLines = 1,
        )
    }
}

@Composable
private fun ShopPromotionBackground(modifier: Modifier = Modifier) {
    Image(
        painter = promotionPainter(PromotionAtlasRegion.CARD_BACKGROUND),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
    )
}

private enum class PromotionAtlasRegion(
    val leftPx: Int,
    val topPx: Int,
    val widthPx: Int,
    val heightPx: Int,
) {
    TWO_PLUS_ONE(leftPx = 8, topPx = 24, widthPx = 354, heightPx = 340),
    DISCOUNT_30(leftPx = 370, topPx = 30, widthPx = 316, heightPx = 326),
    DISCOUNT_50(leftPx = 684, topPx = 32, widthPx = 303, heightPx = 324),
    CARD_BACKGROUND(leftPx = 1_012, topPx = 48, widthPx = 480, heightPx = 276),
}

@Composable
private fun promotionPainter(region: PromotionAtlasRegion): Painter {
    val resources = LocalContext.current.resources
    val atlas = remember(resources) {
        ShopArtworkBitmapCache.load(resources, R.drawable.shop_promotion_atlas)
    }
    return remember(atlas, region) {
        BitmapPainter(
            image = atlas,
            srcOffset = androidx.compose.ui.unit.IntOffset(region.leftPx, region.topPx),
            srcSize = androidx.compose.ui.unit.IntSize(region.widthPx, region.heightPx),
        )
    }
}

@Composable
private fun ShopCartQuantityBadge(
    quantity: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(30.dp),
        shape = AppTheme.shapes.badge,
        color = AppTheme.colors.storefront.primaryAction,
        contentColor = AppTheme.colors.storefront.onSurface,
        border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = quantity.toString(),
                style = AppTheme.typography.bodyStrong,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun ShopDetailIcon(icon: ShopItemDetailIcon) {
    when (icon) {
        ShopItemDetailIcon.SATIETY -> SatietyAppleIcon()
        ShopItemDetailIcon.HAPPINESS -> Text(
            text = "♥",
            style = AppTheme.typography.label,
            color = AppTheme.colors.metricHappiness,
        )
        ShopItemDetailIcon.NONE -> Unit
    }
}

@Composable
private fun SatietyAppleIcon() {
    val bodyColor = AppTheme.colors.storefront.primaryAction
    val outlineColor = AppTheme.colors.actionPrimary
    val strokeWidth = AppTheme.sizes.borderThin

    Canvas(modifier = Modifier.size(18.dp)) {
        val body = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.3f)
            cubicTo(
                size.width * 0.34f,
                size.height * 0.18f,
                size.width * 0.12f,
                size.height * 0.34f,
                size.width * 0.16f,
                size.height * 0.58f,
            )
            cubicTo(
                size.width * 0.2f,
                size.height * 0.86f,
                size.width * 0.4f,
                size.height * 0.96f,
                size.width * 0.5f,
                size.height * 0.82f,
            )
            cubicTo(
                size.width * 0.62f,
                size.height * 0.96f,
                size.width * 0.82f,
                size.height * 0.86f,
                size.width * 0.86f,
                size.height * 0.58f,
            )
            cubicTo(
                size.width * 0.9f,
                size.height * 0.34f,
                size.width * 0.66f,
                size.height * 0.18f,
                size.width * 0.5f,
                size.height * 0.3f,
            )
            close()
        }
        val leaf = Path().apply {
            moveTo(size.width * 0.52f, size.height * 0.24f)
            cubicTo(
                size.width * 0.64f,
                size.height * 0.06f,
                size.width * 0.82f,
                size.height * 0.08f,
                size.width * 0.84f,
                size.height * 0.12f,
            )
            cubicTo(
                size.width * 0.76f,
                size.height * 0.28f,
                size.width * 0.62f,
                size.height * 0.3f,
                size.width * 0.52f,
                size.height * 0.24f,
            )
            close()
        }
        drawPath(body, color = bodyColor)
        drawPath(body, color = outlineColor, style = Stroke(strokeWidth.toPx()))
        drawLine(
            color = outlineColor,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.3f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.54f, size.height * 0.12f),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
        drawPath(leaf, color = bodyColor)
        drawPath(leaf, color = outlineColor, style = Stroke(strokeWidth.toPx()))
    }
}

@Composable
internal fun ShopProductArtwork(
    item: SellableItem,
    artworkResolver: ShopArtworkResolver,
    artworkSize: androidx.compose.ui.unit.Dp = ProductArtworkHeight,
    modifier: Modifier = Modifier.fillMaxWidth(),
    blendWithBackground: Boolean = false,
) {
    val artwork = artworkResolver.resolve(item.imageKey)
    val placeholderDescription = stringResource(R.string.shop_product_image_placeholder)
    Box(
        modifier = modifier.height(artworkSize),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(
                painter = artworkPainter(artwork),
                contentDescription = item.title,
                modifier = Modifier
                    .size(artworkSize)
                    .then(
                        if (blendWithBackground) {
                            Modifier.graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                                blendMode = BlendMode.Darken
                            }
                        } else {
                            Modifier
                        },
                    ),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = item.title.take(1),
                style = AppTheme.typography.brand,
                color = AppTheme.colors.storefront.outline,
                modifier = Modifier.semantics { contentDescription = placeholderDescription },
            )
        }
    }
}

@Composable
internal fun ShopCartIcon(
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.storefront.onPrimaryAction,
) {
    val strokeWidth = AppTheme.sizes.borderStrong
    Canvas(modifier = modifier.size(26.dp)) {
        val basket = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.36f)
            lineTo(size.width * 0.82f, size.height * 0.36f)
            lineTo(size.width * 0.72f, size.height * 0.8f)
            lineTo(size.width * 0.28f, size.height * 0.8f)
            close()
        }
        drawPath(basket, color = tint, style = Stroke(strokeWidth.toPx()))
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.36f, size.height * 0.36f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.14f),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.64f, size.height * 0.36f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.14f),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun artworkPainter(artwork: ShopArtwork): Painter = when (artwork) {
    is ShopArtwork.Resource -> painterResource(artwork.drawableRes)
    is ShopArtwork.AtlasRegion -> {
        val resources = LocalContext.current.resources
        val atlas = remember(resources, artwork.drawableRes) {
            ShopArtworkBitmapCache.load(resources, artwork.drawableRes)
        }
        remember(atlas, artwork) {
            BitmapPainter(
                image = atlas,
                srcOffset = androidx.compose.ui.unit.IntOffset(
                    x = artwork.leftPx,
                    y = artwork.topPx,
                ),
                srcSize = androidx.compose.ui.unit.IntSize(
                    width = artwork.widthPx,
                    height = artwork.heightPx,
                ),
            )
        }
    }
}

private object ShopArtworkBitmapCache {
    private val bitmaps = mutableMapOf<Int, ImageBitmap>()

    fun load(resources: Resources, drawableRes: Int): ImageBitmap = synchronized(bitmaps) {
        bitmaps.getOrPut(drawableRes) { ImageBitmap.imageResource(resources, drawableRes) }
    }
}

@Preview(name = "Product card states", widthDp = 432, heightDp = 440, showBackground = true)
@Composable
private fun ShopProductCardPreview() {
    val items = GroceryCatalog().storefront.items.take(3)
    val detailsResolver = ShopItemDetailsResolver { sellable ->
        val food = sellable as? FoodItem
        if (food == null) {
            emptyList()
        } else {
            listOf(
                github.detrig.feature.shop.api.ShopItemDetail(
                    text = "+${food.effects.satietyPercent}%",
                    icon = ShopItemDetailIcon.SATIETY,
                ),
            )
        }
    }
    val discountEvent = ShopDecisionEvent(
        eventId = "preview-discount",
        type = ShopDecisionEventType.PROMOTION,
        gamePeriod = 1,
        eventPeriod = 1,
        storeId = GroceryCatalog().storefront.storeId,
        productId = items[1].id,
        productTitle = items[1].title,
        regularPriceRub = items[1].priceRub,
        offeredPriceRub = items[1].priceRub * 70 / 100,
        promotionKind = ShopPromotionKind.PERCENT_DISCOUNT,
    )
    val bundleEvent = ShopDecisionEvent(
        eventId = "preview-bundle",
        type = ShopDecisionEventType.PROMOTION,
        gamePeriod = 1,
        eventPeriod = 1,
        storeId = GroceryCatalog().storefront.storeId,
        productId = items[2].id,
        productTitle = items[2].title,
        regularPriceRub = items[2].priceRub,
        offeredPriceRub = items[2].priceRub,
        promotionKind = ShopPromotionKind.BUY_TWO_GET_ONE_FREE,
    )
    FinPetTheme {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                Box(modifier = Modifier.weight(1f)) {
                    ShopProductCard(
                        item = items[0],
                        quantity = 0,
                        onClick = {},
                        artworkResolver = ShopArtworkResolver.Empty,
                        unitPriceRub = items[0].priceRub,
                        decisionEvent = null,
                        itemDetailsResolver = detailsResolver,
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ShopProductCard(
                        item = items[1],
                        quantity = 0,
                        onClick = {},
                        artworkResolver = ShopArtworkResolver.Empty,
                        unitPriceRub = discountEvent.offeredPriceRub,
                        decisionEvent = discountEvent,
                        itemDetailsResolver = detailsResolver,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                Box(modifier = Modifier.weight(1f)) {
                    ShopProductCard(
                        item = items[2],
                        quantity = 3,
                        onClick = {},
                        artworkResolver = ShopArtworkResolver.Empty,
                        unitPriceRub = items[2].priceRub,
                        decisionEvent = bundleEvent,
                        itemDetailsResolver = detailsResolver,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
