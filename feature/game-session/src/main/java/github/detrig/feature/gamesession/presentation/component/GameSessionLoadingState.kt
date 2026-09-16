package github.detrig.feature.gamesession.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import github.detrig.designsystem.theme.AppTheme

@Composable
internal fun GameSessionLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        CircularProgressIndicator(color = AppTheme.colors.actionPrimary)
        Text(
            text = "Загружаем игровую сессию",
            color = AppTheme.colors.textSecondary,
            style = AppTheme.typography.body,
        )
    }
}
