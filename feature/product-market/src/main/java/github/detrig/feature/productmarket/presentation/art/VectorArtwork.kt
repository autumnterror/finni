package github.detrig.feature.productmarket.presentation.art

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import github.detrig.designsystem.theme.AppTheme
import github.detrig.products.ProductKind
import kotlin.math.sin

internal enum class VectorFill { PAPER, INK, NONE }
internal enum class VectorPart { STILL, BODY, LEG_A, LEG_B, WHEEL_A, WHEEL_B }
internal class VectorElement(
    pathData: String, val fill: VectorFill, val stroke: Float, val part: VectorPart,
) {
    val path: Path = PathParser().parsePathString(pathData).toPath()
}
internal data class VectorArtwork(val width: Float, val height: Float, val elements: List<VectorElement>)

internal fun productArtwork(kind: ProductKind): VectorArtwork = when (kind) {
    ProductKind.GROATS -> MarketArtwork.groats
    ProductKind.CARROT -> MarketArtwork.carrot
    ProductKind.APPLE -> MarketArtwork.apple
    ProductKind.BERRIES -> MarketArtwork.berries
    ProductKind.CRACKERS -> MarketArtwork.crackers
    ProductKind.MILK -> MarketArtwork.milk
    ProductKind.YOGURT -> MarketArtwork.yogurt
    ProductKind.READY_MEAL -> MarketArtwork.readyMeal
}

@Composable
internal fun MarketVector(art: VectorArtwork, modifier: Modifier = Modifier, activeSeconds: Double = 0.0) {
    val ink = AppTheme.colors.textPrimary
    val paper = AppTheme.colors.surfaceBase
    Canvas(modifier) {
        val factor = minOf(size.width / art.width, size.height / art.height)
        translate((size.width - art.width * factor) / 2, (size.height - art.height * factor) / 2) {
            scale(factor, factor, Offset.Zero) {
                art.elements.forEach { element ->
                    withTransform({
                        val stride = sin(activeSeconds * 10).toFloat()
                        when (element.part) {
                            VectorPart.BODY -> translate(0f, stride * 1.4f)
                            VectorPart.LEG_A -> rotate(stride * 12, Offset(78f, 150f))
                            VectorPart.LEG_B -> rotate(-stride * 12, Offset(95f, 150f))
                            VectorPart.WHEEL_A -> rotate((activeSeconds * 150 % 360).toFloat(), Offset(152f,178f))
                            VectorPart.WHEEL_B -> rotate((activeSeconds * 150 % 360).toFloat(), Offset(208f,178f))
                            VectorPart.STILL -> Unit
                        }
                    }) { drawElement(element, ink, paper) }
                }
            }
        }
    }
}

private fun DrawScope.drawElement(element: VectorElement, ink: Color, paper: Color) {
    if (element.fill != VectorFill.NONE) drawPath(element.path, if (element.fill == VectorFill.PAPER) paper else ink)
    if (element.stroke > 0) drawPath(element.path, ink,
        style = Stroke(element.stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
