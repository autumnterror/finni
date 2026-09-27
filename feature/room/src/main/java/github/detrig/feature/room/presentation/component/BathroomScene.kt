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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
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
import github.detrig.feature.room.presentation.model.HouseSurfaceLayout
import kotlin.math.pow
import kotlin.math.sin

private data class BathFrame(val slot: String, val x: Float, val y: Float, val width: Float, val height: Float)
private const val CLOSEUP_PET_DROP = 170f

private data class FoamSpot(val position: Offset, val radius: Float)
private data class DryZone(val position: Offset, val radius: Float)
private val dryZones = listOf(
    DryZone(Offset(345f, 655f), 145f),
    DryZone(Offset(585f, 655f), 145f),
    DryZone(Offset(345f, 835f), 150f),
    DryZone(Offset(585f, 835f), 150f),
    DryZone(Offset(360f, 1005f), 140f),
    DryZone(Offset(570f, 1005f), 140f),
)
private data class WetPatch(
    val position: Offset,
    val width: Float,
    val height: Float,
    val rotation: Float,
)
private val wetPatches = listOf(
    WetPatch(Offset(354f, 646f), 24f, 11f, -18f),
    WetPatch(Offset(588f, 674f), 33f, 12f, 24f),
    WetPatch(Offset(434f, 761f), 26f, 9f, -12f),
    WetPatch(Offset(310f, 838f), 30f, 13f, 18f),
    WetPatch(Offset(571f, 881f), 34f, 15f, -27f),
    WetPatch(Offset(396f, 946f), 38f, 14f, 11f),
    WetPatch(Offset(516f, 1010f), 26f, 10f, -14f),
)
private val bathTools = setOf("room_soap", "room_showerhead", "room_bath_dryer")

/** Coordinates match the neighbouring 530-unit bedroom and kitchen furniture scale. */
private val compactOverview = listOf(
    BathFrame("room_bath_tiles", 116f, 113f, 270f, 390f),
    BathFrame("room_towel_hook", 46f, 205f, 25f, 27f),
    BathFrame("room_bath_dryer", 27f, 221f, 78f, 80f),
    BathFrame("room_shower_fixture", 122f, 247f, 96f, 142f),
    BathFrame("room_showerhead", 144f, 243f, 39f, 61f),
    BathFrame("room_bath_mirror", 415f, 140f, 86f, 119f),
    BathFrame("room_bath_shelf", 225f, 280f, 100f, 27f),
    BathFrame("room_shampoo", 235f, 248f, 28f, 47f),
    BathFrame("room_soap_dish", 280f, 279f, 42f, 19f),
    BathFrame("room_soap", 286f, 266f, 33f, 20f),
    BathFrame("room_bath_vanity", 395f, 420f, 113f, 108f),
    BathFrame("room_bath_sink", 411f, 389f, 81f, 56f),
    BathFrame("room_bath_mat", 349f, 531f, 146f, 47f),
    BathFrame("room_bathtub", 103f, 448f, 278f, 93f),
)

private val overview = listOf(
    BathFrame("room_bath_tiles", 370f, 205f, 750f, 643f),
    BathFrame("room_towel_hook", 150f, 250f, 75f, 83f),
    BathFrame("room_bath_dryer", 100f, 275f, 235f, 240f),
    BathFrame("room_shower_fixture", 385f, 180f, 290f, 428f),
    BathFrame("room_showerhead", 465f, 165f, 118f, 185f),
    BathFrame("room_bath_mirror", 1260f, 130f, 260f, 359f),
    BathFrame("room_bath_shelf", 690f, 389f, 300f, 80f),
    BathFrame("room_shampoo", 720f, 305f, 84f, 141f),
    BathFrame("room_soap_dish", 855f, 376f, 125f, 56f),
    BathFrame("room_soap", 872f, 353f, 100f, 61f),
    BathFrame("room_bath_vanity", 1200f, 590f, 340f, 327f),
    BathFrame("room_bath_sink", 1250f, 490f, 245f, 169f),
    BathFrame("room_bath_mat", 1060f, 892f, 440f, 142f),
    BathFrame("room_bathtub", 320f, 664f, 840f, 280f),
)

private val closeup = listOf(
    BathFrame("room_bath_tiles", 185f, 145f, 760f, 840f + CLOSEUP_PET_DROP),
    BathFrame("room_towel_hook", 48f, 232f, 83f, 92f),
    BathFrame("room_bath_dryer", 10f, 270f, 150f, 155f),
    BathFrame("room_shower_fixture", 182f, 226f, 280f, 413f),
    BathFrame("room_showerhead", 245f, 203f, 120f, 189f),
    BathFrame("room_bath_shelf", 566f, 405f, 325f, 87f),
    BathFrame("room_shampoo", 606f, 303f, 90f, 151f),
    BathFrame("room_soap_dish", 744f, 411f, 125f, 56f),
    BathFrame("room_soap", 758f, 385f, 104f, 64f),
    BathFrame("room_bath_mat", 489f, 1250f + CLOSEUP_PET_DROP, 405f, 131f),
    BathFrame("room_bathtub", 15f, 913f + CLOSEUP_PET_DROP, 910f, 303f),
)

/** Both views use the same equipped asset IDs and the matching bathtub front layer. */
@Composable
internal fun BathroomScene(
    closeUp: Boolean,
    equipped: Map<String, FurnitureVariant>,
    modifier: Modifier = Modifier,
    compactRoom: Boolean = false,
    petContent: @Composable (Modifier, RoomPetInteraction) -> Unit = { _, _ -> },
    washStep: BathStep = BathStep.SOAP,
    onToolCompleted: ((BathStep) -> Unit)? = null,
    onToolSoundChanged: (BathStep, Boolean) -> Unit = { _, _ -> },
    onDryerRunningChanged: (Boolean) -> Unit = {},
    onBathtubClick: (() -> Unit)? = null,
    onSlotClick: ((String) -> Unit)? = null,
) {
    val resources = LocalResources.current
    val placements = when {
        closeUp -> closeup
        compactRoom -> compactOverview
        else -> overview
    }
    val referenceWidth = if (closeUp) 940f else if (compactRoom) 530f else 1600f
    val referenceHeight = if (closeUp) 1664f else if (compactRoom) 685f else 1050f
    val colors = AppTheme.colors.house
    val density = LocalDensity.current
    var draggedSlot by remember(washStep) { mutableStateOf<String?>(null) }
    var dragPosition by remember(washStep) { mutableStateOf<Offset?>(null) }
    var contactDistance by remember(washStep) { mutableFloatStateOf(0f) }
    val foamSpots = remember { mutableStateListOf<FoamSpot>() }
    var foamPeakCount by remember { mutableIntStateOf(0) }
    var waterPhase by remember { mutableFloatStateOf(0f) }
    var dryProgress by remember(washStep) { mutableFloatStateOf(0f) }
    var dryerOverPet by remember(washStep) { mutableStateOf(false) }
    var soapOverPet by remember(washStep) { mutableStateOf(false) }
    var soapTouchedThisDrag by remember { mutableStateOf(false) }
    var dryerPhase by remember { mutableFloatStateOf(0f) }
    DisposableEffect(closeUp) {
        onDispose {
            if (closeUp) {
                onToolSoundChanged(BathStep.SOAP, false)
                onToolSoundChanged(BathStep.RINSE, false)
                onDryerRunningChanged(false)
            }
        }
    }
    LaunchedEffect(washStep) {
        if (washStep == BathStep.SOAP) {
            foamSpots.clear()
            foamPeakCount = 0
        }
    }
    LaunchedEffect(closeUp, washStep, draggedSlot) {
        if (closeUp && draggedSlot == "room_showerhead") {
            val start = withFrameNanos { it }
            while (true) {
                withFrameNanos { waterPhase = ((it - start) / 920_000_000f) % 1f }
            }
        } else {
            waterPhase = 0f
        }
    }
    LaunchedEffect(closeUp, draggedSlot) {
        if (closeUp && draggedSlot == "room_bath_dryer") {
            var lastFrame = withFrameNanos { it }
            while (true) {
                val frame = withFrameNanos { it }
                val elapsed = (frame - lastFrame).coerceAtLeast(0L)
                lastFrame = frame
                dryerPhase = (dryerPhase + elapsed / 500_000_000f) % 1f
            }
        }
    }
    LaunchedEffect(closeUp, washStep, draggedSlot, dryerOverPet) {
        if (closeUp && washStep == BathStep.DRY &&
            draggedSlot == "room_bath_dryer" && dryerOverPet) {
            var lastFrame = withFrameNanos { it }
            while (true) {
                val frame = withFrameNanos { it }
                val elapsed = (frame - lastFrame).coerceAtLeast(0L)
                lastFrame = frame
                dryProgress = (dryProgress + elapsed / 4_500_000_000f).coerceAtMost(1f)
            }
        }
    }
    val wetness = when (washStep) {
        BathStep.SOAP -> 0f
        BathStep.RINSE -> if (foamPeakCount == 0) 0f else
            (1f - foamSpots.size.toFloat() / foamPeakCount).coerceIn(0f, 1f)
        BathStep.DRY -> 1f - dryProgress
        BathStep.CLEAN -> 0f
    }
    BoxWithConstraints(modifier) {
        val scale = minOf(maxWidth.value / referenceWidth, maxHeight.value / referenceHeight)
        val originX = (maxWidth - (referenceWidth * scale).dp) / 2
        val originY = (maxHeight - (referenceHeight * scale).dp) / 2
        val unitPx = with(density) { scale.dp.toPx() }
        val originPx = with(density) { Offset(originX.toPx(), originY.toPx()) }
        var dragStartLocal by remember { mutableStateOf(Offset.Zero) }
        var lastToolCenter by remember { mutableStateOf<Offset?>(null) }
        fun onPet(point: Offset): Boolean {
            val horizontal = (point.x - 470f) / 270f
            val vertical = (point.y - 790f - CLOSEUP_PET_DROP) / 350f
            return horizontal * horizontal + vertical * vertical <= 1f
        }
        fun reference(point: Offset) = (point - originPx) / unitPx
        fun traceTool(slot: String, previous: Offset, current: Offset) {
            if (slot != "room_soap" && slot != "room_showerhead") return
            if (slot == "room_showerhead" && washStep != BathStep.RINSE) return
            val from = reference(previous)
            val to = reference(current)
            val distance = (to - from).getDistance()
            val samples = (distance / 16f).toInt().coerceIn(1, 12)
            repeat(samples + 1) { index ->
                val point = from + (to - from) * (index.toFloat() / samples)
                val contact = if (slot == "room_showerhead") point + Offset(10f, 45f) else point
                if (!onPet(contact)) return@repeat
                if (slot == "room_soap") soapTouchedThisDrag = true
                contactDistance += distance / (samples + 1)
                when (slot) {
                    "room_soap" -> if (foamSpots.size < 240 && foamSpots.none {
                        (it.position - contact).getDistance() < 30f
                    }) {
                        val bubbles = listOf(
                            Offset.Zero to 50f,
                            Offset(-43f, -28f) to 42f,
                            Offset(40f, -23f) to 54f,
                            Offset(-35f, 36f) to 60f,
                            Offset(38f, 40f) to 46f,
                            Offset(4f, 65f) to 39f,
                        )
                        bubbles.forEach { (offset, radius) ->
                                if (onPet(contact + offset)) {
                                    foamSpots.add(FoamSpot(contact + offset, radius))
                                }
                            }
                        foamPeakCount = maxOf(foamPeakCount, foamSpots.size)
                    }
                    "room_showerhead" -> foamSpots.removeAll {
                        (it.position - contact).getDistance() < 125f + it.radius * 0.25f
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(colors.hallWall)
            val floorLine = if (closeUp) 1110f + CLOSEUP_PET_DROP else if (compactRoom) {
                HouseSurfaceLayout.WALL_HEIGHT.toFloat()
            } else 705f
            val floorTop = originY.toPx() + floorLine * scale.dp.toPx()
            drawRect(colors.floor, Offset(0f, floorTop), Size(size.width, size.height - floorTop))
            drawLine(colors.skirting, Offset(0f, floorTop), Offset(size.width, floorTop), 2.dp.toPx())
        }
        placements.forEach { frame ->
            val variant = equipped[frame.slot]
            val resource = variant?.drawableId ?: bathOriginalDrawable(frame.slot)
            val onClick = onSlotClick?.let { callback -> { callback(frame.slot) } }
                ?: if (frame.slot == "room_bathtub") onBathtubClick else null
            val draggable = closeUp && frame.slot in bathTools
            val toolModifier = if (draggable) Modifier.pointerInput(frame.slot, washStep, unitPx, originPx) {
                val sourceCenter = originPx + Offset(
                    (frame.x + frame.width / 2f) * unitPx,
                    (frame.y + frame.height / 2f) * unitPx,
                )
                detectDragGestures(
                    onDragStart = { start ->
                        dragStartLocal = start
                        soapTouchedThisDrag = false
                        if (frame.slot == "room_soap" &&
                            (washStep == BathStep.DRY || washStep == BathStep.CLEAN)) {
                            foamSpots.clear()
                            foamPeakCount = 0
                            contactDistance = 0f
                        }
                        draggedSlot = frame.slot
                        dragPosition = sourceCenter
                        lastToolCenter = sourceCenter
                        soapOverPet = false
                        when (frame.slot) {
                            "room_showerhead" -> onToolSoundChanged(BathStep.RINSE, true)
                            "room_bath_dryer" -> onDryerRunningChanged(true)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = sourceCenter + change.position - dragStartLocal
                        lastToolCenter?.let { traceTool(frame.slot, it, center) }
                        lastToolCenter = center
                        dragPosition = center
                        if (frame.slot == "room_soap") {
                            val overPet = onPet(reference(center))
                            if (soapOverPet != overPet) {
                                soapOverPet = overPet
                                onToolSoundChanged(BathStep.SOAP, overPet)
                            }
                        }
                        if (frame.slot == "room_bath_dryer") {
                            val nozzle = center + Offset(frame.width * 0.36f,
                                -frame.height * 0.15f) * unitPx
                            dryerOverPet = onPet(reference(nozzle)) || onPet(reference(center))
                        }
                    },
                    onDragEnd = {
                        val dryerFinished = frame.slot == "room_bath_dryer" &&
                            washStep == BathStep.DRY && dryProgress >= 1f
                        when (frame.slot) {
                            "room_soap" -> onToolSoundChanged(BathStep.SOAP, false)
                            "room_showerhead" -> onToolSoundChanged(BathStep.RINSE, false)
                            "room_bath_dryer" -> onDryerRunningChanged(false)
                        }
                        if (frame.slot == "room_soap" &&
                            soapTouchedThisDrag &&
                            (washStep != BathStep.SOAP ||
                                (contactDistance >= BathStep.SOAP.requiredContactDistance &&
                                    foamSpots.size >= 96))
                        ) onToolCompleted?.invoke(BathStep.SOAP)
                        if (frame.slot == "room_showerhead" && washStep == BathStep.RINSE &&
                            contactDistance >= 60f && foamSpots.isEmpty()
                        ) onToolCompleted?.invoke(BathStep.RINSE)
                        draggedSlot = null
                        dragPosition = null
                        lastToolCenter = null
                        dryerOverPet = false
                        soapOverPet = false
                        soapTouchedThisDrag = false
                        if (dryerFinished) onToolCompleted?.invoke(BathStep.DRY)
                    },
                    onDragCancel = {
                        when (frame.slot) {
                            "room_soap" -> onToolSoundChanged(BathStep.SOAP, false)
                            "room_showerhead" -> onToolSoundChanged(BathStep.RINSE, false)
                            "room_bath_dryer" -> onDryerRunningChanged(false)
                        }
                        draggedSlot = null
                        dragPosition = null
                        lastToolCenter = null
                        dryerOverPet = false
                        soapOverPet = false
                        soapTouchedThisDrag = false
                    },
                )
            } else Modifier
            Image(
                painter = painterResource(resource),
                contentDescription = if (onClick == null && !draggable) null else bathLabel(frame.slot),
                modifier = Modifier.bathFrame(frame, scale, originX, originY)
                    .then(if (draggedSlot == frame.slot) Modifier.graphicsLayer { alpha = 0f } else Modifier)
                    .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
                    .then(toolModifier),
                contentScale = if (frame.slot == "room_bath_tiles" && (compactRoom || closeUp)) {
                    ContentScale.Crop
                } else ContentScale.FillBounds,
            )
        }
        val petFrame = if (closeUp) BathFrame("pet", 160f, 445f + CLOSEUP_PET_DROP,
            620f, 690f)
            else BathFrame("pet", 620f, 520f, 260f, 312f)
        val petModifier = Modifier.bathFrame(petFrame, scale, originX, originY)
            .then(if (closeUp && wetness > 0f) Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    if (washStep == BathStep.DRY) {
                        dryZones.forEach { zone ->
                            val amount = wetness
                            if (amount > 0f) {
                                val center = (zone.position + Offset(0f, CLOSEUP_PET_DROP) -
                                    Offset(petFrame.x, petFrame.y)) * unitPx
                                val radius = zone.radius * 1.42f * unitPx
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            colors.wetShade.copy(alpha = amount * 0.14f),
                                            colors.wetShade.copy(alpha = amount * 0.09f),
                                            colors.wetShade.copy(alpha = 0f),
                                        ),
                                        center = center,
                                        radius = radius,
                                    ),
                                    radius = radius,
                                    center = center,
                                    blendMode = BlendMode.SrcAtop,
                                )
                            }
                        }
                    } else {
                        drawRect(colors.wetShade.copy(alpha = wetness * 0.23f),
                            blendMode = BlendMode.SrcAtop)
                    }
                } else Modifier)
        petContent(petModifier,
            RoomPetInteraction(showShadow = false, isBathing = closeUp,
                dirtStageOverride = if (closeUp &&
                    (washStep == BathStep.DRY || washStep == BathStep.CLEAN)) 0 else null))
        if (closeUp) {
            Canvas(Modifier.fillMaxSize()) {
                wetPatches.forEach { patch ->
                    val patchWetness = wetness
                    if (patchWetness > 0f) {
                        val center = originPx +
                            (patch.position + Offset(0f, CLOSEUP_PET_DROP)) * unitPx
                        rotate(patch.rotation, center) {
                            drawOval(colors.fabric.copy(alpha = patchWetness * 0.58f),
                                topLeft = center - Offset(patch.width, patch.height) * unitPx / 2f,
                                size = Size(patch.width * unitPx, patch.height * unitPx))
                            drawOval(colors.porcelain.copy(alpha = patchWetness * 0.48f),
                                topLeft = center - Offset(patch.width * 0.32f,
                                    patch.height * 0.28f) * unitPx,
                                size = Size(patch.width * 0.45f * unitPx,
                                    patch.height * 0.35f * unitPx))
                        }
                    }
                }
                foamSpots.forEach { spot ->
                    val center = originPx + spot.position * unitPx
                    val radius = spot.radius * unitPx
                    drawCircle(brush = Brush.radialGradient(
                        colors = listOf(
                            colors.porcelain.copy(alpha = 0.68f),
                            colors.porcelain.copy(alpha = 0.39f),
                            colors.fabricLight.copy(alpha = 0.17f),
                        ), center = center, radius = radius), radius = radius, center = center)
                    drawCircle(colors.fabricLight.copy(alpha = 0.7f), radius, center,
                        style = Stroke(width = 2.1f * unitPx))
                    drawCircle(colors.porcelain.copy(alpha = 0.64f), radius * 0.20f,
                        center + Offset(-radius * 0.38f, -radius * 0.36f))
                }
            }
        }
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
        if (closeUp && draggedSlot == "room_showerhead" && dragPosition != null) {
            val center = requireNotNull(dragPosition)
            val showerFrame = closeup.first { it.slot == "room_showerhead" }
            val fixtureSocket = originPx + Offset(264f, 386f) * unitPx
            val handConnector = center + Offset(-showerFrame.width * 0.34f,
                showerFrame.height * 0.47f) * unitPx
            Canvas(Modifier.fillMaxSize()) {
                val sag = maxOf(fixtureSocket.y, handConnector.y) + 85f * unitPx
                val hose = Path().apply {
                    moveTo(fixtureSocket.x, fixtureSocket.y)
                    cubicTo(fixtureSocket.x - 30f * unitPx, sag,
                        handConnector.x - 30f * unitPx, sag, handConnector.x, handConnector.y)
                }
                drawPath(hose, colors.outline, style = Stroke(width = 12f * unitPx))
                drawPath(hose, colors.porcelain, style = Stroke(width = 6f * unitPx))
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
        if (closeUp && draggedSlot == "room_bath_dryer" && dragPosition != null
        ) {
            val center = requireNotNull(dragPosition)
            val dryer = closeup.first { it.slot == "room_bath_dryer" }
            val nozzle = center + Offset(dryer.width * 0.44f, -dryer.height * 0.18f) * unitPx
            Canvas(Modifier.fillMaxSize()) {
                val reach = 150f * unitPx
                val airCone = Path().apply {
                    moveTo(nozzle.x, nozzle.y - 8f * unitPx)
                    cubicTo(nozzle.x + reach * 0.35f, nozzle.y - 18f * unitPx,
                        nozzle.x + reach * 0.72f, nozzle.y - 52f * unitPx,
                        nozzle.x + reach, nozzle.y - 55f * unitPx)
                    lineTo(nozzle.x + reach, nozzle.y + 55f * unitPx)
                    cubicTo(nozzle.x + reach * 0.72f, nozzle.y + 52f * unitPx,
                        nozzle.x + reach * 0.35f, nozzle.y + 18f * unitPx,
                        nozzle.x, nozzle.y + 8f * unitPx)
                    close()
                }
                drawPath(airCone, Brush.horizontalGradient(
                    colors = listOf(colors.fabricLight.copy(alpha = 0.42f),
                        colors.porcelain.copy(alpha = 0.07f)),
                    startX = nozzle.x, endX = nozzle.x + reach))
                repeat(5) { index ->
                    val spread = index - 2
                    val wave = sin(dryerPhase * 6.28318f + index) * 7f * unitPx
                    val stream = Path().apply {
                        moveTo(nozzle.x + 7f * unitPx,
                            nozzle.y + spread * 4f * unitPx)
                        cubicTo(nozzle.x + 42f * unitPx,
                            nozzle.y + spread * 10f * unitPx + wave,
                            nozzle.x + 91f * unitPx,
                            nozzle.y + spread * 22f * unitPx - wave,
                            nozzle.x + 132f * unitPx,
                            nozzle.y + spread * 29f * unitPx)
                    }
                    drawPath(stream, colors.fabricLight.copy(alpha = 0.56f),
                        style = Stroke(width = 4f * unitPx, cap = StrokeCap.Round))
                }
                repeat(7) { index ->
                    val travel = (dryerPhase + index / 7f) % 1f
                    val start = nozzle + Offset(8f + travel * 105f,
                        (index - 3) * (6f + travel * 9f) + sin(travel * 7f) * 4f) * unitPx
                    drawLine(colors.fabricLight.copy(alpha = (1f - travel) * 0.85f),
                        start, start + Offset(22f, (index - 3) * 3f) * unitPx,
                        strokeWidth = 4f * unitPx, cap = StrokeCap.Round)
                }
            }
        }
        if (closeUp && draggedSlot == "room_showerhead" && dragPosition != null
        ) {
            val center = requireNotNull(dragPosition)
            val head = closeup.first { it.slot == "room_showerhead" }
            // Holes span the oval face of the movable showerhead, not its handle.
            val holes = listOf(
                Offset(-0.13f, -0.33f), Offset(-0.02f, -0.40f),
                Offset(0.09f, -0.40f), Offset(0.20f, -0.36f),
                Offset(0.30f, -0.29f), Offset(-0.09f, -0.24f),
                Offset(0.02f, -0.27f), Offset(0.13f, -0.27f),
                Offset(0.25f, -0.21f), Offset(-0.01f, -0.15f),
                Offset(0.11f, -0.15f), Offset(0.19f, -0.11f),
            )
            val floorY = originPx.y + (1110f + CLOSEUP_PET_DROP) * unitPx
            Canvas(Modifier.fillMaxSize()) {
                holes.forEachIndexed { holeIndex, hole ->
                    val source = center + Offset(hole.x * head.width,
                        hole.y * head.height) * unitPx
                    val travel = floorY - source.y
                    if (travel <= 0f) return@forEachIndexed
                    repeat(18) { dropIndex ->
                        val progress = (waterPhase + dropIndex / 18f + holeIndex * 0.023f) % 1f
                        val fall = progress.pow(1.28f)
                        val spread = (holeIndex - 5.5f) * 11.5f * fall * unitPx
                        val drift = sin((progress * 13f + holeIndex) * 1.7f) * 2f * unitPx
                        val drop = Offset(source.x + spread + drift,
                            source.y + travel * fall)
                        val remaining = floorY - drop.y
                        val length = minOf(remaining, (11f + progress * 22f) * unitPx)
                        if (length > 0f) {
                            val fade = minOf(1f, remaining / (80f * unitPx))
                            drawLine(colors.fabricLight.copy(alpha = 0.82f * fade),
                                drop, drop + Offset(spread * 0.015f, length),
                                strokeWidth = 3.2f * unitPx)
                            drawCircle(colors.porcelain.copy(alpha = 0.48f * fade),
                                radius = 2.2f * unitPx, center = drop)
                        }
                    }
                }
            }
        }
    }
}

private val BathStep.requiredContactDistance: Float get() = when (this) {
    BathStep.SOAP -> 170f
    BathStep.RINSE -> 60f
    BathStep.DRY -> 160f
    BathStep.CLEAN -> Float.POSITIVE_INFINITY
}

private fun Modifier.bathFrame(frame: BathFrame, scale: Float, originX: Dp, originY: Dp): Modifier =
    offset(x = originX + (frame.x * scale).dp, y = originY + (frame.y * scale).dp)
        .size((frame.width * scale).dp, (frame.height * scale).dp)

private fun bathLabel(slot: String): String = when (slot) {
    "room_bathtub" -> "Ванна · помыть питомца"
    "room_bath_dryer" -> "Фен"
    "room_towel_hook" -> "Крючок для фена"
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
    "room_bath_dryer" -> R.drawable.bath_room_bath_dryer
    "room_towel_hook" -> R.drawable.bath_room_towel_hook
    "room_bath_shelf" -> R.drawable.bath_room_bath_shelf
    "room_bath_vanity" -> R.drawable.bath_room_bath_vanity
    "room_bath_sink" -> R.drawable.bath_room_bath_sink
    "room_bath_mirror" -> R.drawable.bath_room_bath_mirror
    "room_bath_mat" -> R.drawable.bath_room_bath_mat
    "room_bath_tiles" -> R.drawable.bath_room_bath_tiles
    else -> error("Unknown bathroom slot: $slot")
}

@Preview(name = "Ванная в доме", widthDp = 390, heightDp = 504)
@Composable
private fun BathroomScenePreview() {
    FinPetTheme {
        BathroomScene(closeUp = false, compactRoom = true, equipped = emptyMap(),
            modifier = Modifier.fillMaxSize())
    }
}
