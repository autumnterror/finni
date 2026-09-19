package github.detrig.feature.room.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
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
import github.detrig.feature.room.R
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.RoomZoneUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun RoomObjectLayers(
    zones: List<RoomZoneUiModel>,
    enabled: Boolean,
    buyingZoneId: String?,
    onObjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val click by rememberUpdatedState(onObjectClick)
    val canInteract by rememberUpdatedState(enabled && buyingZoneId == null)
    val placements = remember { HouseLayout.objects.sortedBy { it.layer } }
    val zonesById = remember(zones) { zones.associateBy { it.id } }
    val labels = mapOf(
        "phone" to stringResource(R.string.house_market),
        "bed" to stringResource(R.string.house_bed),
        "calendar" to stringResource(R.string.house_calendar),
        "piggy_bank" to stringResource(R.string.house_piggy_bank),
        "task_board" to stringResource(R.string.house_parent_help_board),
        "wardrobe" to stringResource(R.string.house_wardrobe),
        "fridge" to stringResource(R.string.house_food),
        "sink" to stringResource(R.string.house_dishes),
        "bowls" to stringResource(R.string.house_feeding),
    )
    val motion = AppTheme.motion
    val touchTarget = AppTheme.sizes.preferredTouchTarget
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
        val destinations = remember(widthPx, heightPx) {
            placements.associate { placement ->
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
            for (placement in placements.asReversed()) {
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
            Modifier.fillMaxSize().pointerInput(sprites, enabled, buyingZoneId) {
                if (!canInteract || sprites.isEmpty()) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (dispatching) return@awaitEachGesture
                    val id = hitObject(down.position) ?: return@awaitEachGesture
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
            placements.forEach { placement ->
                val sprite = sprites[placement.id] ?: return@forEach
                val destination = destinations.getValue(placement.id)
                val factor = if (pressedId == placement.id) pressScale.value else 1f
                scale(factor, pivot = Offset(destination.center.x, destination.bottom)) {
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

        placements.filter { it.interactive }.forEach { placement ->
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
                onObjectClick = {},
            )
        }
    }
}
