package github.detrig.feature.room.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface RoomApi {
    /** Показывает обычные условия зоны при возвращении в комнату; ничего не покупает. */
    fun requestZonePreview(zoneId: String)

    /** Непрерывный дом размещает один переданный UI питомца в мировых координатах. */
    @Composable
    fun Content(
        modifier: Modifier = Modifier,
        petContent: @Composable (Modifier) -> Unit = {},
        onMirrorClick: () -> Unit = {},
    )
}
