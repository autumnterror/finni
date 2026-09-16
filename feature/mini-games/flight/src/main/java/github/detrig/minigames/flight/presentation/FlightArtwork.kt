package github.detrig.minigames.flight.presentation

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import github.detrig.minigames.flight.R

internal data class FlightSprite(val bitmap: ImageBitmap, val offset: IntOffset, val size: IntSize)
internal data class FlightArtwork(val sky: ImageBitmap, val pillar: FlightSprite)

/** Вызывается в Default: декодирование и поиск границ прозрачного спрайта вне main. */
internal fun loadFlightArtwork(resources: Resources): FlightArtwork {
    fun decode(id: Int, maxSide: Int) : android.graphics.Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, id, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
        return requireNotNull(BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply {
            inSampleSize = sample
            inScaled = false
        }))
    }
    val column = decode(R.drawable.img_flight_pillar_v2, 2048)
    val pixels = IntArray(column.width * column.height)
    column.getPixels(pixels, 0, column.width, 0, 0, column.width, column.height)
    var left = column.width
    var right = 0
    var top = column.height
    var bottom = 0
    for (y in 0 until column.height) for (x in 0 until column.width) {
        if (pixels[y * column.width + x] ushr 24 >= 128) {
            left = minOf(left, x); right = maxOf(right, x)
            top = minOf(top, y); bottom = maxOf(bottom, y)
        }
    }
    check(left <= right && top <= bottom)
    val croppedColumn = Bitmap.createBitmap(column, left, top, right - left + 1, bottom - top + 1)
    if (croppedColumn !== column) column.recycle()
    return FlightArtwork(
        decode(R.drawable.img_flight_sky_v2, 2048).asImageBitmap(),
        FlightSprite(croppedColumn.asImageBitmap(), IntOffset.Zero, IntSize(croppedColumn.width, croppedColumn.height)),
    )
}
