package github.detrig.feature.room.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.api.RoomPetInteraction
import github.detrig.feature.room.domain.furniture.FurnitureVariant
import github.detrig.feature.room.presentation.BathStep

private data class BathFrame(val slot: String, val x: Float, val y: Float, val width: Float, val height: Float)

private data class FoamSpot(val x: Float, val y: Float, val radius: Float, val order: Int)

private val foamSpots = buildList {
    for (row in 0..10) for (column in 0..10) {
        val x = 0.07f + column * 0.086f + ((row * 3 + column) % 5 - 2) * 0.009f
        val y = 0.06f + row * 0.087f + ((row + column * 2) % 5 - 2) * 0.009f
        val horizontal = (x - 0.5f) / 0.49f
        val vertical = (y - 0.51f) / 0.52f
        if (horizontal * horizontal + vertical * vertical <= 1f) {
            add(FoamSpot(x, y, 0.035f + ((row * 7 + column * 11) % 5) * 0.007f,
                (row * 53 + column * 37) % 127))
        }
    }
}.sortedBy(FoamSpot::order)

private val overview = listOf(
    BathFrame("room_bath_tiles", 370f, 205f, 750f, 643f),
    BathFrame("room_towel_hook", 150f, 250f, 75f, 83f),
    BathFrame("room_bath_towel", 100f, 275f, 195f, 344f),
    BathFrame("room_shower_fixture", 385f, 180f, 290f, 428f),
    BathFrame("room_showerhead", 465f, 165f, 118f, 185f),
    BathFrame("room_bath_mirror", 1260f, 130f, 260f, 359f),
    BathFrame("room_bath_shelf", 690f, 414f, 300f, 80f),
    BathFrame("room_shampoo", 720f, 305f, 84f, 141f),
    BathFrame("room_soap_dish", 855f, 376f, 125f, 56f),
    BathFrame("room_soap", 872f, 353f, 100f, 61f),
    BathFrame("room_bath_vanity", 1200f, 590f, 340f, 327f),
    BathFrame("room_bath_sink", 1250f, 470f, 245f, 169f),
    BathFrame("room_bath_mat", 1060f, 892f, 440f, 142f),
    BathFrame("room_bathtub", 320f, 664f, 840f, 280f),
)

private val closeup = listOf(
    BathFrame("room_bath_tiles", 185f, 290f, 760f, 651f),
    BathFrame("room_towel_hook", 48f, 232f, 83f, 92f),
    BathFrame("room_bath_towel", 16f, 270f, 170f, 300f),
    BathFrame("room_shower_fixture", 182f, 226f, 280f, 413f),
    BathFrame("room_showerhead", 304f, 219f, 120f, 189f),
    BathFrame("room_bath_shelf", 566f, 458f, 325f, 87f),
    BathFrame("room_shampoo", 606f, 328f, 90f, 151f),
    BathFrame("room_soap_dish", 744f, 411f, 125f, 56f),
    BathFrame("room_soap", 758f, 385f, 104f, 64f),
    BathFrame("room_bath_mat", 489f, 1250f, 405f, 131f),
    BathFrame("room_bathtub", 15f, 913f, 910f, 303f),
)

/** Both views use the same equipped asset IDs and the matching bathtub front layer. */
@Composable
internal fun BathroomScene(
    closeUp: Boolean,
    equipped: Map<String, FurnitureVariant>,
    modifier: Modifier = Modifier,
    petContent: @Composable (Modifier, RoomPetInteraction) -> Unit = { _, _ -> },
    washStep: BathStep = BathStep.SOAP,
    onToolCompleted: ((BathStep) -> Unit)? = null,
    onBathtubClick: (() -> Unit)? = null,
    onSlotClick: ((String) -> Unit)? = null,
) {
    val resources = LocalResources.current
    val placements = if (closeUp) closeup else overview
    val referenceWidth = if (closeUp) 940f else 1600f
    val referenceHeight = if (closeUp) 1664f else 1050f
    val colors = AppTheme.colors.house
    val density = LocalDensity.current
    var draggedSlot by remember(washStep) { mutableStateOf<String?>(null) }
    var dragPosition by remember(washStep) { mutableStateOf<Offset?>(null) }
    var contactDistance by remember(washStep) { mutableFloatStateOf(0f) }
    BoxWithConstraints(modifier) {
        val scale = minOf(maxWidth.value / referenceWidth, maxHeight.value / referenceHeight)
        val originX = (maxWidth - (referenceWidth * scale).dp) / 2
        val originY = (maxHeight - (referenceHeight * scale).dp) / 2
        val unitPx = with(density) { scale.dp.toPx() }
        val originPx = with(density) { Offset(originX.toPx(), originY.toPx()) }
        val activeSlot = when (washStep) {
            BathStep.SOAP -> "room_soap"
            BathStep.RINSE -> "room_showerhead"
            BathStep.DRY -> "room_bath_towel"
            BathStep.CLEAN -> null
        }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(colors.hallWall)
            val floorTop = originY.toPx() + (if (closeUp) 1110f else 705f) * scale.dp.toPx()
            drawRect(colors.floor, Offset(0f, floorTop), Size(size.width, size.height - floorTop))
            drawLine(colors.skirting, Offset(0f, floorTop), Offset(size.width, floorTop), 2.dp.toPx())
        }
        placements.forEach { frame ->
            val variant = equipped[frame.slot]
            val resource = variant?.drawableId ?: bathOriginalDrawable(frame.slot)
            val onClick = onSlotClick?.let { callback -> { callback(frame.slot) } }
                ?: if (frame.slot == "room_bathtub") onBathtubClick else null
            if (frame.slot != draggedSlot) {
                Image(
                    painter = painterResource(resource),
                    contentDescription = if (onClick == null) null else bathLabel(frame.slot),
                    modifier = Modifier.bathFrame(frame, scale, originX, originY)
                        .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
                    contentScale = ContentScale.FillBounds,
                )
            }
        }
        val petFrame = if (closeUp) BathFrame("pet", 250f, 565f, 430f, 516f)
            else BathFrame("pet", 620f, 520f, 260f, 312f)
        petContent(Modifier.bathFrame(petFrame, scale, originX, originY),
            RoomPetInteraction(showShadow = false, isBathing = closeUp))
        val tub = placements.first { it.slot == "room_bathtub" }
        val selectedTub = equipped[tub.slot]
        val frontName = "bath_front_" + (selectedTub?.id ?: "room_bathtub").replace("__", "_")
        val frontId = remember(resources, frontName) {
            resources.getIdentifier(frontName, "drawable", resources.getResourcePackageName(R.drawable.bath_front_room_bathtub))
                .takeIf { it != 0 } ?: R.drawable.bath_front_room_bathtub
        }
        Image(
            painter = painterResource(frontId),
            contentDescription = null,
            modifier = Modifier.bathFrame(tub, scale, originX, originY),
            contentScale = ContentScale.FillBounds,
        )
        if (closeUp && (washStep != BathStep.SOAP || contactDistance > 0f)) {
            val effectFrame = BathFrame("effect", 255f, 555f, 430f, 540f)
            Canvas(Modifier.bathFrame(effectFrame, scale, originX, originY)) {
                when (washStep) {
                    BathStep.SOAP, BathStep.RINSE -> {
                        val coverage = if (washStep == BathStep.SOAP) {
                            (contactDistance / BathStep.SOAP.requiredContactDistance).coerceIn(0f, 1f)
                        } else {
                            (1f - contactDistance / BathStep.RINSE.requiredContactDistance).coerceIn(0f, 1f)
                        }
                        val visibleCount = (foamSpots.size * coverage).toInt().coerceIn(0, foamSpots.size)
                        repeat(visibleCount) { index ->
                            val spot = foamSpots[index]
                            val center = Offset(size.width * spot.x, size.height * spot.y)
                            val radius = size.width * spot.radius
                            drawCircle(colors.porcelain.copy(alpha = 0.82f), radius, center)
                            drawCircle(colors.fabricLight.copy(alpha = 0.85f), radius, center,
                                style = Stroke(width = 2.dp.toPx()))
                            if (index % 3 == 0) {
                                drawCircle(colors.porcelain.copy(alpha = 0.9f), radius * 0.38f,
                                    center + Offset(radius * 0.45f, -radius * 0.42f))
                            }
                        }
                    }
                    BathStep.DRY -> repeat((18 - (contactDistance / 12f).toInt()).coerceAtLeast(0)) { index ->
                        val spot = foamSpots[(index * 5) % foamSpots.size]
                        drawCircle(colors.fabricLight.copy(alpha = 0.7f), radius = 4.dp.toPx(),
                            center = Offset(size.width * spot.x, size.height * spot.y))
                    }
                    BathStep.CLEAN -> repeat(6) { index ->
                        val x = size.width * (0.14f + index % 3 * 0.34f)
                        val y = size.height * (0.22f + index / 3 * 0.57f)
                        drawCircle(colors.sunnyAccent, radius = 5.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
        }
        if (closeUp && washStep == BathStep.RINSE && draggedSlot == "room_showerhead" && dragPosition != null) {
            val head = requireNotNull(dragPosition)
            val showerFrame = closeup.first { it.slot == "room_showerhead" }
            val nozzle = head + Offset(showerFrame.width * 0.14f * unitPx,
                -showerFrame.height * 0.07f * unitPx)
            Canvas(Modifier.fillMaxSize()) {
                repeat(9) { index ->
                    val spread = index - 4
                    val start = nozzle + Offset(spread * showerFrame.width * 0.038f * unitPx,
                        (index % 3) * 3f * unitPx)
                    drawLine(colors.fabricLight.copy(alpha = 0.86f), start,
                        start + Offset(spread * 2.5f * unitPx, showerFrame.height * 0.68f * unitPx),
                        3.5f * unitPx)
                }
            }
        }
        if (draggedSlot != null && dragPosition != null) {
            val slot = requireNotNull(draggedSlot)
            val frame = closeup.first { it.slot == slot }
            val resource = equipped[slot]?.drawableId ?: bathOriginalDrawable(slot)
            Image(
                painter = painterResource(resource),
                contentDescription = "Перемещаемый предмет: ${bathLabel(slot)}",
                modifier = Modifier.offset {
                    val center = requireNotNull(dragPosition)
                    IntOffset(
                        (center.x - frame.width * unitPx / 2f).toInt(),
                        (center.y - frame.height * unitPx / 2f).toInt(),
                    )
                }.size((frame.width * scale).dp, (frame.height * scale).dp),
                contentScale = ContentScale.FillBounds,
            )
        }
        if (closeUp) {
            Canvas(Modifier.fillMaxSize().pointerInput(washStep, unitPx, originPx) {
                val tool = activeSlot?.let { slot -> closeup.first { it.slot == slot } }
                val toolBounds = tool?.toRect(unitPx, originPx)
                val petBounds = Rect(
                    originPx.x + 245f * unitPx,
                    originPx.y + 545f * unitPx,
                    originPx.x + 700f * unitPx,
                    originPx.y + 1100f * unitPx,
                )
                detectDragGestures(
                    onDragStart = { start ->
                        if (toolBounds?.contains(start) == true) {
                            draggedSlot = activeSlot
                            dragPosition = start
                        }
                    },
                    onDrag = { change, amount ->
                        if (draggedSlot != null) {
                            change.consume()
                            dragPosition = change.position
                            if (petBounds.contains(change.position)) {
                                contactDistance += amount.getDistance() / unitPx
                            }
                        }
                    },
                    onDragEnd = {
                        if (draggedSlot != null && contactDistance >= washStep.requiredContactDistance) {
                            onToolCompleted?.invoke(washStep)
                        }
                        draggedSlot = null
                        dragPosition = null
                    },
                    onDragCancel = {
                        draggedSlot = null
                        dragPosition = null
                    },
                )
            }) {}
        }
    }
}

private val BathStep.requiredContactDistance: Float get() = when (this) {
    BathStep.SOAP -> 170f
    BathStep.RINSE -> 140f
    BathStep.DRY -> 160f
    BathStep.CLEAN -> Float.POSITIVE_INFINITY
}

private fun BathFrame.toRect(unitPx: Float, origin: Offset) = Rect(
    origin.x + x * unitPx,
    origin.y + y * unitPx,
    origin.x + (x + width) * unitPx,
    origin.y + (y + height) * unitPx,
)

private fun Modifier.bathFrame(frame: BathFrame, scale: Float, originX: Dp, originY: Dp): Modifier =
    offset(x = originX + (frame.x * scale).dp, y = originY + (frame.y * scale).dp)
        .size((frame.width * scale).dp, (frame.height * scale).dp)

private fun bathLabel(slot: String): String = when (slot) {
    "room_bathtub" -> "Ванна · помыть питомца"
    else -> slot.removePrefix("room_").replace('_', ' ')
}

@DrawableRes
internal fun bathOriginalDrawable(slot: String): Int = when (slot) {
    "room_bathtub" -> R.drawable.bath_room_bathtub
    "room_showerhead" -> R.drawable.bath_room_showerhead
    "room_shower_fixture" -> R.drawable.bath_room_shower_fixture
    "room_soap" -> R.drawable.bath_room_soap
    "room_soap_dish" -> R.drawable.bath_room_soap_dish
    "room_shampoo" -> R.drawable.bath_room_shampoo
    "room_bath_towel" -> R.drawable.bath_room_bath_towel
    "room_towel_hook" -> R.drawable.bath_room_towel_hook
    "room_bath_shelf" -> R.drawable.bath_room_bath_shelf
    "room_bath_vanity" -> R.drawable.bath_room_bath_vanity
    "room_bath_sink" -> R.drawable.bath_room_bath_sink
    "room_bath_mirror" -> R.drawable.bath_room_bath_mirror
    "room_bath_mat" -> R.drawable.bath_room_bath_mat
    "room_bath_tiles" -> R.drawable.bath_room_bath_tiles
    else -> error("Unknown bathroom slot: $slot")
}

@Preview(name = "Ванная", widthDp = 390, heightDp = 700)
@Composable
private fun BathroomScenePreview() {
    FinPetTheme { BathroomScene(closeUp = false, equipped = emptyMap(), modifier = Modifier.fillMaxSize()) }
}
