package github.detrig.feature.pet.presentation

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.pet.api.ClothingItem
import github.detrig.feature.pet.domain.model.HamsterAppearance
import github.detrig.feature.pet.domain.model.PetProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class ClothingLayer(
    val path: String,
    val z: Int,
    val conditions: Map<String, String>,
) {
    fun fits(appearance: HamsterAppearance): Boolean =
        conditions.all { (key, value) -> appearance.properties()[key] == value }
}

internal data class ClothingDefinition(
    val item: ClothingItem,
    val thumbnail: String,
    val layers: List<ClothingLayer>,
)

internal data class ClothingDrawLayer(val image: ImageBitmap, val z: Int)

private data class EquippedClothingLayers(
    val itemId: String,
    val appearance: HamsterAppearance,
    val layers: List<ClothingDrawLayer>,
)

internal object ClothingArtwork {
    private const val ROOT = "finni_clothing/"
    private val catalogMutex = Mutex()
    private val imageMutex = Mutex()
    @Volatile
    private var catalog: List<ClothingDefinition>? = null
    @Volatile
    private var equippedLayers: Map<String, EquippedClothingLayers> = emptyMap()
    private val imageCache = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            value.width * value.height * 4
    }

    suspend fun definitions(assets: AssetManager): List<ClothingDefinition> = catalogMutex.withLock {
        catalog?.let { return@withLock it }
        withContext(Dispatchers.IO) {
            val json = assets.open(ROOT + "manifest.json").bufferedReader().use { JSONObject(it.readText()) }
            require(json.getInt("schemaVersion") == 3)
            val source = json.getJSONArray("items")
            List(source.length()) { index ->
                val entry = source.getJSONObject(index)
                val layers = entry.getJSONArray("layers")
                ClothingDefinition(
                    item = ClothingItem(
                        id = entry.getString("id"),
                        name = entry.getString("name"),
                        slot = entry.getString("slot"),
                        priceRub = entry.getLong("price"),
                    ),
                    thumbnail = entry.getString("thumbnail"),
                    layers = List(layers.length()) { layerIndex ->
                        val layer = layers.getJSONObject(layerIndex)
                        val conditions = layer.optJSONObject("when")
                        ClothingLayer(
                            path = layer.getString("webp"),
                            z = layer.getInt("z"),
                            conditions = conditions?.keys()?.asSequence()
                                ?.associateWith(conditions::getString).orEmpty(),
                        )
                    },
                )
            }.also { catalog = it }
        }
    }

    suspend fun items(assets: AssetManager): List<ClothingItem> = definitions(assets).map { it.item }

    fun cachedItems(): List<ClothingItem> = catalog?.map { it.item }.orEmpty()

    suspend fun preload(assets: AssetManager, profile: PetProfile?) {
        definitions(assets).forEach { image(assets, it.thumbnail, sampled = false) }
        profile?.let {
            layers(assets, it.clothing.equippedBySlot, it.hamsterAppearance)
            retainEquipped(it)
        }
    }

    fun retainEquipped(profile: PetProfile) {
        val appearance = profile.hamsterAppearance
        val retained = equippedLayers
        equippedLayers = profile.clothing.equippedBySlot.mapNotNull { (slot, itemId) ->
            val layers = retained[slot]?.takeIf {
                it.itemId == itemId && it.appearance == appearance
            }?.layers ?: cachedLayersForItem(itemId, appearance)
            layers?.let { slot to EquippedClothingLayers(itemId, appearance, it) }
        }.toMap()
    }

    suspend fun layers(
        assets: AssetManager,
        equippedBySlot: Map<String, String>,
        appearance: HamsterAppearance,
    ): List<ClothingDrawLayer> {
        val byId = definitions(assets).associateBy { it.item.id }
        return equippedBySlot.values.mapNotNull(byId::get)
            .flatMap { it.layers }
            .filter { it.fits(appearance) }
            .sortedBy(ClothingLayer::z)
            .map { ClothingDrawLayer(image(assets, it.path, sampled = true), it.z) }
    }

    suspend fun layersForItem(
        assets: AssetManager,
        itemId: String,
        appearance: HamsterAppearance,
    ): List<ClothingDrawLayer> = definitions(assets)
        .firstOrNull { it.item.id == itemId }
        ?.layers.orEmpty()
        .filter { it.fits(appearance) }
        .sortedBy(ClothingLayer::z)
        .map { ClothingDrawLayer(image(assets, it.path, sampled = true), it.z) }

    fun cachedLayersForItem(itemId: String, appearance: HamsterAppearance): List<ClothingDrawLayer>? {
        equippedLayers.values.firstOrNull { it.itemId == itemId && it.appearance == appearance }
            ?.let { return it.layers }
        val definition = catalog?.firstOrNull { it.item.id == itemId } ?: return null
        val result = mutableListOf<ClothingDrawLayer>()
        definition.layers.filter { it.fits(appearance) }.sortedBy(ClothingLayer::z).forEach { layer ->
            val bitmap = cachedImage(layer.path, sampled = true) ?: return null
            result += ClothingDrawLayer(bitmap, layer.z)
        }
        return result
    }

    fun cachedThumbnail(itemId: String): ImageBitmap? {
        val path = catalog?.firstOrNull { it.item.id == itemId }?.thumbnail ?: return null
        return cachedImage(path, sampled = false)
    }

    suspend fun thumbnail(assets: AssetManager, itemId: String): ImageBitmap? {
        val path = definitions(assets).firstOrNull { it.item.id == itemId }?.thumbnail ?: return null
        return image(assets, path, sampled = false)
    }

    private suspend fun image(assets: AssetManager, path: String, sampled: Boolean): ImageBitmap =
        imageMutex.withLock {
            val key = "$path:$sampled"
            cachedImage(path, sampled)?.let { return@withLock it }
            withContext(Dispatchers.IO) {
                val bitmap = assets.open(ROOT + path).use { stream ->
                    requireNotNull(BitmapFactory.decodeStream(
                        stream,
                        null,
                        BitmapFactory.Options().apply {
                            inScaled = false
                            inSampleSize = if (sampled) 2 else 1
                        },
                    ))
                }
                bitmap.asImageBitmap().also { synchronized(imageCache) { imageCache.put(key, it) } }
            }
        }

    private fun cachedImage(path: String, sampled: Boolean): ImageBitmap? =
        synchronized(imageCache) { imageCache.get("$path:$sampled") }
}

@Composable
internal fun rememberClothingLayers(
    equippedBySlot: Map<String, String>,
    appearance: HamsterAppearance,
): List<ClothingDrawLayer> {
    val assets = LocalResources.current.assets
    val loadedBySlot = remember(assets, appearance) {
        mutableStateMapOf<String, List<ClothingDrawLayer>>().apply {
            equippedBySlot.forEach { (slot, itemId) ->
                ClothingArtwork.cachedLayersForItem(itemId, appearance)?.let { this[slot] = it }
            }
        }
    }
    equippedBySlot.forEach { (slot, itemId) ->
        key(slot) {
            LaunchedEffect(assets, itemId, appearance) {
                loadedBySlot[slot] = ClothingArtwork.layersForItem(assets, itemId, appearance)
            }
        }
    }
    SideEffect { loadedBySlot.keys.retainAll(equippedBySlot.keys) }
    return equippedBySlot.keys.flatMap { loadedBySlot[it].orEmpty() }.sortedBy(ClothingDrawLayer::z)
}

@Composable
internal fun ClothingThumbnail(itemId: String, modifier: Modifier = Modifier) {
    val assets = LocalResources.current.assets
    val image by produceState<ImageBitmap?>(ClothingArtwork.cachedThumbnail(itemId), assets, itemId) {
        value = ClothingArtwork.thumbnail(assets, itemId)
    }
    image?.let {
        Image(bitmap = it, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
    }
}

@Preview(name = "Вещь из каталога", widthDp = 120, heightDp = 120)
@Composable
private fun ClothingThumbnailPreview() {
    FinPetTheme { ClothingThumbnail("06_denim_vest", Modifier.size(120.dp)) }
}
