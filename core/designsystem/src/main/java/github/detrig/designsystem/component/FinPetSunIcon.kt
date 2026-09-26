package github.detrig.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme

/** The happiness sun used in the room HUD and item effects. */
@Composable
fun FinPetSunIcon(modifier: Modifier = Modifier) {
    val outline = AppTheme.colors.house.outline
    val sun = AppTheme.colors.house.sunnyAccent

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        val center = Offset(w / 2f, h / 2f)
        repeat(8) { index ->
            val angle = Math.PI * index / 4.0
            val dx = kotlin.math.cos(angle).toFloat()
            val dy = kotlin.math.sin(angle).toFloat()
            drawLine(
                outline,
                center + Offset(dx * w * .36f, dy * h * .36f),
                center + Offset(dx * w * .47f, dy * h * .47f),
                stroke,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(sun, w * .29f, center)
        drawCircle(outline, w * .29f, center, style = Stroke(stroke))
        drawCircle(outline, w * .023f, Offset(w * .41f, h * .46f))
        drawCircle(outline, w * .023f, Offset(w * .59f, h * .46f))
        drawArc(
            outline, 15f, 150f, false,
            Offset(w * .39f, h * .48f), Size(w * .22f, h * .2f),
            style = Stroke(stroke * .75f),
        )
    }
}

@Preview(name = "Иконка счастья", showBackground = true)
@Composable
private fun FinPetSunIconPreview() {
    FinPetTheme { FinPetSunIcon(Modifier.size(25.dp)) }
}
