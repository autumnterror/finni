package github.detrig.feature.room.presentation.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.data.catalog.RoomZoneCatalog
import github.detrig.feature.room.domain.interactor.ResolveRoomZoneAccessInteractor
import github.detrig.feature.room.domain.model.RoomData
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.domain.model.RoomZone
import github.detrig.feature.room.presentation.RoomContent
import github.detrig.feature.room.presentation.RoomViewState
import github.detrig.feature.room.presentation.mapper.toRoomZones

internal object RoomPreviewData {
    val state: RoomViewState.Content
        get() {
            val progress = RoomProgress(500, 1, emptySet(), 500, 0, 75, 80)
            val resolveAccess = ResolveRoomZoneAccessInteractor()
            val roomData = RoomData(
                RoomZoneCatalog().zones.map { RoomZone(it, resolveAccess(it, progress)) },
                progress,
            )
            return RoomViewState.Content(roomData.toRoomZones(), progress)
        }
}

@Preview(name = "Комната", widthDp = 360, heightDp = 740)
@Preview(name = "Крупный текст", widthDp = 360, heightDp = 740, fontScale = 1.5f)
@Composable
private fun RoomPreview() {
    FinPetTheme {
        RoomContent(RoomPreviewData.state, {}, petContent = { modifier ->
            Box(modifier, contentAlignment = Alignment.Center) {
                Text("Питомец", color = AppTheme.colors.onRoomBackground)
            }
        })
    }
}
