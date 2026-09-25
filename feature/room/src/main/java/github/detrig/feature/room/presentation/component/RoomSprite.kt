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
    "decor_rug_bedroom" -> R.drawable.room_rug_bedroom
    "decor_rug_living" -> R.drawable.room_rug_living
    "decor_rug_kitchen" -> R.drawable.room_rug_kitchen
    "decor_window" -> R.drawable.room_window
    "decor_window_night" -> R.drawable.room_window_night
    "decor_bedside_table" -> R.drawable.room_bedside_table
    "decor_lamp" -> R.drawable.room_lamp
    "decor_mirror" -> R.drawable.room_standing_mirror
    "decor_sofa" -> R.drawable.room_sofa
    "decor_coffee_table" -> R.drawable.room_coffee_table
    "decor_cabinet" -> R.drawable.room_green_cabinet
    "decor_shelf_phone" -> R.drawable.room_shelf_phone
    "decor_plant" -> R.drawable.room_plant
    "decor_notice_board" -> R.drawable.room_notice_board
    "decor_range_hood" -> R.drawable.room_range_hood
    "decor_stove" -> R.drawable.room_stove
    "decor_shelf_airplane" -> R.drawable.room_shelf_airplane
    "decor_shelf_keyboard" -> R.drawable.room_shelf_keyboard
    "decor_shelf_fishing" -> R.drawable.room_shelf_fishing
    "flight" -> R.drawable.room_toy_airplane
    "music" -> R.drawable.room_toy_keyboard
    "fishing" -> R.drawable.room_toy_fishing
    "drawing" -> R.drawable.room_easel
    "ball" -> R.drawable.room_toy_bin
    "bed" -> R.drawable.room_bed
    "wardrobe" -> R.drawable.room_wardrobe
    "piggy_bank" -> R.drawable.room_piggy_bank
    "phone" -> R.drawable.room_phone_charging_cluster
    "calendar" -> R.drawable.room_calendar
    "task_board" -> R.drawable.room_open_book
    "decor_pencil" -> R.drawable.room_pencil
    "fridge" -> R.drawable.room_fridge_foreground
    "sink" -> R.drawable.room_kitchen_cabinet
    "decor_cutting_board" -> R.drawable.room_cutting_board_carrot
    "decor_chair" -> R.drawable.room_chair
    "dining_table" -> R.drawable.room_dining_table
    else -> error("Unknown room object: $id")
}
