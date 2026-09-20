package github.detrig.feature.pet.presentation

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import github.detrig.feature.pet.domain.model.HamsterAppearance
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class HamsterSprite(
    val image: ImageBitmap,
    val x: Int,
    val y: Int,
)

internal data class HamsterLayerRule(
    val id: String,
    val tint: String?,
    val conditions: Map<String, Any>,
)

internal data class HamsterDrawLayer(
    val sprite: HamsterSprite,
    val colorFilter: ColorFilter?,
    val tint: Color?,
)

internal class HamsterAssets(
    val canvasSize: Int,
    val layers: Map<String, HamsterSprite>,
    val palettes: Map<String, Map<String, Color>>,
    val rules: List<HamsterLayerRule>,
    val options: Map<String, Set<String>>,
    val thumbnails: Map<String, ImageBitmap>,
) {
    fun resolve(appearance: HamsterAppearance, blink: Boolean): List<HamsterDrawLayer> {
        val properties = appearance.properties()
        properties.forEach { (key, value) ->
            require(value in options.getValue(key)) { "Invalid $key: $value" }
        }
        val palette = palettes.getValue(appearance.palette)
        return rules
            .filter { rule ->
                rule.conditions.all { (key, expected) ->
                    when (key) {
                        "blink" -> blink == expected
                        "markNot" -> appearance.mark != expected
                        else -> properties[key] == expected
                    }
                }
            }
            .map { rule ->
                val id = PLACEHOLDER.replace(rule.id) { properties.getValue(it.groupValues[1]) }
                HamsterDrawLayer(
                    sprite = layers.getValue(id),
                    colorFilter = rule.tint?.let { tintId ->
                        ColorFilter.tint(palette.getValue(tintId), BlendMode.SrcIn)
                    },
                    tint = rule.tint?.let(palette::getValue),
                )
            }
    }

    fun renderBitmap(
        appearance: HamsterAppearance,
        targetSidePx: Int,
        blink: Boolean,
    ): Bitmap {
        val output = Bitmap.createBitmap(targetSidePx, targetSidePx, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(output)
        val scale = targetSidePx.toFloat() / canvasSize
        resolve(appearance, blink).forEach { layer ->
            val bitmap = layer.sprite.image.asAndroidBitmap()
            val destination = Rect(
                (layer.sprite.x * scale).toInt(),
                (layer.sprite.y * scale).toInt(),
                ((layer.sprite.x + bitmap.width) * scale).toInt(),
                ((layer.sprite.y + bitmap.height) * scale).toInt(),
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = layer.tint?.let { color ->
                    PorterDuffColorFilter(color.toArgb(), PorterDuff.Mode.SRC_IN)
                }
            }
            canvas.drawBitmap(bitmap, null, destination, paint)
        }
        return output
    }

    /** Checks the same alpha silhouette that is rendered on screen. */
    fun contains(
        appearance: HamsterAppearance,
        position: Offset,
        containerSize: Size,
        blink: Boolean = false,
    ): Boolean {
        if (containerSize.width <= 0f || containerSize.height <= 0f ||
            position.x !in 0f..containerSize.width || position.y !in 0f..containerSize.height
        ) return false

        val canvasX = (position.x / containerSize.width * canvasSize).toInt()
        val canvasY = (position.y / containerSize.height * canvasSize).toInt()
        return resolve(appearance, blink).any { layer ->
            val bitmap = layer.sprite.image.asAndroidBitmap()
            val localX = canvasX - layer.sprite.x
            val localY = canvasY - layer.sprite.y
            localX in 0 until bitmap.width && localY in 0 until bitmap.height &&
                android.graphics.Color.alpha(bitmap.getPixel(localX, localY)) >= HIT_ALPHA
        }
    }

    private companion object {
        const val HIT_ALPHA = 96
        val PLACEHOLDER = Regex("\\{(\\w+)\\}")
    }
}

/** Process-lifetime cache shared by onboarding, the room and mini-games. */
internal object HamsterAssetsCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadLock = Any()
    private val cachedAssets = MutableStateFlow<HamsterAssets?>(null)
    private var loadJob: Deferred<HamsterAssets>? = null

    fun assets(assetManager: AssetManager): StateFlow<HamsterAssets?> {
        preload(assetManager)
        return cachedAssets
    }

    fun preload(assetManager: AssetManager) {
        getOrCreate(assetManager)
    }

    suspend fun awaitPreloaded(assetManager: AssetManager): HamsterAssets =
        getOrCreate(assetManager).await()

    private fun getOrCreate(assetManager: AssetManager): Deferred<HamsterAssets> {
        synchronized(loadLock) {
            cachedAssets.value?.let { return CompletableDeferred(it) }
            loadJob?.let { return it }

            val job = scope.async(start = CoroutineStart.LAZY) {
                loadHamsterAssets(assetManager).also { cachedAssets.value = it }
            }
            loadJob = job
            job.invokeOnCompletion { cause ->
                if (cause != null) {
                    synchronized(loadLock) {
                        if (loadJob === job) loadJob = null
                    }
                }
            }
            job.start()
            return job
        }
    }
}

internal suspend fun loadHamsterAssets(
    assetManager: AssetManager,
    root: String = "finni_hamster",
): HamsterAssets = withContext(Dispatchers.IO) {
    val json = assetManager.open("$root/manifest.json").bufferedReader().use { reader ->
        JSONObject(reader.readText())
    }
    require(json.getInt("schemaVersion") == 1)

    val canvas = json.getJSONObject("canvas")
    require(canvas.getInt("width") == canvas.getInt("height"))

    val sourceLayers = json.getJSONObject("layers")
    val layers = sourceLayers.keys().asSequence().associateWith { id ->
        val item = sourceLayers.getJSONObject(id)
        val bitmap = assetManager.open("$root/${item.getString("file")}").use { stream ->
            requireNotNull(
                BitmapFactory.decodeStream(
                    stream,
                    null,
                    BitmapFactory.Options().apply { inScaled = false },
                ),
            )
        }
        require(bitmap.width == item.getInt("width"))
        require(bitmap.height == item.getInt("height"))
        HamsterSprite(
            image = bitmap.asImageBitmap(),
            x = item.getInt("x"),
            y = item.getInt("y"),
        )
    }

    val sourcePalettes = json.getJSONObject("palettes")
    val palettes = sourcePalettes.keys().asSequence().associateWith { id ->
        val palette = sourcePalettes.getJSONObject(id)
        palette.keys().asSequence().associateWith { key ->
            Color(android.graphics.Color.parseColor(palette.getString(key)))
        }
    }

    val sourceOptions = json.getJSONObject("options")
    val options = sourceOptions.keys().asSequence().associateWith { key ->
        sourceOptions.getJSONObject(key).keys().asSequence().toSet()
    }

    val renderOrder = json.getJSONArray("renderOrder")
    val rules = List(renderOrder.length()) { index ->
        val item = renderOrder.getJSONObject(index)
        val conditions = item.optJSONObject("when")
        HamsterLayerRule(
            id = item.getString("id"),
            tint = item.optString("tint").takeIf { it.isNotEmpty() },
            conditions = conditions?.keys()?.asSequence()?.associateWith { key ->
                conditions.get(key)
            }.orEmpty(),
        )
    }

    val thumbnails = loadHamsterThumbnails(assetManager, root)
    HamsterAssets(
        canvasSize = canvas.getInt("width"),
        layers = layers,
        palettes = palettes,
        rules = rules,
        options = options,
        thumbnails = thumbnails,
    )
}

private fun loadHamsterThumbnails(
    assetManager: AssetManager,
    root: String,
): Map<String, ImageBitmap> {
    val result = mutableMapOf<String, ImageBitmap>()
    assetManager.list("$root/assets/thumbnails")?.forEach { filename ->
        val key = filename.removeSuffix(".webp")
        val bitmap = assetManager.open("$root/assets/thumbnails/$filename").use { stream ->
            requireNotNull(BitmapFactory.decodeStream(stream))
        }
        result[key] = bitmap.asImageBitmap()
    }
    return result
}

@Composable
internal fun rememberHamsterAssets(): HamsterAssets? {
    val assetManager = LocalContext.current.assets
    val state by HamsterAssetsCache.assets(assetManager).collectAsState()
    return state
}

@Composable
internal fun rememberHamsterBlink(): Boolean {
    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3_800)
            blink = true
            delay(130)
            blink = false
        }
    }
    return blink
}

@Composable
internal fun HamsterPreview(
    assets: HamsterAssets,
    appearance: HamsterAppearance,
    modifier: Modifier = Modifier,
    blink: Boolean = false,
) {
    val drawLayers = remember(assets, appearance, blink) {
        assets.resolve(appearance, blink)
    }
    Canvas(modifier.aspectRatio(1f)) {
        val factor = minOf(size.width, size.height) / assets.canvasSize
        val dx = (size.width - assets.canvasSize * factor) / 2f
        val dy = (size.height - assets.canvasSize * factor) / 2f
        drawLayers.forEach { layer ->
            drawImage(
                image = layer.sprite.image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(layer.sprite.image.width, layer.sprite.image.height),
                dstOffset = IntOffset(
                    (dx + layer.sprite.x * factor).toInt(),
                    (dy + layer.sprite.y * factor).toInt(),
                ),
                dstSize = IntSize(
                    (layer.sprite.image.width * factor).toInt(),
                    (layer.sprite.image.height * factor).toInt(),
                ),
                colorFilter = layer.colorFilter,
                filterQuality = FilterQuality.High,
            )
        }
    }
}
