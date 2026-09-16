package github.detrig.minigames.fishing.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import github.detrig.designsystem.theme.AppTheme
import github.detrig.minigames.fishing.domain.*
import kotlin.math.*

/** Все координаты модели нормализованы; экран не изменяет игровой мир. */
@Composable
internal fun FishingScene(
    art: FishingArt,
    engine: FishingEngine,
    scene: () -> FishingSession,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    petBitmap: ImageBitmap? = null,
    showInputCue: Boolean = false,
) {
    val colors = AppTheme.colors
    Canvas(modifier.clipToBounds()) {
        val s = scene()
        val worldHeight = size.height
        val worldWidth = min(size.width, size.height * 1.2f)
        val worldLeft = (size.width - worldWidth) / 2
        val unit = min(worldWidth, worldHeight * .9f)
        val cameraY = (s.cameraDepth * .75 * worldHeight).toFloat()
        val motionTime = s.activeSeconds
        val flightProgress = (s.phaseSeconds / engine.flightDuration(s)).coerceIn(0.0, 1.0).toFloat()
        fun point(x: Double, depth: Double) = Offset(worldLeft + (x * worldWidth).toFloat(), ((0.25 + 0.75 * depth) * worldHeight).toFloat() - cameraY)
        val bg = art["background"]
        // У исходной иллюстрации берег чуть ниже: обе части сходятся на общей линии воды.
        val split = (bg.height * 0.283).roundToInt()
        fun backdrop(sourceTop: Int, sourceHeight: Int, top: Float, height: Float, alignBottom: Boolean) {
            val scale = max(size.width / bg.width, height / sourceHeight)
            val cropWidth = (size.width / scale).roundToInt().coerceIn(1, bg.width)
            val cropHeight = (height / scale).roundToInt().coerceIn(1, sourceHeight)
            drawImage(bg, srcOffset = IntOffset((bg.width - cropWidth) / 2,
                sourceTop + if (alignBottom) sourceHeight - cropHeight else 0), srcSize = IntSize(cropWidth, cropHeight),
                dstOffset = IntOffset(0, top.roundToInt()), dstSize = IntSize(size.width.roundToInt(), height.roundToInt()),
                filterQuality = FilterQuality.Low)
        }
        clipRect {
            backdrop(0, split, -cameraY, worldHeight * .25f, alignBottom = true)
            val waterTop = worldHeight * .25f - cameraY
            val waterHeight = worldHeight * .75f * (engine.config.world.maxDepth.toFloat() + .16f)
            drawImage(bg, srcOffset = IntOffset(0, split), srcSize = IntSize(bg.width, bg.height - split),
                dstOffset = IntOffset(0, waterTop.roundToInt()), dstSize = IntSize(size.width.roundToInt(), waterHeight.roundToInt()),
                filterQuality = FilterQuality.Low)
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, colors.statusInfo.onContainer.copy(alpha = .22f))),
                topLeft = Offset(0f, waterTop), size = Size(size.width, waterHeight))
        }

        fun sprite(id: String, center: Offset, width: Float, height: Float? = null, mirror: Boolean = false, alpha: Float = 1f) {
            if (center.y < -unit || center.y > size.height + unit) return
            val bitmap = art[id]
            val h = height ?: width * bitmap.height / bitmap.width
            scale(if (mirror) -1f else 1f, 1f, center) {
                drawImage(bitmap, dstOffset = IntOffset((center.x - width / 2).roundToInt(), (center.y - h / 2).roundToInt()),
                    dstSize = IntSize(width.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1)), alpha = alpha, filterQuality = FilterQuality.None)
            }
        }
        fun ripple(at: Offset, progress: Float, radius: Float = unit * .10f) {
            if (progress !in 0f..1f) return
            val r = radius * (.2f + progress)
            drawOval(colors.onActionPrimary.copy(alpha = (1 - progress) * .8f),
                at - Offset(r, r * .24f), Size(r * 2, r * .48f), style = Stroke(unit * .004f))
            if (!reducedMotion) repeat(5) { i ->
                val x = (i - 2) * radius * .27f * progress
                val y = -sin(progress * PI).toFloat() * radius * (.45f + (i % 2) * .4f)
                drawCircle(colors.onActionPrimary.copy(alpha = 1 - progress), unit * .005f, at + Offset(x, y))
            }
        }
        for (fish in s.fish) {
            if (fish.respawnSeconds > 0) {
                if (fish.spawnId == s.lostSpawnId && s.lastLoss in listOf(LossReason.TENSION, LossReason.SLACK) && s.phase == FishingPhase.EMPTY_RETURN) {
                    val t = (s.phaseSeconds / s.returnDuration).toFloat().coerceIn(0f, 1f)
                    sprite("fish_" + fish.speciesId, point(fish.x + .22 * t, fish.y + .06 * t), worldWidth * .126f, alpha = 1 - t)
                }
                continue
            }
            val center = point(fish.x, fish.y)
            val angler = s
            val onHook = fish.behavior == FishBehavior.HOOKED
            val burst = onHook && engine.isBurst(angler)
            val width = worldWidth * .126f
            val tilt = if (reducedMotion) 0f else (sin(motionTime * (if (burst) 24 else 6) + fish.spawnId) * (if (burst) 12 else 3)).toFloat()
            rotate(tilt, center) { sprite("fish_" + fish.speciesId, center, width, mirror = fish.direction < 0,
                alpha = (1 - fish.arrivalSeconds / .45).toFloat().coerceIn(0f, 1f)) }
            if (onHook) {
                if (engine.burstSoon(angler)) drawCircle(colors.currencyAccent, width * .65f, center, style = Stroke(unit * .005f))
                if (angler.fightSeconds < .5) repeat(6) { i ->
                    val angle = i * PI / 3
                    val vector = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    drawLine(colors.onActionPrimary, center + vector * width * .65f, center + vector * width * .85f, unit * .004f)
                }
                if (angler.warning || angler.slackSeconds >= engine.config.fight.slackHintAfterSeconds) {
                    val mark = center - Offset(0f, width * .6f)
                    drawCircle(colors.statusWarning.container, width * .2f, mark)
                    drawLine(colors.statusWarning.onContainer, mark - Offset(0f, width * .1f), mark + Offset(0f, width * .02f), unit * .006f, StrokeCap.Round)
                    drawCircle(colors.statusWarning.onContainer, unit * .004f, mark + Offset(0f, width * .1f))
                }
            }
        }
        s.objects.filter { !it.taken || it.id == s.attachedObjectId }.forEach { obj ->
            val at = point(obj.x, obj.y)
            val flashing = obj.kind == ObjectKind.BOMB && obj.taken && (reducedMotion || (s.activeSeconds * 3).toInt() % 2 == 0)
            fishingObject(obj.kind, at, worldWidth * .072f, colors, flashing)
            if (obj.kind == ObjectKind.BOMB && obj.visibleSeconds < engine.config.world.bombVisibleSeconds) {
                drawCircle(colors.onActionPrimary, worldWidth * .055f, at, style = Stroke(unit * .004f))
            }
        }
        if (!reducedMotion) {
            repeat(6) { index ->
                val x = worldLeft + (.13 + index * .14) * worldWidth
                val cycle = (s.activeSeconds * .018 + index * .16) % 1
                drawCircle(colors.onActionPrimary.copy(alpha = .22f), unit * .004f,
                    Offset(x.toFloat(), ((.93 - cycle * .62) * worldHeight).toFloat()), style = Stroke(unit * .002f))
            }
        }

        val rocking = if (reducedMotion) 0f else (sin(motionTime * 2) * 1.5).toFloat()
        val boatCenter = Offset(worldLeft + worldWidth * .20f, worldHeight * .25f - cameraY)
        val boatWidth = unit * .40f
        val boatHeight = boatWidth * .5f
        val boatTop = boatCenter.y - boatHeight * .65f
        rotate(rocking, boatCenter) {
            val boatDrawCenter = Offset(boatCenter.x, boatTop + boatHeight / 2)
            sprite("boat", boatDrawCenter, boatWidth)
            val petWidth = boatWidth * .5125f
            val castLean = when (s.phase) {
                FishingPhase.CHARGING -> -12f * s.power.toFloat()
                FishingPhase.FLYING -> 14f * sin(flightProgress * PI).toFloat()
                FishingPhase.FIGHTING -> if (s.pulling) -9f else 5f
                else -> 0f
            }
            val hop = if (s.phase == FishingPhase.RELEASING && !reducedMotion)
                sin((s.phaseSeconds / engine.config.round.catchReleaseSeconds) * PI).toFloat() * petWidth * .18f else 0f
            val petCenter = Offset(boatCenter.x - boatWidth * .045f, boatTop + boatHeight * .20f - petWidth * .25f - hop)
            rotate(if (reducedMotion) castLean * .35f else castLean, petCenter + Offset(0f, petWidth * .4f)) {
                // TODO: Добавить отдельные игровые позы для каждого нового вида питомца.
                petBitmap?.let { bitmap ->
                    drawImage(
                        bitmap,
                        dstOffset = IntOffset(
                            (petCenter.x - petWidth / 2).roundToInt(),
                            (petCenter.y - petWidth / 2).roundToInt(),
                        ),
                        dstSize = IntSize(petWidth.roundToInt(), petWidth.roundToInt()),
                        filterQuality = FilterQuality.None,
                    )
                }
            }
            // Передний борт — маска того же мастера, поэтому его геометрия точно совпадает.
            val front = Path().apply {
                moveTo(boatCenter.x - boatWidth / 2, boatTop + boatHeight * .50f)
                lineTo(boatCenter.x + boatWidth * .12f, boatTop + boatHeight * .50f)
                lineTo(boatCenter.x + boatWidth / 2, boatTop + boatHeight * .22f)
                lineTo(boatCenter.x + boatWidth / 2, boatTop + boatHeight)
                lineTo(boatCenter.x - boatWidth / 2, boatTop + boatHeight)
                close()
            }
            clipPath(front) { sprite("boat", boatDrawCenter, boatWidth) }
        }

        val rodBase = Offset(boatCenter.x + boatWidth * .20f, boatTop + boatHeight * .5f)
        val windup = when (s.phase) {
            FishingPhase.CHARGING -> s.power.toFloat()
            FishingPhase.FLYING -> (1f - (flightProgress * 4).coerceAtMost(1f)) * s.power.toFloat()
            else -> 0f
        }
        val bend = if (s.phase == FishingPhase.FIGHTING) (s.tension / 100).toFloat() else 0f
        val shake = if (s.warning && !reducedMotion) sin(s.fightSeconds * 30).toFloat() * unit * .005f else 0f
        val rodTip = point(engine.rodX(s), engine.config.world.rodDepth) + Offset(-unit * windup * .28f + shake, unit * bend * .025f)
        val rod = Path().apply {
            moveTo(rodBase.x, rodBase.y)
            quadraticBezierTo(boatCenter.x + boatWidth * (.325f - windup * .3f), worldHeight * .06f - cameraY, rodTip.x, rodTip.y)
        }
        drawPath(rod, colors.roomObjectBorder, style = Stroke(unit * .007f, cap = StrokeCap.Round))
        val reel = rodBase + Offset(-unit * .012f, unit * .012f)
        drawCircle(colors.currencyAccent, unit * .017f, reel)
        val reelAngle = (s.reelingSeconds * 450 + s.reelCount * 95 + (if (s.phase == FishingPhase.SEARCHING && s.activeSeconds - s.lastReelAt < .2) (s.activeSeconds - s.lastReelAt) * 450 else 0.0)).toFloat()
        rotate(reelAngle, reel) {
            drawLine(colors.roomObjectBorder, reel, reel + Offset(unit * .025f, 0f), unit * .005f, StrokeCap.Round)
            drawCircle(colors.roomObjectBorder, unit * .006f, reel + Offset(unit * .025f, 0f))
        }
        val hook = point(s.hook.x, s.hook.y)
        if (s.phase == FishingPhase.SEARCHING && s.phaseSeconds < .7) ripple(point(s.castX, 0.0), (s.phaseSeconds / .7).toFloat())
        if (s.phase == FishingPhase.BOMB_POP) ripple(point(engine.rodX(s), 0.0), (s.phaseSeconds / engine.config.world.bombPopSeconds).toFloat(), unit * .35f)
        if (s.phase == FishingPhase.JUNK_RELEASE && s.lastObjectKind != null) {
            val t = (s.phaseSeconds / engine.config.world.junkReleaseSeconds).toFloat().coerceIn(0f, 1f)
            fishingObject(s.lastObjectKind, point(engine.rodX(s) * (1 - t) + .14 * t, -.04 * t), unit * .07f, colors)
        }
        if (s.junkCount > 0) {
            drawRoundRect(colors.currencyContainer, point(.12, -.02), Size(unit * .06f, unit * .045f), androidx.compose.ui.geometry.CornerRadius(unit * .009f))
        }
        if (s.phase in listOf(FishingPhase.FLYING, FishingPhase.SEARCHING, FishingPhase.FIGHTING, FishingPhase.HAULING, FishingPhase.CATCH_PENDING, FishingPhase.EMPTY_RETURN)) {
            val taut = s.hook.reelRemaining > 0 || s.pulling && s.inWater
            val line = Path().apply {
                moveTo(rodTip.x, rodTip.y)
                quadraticTo((rodTip.x + hook.x) / 2 + if (taut) 0f else worldWidth * .025f,
                    (rodTip.y + hook.y) / 2 + if (s.slackSeconds >= engine.config.fight.slackHintAfterSeconds) unit * .12f else 0f, hook.x, hook.y)
            }
            drawPath(line, if (s.tension >= engine.config.fight.criticalOn) colors.statusCritical.accent else if (s.warning) colors.currencyAccent else colors.onActionPrimary,
                style = Stroke(unit * .0035f, cap = StrokeCap.Round))
            drawArc(colors.currencyAccent, 0f, 235f, false, hook - Offset(unit * .007f, unit * .005f), Size(unit * .014f, unit * .019f), style = Stroke(unit * .005f))
            drawCircle(colors.onActionPrimary, unit * .004f, hook - Offset(0f, unit * .005f))
        }
        if (s.phase == FishingPhase.CHARGING) {
            val mark = point(engine.splashX(s), 0.0)
            drawOval(colors.onActionPrimary, mark - Offset(worldWidth * .028f, worldHeight * .006f),
                Size(worldWidth * .056f, worldHeight * .012f), style = Stroke(unit * .004f))
        }
        if (s.phase == FishingPhase.RELEASING && s.catches.isNotEmpty()) {
            val t = (s.phaseSeconds / engine.config.round.catchReleaseSeconds).toFloat().coerceIn(0f, 1f)
            val center = point(.40 + .20 * t, .04 + .23 * t) - Offset(0f, sin(t * PI).toFloat() * unit * .2f)
            rotate(20f * t, center) { sprite("fish_" + s.catches.last().speciesId, center, unit * .20f, alpha = 1f - t * .5f) }
            ripple(point(.52, 0.0), ((t - .5f) * 2).coerceAtLeast(0f))
            if (!reducedMotion) repeat(5) { i ->
                val a = (i * PI * 2 / 5).toFloat()
                val star = boatCenter + Offset(cos(a), sin(a)) * unit * (.17f + t * .12f)
                val radius = unit * .015f * (1 - t)
                drawLine(colors.currencyAccent, star - Offset(radius, 0f), star + Offset(radius, 0f), unit * .004f)
                drawLine(colors.currencyAccent, star - Offset(0f, radius), star + Offset(0f, radius), unit * .004f)
            }
        }
        // Подсказка жестом остаётся внутри мира и не закрывает приборы.
        if (showInputCue && (s.phase == FishingPhase.READY || s.tutorial && s.phase == FishingPhase.FIGHTING && (s.warning || !s.pulling))) {
            val lift = s.warning
            val beat = if (reducedMotion) .5f else ((sin(s.phaseSeconds * 4) + 1) / 2).toFloat()
            val at = Offset(worldLeft + worldWidth * .55f, worldHeight * .72f - if (lift) unit * .06f * beat else 0f)
            val r = unit * .045f
            drawCircle(colors.roomBackground.copy(alpha = .45f), r * 1.9f, at)
            drawCircle(colors.onActionPrimary.copy(alpha = .6f), r * (1f + beat * .6f), at, style = Stroke(unit * .004f))
            drawLine(colors.onActionPrimary, at + Offset(r * .45f, r * 1.4f), at, r * .6f, StrokeCap.Round)
            drawCircle(colors.onActionPrimary, r * .48f, at + Offset(r * .55f, r * .9f))
            if (lift) {
                drawLine(colors.onActionPrimary, at - Offset(0f, r), at - Offset(0f, r * 2), unit * .005f)
                drawLine(colors.onActionPrimary, at - Offset(0f, r * 2), at + Offset(-r * .4f, -r * 1.6f), unit * .005f)
                drawLine(colors.onActionPrimary, at - Offset(0f, r * 2), at + Offset(r * .4f, -r * 1.6f), unit * .005f)
            }
        }
        sprite("flora_left", Offset(size.width * .025f, point(.0, engine.config.world.maxDepth).y), worldWidth * .12f, worldHeight * .22f)
        sprite("flora_right", Offset(size.width * .975f, point(.0, engine.config.world.maxDepth).y), worldWidth * .12f, worldHeight * .22f)
    }
}
