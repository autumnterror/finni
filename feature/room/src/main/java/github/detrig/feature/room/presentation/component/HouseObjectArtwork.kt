package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetHouseColors
import github.detrig.feature.room.presentation.model.HouseObjectArt

/** Независимые векторные предметы: рисунок не содержит хитбокс или состояние доступа. */
@Composable
internal fun HouseObjectArtwork(art: HouseObjectArt, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors.house
    Canvas(modifier) {
        scale(size.width / 100, size.height / 100, Offset.Zero) {
            HouseDrawing(this, colors).draw(art)
        }
    }
}

private class HouseDrawing(private val scope: DrawScope, private val c: FinPetHouseColors) {
    private fun box(x: Float, y: Float, w: Float, h: Float, color: Color, r: Float = 0f) = with(scope) {
        drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r))
        drawRoundRect(c.outline, Offset(x, y), Size(w, h), CornerRadius(r), style = Stroke(1.1f))
    }
    private fun oval(x: Float, y: Float, w: Float, h: Float, color: Color) = with(scope) {
        drawOval(color, Offset(x, y), Size(w, h))
        drawOval(c.outline, Offset(x, y), Size(w, h), style = Stroke(1.1f))
    }
    private fun line(x: Float, y: Float, x2: Float, y2: Float, color: Color = c.outline, width: Float = 1.1f) =
        scope.drawLine(color, Offset(x, y), Offset(x2, y2), width, StrokeCap.Round)
    private fun shape(color: Color, vararg points: Float) = with(scope) {
        val p = Path().apply {
            moveTo(points[0], points[1])
            for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1])
            close()
        }
        drawPath(p, color)
        drawPath(p, c.outline, style = Stroke(1.1f))
    }
    private fun legs(y: Float = 65f, color: Color = c.woodShade) {
        box(12f, y, 6f, 98f - y, color)
        box(82f, y, 6f, 98f - y, color)
    }
    private fun table(color: Color = c.woodLight, top: Float = 52f) {
        legs(top + 8)
        shape(color, 7f, top, 85f, top, 96f, top + 15, 3f, top + 15)
        box(3f, top + 15, 93f, 7f, color)
    }
    private fun plant(x: Float, y: Float, scale: Float = 1f) {
        line(x, y, x, y - 26 * scale, c.leafShade, 2f)
        oval(x - 15 * scale, y - 28 * scale, 16 * scale, 10 * scale, c.leaf)
        oval(x + 1, y - 21 * scale, 16 * scale, 10 * scale, c.leafShade)
        shape(c.wood, x - 12 * scale, y, x + 12 * scale, y,
            x + 8 * scale, y + 19 * scale, x - 8 * scale, y + 19 * scale)
    }

    fun draw(art: HouseObjectArt) {
        when (art) {
            HouseObjectArt.DRAWING -> {
                shape(c.wood, 23f, 8f, 30f, 8f, 18f, 98f, 12f, 98f)
                shape(c.wood, 67f, 8f, 73f, 8f, 90f, 98f, 84f, 98f)
                shape(c.porcelain, 22f, 13f, 70f, 11f, 78f, 70f, 16f, 71f)
                line(47f, 57f, 47f, 36f, c.leafShade, 2f)
                oval(34f, 46f, 13f, 6f, c.leaf)
                oval(46f, 39f, 15f, 6f, c.leaf)
                repeat(5) { i ->
                    val angle = i * kotlin.math.PI * 2 / 5
                    oval(43f + kotlin.math.cos(angle).toFloat() * 8, 25f + kotlin.math.sin(angle).toFloat() * 7, 10f, 9f, c.sunnyAccent)
                }
                oval(43f, 26f, 10f, 9f, c.warmAccent)
                box(12f, 72f, 72f, 5f, c.woodLight)
                box(20f, 66f, 6f, 7f, c.warmAccent)
                box(31f, 65f, 6f, 8f, c.fabric)
                box(60f, 65f, 10f, 7f, c.leaf)
            }
            HouseObjectArt.MUSIC -> {
                box(9f, 50f, 5f, 46f, c.fabric)
                box(56f, 50f, 5f, 40f, c.fabric)
                shape(c.fabric, 8f, 20f, 58f, 20f, 67f, 48f, 3f, 48f)
                shape(c.porcelain, 9f, 34f, 55f, 34f, 60f, 48f, 5f, 48f)
                repeat(8) { line(10f + it * 6, 35f, 8f + it * 6, 48f) }
                repeat(5) { box(13f + it * 9, 34f, 3f, 8f, c.outline) }
                oval(20f, 24f, 9f, 4f, c.warmAccent)
                oval(37f, 24f, 8f, 4f, c.sunnyAccent)
                box(63f, 55f, 29f, 30f, c.warmAccent)
                oval(62f, 49f, 31f, 12f, c.sunnyAccent)
                oval(63f, 80f, 29f, 7f, c.sunnyAccent)
                for (i in 0..2) { line(65f + i * 9, 61f, 69f + i * 9, 76f, c.porcelain, 2f); line(69f + i * 9, 76f, 74f + i * 9, 61f, c.porcelain, 2f) }
                line(64f, 94f, 93f, 89f, c.wood, 3f)
            }
            HouseObjectArt.WORKSHOP -> {
                legs()
                box(20f, 67f, 56f, 24f, c.wood)
                box(24f, 69f, 26f, 15f, c.leaf)
                oval(35f, 75f, 3f, 3f, c.outline)
                box(14f, 5f, 69f, 53f, c.woodLight, 3f)
                line(33f, 23f, 33f, 44f, c.woodShade, 5f)
                box(23f, 16f, 23f, 8f, c.metal, 2f)
                line(63f, 21f, 63f, 44f, c.fabric, 6f)
                oval(58f, 15f, 10f, 8f, c.metal)
                table(top = 52f)
                box(20f, 43f, 14f, 16f, c.fabric)
                box(41f, 36f, 10f, 23f, c.sunnyAccent)
                shape(c.warmAccent, 35f, 39f, 46f, 24f, 58f, 39f)
                box(64f, 53f, 15f, 6f, c.leaf)
            }
            HouseObjectArt.PUZZLE -> {
                table(c.fabric, 37f)
                shape(c.fabricLight, 18f, 18f, 82f, 18f, 89f, 48f, 10f, 48f)
                box(21f, 23f, 23f, 13f, c.warmAccent)
                box(45f, 23f, 24f, 13f, c.sunnyAccent)
                box(22f, 36f, 25f, 12f, c.leaf)
                box(48f, 36f, 28f, 12f, c.fabric)
                oval(40f, 27f, 9f, 6f, c.warmAccent)
                oval(33f, 32f, 8f, 6f, c.leaf)
                oval(63f, 33f, 8f, 6f, c.sunnyAccent)
            }
            HouseObjectArt.BALL -> {
                oval(18f, 84f, 41f, 9f, c.leaf)
                box(36f, 26f, 6f, 60f, c.wood)
                box(19f, 5f, 42f, 35f, c.sunnyAccent, 10f)
                box(31f, 17f, 19f, 17f, c.wood, 2f)
                repeat(4) { line(28f + it * 8, 44f, 35f + it * 4, 62f, c.metal) }
                line(32f, 56f, 49f, 56f, c.metal)
                oval(23f, 36f, 34f, 10f, c.warmAccent)
                oval(60f, 73f, 29f, 24f, c.warmAccent)
                line(73f, 74f, 75f, 96f)
                line(61f, 84f, 87f, 86f)
            }
            HouseObjectArt.FOOTBALL -> {
                repeat(6) { line(18f + it * 13, 23f, 16f + it * 13, 73f, c.metal) }
                repeat(4) { line(16f, 25f + it * 14, 84f, 25f + it * 14, c.metal) }
                line(11f, 81f, 16f, 18f, c.metal, 6f)
                line(16f, 18f, 84f, 18f, c.metal, 6f)
                line(84f, 18f, 90f, 81f, c.metal, 6f)
                oval(42f, 68f, 26f, 27f, c.porcelain)
                shape(c.outline, 51f, 75f, 59f, 74f, 63f, 81f, 57f, 86f, 49f, 82f)
                line(52f, 70f, 51f, 75f); line(65f, 89f, 58f, 85f)
            }
            HouseObjectArt.RACING -> {
                box(3f, 7f, 94f, 87f, c.leaf, 14f)
                box(13f, 19f, 74f, 61f, c.metal, 20f)
                box(28f, 34f, 44f, 31f, c.leaf, 12f)
                repeat(5) { line(31f + it * 9, 25f, 35f + it * 9, 25f, c.porcelain, 2f) }
                repeat(5) { line(31f + it * 9, 72f, 35f + it * 9, 72f, c.porcelain, 2f) }
                box(39f, 65f, 24f, 12f, c.warmAccent, 4f)
                oval(41f, 73f, 6f, 7f, c.outline); oval(55f, 73f, 6f, 7f, c.outline)
                box(46f, 62f, 11f, 7f, c.fabricLight, 2f)
            }
            HouseObjectArt.THEATER -> {
                box(11f, 12f, 78f, 83f, c.wood)
                box(20f, 23f, 60f, 55f, c.porcelain)
                shape(c.warmAccent, 20f, 23f, 46f, 23f, 28f, 55f, 20f, 61f)
                shape(c.warmAccent, 55f, 23f, 80f, 23f, 80f, 61f, 72f, 55f)
                box(15f, 74f, 69f, 22f, c.fabric)
                shape(c.sunnyAccent, 8f, 14f, 50f, 2f, 93f, 14f, 93f, 20f, 8f, 20f)
                oval(46f, 7f, 8f, 8f, c.warmAccent)
                repeat(5) { line(22f + it * 12, 77f, 22f + it * 12, 92f, c.fabricLight) }
            }
            HouseObjectArt.FISHING -> {
                box(5f, 65f, 90f, 22f, c.fabric, 9f)
                oval(4f, 48f, 92f, 33f, c.fabric)
                oval(12f, 53f, 76f, 23f, c.fabricLight)
                line(23f, 3f, 62f, 57f, c.woodShade, 3f)
                line(23f, 3f, 65f, 31f)
                line(65f, 31f, 65f, 64f)
                listOf(Triple(25f, 58f, c.sunnyAccent), Triple(53f, 65f, c.blush), Triple(65f, 57f, c.leaf)).forEach { (x,y,color) ->
                    shape(color, x + 10, y + 2, x + 17, y - 2, x + 17, y + 7)
                    oval(x, y, 13f, 7f, color)
                    oval(x + 2, y + 2, 2f, 2f, c.outline)
                }
            }
            HouseObjectArt.GARDEN -> {
                box(3f, 62f, 94f, 30f, c.leaf)
                shape(c.bedroomWall, 10f, 51f, 88f, 51f, 97f, 66f, 3f, 66f)
                plant(24f, 44f, 0.8f); plant(48f, 44f, 0.8f)
                oval(68f, 37f, 22f, 25f, c.sunnyAccent)
                oval(83f, 33f, 12f, 16f, c.sunnyAccent)
                shape(c.sunnyAccent, 70f, 43f, 60f, 31f, 59f, 37f, 68f, 52f)
            }
            HouseObjectArt.SPACE -> {
                box(8f, 86f, 44f, 10f, c.metal)
                shape(c.warmAccent, 10f, 75f, 20f, 48f, 20f, 81f)
                shape(c.warmAccent, 42f, 76f, 36f, 48f, 36f, 82f)
                box(20f, 27f, 18f, 56f, c.porcelain, 4f)
                shape(c.warmAccent, 20f, 30f, 29f, 5f, 38f, 30f)
                oval(24f, 38f, 10f, 13f, c.fabric)
                line(77f, 55f, 63f, 95f, c.metal, 3f)
                line(77f, 55f, 93f, 95f, c.metal, 3f)
                line(77f, 55f, 77f, 91f, c.metal, 3f)
                shape(c.fabricLight, 58f, 47f, 91f, 32f, 95f, 40f, 62f, 56f)
            }
            HouseObjectArt.SCIENCE -> {
                table(top = 51f)
                repeat(4) { i ->
                    box(20f + i * 16, 22f + (i % 2) * 6, 8f, 35f, c.porcelain, 3f)
                    box(21f + i * 16, 43f, 6f, 12f, listOf(c.fabric, c.warmAccent, c.leaf, c.blush)[i], 2f)
                }
                box(14f, 31f, 65f, 5f, c.wood)
                shape(c.fabricLight, 83f, 39f, 89f, 39f, 89f, 51f, 96f, 62f, 78f, 62f, 83f, 51f)
            }
            HouseObjectArt.FLIGHT -> {
                box(4f, 78f, 92f, 7f, c.wood)
                box(16f, 85f, 5f, 11f, c.woodShade); box(80f, 85f, 5f, 11f, c.woodShade)
                shape(c.porcelain, 13f, 45f, 56f, 40f, 83f, 13f, 92f, 13f, 87f, 47f, 98f, 56f, 81f, 67f, 17f, 63f, 7f, 55f)
                shape(c.fabricLight, 36f, 45f, 17f, 18f, 28f, 16f, 64f, 48f)
                shape(c.fabric, 39f, 57f, 23f, 75f, 40f, 75f, 63f, 58f)
                repeat(4) { oval(24f + it * 13, 47f, 6f, 6f, c.fabric) }
                oval(20f, 67f, 5f, 8f, c.outline); oval(78f, 67f, 5f, 8f, c.outline)
            }
            HouseObjectArt.BED -> {
                box(9f, 10f, 80f, 42f, c.wood, 16f)
                box(7f, 17f, 5f, 75f, c.woodShade)
                box(86f, 17f, 5f, 75f, c.woodShade)
                box(25f, 29f, 45f, 20f, c.blush, 5f)
                shape(c.fabricLight, 16f, 47f, 84f, 47f, 96f, 78f, 5f, 78f)
                shape(c.fabric, 9f, 58f, 88f, 58f, 96f, 84f, 5f, 84f)
                box(5f, 77f, 90f, 15f, c.wood, 5f)
                box(3f, 74f, 5f, 24f, c.woodShade); box(91f, 74f, 5f, 24f, c.woodShade)
            }
            HouseObjectArt.NIGHTSTAND -> {
                box(10f, 53f, 80f, 40f, c.wood)
                box(7f, 49f, 86f, 7f, c.woodLight)
                box(17f, 60f, 66f, 12f, c.woodLight)
                oval(46f, 64f, 7f, 3f, c.woodShade)
                line(50f, 44f, 50f, 19f, c.woodShade, 3f)
                oval(34f, 34f, 31f, 15f, c.fabric)
                shape(c.sunnyAccent, 31f, 9f, 69f, 9f, 83f, 30f, 17f, 30f)
            }
            HouseObjectArt.WINDOW -> {
                oval(22f, 12f, 56f, 78f, c.wood)
                oval(27f, 17f, 46f, 68f, c.fabricLight)
                shape(c.leaf, 28f, 68f, 49f, 62f, 72f, 71f, 62f, 82f, 40f, 83f)
                line(50f, 18f, 50f, 84f, c.wood, 3f)
                line(27f, 48f, 73f, 48f, c.wood, 3f)
                line(7f, 8f, 94f, 8f, c.woodShade, 3f)
                shape(c.fabricLight, 10f, 6f, 36f, 6f, 27f, 56f, 32f, 92f, 5f, 92f, 16f, 56f)
                shape(c.fabricLight, 64f, 6f, 90f, 6f, 84f, 56f, 95f, 92f, 68f, 92f, 73f, 56f)
                line(16f, 56f, 27f, 56f, c.sunnyAccent, 3f); line(73f, 56f, 84f, 56f, c.sunnyAccent, 3f)
            }
            HouseObjectArt.WARDROBE -> {
                box(8f, 7f, 84f, 86f, c.wood)
                box(6f, 4f, 88f, 5f, c.woodLight)
                box(14f, 13f, 34f, 58f, c.woodLight)
                box(52f, 13f, 34f, 58f, c.woodLight)
                oval(39f, 48f, 5f, 5f, c.woodShade); oval(56f, 48f, 5f, 5f, c.woodShade)
                box(14f, 77f, 72f, 11f, c.woodLight)
                oval(46f, 81f, 7f, 3f, c.woodShade)
                box(11f, 93f, 6f, 5f, c.woodShade); box(83f, 93f, 6f, 5f, c.woodShade)
            }
            HouseObjectArt.MIRROR -> {
                shape(c.woodShade, 67f, 36f, 75f, 35f, 89f, 98f, 78f, 98f)
                shape(c.wood, 24f, 5f, 79f, 5f, 67f, 96f, 10f, 96f)
                shape(c.fabricLight, 30f, 11f, 70f, 11f, 60f, 89f, 20f, 89f)
                line(35f, 69f, 61f, 29f, c.porcelain, 2f)
            }
            HouseObjectArt.SOFA -> {
                box(16f, 11f, 68f, 55f, c.warmAccent, 18f)
                line(50f, 15f, 50f, 62f)
                box(14f, 62f, 73f, 27f, c.warmAccent, 9f)
                box(5f, 47f, 20f, 38f, c.sunnyAccent, 10f)
                box(77f, 47f, 19f, 38f, c.sunnyAccent, 10f)
                box(14f, 89f, 7f, 7f, c.woodShade); box(80f, 89f, 7f, 7f, c.woodShade)
                line(27f, 71f, 76f, 71f)
            }
            HouseObjectArt.PIGGY_BANK -> {
                table(top = 62f)
                oval(29f, 13f, 44f, 39f, c.blush)
                shape(c.blush, 57f, 17f, 62f, 5f, 69f, 21f)
                oval(63f, 27f, 18f, 14f, c.blush)
                oval(67f, 31f, 2f, 4f, c.outline); oval(73f, 31f, 2f, 4f, c.outline)
                oval(58f, 22f, 3f, 3f, c.outline)
                box(37f, 46f, 6f, 9f, c.blush); box(60f, 46f, 6f, 9f, c.blush)
                line(40f, 17f, 51f, 16f, c.outline, 2f)
            }
            HouseObjectArt.DOOR -> {
                box(9f, 2f, 82f, 95f, c.woodLight)
                box(16f, 8f, 68f, 89f, c.woodShade)
                box(23f, 15f, 54f, 61f, c.wood)
                oval(71f, 59f, 5f, 3f, c.outline)
                box(34f, 23f, 31f, 17f, c.porcelain, 2f)
                shape(c.sunnyAccent, 40f, 28f, 60f, 28f, 57f, 36f, 43f, 36f)
                line(39f, 26f, 42f, 36f, c.outline, 1.2f)
                oval(44f, 37f, 3f, 2f, c.outline); oval(54f, 37f, 3f, 2f, c.outline)
            }
            HouseObjectArt.CALENDAR -> {
                box(10f, 12f, 80f, 81f, c.porcelain)
                box(10f, 10f, 80f, 13f, c.leaf)
                repeat(4) { row -> repeat(5) { col -> box(18f + col * 13, 32f + row * 12, 10f, 9f, c.porcelain) } }
                box(44f, 44f, 10f, 9f, c.sunnyAccent)
                line(34f, 10f, 50f, 2f); line(50f, 2f, 66f, 10f)
            }
            HouseObjectArt.TASK_BOARD -> {
                box(5f, 6f, 90f, 87f, c.wood)
                box(10f, 12f, 80f, 75f, c.woodLight)
                box(18f, 24f, 27f, 38f, c.porcelain)
                box(56f, 36f, 25f, 36f, c.porcelain)
                oval(28f, 20f, 5f, 6f, c.sunnyAccent); oval(65f, 32f, 5f, 6f, c.leaf)
                line(24f, 40f, 38f, 40f, c.woodShade); line(24f, 46f, 37f, 46f, c.woodShade)
            }
            HouseObjectArt.CABINET -> {
                box(4f, 14f, 92f, 74f, c.wood)
                box(2f, 10f, 96f, 7f, c.woodLight)
                box(9f, 24f, 24f, 51f, c.warmAccent)
                box(67f, 24f, 24f, 51f, c.warmAccent)
                box(40f, 58f, 20f, 17f, c.leaf)
                repeat(3) { box(38f + it * 7, 26f, 5f, 27f, if (it == 1) c.fabric else c.leafShade) }
                box(7f, 88f, 5f, 9f, c.woodShade); box(88f, 88f, 5f, 9f, c.woodShade)
                oval(27f, 43f, 2f, 5f, c.outline); oval(72f, 43f, 2f, 5f, c.outline)
            }
            HouseObjectArt.PHONE -> {
                box(16f, 4f, 68f, 91f, c.outline, 7f)
                box(23f, 14f, 54f, 66f, c.fabricLight, 3f)
                oval(45f, 85f, 10f, 4f, c.porcelain)
                line(34f, 36f, 65f, 36f, c.porcelain, 4f)
                line(34f, 47f, 58f, 47f, c.porcelain, 4f)
            }
            HouseObjectArt.FRIDGE -> {
                box(11f, 3f, 78f, 92f, c.metal, 7f)
                box(12f, 7f, 71f, 33f, c.porcelain, 5f)
                box(12f, 42f, 71f, 50f, c.porcelain, 5f)
                box(23f, 22f, 3f, 13f, c.metal, 1f)
                box(23f, 47f, 3f, 17f, c.metal, 1f)
                box(17f, 95f, 6f, 3f, c.metal); box(76f, 95f, 6f, 3f, c.metal)
            }
            HouseObjectArt.SINK, HouseObjectArt.COUNTER -> {
                box(6f, 48f, 88f, 47f, c.leaf)
                box(10f, 54f, 37f, 35f, c.bedroomWall)
                box(53f, 54f, 37f, 35f, c.bedroomWall)
                oval(39f, 63f, 3f, 4f, c.metal); oval(58f, 63f, 3f, 4f, c.metal)
                shape(c.woodLight, 8f, 35f, 91f, 35f, 97f, 47f, 3f, 47f)
                box(3f, 47f, 94f, 5f, c.woodLight)
                if (art == HouseObjectArt.SINK) {
                    oval(22f, 36f, 43f, 9f, c.metal)
                    line(48f, 36f, 48f, 15f, c.metal, 4f)
                    line(48f, 15f, 56f, 15f, c.metal, 4f)
                    line(56f, 15f, 56f, 20f, c.metal, 4f)
                    oval(65f, 32f, 5f, 4f, c.warmAccent)
                } else {
                    box(28f, 34f, 38f, 8f, c.wood)
                    shape(c.warmAccent, 40f, 33f, 58f, 29f, 58f, 36f)
                    line(59f, 31f, 66f, 27f, c.leafShade, 3f)
                }
            }
            HouseObjectArt.STOVE -> {
                box(30f, 3f, 39f, 27f, c.metal)
                shape(c.metal, 30f, 30f, 69f, 30f, 91f, 40f, 91f, 44f, 9f, 44f, 9f, 40f)
                box(9f, 64f, 82f, 31f, c.porcelain, 2f)
                shape(c.metal, 13f, 56f, 87f, 56f, 91f, 64f, 9f, 64f)
                repeat(2) { row -> repeat(2) { col -> oval(24f + col * 36, 57f + row * 4, 17f, 3f, c.outline) } }
                box(20f, 74f, 60f, 17f, c.fabric, 3f)
                line(29f, 72f, 70f, 72f, c.outline, 2f)
                repeat(4) { oval(23f + it * 16, 67f, 4f, 2f, c.metal) }
            }
            HouseObjectArt.DINING_TABLE -> {
                box(3f, 18f, 18f, 37f, c.wood, 3f)
                box(79f, 18f, 18f, 37f, c.wood, 3f)
                box(4f, 57f, 20f, 7f, c.leaf); box(77f, 57f, 20f, 7f, c.leaf)
                line(7f, 64f, 7f, 94f, c.woodShade, 4f); line(91f, 64f, 91f, 94f, c.woodShade, 4f)
                box(27f, 49f, 5f, 46f, c.woodShade); box(67f, 49f, 5f, 46f, c.woodShade)
                box(23f, 42f, 53f, 12f, c.woodLight)
            }
        }
    }
}
