package github.detrig.minigames.fishing.presentation

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class FishingArt(val images: Map<String, ImageBitmap>) {
    operator fun get(id: String): ImageBitmap = requireNotNull(images[id]) { "Missing fishing image: $id" }
    companion object {
        private val ids = listOf("background", "boat",
            "fish_crucian", "fish_roach", "fish_perch", "fish_trout", "fish_catfish", "fish_carp", "flora_left", "flora_right")
        suspend fun load(context: Context): FishingArt = withContext(Dispatchers.IO) {
            FishingArt(ids.associateWith { id ->
                val path = "fishing/art/" + id + ".png"
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
                val target = if (id == "background") 1080 else 640
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= target) sample *= 2
                val options = BitmapFactory.Options().apply { inScaled = false; inSampleSize = sample }
                requireNotNull(context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }).asImageBitmap()
            })
        }
    }
}
