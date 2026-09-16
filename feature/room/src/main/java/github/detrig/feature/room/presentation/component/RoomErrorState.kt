package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.room.R

@Composable
internal fun RoomErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.room_error), color = AppTheme.colors.onRoomBackground)
        Spacer(Modifier.height(AppTheme.spacing.lg))
        Button(onClick = onRetry) { Text(stringResource(R.string.room_retry)) }
    }
}
