package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import github.detrig.feature.room.domain.model.HousePosition
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.HouseMotionState
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
    onZoneClick: (String) -> Unit,
    onMarketClick: () -> Unit,
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
    onPreviewReady: (String) -> Unit,
    modifier: Modifier = Modifier,
    petContent: @Composable (Modifier) -> Unit = {},
) {
    val motion = rememberSaveable(saver = HouseMotionState.Saver) { HouseMotionState(initialPosition) }
    val savePosition by rememberUpdatedState(onSavePosition)
    val previewReady by rememberUpdatedState(onPreviewReady)

    BoxWithConstraints(modifier.clipToBounds().testTag("house_scene")) {
        val unitDp = maxWidth
        val sceneHeightDp = maxHeight
        val unitPx = with(LocalDensity.current) { unitDp.toPx() }
        val heightPx = with(LocalDensity.current) { sceneHeightDp.toPx() }
        // Первая отрисовка уже в сохранённой точке, без кадра с левой границей дома.
        val scroll = remember { ScrollState((motion.cameraLeftX * unitPx).roundToInt()) }
        var ready by remember { mutableStateOf(false) }

        fun save() {
            if (ready) {
                motion.cameraLeftX = HouseLayout.clampCamera(scroll.value / unitPx)
                savePosition(motion.position())
            }
        }

        LaunchedEffect(unitPx) {
            ready = false
            scroll.scrollTo((HouseLayout.clampCamera(motion.cameraLeftX) * unitPx).roundToInt())
            ready = true
            snapshotFlow { scroll.value }.collect {
                motion.cameraLeftX = HouseLayout.clampCamera(it / unitPx)
                if (scroll.isScrollInProgress) motion.hintSeen = true
            }
        }
        LaunchedEffect(active, ready) {
            if (!ready) return@LaunchedEffect
            if (!active) {
                motion.pause()
                scroll.stopScroll(MutatePriority.PreventUserInput)
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
        DisposableEffect(motion, unitPx) { onDispose { save() } }
        LaunchedEffect(previewZoneId, ready, active) {
            val id = previewZoneId ?: return@LaunchedEffect
            if (!ready || !active) return@LaunchedEffect
            val placement = HouseLayout.objects.find { it.zoneId == id }
            if (placement != null) {
                scroll.scrollTo((HouseLayout.clampCamera(placement.centerX - 0.5f) * unitPx).roundToInt())
                motion.cameraLeftX = scroll.value / unitPx
                motion.hintSeen = true
                save()
            }
            previewReady(id)
        }

        Box(
            Modifier.fillMaxSize().testTag("house_scroll")
                .horizontalScroll(scroll, enabled = active && ready),
        ) {
            Box(Modifier.width(unitDp * HouseLayout.WORLD_WIDTH).fillMaxHeight()) {
                HouseBackground(Modifier.fillMaxSize())

                RoomObjectLayers(
                    zones = zones,
                    enabled = active && ready,
                    buyingZoneId = buyingZoneId,
                    onObjectClick = { id ->
                        motion.pause()
                        save()
                        when (id) {
                            "decor_mirror" -> onMirrorClick()
                            "phone" -> onMarketClick()
                            "bed" -> onBedClick()
                            "calendar" -> onCalendarClick()
                            "piggy_bank" -> onPiggyBankClick()
                            "task_board" -> onTestsClick()
                            "wardrobe" -> onWardrobeClick()
                            "fridge" -> onFoodClick()
                            "sink" -> onDishesClick()
                            "bowls" -> onFeedingClick()
                            else -> onZoneClick(id)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Один питомец, без обработчика касаний поверх предметов.
                petContent(Modifier.offset {
                    IntOffset(
                        ((motion.petX - HouseLayout.PET_WIDTH / 2) * unitPx).roundToInt(),
                        (HouseLayout.PET_FLOOR_BASELINE * heightPx -
                            HouseLayout.PET_WIDTH * unitPx).roundToInt(),
                    )
                }.size(unitDp * HouseLayout.PET_WIDTH).zIndex(3f).graphicsLayer {
                    scaleX = if (motion.facingRight) 1f else -1f
                    val step = if (active && motion.isWalking) sin(motion.walkPhase * PI * 2).toFloat() else 0f
                    translationY = -kotlin.math.abs(step) * size.height * 0.035f
                    rotationZ = step * 2f
                }.testTag("house_pet"))
            }
        }
    }
}
