package github.detrig.feature.room.presentation.component

import android.graphics.BitmapFactory
import android.content.res.Resources
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import github.detrig.feature.room.domain.surface.SurfaceCatalog
import github.detrig.feature.room.domain.surface.SurfaceVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Small card bitmaps stay warm; full resolution images are bounded to avoid keeping the catalog in RAM. */
internal object RoomSurfaceCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val thumbnailMutex = Mutex()
    private val fullMutex = Mutex()
    private val mutableThumbnails = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    private val mutableFull = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, ImageBitmap>> = mutableThumbnails
    val full: StateFlow<Map<String, ImageBitmap>> = mutableFull
    private val fullOrder = ArrayDeque<String>()

    fun preloadThumbnails(resources: Resources, catalog: SurfaceCatalog) {
        scope.launch {
            catalog.bySlot.values.forEach { awaitThumbnails(resources, it) }
        }
    }

    suspend fun awaitThumbnails(resources: Resources, variants: List<SurfaceVariant>) = thumbnailMutex.withLock {
        val missing = variants.filterNot { it.id in mutableThumbnails.value }
        if (missing.isEmpty()) return@withLock
        val loaded = withContext(Dispatchers.IO) {
            missing.mapNotNull { variant ->
                decode(resources, variant.drawableId, sampleSize = 4)?.let { variant.id to it }
            }.toMap()
        }
        mutableThumbnails.value = mutableThumbnails.value + loaded
    }

    suspend fun awaitFull(resources: Resources, variants: Collection<SurfaceVariant>) = fullMutex.withLock {
        for (variant in variants.distinctBy(SurfaceVariant::id)) {
            if (variant.id in mutableFull.value) continue
            val bitmap = withContext(Dispatchers.IO) { decode(resources, variant.drawableId, sampleSize = 1) } ?: continue
            val current = mutableFull.value.toMutableMap()
            current[variant.id] = bitmap
            fullOrder.addLast(variant.id)
            while (fullOrder.size > MAX_FULL_BITMAPS) current.remove(fullOrder.removeFirst())
            mutableFull.value = current
        }
    }

    private fun decode(resources: Resources, drawableId: Int, sampleSize: Int): ImageBitmap? {
        val options = BitmapFactory.Options().apply {
            inScaled = false
            inSampleSize = sampleSize
        }
        return BitmapFactory.decodeResource(resources, drawableId, options)?.asImageBitmap()
    }

    private const val MAX_FULL_BITMAPS = 16
}
