package github.detrig.feature.room.presentation.component

import android.content.res.Resources
import androidx.compose.ui.geometry.Rect
import github.detrig.feature.room.presentation.model.HouseLayout
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/** Process-lifetime cache for the main room only. */
internal object RoomSpriteCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loadLock = Any()
    private val cachedSprites = MutableStateFlow<Map<String, RoomSprite>>(emptyMap())
    private var loadJob: Deferred<Map<String, RoomSprite>>? = null

    fun sprites(resources: Resources): StateFlow<Map<String, RoomSprite>> {
        preload(resources)
        return cachedSprites
    }

    fun preload(resources: Resources) {
        getOrCreate(resources)
    }

    suspend fun awaitPreloaded(resources: Resources): Map<String, RoomSprite> =
        getOrCreate(resources).await()

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
        return HouseLayout.objects.associate { placement ->
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
    }
}
