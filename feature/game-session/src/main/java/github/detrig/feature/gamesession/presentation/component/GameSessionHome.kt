package github.detrig.feature.gamesession.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.designsystem.theme.AppTheme

@Composable
internal fun GameSessionHome(
    modifier: Modifier = Modifier,
    roomContent: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.roomBackground),
    ) {
        roomContent(Modifier.fillMaxSize())
    }
}
