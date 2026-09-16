package github.detrig.minigames.fishing.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import github.detrig.designsystem.theme.FinPetColors
import github.detrig.minigames.fishing.domain.ObjectKind

/** Простые векторные предметы: общая палитра, читаемый силуэт и отдельный знак опасности. */
internal fun DrawScope.fishingObject(kind: ObjectKind, at: Offset, width: Float, colors: FinPetColors, warning: Boolean = false) {
    val r = width / 2
    val outline = colors.roomObjectBorder
    when (kind) {
        ObjectKind.BOOT -> {
            val boot = Path().apply {
                moveTo(at.x - r * .8f, at.y - r)
                lineTo(at.x + r * .1f, at.y - r)
                lineTo(at.x + r * .1f, at.y + r * .2f)
                cubicTo(at.x + r * 1.1f, at.y, at.x + r * 1.2f, at.y + r * .8f, at.x + r * .7f, at.y + r)
                lineTo(at.x - r * .8f, at.y + r)
                close()
            }
            drawPath(boot, Brush.linearGradient(listOf(colors.actionPrimary, colors.roomObjectBorder),
                at - Offset(r, r), at + Offset(r, r)))
            drawPath(boot, outline, style = Stroke(width * .045f))
            drawLine(outline, at + Offset(-r * .75f, r * .72f), at + Offset(r * .7f, r * .72f), width * .065f, StrokeCap.Round)
            drawLine(colors.onActionPrimary.copy(alpha = .65f), at + Offset(-r * .5f, -r * .7f),
                at + Offset(-r * .5f, r * .1f), width * .09f, StrokeCap.Round)
        }
        ObjectKind.CAN -> rotate(-18f, at) {
            drawRoundRect(Brush.horizontalGradient(listOf(colors.borderDefault, colors.surfaceElevated, colors.textSecondary), at.x - r, at.x + r), at - Offset(r * .65f, r), Size(r * 1.3f, r * 2),
                androidx.compose.ui.geometry.CornerRadius(r * .18f))
            drawRoundRect(outline, at - Offset(r * .65f, r), Size(r * 1.3f, r * 2),
                androidx.compose.ui.geometry.CornerRadius(r * .18f), style = Stroke(width * .045f))
            drawOval(outline, at - Offset(r * .65f, r), Size(r * 1.3f, r * .35f), style = Stroke(width * .04f))
            drawOval(colors.onActionPrimary, at - Offset(r * .25f, r * .93f), Size(r * .5f, r * .2f))
            drawLine(outline.copy(alpha = .35f), at + Offset(-r * .45f, -r * .3f), at + Offset(r * .45f, -r * .3f), width * .035f)
            drawLine(outline.copy(alpha = .35f), at + Offset(-r * .45f, r * .45f), at + Offset(r * .45f, r * .45f), width * .035f)
        }
        ObjectKind.BOMB -> {
            drawLine(outline, at + Offset(r * .25f, -r * .8f), at + Offset(r * .6f, -r * 1.25f), width * .12f, StrokeCap.Round)
            drawCircle(if (warning) colors.statusWarning.accent else outline, r, at)
            drawCircle(colors.onActionPrimary.copy(alpha = .2f), r * .26f, at + Offset(-r * .3f, -r * .4f))
            val mark = Path().apply {
                moveTo(at.x, at.y - r * .65f)
                lineTo(at.x - r * .6f, at.y + r * .5f)
                lineTo(at.x + r * .6f, at.y + r * .5f)
                close()
            }
            drawPath(mark, colors.currencyContainer)
            drawLine(outline, at - Offset(0f, r * .24f), at + Offset(0f, r * .07f), width * .065f, StrokeCap.Round)
            drawCircle(outline, width * .036f, at + Offset(0f, r * .3f))
        }
    }
}
