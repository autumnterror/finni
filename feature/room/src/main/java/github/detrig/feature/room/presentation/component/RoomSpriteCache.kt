package github.detrig.feature.room.presentation.component

import android.content.res.Resources
import androidx.compose.ui.geometry.Rect
import github.detrig.feature.room.presentation.model.HouseLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Process-lifetime cache for the main room only. */
internal object RoomSpriteCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loadLock = Any()
    private val cachedSprites = MutableStateFlow<Map<String, RoomSprite>>(emptyMap())
    private var isLoading = false

    fun sprites(resources: Resources): StateFlow<Map<String, RoomSprite>> {
        preload(resources)
        return cachedSprites
    }

    fun preload(resources: Resources) {
        synchronized(loadLock) {
            if (cachedSprites.value.isNotEmpty() || isLoading) return
            isLoading = true
        }
        scope.launch {
            val loaded = runCatching { loadAll(resources) }
            synchronized(loadLock) {
                loaded.onSuccess { cachedSprites.value = it }
                isLoading = false
            }
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
