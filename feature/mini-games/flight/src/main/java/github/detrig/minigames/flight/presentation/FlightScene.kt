package github.detrig.minigames.flight.presentation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import github.detrig.designsystem.theme.AppTheme
import github.detrig.minigames.flight.domain.FlightConfig
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Кадровое состояние читается только в draw: без рекомпозиции дерева экрана. */
@Composable
internal fun FlightScene(
    frames: State<FlightRenderFrame>, config: FlightConfig, artwork: FlightArtwork?,
    reducedMotion: Boolean, interactive: Boolean, flapLabel: String,
    onFlap: () -> Unit, modifier: Modifier = Modifier,
    petBitmap: ImageBitmap? = null,
) {
    val colors = AppTheme.colors
    val input = if (interactive) Modifier.pointerInput(onFlap) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            down.consume()
            onFlap()
            do {
                val event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }.semantics {
        contentDescription = flapLabel
        role = Role.Button
        onClick(label = flapLabel) { onFlap(); true }
    } else Modifier
    Box(modifier.then(input).drawWithCache {
        val ww = config.world.width.toFloat()
        val wh = config.world.height.toFloat()
        val sx = size.width / ww
        val sy = size.height / wh
        val spriteScale = min(sx, sy)
        val fallback = Brush.verticalGradient(listOf(colors.flightSkyTop, colors.flightSkyBottom), endY = size.height)
        val background = artwork?.sky
        val backgroundScale = if (background == null) 1f else max(size.width / background.width, size.height / background.height)
        val backgroundSize = IntSize(((background?.width ?: 0) * backgroundScale).roundToInt(),
            ((background?.height ?: 0) * backgroundScale).roundToInt())
        val backgroundOffset = IntOffset(((size.width - backgroundSize.width) / 2).roundToInt(),
            ((size.height - backgroundSize.height) / 2).roundToInt())
        onDrawBehind {
            if (background != null) drawImage(background, dstOffset = backgroundOffset,
                dstSize = backgroundSize, filterQuality = FilterQuality.Low)
            else drawRect(fallback)
            val frame = frames.value
            val session = frame.session
            val moving = session?.started == true && session.outcome == null
            val secondsAhead = if (moving) frame.fraction / config.physics.tickRate else 0f
            val speed = config.stages.last { (session?.tick ?: 0) >= it.fromTick }.speed.toFloat()
            scale(sx, sy, Offset.Zero) {
                session?.gates?.forEach { gate ->
                    val x = gate.x.toFloat() - speed * secondsAhead
                    if (x + config.gates.width >= 0 && x <= ww) {
                        val upper = (gate.center - gate.gap / 2).toFloat()
                        val lower = (gate.center + gate.gap / 2).toFloat()
                        artwork?.pillar?.let { sprite ->
                            pillar(sprite, x, upper, config.gates.width.toFloat(), upper, sx / sy, lower = false)
                            pillar(sprite, x, lower, config.gates.width.toFloat(), wh - lower, sx / sy, lower = true)
                        }
                    }
                }
            }
            val y = ((session?.y ?: config.world.startY) + (session?.velocity ?: 0.0) * secondsAhead)
                .toFloat().coerceIn(config.world.radius.toFloat(), wh - config.world.radius.toFloat()) * sy
            val x = config.world.petX.toFloat() * sx
            if (!reducedMotion && moving) repeat(3) { i ->
                drawRect(colors.flightCloud.copy(alpha = .55f - .15f * i),
                    Offset(x - (31 + i * 12) * spriteScale, y + i * 3 * spriteScale),
                    Size((10 - i * 2) * spriteScale, 2 * spriteScale))
            }
            val angle = if (reducedMotion) 0f else ((session?.velocity ?: 0.0) / 19.0).toFloat().coerceIn(-18f, 28f)
            // TODO: Добавить отдельные полётные анимации для каждого нового вида питомца.
            petBitmap?.let { bitmap ->
                val side = (52 * spriteScale).toInt().coerceAtLeast(1)
                rotate(angle, Offset(x, y)) {
                    drawImage(bitmap, dstOffset = IntOffset((x - side / 2).toInt(), (y - side / 2).toInt()),
                        dstSize = IntSize(side, side), filterQuality = FilterQuality.Low)
                }
            }
        }
    })
}

/** Трава обращена к просвету. Текстура обрезается, а не растягивается по высоте. */
private fun DrawScope.pillar(sprite: FlightSprite, x: Float, edge: Float, width: Float, height: Float,
    pixelAspect: Float, lower: Boolean) {
    if (height <= 0) return
    translate(x, edge) {
        scale(1f, if (lower) 1f else -1f, Offset.Zero) {
            clipRect(0f, 0f, width, height) {
                val naturalHeight = width * sprite.size.height / sprite.size.width * pixelAspect
                drawImage(sprite.bitmap, srcOffset = sprite.offset, srcSize = sprite.size,
                    dstSize = IntSize(width.roundToInt(), naturalHeight.roundToInt()), filterQuality = FilterQuality.Low)
                // На очень длинной колонне повторяется камень без второй травяной кромки.
                val bodyOffset = IntOffset(sprite.offset.x, sprite.offset.y + sprite.size.height / 3)
                val bodySize = IntSize(sprite.size.width, sprite.size.height / 3)
                val bodyHeight = naturalHeight / 3
                var y = naturalHeight
                while (y < height) {
                    drawImage(sprite.bitmap, srcOffset = bodyOffset, srcSize = bodySize,
                        dstOffset = IntOffset(0, y.roundToInt()),
                        dstSize = IntSize(width.roundToInt(), bodyHeight.roundToInt() + 1), filterQuality = FilterQuality.Low)
                    y += bodyHeight
                }
            }
        }
    }
}
