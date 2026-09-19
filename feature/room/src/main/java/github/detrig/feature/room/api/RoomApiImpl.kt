package github.detrig.feature.room.api

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.feature.room.presentation.RoomScreen
import github.detrig.feature.room.navigation.RoomPreviewRequests
import github.detrig.feature.room.presentation.component.RoomSpriteCache

internal class RoomApiImpl(
    private val requests: RoomPreviewRequests,
    resources: Resources,
) : RoomApi {
    init {
        RoomSpriteCache.preload(resources)
    }

    override fun requestZonePreview(zoneId: String) = requests.request(zoneId)
    @Composable
    override fun Content(modifier: Modifier, petContent: @Composable (Modifier) -> Unit) {
        RoomScreen(modifier = modifier, petContent = petContent, previewRequests = requests)
    }
}
