package github.detrig.feature.pet.presentation

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalResources
import github.detrig.feature.pet.domain.model.HamsterAppearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DirtLayers(
    val head: ImageBitmap,
    val body: ImageBitmap,
    val ears: ImageBitmap,
    val clothing: Map<String, ImageBitmap>,
)

/** Transparent 1024-square overlays from Finni_Dirt_Shower_Kit. */
internal object DirtArtwork {
    private const val ROOT = "finni_dirt/runtime/webp/"
    private val cache = object : LruCache<String, ImageBitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    fun cachedLayers(
        appearance: HamsterAppearance,
        clothingSources: List<String>,
    ): DirtLayers? = synchronized(cache) {
        DirtLayers(
            head = cache.get("pet/head_${appearance.fur}.webp") ?: return null,
            body = cache.get("pet/body_${appearance.fur}.webp") ?: return null,
            ears = cache.get("pet/ears_${appearance.ears}.webp") ?: return null,
            clothing = clothingSources.distinct().associateWith { source ->
                cache.get("clothing/$source.webp") ?: return null
            },
        )
    }

    suspend fun layers(
        assets: AssetManager,
        appearance: HamsterAppearance,
        clothingSources: List<String>,
    ): DirtLayers = withContext(Dispatchers.IO) {
        DirtLayers(
            head = image(assets, "pet/head_${appearance.fur}.webp"),
            body = image(assets, "pet/body_${appearance.fur}.webp"),
            ears = image(assets, "pet/ears_${appearance.ears}.webp"),
            clothing = clothingSources.distinct().associateWith { source ->
                image(assets, "clothing/$source.webp")
            },
        )
    }

    private fun image(assets: AssetManager, path: String): ImageBitmap {
        synchronized(cache) { cache.get(path)?.let { return it } }
        val bitmap = assets.open(ROOT + path).use { stream ->
            requireNotNull(BitmapFactory.decodeStream(stream, null,
                BitmapFactory.Options().apply { inScaled = false; inSampleSize = 2 }))
        }.asImageBitmap()
        synchronized(cache) { cache.put(path, bitmap) }
        return bitmap
    }
}

@Composable
internal fun rememberDirtLayers(
    appearance: HamsterAppearance,
    clothingLayers: List<ClothingDrawLayer>,
    dirtStage: Int,
): DirtLayers? {
    if (dirtStage <= 0) return null
    val assets = LocalResources.current.assets
    val sources = clothingLayers.map(ClothingDrawLayer::sourceKey)
    val layers by produceState<DirtLayers?>(
        DirtArtwork.cachedLayers(appearance, sources), assets, appearance, sources,
    ) {
        if (value == null) value = DirtArtwork.layers(assets, appearance, sources)
    }
    return layers
}
