package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.ScrollState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.domain.model.HousePosition
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.HouseMotionState
import github.detrig.feature.room.presentation.model.HouseObjectPlacement
import github.detrig.feature.room.presentation.model.RoomZoneUiModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(FlowPreview::class)
@Composable
internal fun HouseScene(
    zones: List<RoomZoneUiModel>,
    initialPosition: HousePosition,
    active: Boolean,
    buyingZoneId: String?,
    nightMode: Boolean = false,
    onZoneClick: (String) -> Unit,
    onPhoneClick: () -> Unit,
    onBedClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPiggyBankClick: () -> Unit,
    onTestsClick: () -> Unit,
    onWardrobeClick: () -> Unit,
    onMirrorClick: () -> Unit,
    onFoodClick: () -> Unit,
    onDishesClick: () -> Unit,
    onFeedingClick: () -> Unit,
    onSavePosition: (HousePosition) -> Unit,
    previewZoneId: String?,
    focusObjectId: String?,
    petAnchorObjectId: String?,
    petZIndex: Float,
    petBaselineFraction: Float?,
    highlightedObjectIds: Set<String>,
    allowedObjectIds: Set<String>,
    onHighlightedObjectBoundsChanged: (Rect?) -> Unit,
    onPreviewReady: (String) -> Unit,
    modifier: Modifier = Modifier,
    petContent: @Composable (Modifier) -> Unit = {},
    tableFoodContent: @Composable (Modifier) -> Unit = {},
) {
    val motion = rememberSaveable(saver = HouseMotionState.Saver) { HouseMotionState(initialPosition) }
    val appMotion = AppTheme.motion
    val savePosition by rememberUpdatedState(onSavePosition)
    val previewReady by rememberUpdatedState(onPreviewReady)
    val diningTableBounds = remember {
        requireNotNull(HouseLayout.objects.first { it.id == "dining_table" }.bounds)
    }
    val focusedPlacement = remember(focusObjectId) {
        focusObjectId?.let { id -> HouseLayout.objects.firstOrNull { it.id == id } }
    }
    val feedingScene = focusObjectId == "dining_table"
    val sceneZoom = if (feedingScene) FEEDING_SCENE_ZOOM else 1f
    val scenePivotX = focusedPlacement?.centerX?.div(HouseLayout.WORLD_WIDTH) ?: 0.5f
    val scenePivotY = focusedPlacement?.bounds?.let { (it.top + it.bottom) / 2f } ?: 0.5f
    val initialCameraLeftX = if (feedingScene) {
        focusedPlacement?.let { HouseLayout.clampCamera(it.centerX - 0.5f) } ?: motion.cameraLeftX
    } else {
        motion.cameraLeftX
    }
    val feedingRoomObjects = remember {
        val chairShift = FEEDING_CHAIR_CENTER_X - requireNotNull(
            HouseLayout.objects.first { it.id == "decor_chair" }.bounds,
        ).centerX * HOUSE_REFERENCE_WIDTH
        HouseLayout.objects.map { placement ->
            val bounds = requireNotNull(placement.bounds)
            when (placement.id) {
                "decor_chair" -> {
                    val shift = chairShift / HOUSE_REFERENCE_WIDTH
                    placement.copy(
                        centerX = placement.centerX + shift * HouseLayout.WORLD_WIDTH,
                        bounds = bounds.copy(
                            left = bounds.left + shift,
                            right = bounds.right + shift,
                            top = bounds.top - FEEDING_CHAIR_BACK_SHIFT,
                            bottom = bounds.bottom - FEEDING_CHAIR_BACK_SHIFT,
                        ),
                    )
                }
                "dining_table" -> {
                    val extraWidth = bounds.width * (FEEDING_TABLE_WIDTH_SCALE - 1f) / 2f
                    placement.copy(
                        width = placement.width * FEEDING_TABLE_WIDTH_SCALE,
                        height = placement.height * FEEDING_TABLE_HEIGHT_SCALE,
                        bounds = bounds.copy(
                            left = bounds.left - extraWidth,
                            right = bounds.right + extraWidth,
                            top = bounds.bottom - bounds.height * FEEDING_TABLE_HEIGHT_SCALE,
                        ),
                    )
                }
                else -> placement
            }
        }
    }
    val feedingTableBounds = remember(feedingRoomObjects) {
        requireNotNull(feedingRoomObjects.first { it.id == "dining_table" }.bounds)
    }
    val petAnchorX = remember(petAnchorObjectId, feedingScene) {
        petAnchorObjectId?.let { anchorId ->
            val placements = if (feedingScene) feedingRoomObjects else HouseLayout.objects
            placements.firstOrNull { it.id == anchorId }?.centerX
        }
    }

    BoxWithConstraints(modifier.clipToBounds().testTag("house_scene")) {
        val unitDp = maxWidth
        val sceneHeightDp = maxHeight
        val unitPx = with(LocalDensity.current) { unitDp.toPx() }
        val heightPx = with(LocalDensity.current) { sceneHeightDp.toPx() }
        // Первая отрисовка уже в сохранённой точке, без кадра с левой границей дома.
        val scroll = remember(unitPx, focusObjectId, sceneZoom) {
            ScrollState((initialCameraLeftX * unitPx).roundToInt())
        }
        var ready by remember { mutableStateOf(false) }

        fun save() {
            // A temporary focus (for example, the feeding scene) must not rewrite
            // the player's last room camera position.
            if (ready && focusObjectId == null) {
                motion.cameraLeftX = HouseLayout.clampCamera(scroll.value / unitPx)
                savePosition(motion.position())
            }
        }

        LaunchedEffect(unitPx, focusObjectId, sceneZoom) {
            ready = false
            scroll.scrollTo((initialCameraLeftX * unitPx).roundToInt())
            ready = true
            snapshotFlow { scroll.value }.collect {
                if (!feedingScene) {
                    motion.cameraLeftX = HouseLayout.clampCamera(it / unitPx)
                    if (scroll.isScrollInProgress) motion.hintSeen = true
                }
            }
        }
        LaunchedEffect(active, ready) {
            if (!ready) return@LaunchedEffect
            if (!active) {
                motion.pause()
                save()
                return@LaunchedEffect
            }
            try {
                while (isActive) {
                    snapshotFlow<Boolean> { motion.needsStep }.first { it }
                    var previous = withFrameNanos { it }
                    while (motion.needsStep && isActive) {
                        val now = withFrameNanos { it }
                        motion.step((now - previous) / 1_000_000_000f)
                        previous = now
                    }
                    motion.pause()
                }
            } finally {
                motion.pause()
                save()
            }
        }
        LaunchedEffect(ready) {
            if (!ready) return@LaunchedEffect
            snapshotFlow { !scroll.isScrollInProgress && !motion.needsStep }
                .debounce(HouseLayout.SAVE_DELAY_MILLIS).filter { it }.collect { save() }
        }
        DisposableEffect(motion, unitPx, focusObjectId) { onDispose { save() } }
        LaunchedEffect(previewZoneId, focusObjectId, ready) {
            val id = focusObjectId ?: previewZoneId ?: return@LaunchedEffect
            if (!ready) return@LaunchedEffect
            val placement = HouseLayout.objects.find { it.id == id || it.zoneId == id }
            if (placement != null) {
                val target = (HouseLayout.clampCamera(placement.centerX - 0.5f) * unitPx).roundToInt()
                scroll.stopScroll()
                if (focusObjectId != null) {
                    scroll.animateScrollTo(
                        value = target,
                        animationSpec = tween(
                            durationMillis = appMotion.durationSlowMillis,
                            easing = appMotion.standardEasing,
                        ),
                    )
                } else {
                    scroll.scrollTo(target)
                }
                if (!feedingScene) {
                    motion.cameraLeftX = scroll.value / unitPx
                    motion.hintSeen = true
                    save()
                }
            }
            previewReady(id)
        }

        Box(
            Modifier.fillMaxSize().testTag("house_scroll")
                .horizontalScroll(scroll, enabled = active && ready),
        ) {
            Box(Modifier.width(unitDp * HouseLayout.WORLD_WIDTH * sceneZoom).fillMaxHeight()) {
                Box(
                    Modifier.width(unitDp * HouseLayout.WORLD_WIDTH).fillMaxHeight().then(
                        if (feedingScene) Modifier.graphicsLayer {
                            scaleX = sceneZoom
                            scaleY = sceneZoom
                            transformOrigin = TransformOrigin(scenePivotX, scenePivotY)
                        } else Modifier,
                    ),
                ) {
                    HouseBackground(Modifier.fillMaxSize())
                    val tableBounds = if (feedingScene) feedingTableBounds else diningTableBounds
                    val tableFoodTopFraction = tableBounds.top - if (feedingScene) {
                        0f
                    } else {
                        ROOM_TABLE_FOOD_RAISE_FRACTION
                    }
                    val tableFoodModifier = Modifier
                        .offset {
                            IntOffset(
                                (tableBounds.left * HouseLayout.WORLD_WIDTH * unitPx).roundToInt(),
                                (tableFoodTopFraction * heightPx).roundToInt(),
                            )
                        }
                        .size(
                            unitDp * tableBounds.width * HouseLayout.WORLD_WIDTH,
                            sceneHeightDp * tableBounds.height *
                                if (feedingScene) FEEDING_TABLETOP_DEPTH_FRACTION else ROOM_TABLETOP_DEPTH_FRACTION,
                        )
                        .zIndex(if (feedingScene) 3f else ROOM_TABLE_FOOD_Z_INDEX)
                    if (feedingScene) {
                        // Only the feeding view separates the table from the room
                        // sprites, allowing the seated pet to sit behind its edge.
                        RoomObjectLayers(
                            zones = zones,
                            enabled = false,
                            buyingZoneId = null,
                            onObjectClick = {},
                            modifier = Modifier.fillMaxSize(),
                            placements = feedingRoomObjects,
                            drawObjectIds = ROOM_OBJECTS_BEHIND_PET,
                            exposeInteractions = false,
                            nightMode = nightMode,
                            rotationByObjectId = mapOf("decor_chair" to FEEDING_CHAIR_ROTATION),
                        )

                        petContent(
                            petModifier(
                                unitDp, unitPx, heightPx, petAnchorX, petBaselineFraction ?: 0.742f,
                                petZIndex, motion, active,
                            ),
                        )

                        RoomObjectLayers(
                            zones = zones,
                            enabled = false,
                            buyingZoneId = null,
                            onObjectClick = {},
                            modifier = Modifier.fillMaxSize().zIndex(2f),
                            placements = feedingRoomObjects,
                            drawObjectIds = DINING_TABLE_OBJECT,
                            exposeInteractions = false,
                            nightMode = nightMode,
                        )

                        tableFoodContent(tableFoodModifier)
                    } else {
                        // Keep the everyday room's original single interactive
                        // furniture layer and pet positioning untouched.
                        RoomObjectLayers(
                            zones = zones,
                            enabled = active && ready,
                            buyingZoneId = buyingZoneId,
                            highlightedObjectIds = highlightedObjectIds,
                            allowedObjectIds = allowedObjectIds,
                            onHighlightedObjectBoundsChanged = onHighlightedObjectBoundsChanged,
                            onObjectClick = { id ->
                                motion.pause()
                                save()
                                when (id) {
                                    "decor_mirror" -> onMirrorClick()
                                    "phone" -> onPhoneClick()
                                    "fridge" -> onFoodClick()
                                    "dining_table" -> onFeedingClick()
                                    "bed" -> onBedClick()
                                    "calendar" -> onCalendarClick()
                                    "piggy_bank" -> onPiggyBankClick()
                                    "task_board" -> onTestsClick()
                                    "wardrobe" -> onWardrobeClick()
                                    "sink" -> onDishesClick()
                                    else -> onZoneClick(id)
                                }
                            },
                            nightMode = nightMode,
                            modifier = Modifier.fillMaxSize(),
                        )
                        petContent(
                            petModifier(
                                unitDp, unitPx, heightPx, null, HouseLayout.PET_FLOOR_BASELINE,
                                petZIndex, motion, active,
                            ),
                        )
                        // The groceries rest on the tabletop behind a pet walking
                        // in front of it; the focused feeding view keeps its own layers.
                        tableFoodContent(tableFoodModifier)
                    }
                }
            }
        }
    }
}

private const val FEEDING_TABLETOP_DEPTH_FRACTION = 0.42f
private const val ROOM_TABLETOP_DEPTH_FRACTION = 0.34f
private const val ROOM_TABLE_FOOD_Z_INDEX = 2f
private const val ROOM_TABLE_FOOD_RAISE_FRACTION = 0.02f
private const val FEEDING_SCENE_ZOOM = 1.32f
private const val HOUSE_REFERENCE_WIDTH = 2048f
private const val FEEDING_CHAIR_CENTER_X = 1877.5f
private const val FEEDING_CHAIR_BACK_SHIFT = 0.018f
private const val FEEDING_CHAIR_ROTATION = -6f
private const val FEEDING_TABLE_WIDTH_SCALE = 1.24f
private const val FEEDING_TABLE_HEIGHT_SCALE = 1.12f
private val DINING_TABLE_OBJECT = setOf("dining_table")
private val ROOM_OBJECTS_BEHIND_PET = HouseLayout.objects
    .asSequence()
    .map { it.id }
    .filterNot { it == "dining_table" }
    .toSet()

private fun petModifier(
    unitDp: androidx.compose.ui.unit.Dp,
    unitPx: Float,
    heightPx: Float,
    anchorX: Float?,
    baseline: Float,
    zIndex: Float,
    motion: HouseMotionState,
    active: Boolean,
): Modifier = Modifier.offset {
    val petX = anchorX ?: motion.petX
    IntOffset(
        ((petX - HouseLayout.PET_WIDTH / 2) * unitPx).roundToInt(),
        (baseline * heightPx - HouseLayout.PET_WIDTH * unitPx).roundToInt(),
    )
}.size(unitDp * HouseLayout.PET_WIDTH).zIndex(zIndex).graphicsLayer {
    scaleX = if (motion.facingRight) 1f else -1f
    val step = if (active && motion.isWalking) sin(motion.walkPhase * PI * 2).toFloat() else 0f
    translationY = -kotlin.math.abs(step) * size.height * 0.035f
    rotationZ = step * 2f
}.testTag("house_pet")

@Preview(name = "Feeding furniture", widthDp = 360, heightDp = 640)
@Composable
private fun FeedingFurniturePreview() {
    FinPetTheme {
        HouseScene(
            zones = emptyList(),
            initialPosition = HouseLayout.initialPosition(),
            active = false,
            buyingZoneId = null,
            onZoneClick = {},
            onPhoneClick = {},
            onBedClick = {},
            onCalendarClick = {},
            onPiggyBankClick = {},
            onTestsClick = {},
            onWardrobeClick = {},
            onMirrorClick = {},
            onFoodClick = {},
            onDishesClick = {},
            onFeedingClick = {},
            onSavePosition = {},
            previewZoneId = null,
            focusObjectId = "dining_table",
            petAnchorObjectId = "decor_chair",
            petZIndex = 1.5f,
            petBaselineFraction = 0.742f,
            highlightedObjectIds = emptySet(),
            allowedObjectIds = emptySet(),
            onHighlightedObjectBoundsChanged = {},
            onPreviewReady = {},
            petContent = { petModifier ->
                Box(petModifier.background(AppTheme.colors.house.blush))
            },
        )
    }
}
