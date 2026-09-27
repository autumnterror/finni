package github.detrig.feature.room.presentation.component

import android.content.res.Resources
import androidx.compose.ui.geometry.Rect
import github.detrig.feature.room.presentation.model.HouseLayout
import github.detrig.feature.room.domain.furniture.FurnitureVariant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/** Process-lifetime cache shared by the room and its furniture preview. */
internal object RoomSpriteCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loadLock = Any()
    private val cachedSprites = MutableStateFlow<Map<String, RoomSprite>>(emptyMap())
    private var loadJob: Deferred<Map<String, RoomSprite>>? = null
    private val variantMutex = Mutex()

    fun sprites(resources: Resources): StateFlow<Map<String, RoomSprite>> {
        preload(resources)
        return cachedSprites
    }

    fun preload(resources: Resources) {
        getOrCreate(resources)
    }

    suspend fun awaitPreloaded(resources: Resources): Map<String, RoomSprite> =
        getOrCreate(resources).await()

    fun preloadVariants(resources: Resources, variants: Collection<FurnitureVariant>) {
        if (variants.isNotEmpty()) scope.launch { awaitVariants(resources, variants) }
    }

    suspend fun awaitVariants(resources: Resources, variants: Collection<FurnitureVariant>) {
        awaitPreloaded(resources)
        variantMutex.withLock {
            val missing = variants.filter { it.id !in cachedSprites.value }
            if (missing.isEmpty()) return@withLock
            val loaded = withContext(Dispatchers.Default) {
                val metrics = resources.displayMetrics
                val sceneWidth = (metrics.widthPixels.coerceAtLeast(1) * HouseLayout.WORLD_WIDTH).roundToInt()
                val sceneHeight = metrics.heightPixels.coerceAtLeast(1)
                missing.associate { variant ->
                    val placement = HouseLayout.objects.first { it.id == variant.placementId }
                    val bounds = requireNotNull(placement.bounds)
                    variant.id to RoomSprite.load(
                        resources = resources,
                        resource = variant.drawableId,
                        width = (bounds.width * sceneWidth).roundToInt(),
                        height = (bounds.height * sceneHeight).roundToInt(),
                        preserveCanvas = true,
                    )
                }
            }
            cachedSprites.value = cachedSprites.value + loaded
        }
    }

    private fun getOrCreate(resources: Resources): Deferred<Map<String, RoomSprite>> {
        synchronized(loadLock) {
            cachedSprites.value.takeIf { it.isNotEmpty() }?.let { return CompletableDeferred(it) }
            loadJob?.let { return it }

            val job = scope.async(start = CoroutineStart.LAZY) {
                loadAll(resources).also { cachedSprites.value = it }
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

    private fun loadAll(resources: Resources): Map<String, RoomSprite> {
        val metrics = resources.displayMetrics
        val sceneWidth = (metrics.widthPixels.coerceAtLeast(1) * HouseLayout.WORLD_WIDTH).roundToInt()
        val sceneHeight = metrics.heightPixels.coerceAtLeast(1)
        val sprites = HouseLayout.objects.associate { placement ->
            val bounds = requireNotNull(placement.bounds)
            val destination = Rect(
                bounds.left * sceneWidth,
                bounds.top * sceneHeight,
                bounds.right * sceneWidth,
                bounds.bottom * sceneHeight,
            )
            placement.id to RoomSprite.load(
                resources = resources,
                resource = roomObjectAsset(placement.id),
                width = destination.width.roundToInt(),
                height = destination.height.roundToInt(),
            )
        }
        val windowPlacement = HouseLayout.objects.first { it.id == "decor_window" }
        val windowBounds = requireNotNull(windowPlacement.bounds)
        return sprites + ("decor_window_night" to RoomSprite.load(
            resources = resources,
            resource = roomObjectAsset("decor_window_night"),
            width = (windowBounds.width * sceneWidth).roundToInt(),
            height = (windowBounds.height * sceneHeight).roundToInt(),
        ))
    }
}
