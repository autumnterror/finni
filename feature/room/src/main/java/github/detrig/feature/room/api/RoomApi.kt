package github.detrig.feature.room.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface RoomApi {
    /** Blocks until the process-wide room sprites are ready for the first frame. */
    suspend fun preloadAssets()

    /** Показывает обычные условия зоны при возвращении в комнату; ничего не покупает. */
    fun requestZonePreview(zoneId: String)

    /** Непрерывный дом размещает один переданный UI питомца в мировых координатах. */
    @Composable
    fun Content(
        modifier: Modifier = Modifier,
        petContent: @Composable (Modifier) -> Unit = {},
        onMirrorClick: () -> Unit = {},
        onPhoneClick: () -> Unit = {},
        active: Boolean = true,
    )
}
