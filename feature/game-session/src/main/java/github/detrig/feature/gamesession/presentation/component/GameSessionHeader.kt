package github.detrig.feature.gamesession.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.gamesession.presentation.GameSessionViewState

@Composable
internal fun GameSessionHeader(state: GameSessionViewState.Content) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        ) {
            Text(
                text = "FinPet",
                style = AppTheme.typography.brand,
                color = AppTheme.colors.textPrimary,
            )
            Text(
                text = state.levelText,
                style = AppTheme.typography.body,
                color = AppTheme.colors.textSecondary,
            )
            Text(
                text = state.nextAllowanceText,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
        }
        BalanceBadge(balanceText = state.balanceText)
    }
}
