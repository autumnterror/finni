package github.detrig.feature.gamesession.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun PixelImage(
    @DrawableRes drawableRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp? = 24.dp,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val image = ImageBitmap.imageResource(id = drawableRes)
    val painter = remember(image) {
        BitmapPainter(
            image = image,
            filterQuality = FilterQuality.None,
        )
    }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = if (size != null) modifier.size(size) else modifier,
        contentScale = contentScale,
    )
}
