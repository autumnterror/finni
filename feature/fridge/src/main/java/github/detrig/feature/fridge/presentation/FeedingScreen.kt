package github.detrig.feature.fridge.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.feature.fridge.FridgeFeature
import github.detrig.feature.fridge.R
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.products.FoodItem
import github.detrig.products.GroceryCatalog
import github.detrig.products.GroceryCategoryIds
import github.detrig.products.ProductId
import kotlin.math.roundToInt
import github.detrig.feature.room.api.FirstRunOnboardingStep
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun FeedingScreen() {
    val component = FridgeFeature.component()
    val viewModel: FeedingViewModel = viewModel { component.feedingViewModel() }
    val state by viewModel.state().observeAsState(FeedingViewState())
    val petProfile by component.petApi.observeProfile().collectAsState(initial = null)
    val firstRunStep by component.roomApi.firstRunGuide.step.collectAsState()
    val soundPlayer = component.gameAudio
    val catalog = remember { GroceryCatalog() }
    val isDrink = state.activePortion?.productId?.let { catalog.find(it)?.categoryId == GroceryCategoryIds.Drinks } == true

    DisposableEffect(soundPlayer) {
        soundPlayer.preload(FeedingSound.entries.map { it.cue })
        onDispose { soundPlayer.stop("feeding") }
    }

    LaunchedEffect(viewModel) { viewModel.perform(FeedingViewEvent.Load) }
    BackHandler { viewModel.perform(FeedingViewEvent.Back) }
    LaunchedEffect(soundPlayer, state.animation, isDrink) {
        when (state.animation) {
            FeedingAnimation.MouthOpen -> soundPlayer.play(if (isDrink) FeedingSound.Drink.cue else FeedingSound.Bite.cue)
            FeedingAnimation.ChewA -> if (!isDrink) soundPlayer.play(FeedingSound.Chew.cue)
            FeedingAnimation.Idle,
            FeedingAnimation.ChewB -> Unit
        }
    }

    FeedingContent(
        state = state,
        artworkResolver = component.artworkResolver,
        onEvent = viewModel::perform,
        onPlaySound = { soundPlayer.play(it.cue) },
        roomContent = { roomModifier, roomPetContent, roomTableContent ->
            component.roomApi.Content(
                modifier = roomModifier,
                active = false,
                showHud = true,
                focusObjectId = "dining_table",
                petAnchorObjectId = "decor_chair",
                petZIndex = 1.5f,
                petBaselineFraction = 0.742f,
                petContent = roomPetContent,
                tableFoodContent = roomTableContent,
            )
        },
        petContent = { petModifier, mouthOpen, lookAt ->
            petProfile?.let { profile ->
                component.petApi.Content(
                    profile = profile,
                    modifier = petModifier,
                    animateIdle = false,
                    mouthOpen = mouthOpen,
                    lookAt = lookAt,
                )
            }
        },
    )
    val profile = petProfile
    if (profile != null && firstRunStep == FirstRunOnboardingStep.FEEDING_DONE) {
        FinPetDialogueDialog(
            speakerName = profile.name,
            cards = listOf(LocalContext.current.getString(R.string.first_run_fed_thanks)),
            portrait = { modifier -> component.petApi.Portrait(profile, modifier) },
            dismissOnBackPress = false,
            onFinished = { viewModel.perform(FeedingViewEvent.FirstRunThanksDismissed) },
        )
    }
}

@Composable
private fun FeedingContent(
    state: FeedingViewState,
    artworkResolver: ShopArtworkResolver,
    onEvent: (FeedingViewEvent) -> Unit,
    onPlaySound: (FeedingSound) -> Unit = {},
    roomContent: @Composable (
        Modifier,
        @Composable (Modifier) -> Unit,
        @Composable (Modifier) -> Unit,
    ) -> Unit,
    petContent: @Composable (Modifier, Boolean, Offset?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalog = remember { GroceryCatalog() }
    val density = LocalDensity.current
    val activeProduct = state.activePortion?.let { catalog.find(it.productId) }

    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var petCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var tableCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().onGloballyPositioned { rootCoordinates = it },
    ) {
        val foodSize = (maxWidth * FEEDING_FOOD_SIZE_FRACTION).coerceIn(46.dp, 66.dp)
        val foodSizePx = with(density) { foodSize.toPx() }
        val rootWidthPx = with(density) { maxWidth.toPx() }
        val rootHeightPx = with(density) { maxHeight.toPx() }
        val rootOrigin = rootCoordinates?.positionInRoot() ?: Offset.Zero
        val fallbackMouthCenter = rootOrigin + Offset(rootWidthPx * 0.5f, rootHeightPx * 0.50f)
        val mouthCenterGlobal = petCoordinates?.let { coordinates ->
            coordinates.localToRoot(
                Offset(coordinates.size.width * 0.5f, coordinates.size.height * 0.43f),
            )
        } ?: fallbackMouthCenter
        val mouthCenter = mouthCenterGlobal - rootOrigin
        val mouthTargetRadius = foodSizePx * 0.85f
        var dragState by remember { mutableStateOf<FeedingDrag?>(null) }
        val dragCenterGlobal = dragState?.let { drag ->
            tableCoordinates?.localToRoot(drag.center)
        }
        val petLookAt = dragState?.let { drag ->
            tableCoordinates?.let { sourceCoordinates ->
                petCoordinates?.let { coordinates ->
                    val localTarget = coordinates.localPositionOf(sourceCoordinates, drag.center)
                    val width = coordinates.size.width.toFloat()
                    val height = coordinates.size.height.toFloat()
                    if (width > 0f && height > 0f) {
                        Offset(
                            x = (localTarget.x / width).coerceIn(0f, 1f),
                            y = (localTarget.y / height).coerceIn(0f, 1f),
                        )
                    } else {
                        null
                    }
                }
            }
        } ?: Offset(0.5f, 0.43f).takeIf { state.activePortion != null }

        roomContent(
            Modifier.fillMaxSize(),
            { petModifier ->
                petContent(
                    petModifier.onGloballyPositioned { petCoordinates = it },
                    dragState != null || state.animation == FeedingAnimation.MouthOpen ||
                        state.animation == FeedingAnimation.ChewA,
                    petLookAt,
                )
            },
            { tableModifier ->
                BoxWithConstraints(
                    modifier = tableModifier.onGloballyPositioned { tableCoordinates = it },
                ) {
                    val tableWidthPx = with(density) { maxWidth.toPx() }
                    val tableHeightPx = with(density) { maxHeight.toPx() }
                    // Rest the artwork on the tabletop while keeping its count
                    // badge inside the widened table's front edge.
                    val foodCenterY = tableHeightPx - foodSizePx * 0.85f
                    val slotCenters = listOf(0.25f, 0.50f, 0.75f).map { x ->
                        Offset(tableWidthPx * x, foodCenterY)
                    }
                    val arrowSize = (maxWidth * 0.16f).coerceIn(52.dp, 60.dp)
                    val arrowSizePx = with(density) { arrowSize.toPx() }
                    val visibleStacks = state.foodStacks
                        .drop(state.page * FEEDING_PAGE_SIZE)
                        .take(FEEDING_PAGE_SIZE)

                    visibleStacks.forEachIndexed { index, stack ->
                        val food = catalog.find(stack.productId) ?: return@forEachIndexed
                        val reservedIds = state.pendingPortions.mapTo(mutableSetOf()) { it.id }
                        val visibleQuantity = stack.portionIds.count { it !in reservedIds }
                        if (visibleQuantity > 0) {
                            FeedingFoodSlot(
                                food = food,
                                quantity = visibleQuantity,
                                artworkResolver = artworkResolver,
                                size = foodSize,
                                center = slotCenters[index],
                                rootFoodSizePx = foodSizePx,
                                // Keep the pointer handler of the item being dragged alive.
                                // Disabling it as soon as dragState changes cancels the gesture.
                                enabled = dragState?.productId == null || dragState?.productId == stack.productId,
                                // Keep a multi-portion stack and its count on the table.
                                // Only the one-portion stack leaves its source slot.
                                hidden = visibleQuantity == 1 && dragState?.productId == stack.productId,
                                onDragStart = {
                                    onPlaySound(FeedingSound.Pickup)
                                    dragState = FeedingDrag(
                                        food = food,
                                        productId = stack.productId,
                                        startCenter = slotCenters[index],
                                    )
                                },
                                onDrag = { delta ->
                                    val activeDrag = dragState?.takeIf { it.productId == stack.productId }
                                    if (activeDrag != null) {
                                        dragState = activeDrag.copy(offset = activeDrag.offset + delta)
                                    }
                                },
                                onDragEnd = {
                                    val finishedDrag = dragState
                                    dragState = null
                                    val dropCenter = finishedDrag?.let { drag ->
                                        tableCoordinates?.localToRoot(drag.center)
                                    }
                                    if (finishedDrag != null && dropCenter != null &&
                                        (dropCenter - mouthCenterGlobal).getDistance() <= mouthTargetRadius
                                    ) {
                                        onEvent(FeedingViewEvent.FoodDroppedIntoMouth(finishedDrag.productId))
                                    }
                                },
                                onDragCancel = { dragState = null },
                            )
                        }
                    }

                    if (state.foodStacks.size > FEEDING_PAGE_SIZE) {
                        FeedingArrow(
                            drawable = R.drawable.feeding_arrow_left,
                            description = "Предыдущие продукты",
                            enabled = state.page > 0,
                            onClick = { onEvent(FeedingViewEvent.PreviousPage) },
                            modifier = Modifier
                                .size(arrowSize)
                                .offset {
                                    IntOffset(
                                        (arrowSizePx * 0.10f).roundToInt(),
                                        (foodCenterY - arrowSizePx / 2f).roundToInt(),
                                    )
                                },
                        )
                        FeedingArrow(
                            drawable = R.drawable.feeding_arrow_right,
                            description = "Следующие продукты",
                            enabled = state.page < state.foodStacks.lastFeedingPageIndex(),
                            onClick = { onEvent(FeedingViewEvent.NextPage) },
                            modifier = Modifier
                                .size(arrowSize)
                                .offset {
                                    IntOffset(
                                        (tableWidthPx - arrowSizePx * 1.10f).roundToInt(),
                                        (foodCenterY - arrowSizePx / 2f).roundToInt(),
                                    )
                                },
                        )
                    }

                    dragState?.let { drag ->
                        FoodArtwork(
                            food = drag.food,
                            artworkResolver = artworkResolver,
                            modifier = Modifier
                                .size(foodSize)
                                .offset {
                                    IntOffset(
                                        (drag.center.x - foodSizePx / 2f).roundToInt(),
                                        (drag.center.y - foodSizePx / 2f).roundToInt(),
                                    )
                                }
                                .graphicsLayer { scaleX = 1.08f; scaleY = 1.08f },
                        )
                    }
                }
            },
        )

        Image(
            painter = painterResource(R.drawable.feeding_close),
            contentDescription = "Закрыть кормление",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = maxWidth * 0.045f, top = maxHeight * 0.18f)
                .size((maxWidth * 0.14f).coerceAtLeast(AppTheme.sizes.preferredTouchTarget))
                .clickable(role = Role.Button) { onEvent(FeedingViewEvent.Back) },
            contentScale = ContentScale.Fit,
        )

        activeProduct?.let { food ->
            FeedingFoodAtMouth(
                food = food,
                animation = state.animation,
                artworkResolver = artworkResolver,
                foodSize = foodSize,
                foodSizePx = foodSizePx,
                mouthCenter = mouthCenter,
            )
        }

        state.message?.let { message ->
            androidx.compose.material3.Text(
                text = message,
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.statusCritical.onContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = AppTheme.spacing.xl, start = AppTheme.spacing.xl, end = AppTheme.spacing.xl),
            )
        }
    }
}

@Composable
private fun FeedingFoodSlot(
    food: FoodItem,
    quantity: Int,
    artworkResolver: ShopArtworkResolver,
    size: Dp,
    center: Offset,
    rootFoodSizePx: Float,
    enabled: Boolean,
    hidden: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .offset {
                IntOffset(
                    (center.x - rootFoodSizePx / 2f).roundToInt(),
                    (center.y - rootFoodSizePx / 2f).roundToInt(),
                )
            }
            .alpha(if (hidden) 0f else 1f)
            .pointerInput(enabled, food.id) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
            }
            .semantics { contentDescription = food.title; role = Role.Button },
    ) {
        FoodArtwork(
            food = food,
            artworkResolver = artworkResolver,
            modifier = Modifier.fillMaxSize(),
        )
        FeedingQuantityBadge(
            quantity = quantity,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = size * 0.25f)
                .width(size * 0.5f)
                .height(size * 0.28f),
        )
    }
}

@Composable
private fun FeedingQuantityBadge(
    quantity: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.feeding_quantity_badge),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        androidx.compose.material3.Text(
            text = "×$quantity",
            style = AppTheme.typography.bodyStrong,
            color = AppTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun FeedingArrow(
    drawable: Int,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(drawable),
        contentDescription = description,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun FeedingFoodAtMouth(
    food: FoodItem,
    animation: FeedingAnimation,
    artworkResolver: ShopArtworkResolver,
    foodSize: Dp,
    foodSizePx: Float,
    mouthCenter: Offset,
) {
    val alpha by animateFloatAsState(
        targetValue = if (animation == FeedingAnimation.ChewA || animation == FeedingAnimation.ChewB) 0f else 1f,
        animationSpec = tween(AppTheme.motion.durationFastMillis),
        label = "feeding_food_visibility",
    )
    FoodArtwork(
        food = food,
        artworkResolver = artworkResolver,
        modifier = Modifier
            .size(foodSize)
            .offset {
                IntOffset(
                    (mouthCenter.x - foodSizePx / 2f).roundToInt(),
                    (mouthCenter.y - foodSizePx / 2f).roundToInt(),
                )
            }
            .alpha(alpha),
    )
}

@Composable
private fun FoodArtwork(
    food: FoodItem,
    artworkResolver: ShopArtworkResolver,
    modifier: Modifier,
) {
    FridgeProductArtwork(
        item = food,
        artworkResolver = artworkResolver,
        modifier = modifier,
    )
}

private data class FeedingDrag(
    val food: FoodItem,
    val productId: ProductId,
    val startCenter: Offset,
    val offset: Offset = Offset.Zero,
) {
    val center: Offset get() = startCenter + offset
}

private const val FEEDING_FOOD_SIZE_FRACTION = 0.13f

@Preview(name = "Кормление", widthDp = 360, heightDp = 640)
@Composable
private fun FeedingContentPreview() {
    FinPetTheme {
        FeedingContent(
            state = FeedingViewState(
                foodStacks = listOf(
                    FeedingFoodStack(ProductId("store.grocery.item.apple"), listOf("apple-1", "apple-2")),
                    FeedingFoodStack(ProductId("store.grocery.item.banana"), listOf("banana-1", "banana-2", "banana-3")),
                    FeedingFoodStack(ProductId("store.grocery.item.sandwich"), listOf("sandwich-1")),
                ),
                hunger = 62,
                loading = false,
            ),
            artworkResolver = ShopArtworkResolver.Empty,
            onEvent = {},
            roomContent = { roomModifier, _, _ ->
                Image(
                    painter = painterResource(R.drawable.feeding_kitchen),
                    contentDescription = null,
                    modifier = roomModifier,
                    contentScale = ContentScale.Crop,
                )
            },
            petContent = { petModifier, _, _ ->
                Image(
                    painter = painterResource(R.drawable.feeding_hamster_idle),
                    contentDescription = "Питомец",
                    modifier = petModifier,
                    contentScale = ContentScale.Fit,
                )
            },
        )
    }
}
