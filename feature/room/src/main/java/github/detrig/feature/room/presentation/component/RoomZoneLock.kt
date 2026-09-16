package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import github.detrig.feature.room.R

@Composable
internal fun RoomZoneLock(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_room_zone_lock),
        contentDescription = null,
        modifier = modifier,
    )
}
