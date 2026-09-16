package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.presentation.model.HouseRoom
import kotlin.math.floor

/** Рисуется только видимое окно. Пол и перегородки имеют общий горизонт. */
@Composable
internal fun HouseBackground(cameraLeftX: () -> Float, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors.house
    Canvas(modifier) {
        val unit = size.width
        val camera = cameraLeftX()
        val horizon = HouseLayout.horizon(size.height, unit)
        drawRect(colors.floor)
        HouseLayout.sections.forEach { section ->
            if (section.endX < camera || section.startX > camera + 1) return@forEach
            val left = (section.startX - camera) * unit
            val width = section.width * unit
            val wall = when (section.room) {
                HouseRoom.PLAYROOM -> colors.playroomWall
                HouseRoom.BEDROOM -> colors.bedroomWall
                HouseRoom.HALL -> colors.hallWall
                HouseRoom.KITCHEN -> colors.kitchenWall
            }
            drawRect(wall, Offset(left, 0f), Size(width, horizon))
            when (section.room) {
                HouseRoom.PLAYROOM -> {
                    val start = floor(maxOf(section.startX, camera) / 0.27f).toInt()
                    val end = ((minOf(section.endX, camera + 1) / 0.27f).toInt() + 1)
                    for (column in start..end) for (row in 0..7) {
                        val x = (column * 0.27f + (row % 2) * 0.09f - camera) * unit
                        val y = 0.08f * unit + row * 0.21f * unit
                        if (y > horizon - 0.04f * unit) continue
                        val fish = Path().apply {
                            moveTo(x, y)
                            quadraticTo(x + unit * 0.018f, y - unit * 0.013f, x + unit * 0.038f, y)
                            lineTo(x + unit * 0.047f, y - unit * 0.009f)
                            lineTo(x + unit * 0.047f, y + unit * 0.009f)
                            lineTo(x + unit * 0.038f, y)
                            quadraticTo(x + unit * 0.018f, y + unit * 0.013f, x, y)
                        }
                        drawPath(fish, colors.wallPattern, style = Stroke(unit * 0.0015f))
                    }
                }
                HouseRoom.HALL -> {
                    repeat((section.width / 0.09f).toInt()) { column ->
                        drawRect(colors.hallStripe, Offset(left + column * 0.09f * unit, 0f),
                            Size(unit * 0.045f, horizon))
                    }
                }
                HouseRoom.KITCHEN -> {
                    val tile = unit * 0.16f
                    for (row in 0..(horizon / tile).toInt()) {
                        drawLine(colors.porcelain.copy(alpha = 0.65f), Offset(left, row * tile),
                            Offset(left + width, row * tile), unit * 0.0015f)
                    }
                    for (column in 0..(width / tile).toInt()) {
                        drawLine(colors.porcelain.copy(alpha = 0.65f), Offset(left + column * tile, 0f),
                            Offset(left + column * tile, horizon), unit * 0.0015f)
                    }
                    repeat(3) { row ->
                        repeat((width / tile).toInt() + 1) { column ->
                            drawRect(if ((row + column) % 2 == 0) colors.leaf.copy(alpha = 0.68f)
                                else colors.bedroomWall, Offset(left + column * tile, horizon + row * tile),
                                Size(tile, tile))
                        }
                    }
                }
                HouseRoom.BEDROOM -> Unit
            }
            drawRect(colors.skirting, Offset(left, horizon - unit * 0.006f), Size(width, unit * 0.008f))
        }
        // Два коврика лежат за свободной передней полосой ходьбы.
        listOf(7.42f to 0.54f, 9.76f to 0.99f).forEach { (x, width) ->
            val left = (x - width / 2 - camera) * unit
            val rug = Path().apply {
                moveTo(left + unit * 0.04f, horizon + unit * 0.18f)
                lineTo(left + width * unit - unit * 0.07f, horizon + unit * 0.18f)
                lineTo(left + width * unit, horizon + unit * 0.34f)
                lineTo(left, horizon + unit * 0.34f)
                close()
            }
            drawPath(rug, colors.leaf)
            drawPath(rug, colors.leafShade, style = Stroke(unit * 0.002f))
        }
        HouseLayout.sections.drop(1).forEach { section ->
            val x = (section.startX - camera) * unit
            if (x < -unit * 0.08f || x > unit * 1.08f) return@forEach
            val top = horizon * 0.28f
            val partition = Path().apply {
                moveTo(x - unit * 0.018f, top)
                lineTo(x + unit * 0.003f, top - unit * 0.015f)
                lineTo(x + unit * 0.018f, top)
                lineTo(x + unit * 0.018f, horizon + unit * 0.18f)
                lineTo(x - unit * 0.018f, horizon + unit * 0.18f)
                close()
            }
            drawPath(partition, colors.partition)
            drawPath(partition, colors.outline, style = Stroke(unit * 0.002f))
        }
    }
}
