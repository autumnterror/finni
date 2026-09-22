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
        petName: String = "",
        canShowDialogs: Boolean = true,
        petContent: @Composable (Modifier) -> Unit = {},
        petPortrait: @Composable (Modifier) -> Unit = {},
        onMirrorClick: () -> Unit = {},
        onPhoneClick: () -> Unit = {},
        onFoodClick: () -> Unit = {},
        onFeedingClick: () -> Unit = {},
        tableFoodContent: @Composable (Modifier) -> Unit = {},
        active: Boolean = true,
        /** Optional object id to bring into view without changing room state. */
        focusObjectId: String? = null,
        /** Optional object id to anchor the injected pet to while this scene is shown. */
        petAnchorObjectId: String? = null,
        /** Layer for the injected pet; values below the dining table keep it seated behind it. */
        petZIndex: Float = 3f,
        /** Optional vertical baseline in scene coordinates for seated poses. */
        petBaselineFraction: Float? = null,
    )
}
