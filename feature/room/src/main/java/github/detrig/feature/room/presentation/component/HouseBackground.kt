package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetHouseColors
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.presentation.model.HouseSurfaceLayout
import github.detrig.feature.room.presentation.model.HouseSurfaceTextures
import kotlin.math.roundToInt

/** Replaceable room surfaces in native scene coordinates; furniture is rendered above them. */
@Composable
internal fun HouseBackground(
    modifier: Modifier = Modifier,
    surfaces: HouseSurfaceTextures = HouseSurfaceTextures.EMPTY,
) {
    val colors = AppTheme.colors.house
    Canvas(modifier) {
        scale(
            size.width / HouseSurfaceLayout.SCENE_WIDTH,
            size.height / HouseSurfaceLayout.SCENE_HEIGHT,
            pivot = Offset.Zero,
        ) {
            HouseSurfaceLayout.Room.entries.forEach { room ->
                val image = surfaces.walls[room]
                if (image != null) {
                    drawSurfaceImage(image, room.left, 0, room.width, HouseSurfaceLayout.WALL_HEIGHT)
                } else {
                    val color = when (room) {
                        HouseSurfaceLayout.Room.PLAYROOM -> colors.playroomWall
                        HouseSurfaceLayout.Room.BEDROOM -> colors.bedroomWall
                        HouseSurfaceLayout.Room.LIVING -> colors.hallWall
                        HouseSurfaceLayout.Room.KITCHEN -> colors.kitchenWall
                        HouseSurfaceLayout.Room.BATHROOM -> colors.hallWall
                    }
                    wall(room.left.toFloat(), room.right.toFloat(), color, colors)
                }
            }

            if (HouseSurfaceLayout.Room.PLAYROOM !in surfaces.walls) {
                clipRect(0f, 0f, 322f, 474f) {
                    for (row in 0..8) {
                        for (column in 0..3) {
                            fish(Offset(column * 147f + if (row % 2 == 0) 102f else 31f, row * 57f - 8f), colors.wallPattern)
                        }
                    }
                }
            }
            if (HouseSurfaceLayout.Room.LIVING !in surfaces.walls) {
                clipRect(854f, 0f, 1548f, 474f) {
                    for (x in 865..1548 step 70) {
                        drawRect(colors.hallStripe, Offset(x.toFloat(), 0f), Size(35f, 474f))
                    }
                }
            }
            if (HouseSurfaceLayout.Room.KITCHEN !in surfaces.walls) {
                clipRect(1548f, 0f, 2048f, 474f) {
                    for (x in 1573..2048 step 83) {
                        drawLine(colors.tileGrout, Offset(x.toFloat(), 0f), Offset(x.toFloat(), 474f), 1.5f)
                    }
                    for (y in 33..474 step 89) {
                        drawLine(colors.tileGrout, Offset(1548f, y.toFloat()), Offset(2048f, y.toFloat()), 1.5f)
                    }
                }
            }
            drawRect(
                Brush.verticalGradient(
                    listOf(colors.floorHighlight, colors.floor, colors.floorShade),
                    startY = 472f, endY = 685f,
                ),
                Offset(0f, 472f), Size(HouseSurfaceLayout.SCENE_WIDTH.toFloat(), 213f),
            )
            HouseSurfaceLayout.Room.entries.forEach { room ->
                surfaces.floors[room]?.let { image ->
                    drawSurfaceImage(
                        image, room.left, HouseSurfaceLayout.WALL_HEIGHT - 2,
                        room.width, HouseSurfaceLayout.FLOOR_HEIGHT + 2,
                    )
                }
            }
            // The floor bleeds two scene pixels under the skirting, so fractional scaling cannot expose a gap.
            drawLine(colors.skirting, Offset(0f, 474f), Offset(HouseSurfaceLayout.SCENE_WIDTH.toFloat(), 474f), 2f)

            listOf(308f, 844f, 1539f, 2039f).forEach { x ->
                drawRect(
                    Brush.horizontalGradient(
                        listOf(colors.outline.copy(alpha = 0.10f), Color.Transparent),
                        startX = x + 20f, endX = x + 38f,
                    ), Offset(x + 20f, 0f), Size(18f, 535f),
                )
                drawRoundRect(colors.partition, Offset(x, 75f), Size(20f, 461f), CornerRadius(1.5f))
                drawRoundRect(colors.outline, Offset(x, 75f), Size(20f, 461f), CornerRadius(1.5f), style = Stroke(2.5f))
                drawLine(colors.outline, Offset(x, 87f), Offset(x + 20f, 87f), 2f)
                drawLine(colors.wallHighlight, Offset(x + 4f, 90f), Offset(x + 4f, 532f), 3f)
            }
        }
    }
}

/** Center-crops unexpected source ratios instead of stretching wallpaper or floor artwork. */
private fun DrawScope.drawSurfaceImage(
    image: ImageBitmap,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
) {
    val targetAspect = width.toFloat() / height
    val sourceAspect = image.width.toFloat() / image.height
    val sourceWidth = if (sourceAspect > targetAspect) {
        (image.height * targetAspect).roundToInt().coerceIn(1, image.width)
    } else image.width
    val sourceHeight = if (sourceAspect < targetAspect) {
        (image.width / targetAspect).roundToInt().coerceIn(1, image.height)
    } else image.height
    clipRect(left.toFloat(), top.toFloat(), (left + width).toFloat(), (top + height).toFloat()) {
        drawImage(
            image = image,
            srcOffset = IntOffset((image.width - sourceWidth) / 2, (image.height - sourceHeight) / 2),
            srcSize = IntSize(sourceWidth, sourceHeight),
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(width, height),
            filterQuality = FilterQuality.High,
        )
    }
}

private fun DrawScope.wall(left: Float, right: Float, color: Color, colors: FinPetHouseColors) {
    drawRect(color, Offset(left, 0f), Size(right - left, 474f))
    drawRect(
        Brush.linearGradient(
            listOf(colors.wallHighlight.copy(alpha = 0.14f), Color.Transparent),
            start = Offset(left, 0f), end = Offset(right, 474f),
        ), Offset(left, 0f), Size(right - left, 474f),
    )
}

private fun DrawScope.fish(center: Offset, color: Color) {
    val x = center.x
    val y = center.y
    val path = Path().apply {
        moveTo(x + 7f, y)
        cubicTo(x - 2f, y - 11f, x - 15f, y - 8f, x - 15f, y)
        cubicTo(x - 15f, y + 8f, x - 2f, y + 11f, x + 7f, y)
        lineTo(x + 15f, y - 8f)
        lineTo(x + 15f, y + 8f)
        close()
    }
    drawPath(path, color, style = Stroke(2f))
}

@Preview(name = "Фон комнаты", widthDp = 360, heightDp = 240)
@Composable
private fun HouseBackgroundPreview() {
    FinPetTheme {
        HouseBackground(Modifier.size(360.dp, 240.dp))
    }
}
