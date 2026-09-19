package github.detrig.feature.room.presentation.component

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntRect
import github.detrig.feature.room.R

/** Drawing and picking share the same alpha and content bounds. */
internal class RoomSprite private constructor(
    val image: ImageBitmap,
    val content: IntRect,
    private val hitMask: LongArray,
) {
    fun contains(position: Offset, destination: Rect): Boolean {
        if (!destination.contains(position)) return false
        val x = (content.left + (position.x - destination.left) / destination.width * content.width)
            .toInt().coerceIn(content.left, content.right - 1)
        val y = (content.top + (position.y - destination.top) / destination.height * content.height)
            .toInt().coerceIn(content.top, content.bottom - 1)
        val index = y * image.width + x
        return (hitMask[index ushr 6] and (1L shl (index and 63))) != 0L
    }

    companion object {
        private const val HIT_ALPHA = 96
        private const val DECODE_HEADROOM = 1.1f

        fun load(resources: Resources, @DrawableRes resource: Int, width: Int, height: Int): RoomSprite {
            val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(resources, resource, dimensions)
            var sample = 1
            // The press animation peaks at 1.045x, so 10% keeps the rendered sprite sharp
            // without retaining source-sized bitmaps in the long-lived room cache.
            while (dimensions.outWidth / (sample * 2) >= width * DECODE_HEADROOM &&
                dimensions.outHeight / (sample * 2) >= height * DECODE_HEADROOM
            ) sample *= 2
            val bitmap = requireNotNull(BitmapFactory.decodeResource(resources, resource,
                BitmapFactory.Options().apply { inScaled = false; inSampleSize = sample },
            ))
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            var left = bitmap.width
            var top = bitmap.height
            var right = -1
            var bottom = -1
            pixels.forEachIndexed { index, color ->
                if ((color ushr 24) >= HIT_ALPHA) {
                    val x = index % bitmap.width
                    val y = index / bitmap.width
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
            }
            require(right >= left && bottom >= top) { "Room sprite has no visible content: $resource" }
            val content = IntRect(
                (left - 2).coerceAtLeast(0),
                (top - 2).coerceAtLeast(0),
                (right + 3).coerceAtMost(bitmap.width),
                (bottom + 3).coerceAtMost(bitmap.height),
            )
            val contentPixels = pixels.crop(content, bitmap.width)
            val contentBitmap = Bitmap.createBitmap(
                content.width,
                content.height,
                Bitmap.Config.ARGB_8888,
            ).apply {
                setPixels(contentPixels, 0, content.width, 0, 0, content.width, content.height)
            }
            bitmap.recycle()

            // One bit per rendered pixel preserves the exact alpha hit area without a BooleanArray.
            val hitMask = LongArray((contentPixels.size + Long.SIZE_BITS - 1) / Long.SIZE_BITS)
            contentPixels.forEachIndexed { index, color ->
                if ((color ushr 24) >= HIT_ALPHA) {
                    hitMask[index ushr 6] = hitMask[index ushr 6] or (1L shl (index and 63))
                }
            }
            return RoomSprite(
                image = contentBitmap.asImageBitmap(),
                content = IntRect(0, 0, content.width, content.height),
                hitMask = hitMask,
            )
        }
    }
}

private fun IntArray.crop(content: IntRect, sourceWidth: Int): IntArray {
    val cropped = IntArray(content.width * content.height)
    repeat(content.height) { row ->
        copyInto(
            destination = cropped,
            destinationOffset = row * content.width,
            startIndex = (content.top + row) * sourceWidth + content.left,
            endIndex = (content.top + row) * sourceWidth + content.right,
        )
    }
    return cropped
}

@DrawableRes
internal fun roomObjectAsset(id: String): Int = when (id) {
    "decor_window" -> R.drawable.room_decor_window
    "decor_nightstand" -> R.drawable.room_decor_nightstand
    "decor_mirror" -> R.drawable.room_decor_mirror
    "decor_sofa" -> R.drawable.room_decor_sofa
    "decor_coffee_table" -> R.drawable.room_decor_coffee_table
    "decor_cabinet" -> R.drawable.room_decor_cabinet
    "decor_shelf" -> R.drawable.room_decor_shelf
    "decor_notice_board" -> R.drawable.room_decor_notice_board
    "decor_stove" -> R.drawable.room_decor_stove
    "flight" -> R.drawable.room_object_flight
    "music" -> R.drawable.room_object_music
    "fishing" -> R.drawable.room_object_fishing
    "drawing" -> R.drawable.room_object_drawing
    "ball" -> R.drawable.room_object_ball
    "bed" -> R.drawable.room_object_bed
    "wardrobe" -> R.drawable.room_object_wardrobe
    "phone" -> R.drawable.room_object_phone
    "calendar" -> R.drawable.room_object_calendar
    "task_board" -> R.drawable.room_object_task_board
    "piggy_bank" -> R.drawable.room_object_piggy_bank
    "fridge" -> R.drawable.room_object_fridge
    "sink" -> R.drawable.room_object_sink
    "dining_table" -> R.drawable.room_object_dining_table
    "bowls" -> R.drawable.room_object_bowls
    else -> error("Unknown room object: $id")
}
