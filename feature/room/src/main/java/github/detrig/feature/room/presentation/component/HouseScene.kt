package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.room.R
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
    onSavePosition: (HousePosition) -> Unit,
    previewZoneId: String?,
    onPreviewReady: (String) -> Unit,
    modifier: Modifier = Modifier,
    petContent: @Composable (Modifier) -> Unit = {},
) {
    val motion = rememberSaveable(saver = HouseMotionState.Saver) { HouseMotionState(initialPosition) }
    val savePosition by rememberUpdatedState(onSavePosition)
    val previewReady by rememberUpdatedState(onPreviewReady)
    val zonesById = remember(zones) { zones.associateBy { it.id } }
    val marketDescription = stringResource(R.string.house_market)
    val bedDescription = stringResource(R.string.house_bed)
    val touchTarget = AppTheme.sizes.preferredTouchTarget

    BoxWithConstraints(modifier.clipToBounds().testTag("house_scene")) {
        val unitDp = maxWidth
        val unitPx = with(LocalDensity.current) { unitDp.toPx() }
        // Первая отрисовка уже в сохранённой точке, без кадра с левой границей дома.
        val scroll = remember { ScrollState((motion.cameraLeftX * unitPx).roundToInt()) }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val horizon = HouseLayout.horizon(heightPx, unitPx)
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
                    snapshotFlow { motion.needsStep }.first { it }
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

        HouseBackground(cameraLeftX = { scroll.value / unitPx }, modifier = Modifier.fillMaxSize())
        // Общая прокрутка даёт touch slop, отмену тапа при свайпе, инерцию и TalkBack scrollTo.
        Box(Modifier.fillMaxSize().testTag("house_scroll").horizontalScroll(scroll, enabled = active && ready)) {
            Box(Modifier.width(unitDp * HouseLayout.WORLD_WIDTH).fillMaxHeight()) {
                HouseLayout.objects.forEach { placement ->
                    val width = unitDp * placement.width
                    val height = unitDp * placement.height
                    val interactive = placement.zoneId != null || placement.opensMarket || placement.id == "bed"
                    val hitWidth = if (interactive) maxOf(width, touchTarget) else width
                    val hitHeight = if (interactive) maxOf(height, touchTarget) else height
                    val hitWidthPx = with(LocalDensity.current) { hitWidth.toPx() }
                    val hitHeightPx = with(LocalDensity.current) { hitHeight.toPx() }
                    val bounds = Modifier.offset {
                        IntOffset(
                            (placement.centerX * unitPx - hitWidthPx / 2).roundToInt(),
                            (horizon + placement.baseY * unitPx - placement.height * unitPx / 2 - hitHeightPx / 2).roundToInt(),
                        )
                    }.size(hitWidth, hitHeight).zIndex(placement.layer)
                    val zone = placement.zoneId?.let(zonesById::get)
                    when {
                        zone != null -> RoomZonePlace(
                            zone, placement.art, width, height,
                            isBuying = buyingZoneId == zone.id,
                            enabled = active && ready && buyingZoneId == null,
                            onClick = { motion.pause(); save(); onZoneClick(zone.id) },
                            modifier = bounds,
                        )
                        placement.opensMarket -> Box(
                            bounds.testTag("room_product_market")
                                .clickable(enabled = active && ready, role = Role.Button) {
                                    motion.pause(); save(); onMarketClick()
                                }.semantics { contentDescription = marketDescription },
                            contentAlignment = Alignment.Center,
                        ) { HouseObjectArtwork(placement.art, Modifier.size(width, height)) }
                        placement.id == "bed" -> Box(
                            bounds.testTag("room_bed")
                                .clickable(enabled = active && ready, role = Role.Button) {
                                    motion.pause(); save(); onBedClick()
                                }.semantics { contentDescription = bedDescription },
                            contentAlignment = Alignment.Center,
                        ) { HouseObjectArtwork(placement.art, Modifier.size(width, height)) }
                        else -> HouseObjectArtwork(placement.art, bounds)
                    }
                }
                // Один питомец, без обработчика касаний поверх предметов.
                petContent(Modifier.offset {
                    IntOffset(
                        ((motion.petX - HouseLayout.PET_WIDTH / 2) * unitPx).roundToInt(),
                        (horizon + (HouseLayout.PET_FLOOR_OFFSET - HouseLayout.PET_WIDTH) * unitPx).roundToInt(),
                    )
                }.size(unitDp * HouseLayout.PET_WIDTH).zIndex(3f).graphicsLayer {
                    scaleX = if (motion.facingRight) 1f else -1f
                    val step = if (active && motion.isWalking) sin(motion.walkPhase * PI * 2).toFloat() else 0f
                    translationY = -kotlin.math.abs(step) * size.height * 0.035f
                    rotationZ = step * 2f
                }.testTag("house_pet"))
            }
        }
        if (!motion.hintSeen) {
            Text(stringResource(R.string.house_swipe_hint),
                style = AppTheme.typography.caption, color = AppTheme.colors.house.outline,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(AppTheme.spacing.lg))
        }
    }
}
