package github.detrig.feature.room.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.designsystem.component.FinPetCard
import github.detrig.feature.room.R
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.HouseObjectPlacement
import github.detrig.feature.room.presentation.model.RoomZoneUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun RoomObjectLayers(
    zones: List<RoomZoneUiModel>,
    enabled: Boolean,
    buyingZoneId: String?,
    highlightedObjectIds: Set<String> = emptySet(),
    allowedObjectIds: Set<String> = emptySet(),
    onHighlightedObjectBoundsChanged: (Rect?) -> Unit = {},
    onObjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    placements: List<HouseObjectPlacement> = HouseLayout.objects,
    drawObjectIds: Set<String>? = null,
    exposeInteractions: Boolean = true,
    rotationByObjectId: Map<String, Float> = emptyMap(),
) {
    val resources = LocalContext.current.resources
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val click by rememberUpdatedState(onObjectClick)
    val highlightedBoundsChanged by rememberUpdatedState(onHighlightedObjectBoundsChanged)
    val canInteract by rememberUpdatedState(enabled && exposeInteractions && buyingZoneId == null)
    val orderedPlacements = remember(placements) { placements.sortedBy { it.layer } }
    val zonesById = remember(zones) { zones.associateBy { it.id } }
    val labels = mapOf(
        "phone" to stringResource(R.string.house_market),
        "bed" to stringResource(R.string.house_bed),
        "calendar" to stringResource(R.string.house_calendar),
        "piggy_bank" to stringResource(R.string.house_piggy_bank),
        "task_board" to stringResource(R.string.house_parent_help_board),
        "wardrobe" to stringResource(R.string.house_wardrobe),
        "decor_mirror" to stringResource(R.string.house_mirror),
        "fridge" to stringResource(R.string.house_food),
        "sink" to stringResource(R.string.house_dishes),
        "dining_table" to stringResource(R.string.house_feeding),
    )
    val motion = AppTheme.motion
    val touchTarget = AppTheme.sizes.preferredTouchTarget
    val priceBadgeOffset = AppTheme.spacing.xl
    val lockSize = AppTheme.sizes.iconMedium
    val priceBadgeOffsetPx = with(density) { priceBadgeOffset.toPx() }
    val lockSizePx = with(density) { lockSize.toPx() }
    val pressScale = remember { Animatable(1f) }
    var pressedId by remember { mutableStateOf<String?>(null) }
    var animationJob by remember { mutableStateOf<Job?>(null) }
    var dispatching by remember { mutableStateOf(false) }
    val pressAnimation = tween<Float>(motion.durationFastMillis, easing = motion.standardEasing)

    fun release() {
        animationJob?.cancel()
        animationJob = scope.launch {
            pressScale.animateTo(1f, pressAnimation)
            pressedId = null
            dispatching = false
        }
    }

    fun press(id: String) {
        animationJob?.cancel()
        pressedId = id
        animationJob = scope.launch {
            pressScale.snapTo(1f)
            pressScale.animateTo(PRESS_SCALE, pressAnimation)
        }
    }

    fun activate(id: String) {
        if (!canInteract || dispatching) return
        dispatching = true
        val grow = animationJob
        animationJob = scope.launch {
            // Even a fast tap completes the small object animation before navigation.
            grow?.join()
            pressScale.animateTo(1f, pressAnimation)
            pressedId = null
            if (canInteract) click(id)
            dispatching = false
        }
    }

    LaunchedEffect(canInteract) {
        if (!canInteract) {
            animationJob?.cancel()
            pressScale.snapTo(1f)
            pressedId = null
            dispatching = false
        }
    }

    BoxWithConstraints(modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val destinations = remember(orderedPlacements, widthPx, heightPx) {
            orderedPlacements.associate { placement ->
                val bounds = requireNotNull(placement.bounds)
                placement.id to Rect(
                    bounds.left * widthPx, bounds.top * heightPx,
                    bounds.right * widthPx, bounds.bottom * heightPx,
                )
            }
        }
        val sprites by RoomSpriteCache.sprites(resources).collectAsState()

        fun hitObject(position: Offset): String? {
            // A foreground decoration blocks objects behind it, but transparent gaps pass through.
            for (placement in orderedPlacements.asReversed()) {
                val sprite = sprites[placement.id] ?: continue
                val base = destinations.getValue(placement.id)
                val destination = if (pressedId == placement.id) base.scaledFromBottom(pressScale.value) else base
                if (sprite.contains(position, destination)) {
                    return placement.id.takeIf { placement.interactive }
                }
            }
            return null
        }

        Canvas(
            Modifier.fillMaxSize().pointerInput(
                sprites, enabled, buyingZoneId, allowedObjectIds, exposeInteractions,
            ) {
                if (!canInteract || sprites.isEmpty()) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (dispatching) return@awaitEachGesture
                    val id = hitObject(down.position)?.takeIf {
                        allowedObjectIds.isEmpty() || it in allowedObjectIds
                    } ?: return@awaitEachGesture
                    press(id)
                    try {
                        val up = waitForUpOrCancellation()
                        if (up != null && hitObject(up.position) == id) {
                            up.consume()
                            activate(id)
                        } else release()
                    } finally {
                        if (!dispatching && pressedId != null) release()
                    }
                }
            },
        ) {
            orderedPlacements.forEach { placement ->
                if (drawObjectIds != null && placement.id !in drawObjectIds) return@forEach
                val sprite = sprites[placement.id] ?: return@forEach
                val destination = destinations.getValue(placement.id)
                val factor = if (pressedId == placement.id) pressScale.value else 1f
                scale(factor, pivot = Offset(destination.center.x, destination.bottom)) {
                    rotate(rotationByObjectId[placement.id] ?: 0f, pivot = Offset(destination.center.x, destination.bottom)) {
                        drawImage(
                            image = sprite.image,
                            srcOffset = IntOffset(sprite.content.left, sprite.content.top),
                            srcSize = IntSize(sprite.content.width, sprite.content.height),
                            dstOffset = IntOffset(destination.left.roundToInt(), destination.top.roundToInt()),
                            dstSize = IntSize(destination.width.roundToInt(), destination.height.roundToInt()),
                            filterQuality = FilterQuality.High,
                        )
                    }
                }
            }
        }

        if (exposeInteractions) orderedPlacements.filter { it.interactive }.forEach { placement ->
            val zone = placement.zoneId?.let(zonesById::get)
            val label = zone?.let { stringResource(it.appearance.titleRes) } ?: labels[placement.id]
                ?: return@forEach
            val accessDescription = when (val access = zone?.access) {
                RoomZoneAccess.Open -> stringResource(R.string.room_open)
                is RoomZoneAccess.Unavailable -> stringResource(R.string.room_from_level, access.requiredLevel)
                is RoomZoneAccess.Buyable -> stringResource(R.string.room_buy_price, access.priceRub)
                null -> null
            }
            val destination = destinations.getValue(placement.id)
            val targetWidth = maxOf(with(density) { destination.width.toDp() }, touchTarget)
            val targetHeight = maxOf(with(density) { destination.height.toDp() }, touchTarget)
            val targetWidthPx = with(density) { targetWidth.toPx() }
            val targetHeightPx = with(density) { targetHeight.toPx() }
            val actionable = canInteract && sprites.isNotEmpty()
                && (allowedObjectIds.isEmpty() || placement.id in allowedObjectIds)
            val performClick = {
                if (actionable && !dispatching) { press(placement.id); activate(placement.id) }
            }
            Box(
                Modifier.offset {
                    IntOffset(
                        (destination.center.x - targetWidthPx / 2f).roundToInt(),
                        (destination.center.y - targetHeightPx / 2f).roundToInt(),
                    )
                }.size(targetWidth, targetHeight)
                    .testTag(if (zone != null) "room_zone_${zone.id}" else "room_${placement.id}")
                    .semantics(mergeDescendants = true) {
                        contentDescription = label
                        role = Role.Button
                        accessDescription?.let { stateDescription = it }
                        if (!actionable) disabled()
                        onClick { performClick(); actionable }
                    }
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp && event.key in listOf(Key.Enter, Key.Spacebar, Key.DirectionCenter)) {
                            performClick()
                            actionable
                        } else false
                    }
                    .then(
                        if (actionable && placement.id in allowedObjectIds) {
                            Modifier.pointerInput(placement.id, actionable) {
                                detectTapGestures { performClick() }
                            }
                        } else {
                            Modifier
                        },
                    )
                    .focusable(actionable),
            )
            if (buyingZoneId == placement.id) {
                val progressSize = AppTheme.sizes.iconMedium
                val progressPx = with(density) { progressSize.toPx() }
                CircularProgressIndicator(
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.offset {
                        IntOffset(
                            (destination.center.x - progressPx / 2f).roundToInt(),
                            (destination.bottom - progressPx).roundToInt(),
                        )
                    }.size(progressSize),
                )
            }
            if (zone?.access is RoomZoneAccess.Buyable) {
                val price = zone.access.priceRub
                val badgeWidth = maxOf(targetWidth, AppTheme.sizes.preferredTouchTarget)
                FinPetCard(
                    modifier = Modifier.offset {
                        IntOffset(
                            (destination.center.x - with(density) { badgeWidth.toPx() } / 2f).roundToInt(),
                            (destination.bottom - priceBadgeOffsetPx).roundToInt(),
                        )
                    }.width(badgeWidth),
                    shape = AppTheme.shapes.compact,
                    borderColor = AppTheme.colors.currencyAccent,
                    borderWidth = AppTheme.sizes.borderThin,
                    elevation = AppTheme.elevation.low,
                ) {
                    Text(
                        text = stringResource(R.string.room_money, price),
                        modifier = Modifier.padding(horizontal = AppTheme.spacing.xs),
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.textPrimary,
                    )
                }
                RoomZoneLock(
                    modifier = Modifier.offset {
                        IntOffset(
                            (destination.center.x - lockSizePx / 2f).roundToInt(),
                            (destination.center.y - lockSizePx / 2f).roundToInt(),
                        )
                    }.size(lockSize),
                )
            }
        }

        val highlightedBounds = orderedPlacements.mapNotNull { placement ->
            destinations[placement.id]?.takeIf {
                placement.id in highlightedObjectIds ||
                    placement.zoneId?.let(highlightedObjectIds::contains) == true
            }
        }.reduceOrNull { combined, bounds ->
            Rect(
                left = minOf(combined.left, bounds.left),
                top = minOf(combined.top, bounds.top),
                right = maxOf(combined.right, bounds.right),
                bottom = maxOf(combined.bottom, bounds.bottom),
            )
        }
        if (highlightedBounds != null) {
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            highlightedBounds.left.roundToInt(),
                            highlightedBounds.top.roundToInt(),
                        )
                    }
                    .size(
                        with(density) { highlightedBounds.width.toDp() },
                        with(density) { highlightedBounds.height.toDp() },
                    )
                    .onGloballyPositioned { coordinates ->
                        highlightedBoundsChanged(coordinates.boundsInWindow())
                    },
            )
        }

        DisposableEffect(highlightedObjectIds) {
            if (highlightedObjectIds.isEmpty()) highlightedBoundsChanged(null)
            onDispose { highlightedBoundsChanged(null) }
        }
    }
}

private fun Rect.scaledFromBottom(scale: Float) = Rect(
    center.x - width * scale / 2f,
    bottom - height * scale,
    center.x + width * scale / 2f,
    bottom,
)

private const val PRESS_SCALE = 1.045f

@Preview(name = "Предметы комнаты", widthDp = 360, heightDp = 240)
@Composable
private fun RoomObjectLayersPreview() {
    FinPetTheme {
        Box(
            Modifier.size(360.dp, 240.dp)
                .background(AppTheme.colors.house.floor),
        ) {
            RoomObjectLayers(
                zones = emptyList(),
                enabled = false,
                buyingZoneId = null,
                highlightedObjectIds = emptySet(),
                allowedObjectIds = emptySet(),
                onHighlightedObjectBoundsChanged = {},
                onObjectClick = {},
            )
        }
    }
}
