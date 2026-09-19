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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetHouseColors
import github.detrig.designsystem.theme.FinPetTheme

/** Resolution-independent surfaces; furniture shares the interactive sprite renderer. */
@Composable
internal fun HouseBackground(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors.house
    Canvas(modifier) {
        scale(size.width / 2048f, size.height / 685f, pivot = Offset.Zero) {
            wall(0f, 322f, colors.playroomWall, colors)
            wall(322f, 854f, colors.bedroomWall, colors)
            wall(854f, 1548f, colors.hallWall, colors)
            wall(1548f, 2048f, colors.kitchenWall, colors)

            clipRect(0f, 0f, 322f, 474f) {
                for (row in 0..8) {
                    for (column in 0..3) {
                        fish(Offset(column * 147f + if (row % 2 == 0) 102f else 31f, row * 57f - 8f), colors.wallPattern)
                    }
                }
            }
            clipRect(854f, 0f, 1548f, 474f) {
                for (x in 865..1548 step 70) {
                    drawRect(colors.hallStripe, Offset(x.toFloat(), 0f), Size(35f, 474f))
                }
            }
            clipRect(1548f, 0f, 2048f, 474f) {
                for (x in 1573..2048 step 83) {
                    drawLine(colors.tileGrout, Offset(x.toFloat(), 0f), Offset(x.toFloat(), 474f), 1.5f)
                }
                for (y in 33..474 step 89) {
                    drawLine(colors.tileGrout, Offset(1548f, y.toFloat()), Offset(2048f, y.toFloat()), 1.5f)
                }
            }
            drawRect(
                Brush.verticalGradient(
                    listOf(colors.floorHighlight, colors.floor, colors.floorShade),
                    startY = 474f, endY = 685f,
                ),
                Offset(0f, 474f), Size(2048f, 211f),
            )
            drawLine(colors.skirting, Offset(0f, 474f), Offset(2048f, 474f), 2f)

            listOf(308f, 844f, 1539f).forEach { x ->
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
