package github.detrig.feature.fridge.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.component.FinPetIconButton
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.fridge.FridgeFeature
import github.detrig.feature.fridge.R
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.inventory.domain.StockItem
import github.detrig.feature.shop.api.ShopArtwork
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.feature.shop.api.ShopItemDetailIcon
import github.detrig.feature.shop.presentation.ShopDetailIcon
import github.detrig.products.FoodItem
import github.detrig.products.GroceryCatalog
import github.detrig.products.ProductId
import kotlinx.coroutines.delay

private const val FLIGHT_DURATION_MILLIS = 360
private const val DEFAULT_FRIDGE_SHELF_COUNT = 5

@Composable
internal fun FridgeScreen() {
    val component = FridgeFeature.component()
    val viewModel: FridgeViewModel = viewModel { component.viewModel() }
    val state by viewModel.state().observeAsState(FridgeViewState())

    LaunchedEffect(viewModel) { viewModel.perform(FridgeViewEvent.Load) }
    BackHandler { viewModel.perform(FridgeViewEvent.Back) }

    FridgeDevice(
        state = state,
        artworkResolver = component.artworkResolver,
        onBack = { viewModel.perform(FridgeViewEvent.Back) },
        onProductClick = { viewModel.perform(FridgeViewEvent.ProductClicked(it)) },
        onAnimationFinished = {
            viewModel.perform(FridgeViewEvent.FlightAnimationFinished(it))
        },
    )
}

@Composable
private fun FridgeDevice(
    state: FridgeViewState,
    artworkResolver: ShopArtworkResolver,
    onBack: () -> Unit,
    onProductClick: (ProductId) -> Unit,
    onAnimationFinished: (ProductId) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.fridge_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val frameWidth = maxWidth
            val frameHeight = maxHeight
            val frameHorizontalInset = frameWidth * 0.012f
            val frameVerticalInset = frameHeight * 0.004f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = frameHorizontalInset,
                        vertical = frameVerticalInset,
                    ),
            ) {
                Image(
                    painter = painterResource(R.drawable.fridge_shell),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )

                FridgeContent(
                    state = state,
                    artworkResolver = artworkResolver,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = frameWidth * 0.08f,
                            top = frameHeight * 0.125f,
                            end = frameWidth * 0.08f,
                            bottom = frameHeight * 0.105f,
                        ),
                    onProductClick = onProductClick,
                    onAnimationFinished = onAnimationFinished,
                )

                FinPetIconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            end = frameWidth * 0.012f,
                            top = frameHeight * 0.012f,
                        )
                        .semantics {
                            contentDescription = "Закрыть холодильник"
                        },
                ) {
                    Image(
                        painter = painterResource(R.drawable.fridge_close),
                        contentDescription = null,
                        modifier = Modifier.size(45.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}

@Composable
private fun FridgeContent(
    state: FridgeViewState,
    artworkResolver: ShopArtworkResolver,
    modifier: Modifier = Modifier,
    onProductClick: (ProductId) -> Unit,
    onAnimationFinished: (ProductId) -> Unit,
) {
    val listState = rememberLazyListState()
    val catalog = remember { GroceryCatalog() }
    val stockById = remember(state.stock) { state.stock.associateBy { it.productId } }
    val rows = remember(state.sessionSlots) {
        val filledRows = state.sessionSlots.chunked(3)
        List(maxOf(DEFAULT_FRIDGE_SHELF_COUNT, filledRows.size)) { index ->
            filledRows.getOrNull(index) ?: emptyList<ProductId?>()
        }
    }
    val finishAnimation by rememberUpdatedState(onAnimationFinished)

    state.animatingProductIds.forEach { productId ->
        LaunchedEffect(productId) {
            delay(FLIGHT_DURATION_MILLIS.toLong())
            finishAnimation(productId)
        }
    }

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.actionPrimary)
            }

            else -> {
                // The fridge artwork has five fixed shelf levels. Keep all five
                // visible even when the inventory has only one or two products.
                val rowHeight = maxHeight / DEFAULT_FRIDGE_SHELF_COUNT
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(
                        count = rows.size,
                        key = { index -> "fridge_row_$index" },
                    ) { index ->
                        FridgeShelfRow(
                            slots = rows[index],
                            rowHeight = rowHeight,
                            stockById = stockById,
                            catalog = catalog,
                            artworkResolver = artworkResolver,
                            animatingProductIds = state.animatingProductIds,
                            onProductClick = onProductClick,
                        )
                    }
                }
                FridgeScrollbar(
                    listState = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .width(13.dp),
                )
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.actionPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FridgeShelfRow(
    slots: List<ProductId?>,
    rowHeight: Dp,
    stockById: Map<ProductId, StockItem>,
    catalog: GroceryCatalog,
    artworkResolver: ShopArtworkResolver,
    animatingProductIds: Set<ProductId>,
    onProductClick: (ProductId) -> Unit,
) {
    val shelfHeight = rowHeight * 0.13f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 20.dp, bottom = shelfHeight * 0.55f),
            verticalAlignment = Alignment.Bottom,
        ) {
            slots.forEach { productId ->
                FridgeFoodCell(
                    productId = productId,
                    stock = productId?.let(stockById::get),
                    item = productId?.let(catalog::find),
                    artworkResolver = artworkResolver,
                    isAnimating = productId in animatingProductIds,
                    onClick = { productId?.let(onProductClick) },
                    rowHeight = rowHeight,
                    shelfHeight = shelfHeight,
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(3 - slots.size) {
                Box(Modifier.weight(1f).fillMaxHeight())
            }
        }
        Image(
            painter = painterResource(R.drawable.fridge_shelf),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(shelfHeight),
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
private fun FridgeFoodCell(
    productId: ProductId?,
    stock: StockItem?,
    item: FoodItem?,
    artworkResolver: ShopArtworkResolver,
    isAnimating: Boolean,
    onClick: () -> Unit,
    rowHeight: Dp,
    shelfHeight: Dp,
    modifier: Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = tween(FLIGHT_DURATION_MILLIS),
        label = "fridge_food_flight",
    )
    val enabled = productId != null && item != null && stock != null && stock.quantity > 0 && !isAnimating

    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        if (item != null && stock != null && stock.quantity > 0) {
            val artworkSize = (rowHeight * 0.54f).coerceIn(48.dp, 78.dp)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = shelfHeight * 0.48f)
                    .graphicsLayer {
                        translationY = size.height * 0.58f * progress
                        translationX = size.width * 0.12f * progress
                        scaleX = 1f + progress * 0.18f
                        scaleY = 1f + progress * 0.18f
                        rotationZ = -7f * progress
                        alpha = 1f - progress
                    }
                    .padding(horizontal = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(artworkSize),
                    contentAlignment = Alignment.Center,
                ) {
                    FridgeProductArtwork(
                        item = item,
                        artworkResolver = artworkResolver,
                        modifier = Modifier.size(artworkSize),
                        enabled = enabled,
                        onClick = onClick,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(width = 36.dp, height = 26.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.fridge_quantity_badge),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                        Text(
                            text = "×${stock.quantity}",
                            style = AppTheme.typography.caption,
                            color = AppTheme.colors.textPrimary,
                            maxLines = 1,
                        )
                    }
                }
                Row(
                    modifier = Modifier.height(23.dp),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShopDetailIcon(ShopItemDetailIcon.SATIETY)
                    Text(
                        text = "+${item.effects.satietyPercent}%",
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.actionPrimary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun FridgeScrollbar(
    listState: LazyListState,
    modifier: Modifier,
) {
    val scrollProgress by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val visible = layoutInfo.visibleItemsInfo.size
            if (total <= visible || total == 0) {
                0f
            } else {
                (listState.firstVisibleItemIndex.toFloat() / (total - visible).toFloat())
                    .coerceIn(0f, 1f)
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.fridge_scroll_track),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        val thumbHeight = (maxHeight * 0.24f).coerceAtLeast(50.dp)
        Image(
            painter = painterResource(R.drawable.fridge_scroll_thumb),
            contentDescription = "Прокрутка холодильника",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(thumbHeight)
                .offset {
                    IntOffset(
                        x = 0,
                        y = ((maxHeight - thumbHeight) * scrollProgress).roundToPx(),
                    )
                },
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
internal fun FridgeTableContent(
    inventoryApi: InventoryApi,
    artworkResolver: ShopArtworkResolver,
    modifier: Modifier = Modifier,
) {
    val tableItems by inventoryApi.observeTable().collectAsState(emptyList())
    val catalog = remember { GroceryCatalog() }
    val products = remember(tableItems) {
        tableItems.take(3).mapNotNull { stagedItem -> catalog.find(stagedItem.productId) }
    }
    if (products.isEmpty()) return

    BoxWithConstraints(modifier = modifier) {
        val artworkSize = (maxWidth * 0.22f).coerceIn(38.dp, 54.dp)
        val horizontalSlots = listOf(0.18f, 0.50f, 0.82f)

        products.forEachIndexed { index, item ->
            FridgeProductArtwork(
                item = item,
                artworkResolver = artworkResolver,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(
                        x = maxWidth * horizontalSlots[index] - artworkSize / 2,
                    )
                    .size(artworkSize),
            )
        }
    }
}

@Composable
internal fun FridgeProductArtwork(
    item: FoodItem,
    artworkResolver: ShopArtworkResolver,
    modifier: Modifier,
    enabled: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val artwork = artworkResolver.resolve(item.imageKey)
    if (artwork == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text = item.title.take(1),
                style = AppTheme.typography.sectionTitle,
                color = AppTheme.colors.textPrimary,
            )
        }
        return
    }

    when (artwork) {
        is ShopArtwork.Resource -> Image(
            painter = painterResource(artwork.drawableRes),
            contentDescription = item.title,
            modifier = modifier.fridgeProductClickTarget(
                label = item.title,
                bitmap = null,
                enabled = enabled,
                onClick = onClick,
            ),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
        )

        is ShopArtwork.AtlasRegion -> {
            val resources = LocalContext.current.resources
            val productImage = remember(resources, artwork) {
                FridgeArtworkBitmapCache.load(resources, artwork)
            }
            Image(
                painter = BitmapPainter(image = productImage.image),
                contentDescription = item.title,
                modifier = modifier.fridgeProductClickTarget(
                    label = item.title,
                    bitmap = productImage.bitmap,
                    enabled = enabled,
                    onClick = onClick,
                ),
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
            )
        }
    }
}

private fun Modifier.fridgeProductClickTarget(
    label: String,
    bitmap: Bitmap?,
    enabled: Boolean,
    onClick: (() -> Unit)?,
): Modifier {
    val performClick = onClick ?: return this
    return pointerInput(enabled, bitmap) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown()
            if (!bitmap.isOpaqueAt(down.position, size)) return@awaitEachGesture
            val up = waitForUpOrCancellation()
            if (up != null && bitmap.isOpaqueAt(up.position, size)) {
                up.consume()
                performClick()
            }
        }
    }.semantics(mergeDescendants = true) {
        contentDescription = label
        role = Role.Button
        if (!enabled) disabled()
        onClick {
            if (enabled) {
                performClick()
                true
            } else {
                false
            }
        }
    }
}

private fun Bitmap?.isOpaqueAt(position: androidx.compose.ui.geometry.Offset, size: IntSize): Boolean {
    if (this == null) return true
    if (size.width <= 0 || size.height <= 0 ||
        position.x !in 0f..size.width.toFloat() || position.y !in 0f..size.height.toFloat()
    ) {
        return false
    }
    val x = (position.x / size.width * width).toInt().coerceIn(0, width - 1)
    val y = (position.y / size.height * height).toInt().coerceIn(0, height - 1)
    return android.graphics.Color.alpha(getPixel(x, y)) >= PRODUCT_HIT_ALPHA
}

private object FridgeArtworkBitmapCache {
    private val atlasBitmaps = mutableMapOf<Int, Bitmap>()
    private val productBitmaps = mutableMapOf<ShopArtwork.AtlasRegion, FridgeProductBitmap>()

    fun load(
        resources: android.content.res.Resources,
        artwork: ShopArtwork.AtlasRegion,
    ): FridgeProductBitmap = synchronized(this) {
        productBitmaps.getOrPut(artwork) {
            val atlas = atlasBitmaps.getOrPut(artwork.drawableRes) {
                BitmapFactory.decodeResource(resources, artwork.drawableRes)
            }
            val crop = Bitmap.createBitmap(
                atlas,
                artwork.leftPx,
                artwork.topPx,
                artwork.widthPx,
                artwork.heightPx,
            ).copy(Bitmap.Config.ARGB_8888, true)
            val contentBitmap = trimTransparentPadding(makeBackgroundTransparent(crop))
            if (contentBitmap !== crop) crop.recycle()
            FridgeProductBitmap(
                bitmap = contentBitmap,
                image = contentBitmap.asImageBitmap(),
            )
        }
    }

    private fun trimTransparentPadding(bitmap: Bitmap): Bitmap {
        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (android.graphics.Color.alpha(bitmap.getPixel(x, y)) < PRODUCT_HIT_ALPHA) continue
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }
        if (right < left || bottom < top) return bitmap

        val padding = 2
        val croppedLeft = (left - padding).coerceAtLeast(0)
        val croppedTop = (top - padding).coerceAtLeast(0)
        val croppedRight = (right + padding + 1).coerceAtMost(bitmap.width)
        val croppedBottom = (bottom + padding + 1).coerceAtMost(bitmap.height)
        return Bitmap.createBitmap(
            bitmap,
            croppedLeft,
            croppedTop,
            croppedRight - croppedLeft,
            croppedBottom - croppedTop,
        )
    }

    private fun makeBackgroundTransparent(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val background = bitmap.getPixel(0, 0)
        val visited = BooleanArray(width * height)
        val queue = IntArray(width * height)
        var head = 0
        var tail = 0

        fun isBackground(pixel: Int): Boolean {
            val redDistance = kotlin.math.abs(
                android.graphics.Color.red(pixel) - android.graphics.Color.red(background),
            )
            val greenDistance = kotlin.math.abs(
                android.graphics.Color.green(pixel) - android.graphics.Color.green(background),
            )
            val blueDistance = kotlin.math.abs(
                android.graphics.Color.blue(pixel) - android.graphics.Color.blue(background),
            )
            return redDistance <= 42 && greenDistance <= 42 && blueDistance <= 42
        }

        fun enqueue(x: Int, y: Int) {
            if (x !in 0 until width || y !in 0 until height) return
            val index = y * width + x
            if (visited[index] || !isBackground(bitmap.getPixel(x, y))) return
            visited[index] = true
            queue[tail++] = index
        }

        for (x in 0 until width) {
            enqueue(x, 0)
            enqueue(x, height - 1)
        }
        for (y in 0 until height) {
            enqueue(0, y)
            enqueue(width - 1, y)
        }

        while (head < tail) {
            val index = queue[head++]
            val x = index % width
            val y = index / width
            bitmap.setPixel(x, y, android.graphics.Color.TRANSPARENT)
            enqueue(x - 1, y)
            enqueue(x + 1, y)
            enqueue(x, y - 1)
            enqueue(x, y + 1)
        }
        return bitmap
    }
}

private data class FridgeProductBitmap(
    val bitmap: Bitmap,
    val image: androidx.compose.ui.graphics.ImageBitmap,
)

private const val PRODUCT_HIT_ALPHA = 96

@Preview(name = "Fridge", widthDp = 360, heightDp = 760)
@Composable
private fun FridgeContentPreview() {
    val catalog = remember { GroceryCatalog() }
    val previewIds = catalog.storefront.items.take(15).map { it.id }
    FinPetTheme {
        FridgeDevice(
            state = FridgeViewState(
                stock = previewIds.map { StockItem(it, 2) },
                sessionSlots = previewIds,
                loading = false,
            ),
            artworkResolver = ShopArtworkResolver.Empty,
            onBack = {},
            onProductClick = {},
            onAnimationFinished = {},
        )
    }
}
