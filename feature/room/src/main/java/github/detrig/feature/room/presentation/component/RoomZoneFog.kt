package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import github.detrig.designsystem.theme.AppTheme

/** Туман заменяет предмет целиком: скрытое изображение даже не рисуется. */
@Composable
internal fun RoomZoneFog(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors.house
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawOval(colors.fogShade.copy(alpha = 0.45f), Offset(w * 0.02f, h * 0.72f), Size(w * 0.96f, h * 0.23f))
        drawOval(colors.fogShade, Offset(w * 0.03f, h * 0.31f), Size(w * 0.94f, h * 0.55f))
        drawOval(colors.fog, Offset(w * 0.03f, h * 0.35f), Size(w * 0.49f, h * 0.39f))
        drawOval(colors.fog, Offset(w * 0.20f, h * 0.17f), Size(w * 0.52f, h * 0.61f))
        drawOval(colors.fog, Offset(w * 0.54f, h * 0.32f), Size(w * 0.43f, h * 0.44f))
        drawOval(colors.fog, Offset(w * 0.13f, h * 0.49f), Size(w * 0.77f, h * 0.35f))
    }
}
