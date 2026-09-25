package github.detrig.feature.productmarket.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import github.detrig.designsystem.component.FinPetCoinText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.productmarket.R
import github.detrig.feature.productmarket.domain.*
import github.detrig.feature.productmarket.presentation.art.*
import github.detrig.products.ProductCatalog
import github.detrig.products.DefaultProductCatalog
import kotlin.math.roundToInt
import kotlin.math.sin

/** Геометрия сцены в dp; координаты мира одинаковы во всех фазах похода. */
private object SceneLayout {
    val shelfTop = 36.dp
    val shelfLabel = 30.dp
    val shelfFoot = 16.dp
    val productWidth = 48.dp
    val productHeight = 62.dp
    val actorLeft = 12.dp
    val actorBottom = 16.dp
    val counterWidth = 480.dp
    val counterHeight = 284.dp
    val counterBottom = 90.dp
}

@Composable
internal fun MarketScene(
    state: MarketViewState, configuration: MarketConfiguration, catalog: ProductCatalog,
    pickup: MarketViewCommand.Pickup?, onEvent: (MarketViewEvent) -> Unit, modifier: Modifier,
) {
    val trip = requireNotNull(state.trip)
    val colors = AppTheme.colors
    val border = AppTheme.sizes.borderStrong
    val density = LocalDensity.current
    BoxWithConstraints(modifier.clipToBounds().testTag("market_scene")) {
        val viewport = maxWidth.value.toDouble()
        LaunchedEffect(viewport) { onEvent(MarketViewEvent.Viewport(viewport)) }
        val actorHeight = if (maxHeight >= 500.dp) 166.dp else 132.dp
        val actorWidth = actorHeight * (244f / 192f)
        val shelfHeight = (maxHeight - actorHeight - SceneLayout.shelfTop - 60.dp).coerceIn(210.dp, 325.dp)
        val rowHeight = (shelfHeight - SceneLayout.shelfLabel - SceneLayout.shelfFoot) / 3
        val camera = trip.distance
        Canvas(Modifier.matchParentSize()) {
            val floorY = size.height - actorHeight.toPx() * .72f
            drawLine(colors.sceneBorder, Offset(0f, floorY), Offset(size.width, floorY), border.toPx() / 2)
            for (mark in 0..12) {
                val x = ((mark * 300.0 + 120 - camera) * density.density).toFloat()
                drawLine(colors.sceneBorder, Offset(x, floorY), Offset(x - 55.dp.toPx(), size.height), border.toPx() / 2)
            }
        }
        configuration.shelves.forEachIndexed { index, shelf ->
            val x = index * configuration.bayWidth + configuration.shelfInset - camera
            if (x + configuration.shelfWidth > 0 && x < viewport) {
                val offset = with(density) { IntOffset(x.dp.roundToPx(), SceneLayout.shelfTop.roundToPx()) }
                Box(Modifier.offset { offset }.size(configuration.shelfWidth.dp, shelfHeight)) {
                    ShelfFrame(Modifier.matchParentSize(), rowHeight.value, configuration.slotInset.toFloat())
                    Text(stringResource(shelf.department.nameRes()),
                        Modifier.align(Alignment.TopCenter).padding(top = AppTheme.spacing.xs),
                        style = AppTheme.typography.label, maxLines = 1)
                }
            }
        }
        configuration.slots.forEach { slot ->
            val x = slot.worldX - camera
            val fullVisible = x >= 0 && x + slot.width <= viewport
            if (x + slot.width > 0 && x < viewport && slot.instanceId(trip.lap) !in trip.picked) {
                val product = requireNotNull(catalog.find(slot.productId))
                val label = stringResource(R.string.market_pick, stringResource(product.kind.nameRes()), product.unitPriceRub)
                val y = SceneLayout.shelfTop + SceneLayout.shelfLabel + rowHeight * slot.row
                val offset = with(density) { IntOffset(x.dp.roundToPx(), y.roundToPx()) }
                var itemModifier = Modifier.offset { offset }.size(slot.width.dp, rowHeight)
                if (fullVisible && state.canAdvance && trip.phase == MarketPhase.WALKING) {
                    itemModifier = itemModifier.testTag("product_${slot.instanceId(trip.lap)}")
                        .clickable(role = Role.Button, onClickLabel = label) {
                            onEvent(MarketViewEvent.Pick(slot.instanceId(trip.lap)))
                        }.semantics { contentDescription = label }
                } else itemModifier = itemModifier.clearAndSetSemantics {}
                Column(itemModifier.padding(bottom = AppTheme.spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        MarketVector(productArtwork(product.kind), Modifier
                            .size(SceneLayout.productWidth, SceneLayout.productHeight))
                    }
                    Text(stringResource(R.string.market_price, product.unitPriceRub),
                        Modifier.testTag("price_${slot.instanceId(trip.lap)}"),
                        style = AppTheme.typography.caption, maxLines = 1)
                }
            }
        }

        // Касса всегда стоит в конце того же мира. Меняется только положение камеры.
        val counterX = configuration.checkoutWorldX + 36 - camera
        if (counterX < viewport && counterX + SceneLayout.counterWidth.value > 0) {
            Box(Modifier.offset { with(density) {
                IntOffset(counterX.dp.roundToPx(), (maxHeight - SceneLayout.counterBottom - SceneLayout.counterHeight).roundToPx())
            } }.size(SceneLayout.counterWidth, SceneLayout.counterHeight).testTag("market_counter")) {
                MarketVector(MarketArtwork.checkout, Modifier.fillMaxSize())
                Text(stringResource(R.string.market_cash_sign), Modifier
                    .offset(x = 88.dp, y = 8.dp), style = AppTheme.typography.sectionTitle)
            }
        }
        Box(Modifier.align(Alignment.BottomStart).padding(start = SceneLayout.actorLeft, bottom = SceneLayout.actorBottom)
            .size(actorWidth, actorHeight).testTag("market_actor")) {
            MarketVector(MarketArtwork.rooster, Modifier.fillMaxSize(), trip.activeSeconds)
            Row(Modifier.align(Alignment.TopStart)
                .offset(x = actorWidth * .59f, y = actorHeight * .56f)) {
                trip.cart.keys.take(3).forEach {
                    MarketVector(productArtwork(requireNotNull(catalog.find(it)).kind), Modifier.size(18.dp, 23.dp))
                }
            }
            val cartLabel = stringResource(R.string.market_cart_description, trip.itemCount)
            Box(Modifier.offset(x = actorWidth * .54f, y = actorHeight * .56f)
                .size(actorWidth * .39f, AppTheme.sizes.preferredTouchTarget)
                .testTag("market_cart")
                .clickable(enabled = trip.phase == MarketPhase.WALKING && !state.busy && state.error == null,
                    role = Role.Button) { onEvent(MarketViewEvent.OpenCart) }
                .semantics { contentDescription = cartLabel }, contentAlignment = Alignment.Center) {
                Text(trip.itemCount.toString(), Modifier.background(colors.surfaceBase).padding(horizontal = AppTheme.spacing.xs),
                    style = AppTheme.typography.bodyStrong)
            }
        }
        if (!trip.hintDismissed && trip.phase == MarketPhase.WALKING) {
            Text(stringResource(R.string.market_hint), Modifier
                .align(Alignment.TopCenter).offset(y = SceneLayout.shelfTop + shelfHeight + AppTheme.spacing.md)
                .background(colors.surfaceBase).padding(horizontal = AppTheme.spacing.md),
                style = AppTheme.typography.caption)
        }

        PickupAnimation(pickup, catalog, rowHeight.value,
            cartX = (SceneLayout.actorLeft + actorWidth * .74f).value,
            cartY = (maxHeight - SceneLayout.actorBottom - actorHeight * .35f).value)
        if (trip.phase == MarketPhase.CHECKOUT || trip.phase == MarketPhase.FINISHED) {
            MarketCheckout(state, catalog, onEvent, Modifier
                .padding(horizontal = AppTheme.spacing.md)
                .offset(y = AppTheme.spacing.sm).fillMaxWidth()
                .heightIn(max = (maxHeight - actorHeight - 44.dp).coerceAtLeast(260.dp)))
        }
    }
}

@Composable
private fun ShelfFrame(modifier: Modifier, rowHeight: Float, side: Float) {
    val colors = AppTheme.colors
    val border = AppTheme.sizes.borderStrong
    Canvas(modifier) {
        val line = border.toPx()
        drawRoundRect(colors.surfaceBase, Offset(line, line), Size(size.width - line * 2, size.height - line * 2),
            CornerRadius(10.dp.toPx()))
        drawRoundRect(colors.sceneBorder, Offset(line, line), Size(size.width - line * 2, size.height - line * 2),
            CornerRadius(10.dp.toPx()), style = Stroke(line))
        repeat(3) { row ->
            val y = (SceneLayout.shelfLabel.value + rowHeight * (row + 1)).dp.toPx()
            drawLine(colors.sceneBorder, Offset(side.dp.toPx(), y), Offset(size.width - side.dp.toPx(), y), line)
        }
    }
}

@Composable
private fun PickupAnimation(
    pickup: MarketViewCommand.Pickup?, catalog: ProductCatalog, rowHeight: Float, cartX: Float, cartY: Float,
) {
    val progress = remember { Animatable(1f) }
    val duration = AppTheme.motion.durationSlowMillis
    val easing = AppTheme.motion.standardEasing
    LaunchedEffect(pickup?.sequence) {
        if (pickup != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(duration, easing = easing))
        }
    }
    if (pickup != null && progress.value < 1f) {
        val p = progress.value
        val startX = (pickup.slot.worldX - pickup.camera + pickup.slot.width / 2).toFloat()
        val startY = SceneLayout.shelfTop.value + SceneLayout.shelfLabel.value +
            rowHeight * (pickup.slot.row + .5f)
        val x = startX + (cartX - startX) * p - 20
        val y = startY + (cartY - startY) * p - sin(p * Math.PI).toFloat() * 48 - 25
        MarketVector(productArtwork(requireNotNull(catalog.find(pickup.slot.productId)).kind),
            Modifier.offset(x = x.dp, y = y.dp).size(40.dp, 50.dp).clearAndSetSemantics {})
    }
}

@Preview(name = "Полки магазина", widthDp = 360, heightDp = 480)
@Composable
private fun MarketScenePreview() {
    val config = MarketConfiguration()
    FinPetTheme {
        MarketScene(
            state = MarketViewState(
                trip = MarketTrip(id = "preview", routeVersion = config.routeVersion, requested = config.requested),
                balanceRub = 500,
                loading = false,
                foreground = true,
            ),
            configuration = config,
            catalog = DefaultProductCatalog(),
            pickup = null,
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
