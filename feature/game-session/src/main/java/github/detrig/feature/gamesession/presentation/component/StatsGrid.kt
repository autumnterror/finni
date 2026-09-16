package github.detrig.feature.gamesession.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.gamesession.R
import github.detrig.feature.gamesession.presentation.GameSessionViewState

@Composable
internal fun StatsGrid(state: GameSessionViewState.Content) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        StatPanel(
            modifier = Modifier.weight(1f),
            title = "Голод",
            valueText = "${state.hunger}/100",
            progress = state.hunger,
            drawableRes = R.drawable.ic_finpet_food,
            iconDescription = "Голод",
            accentColor = AppTheme.colors.metricHunger,
        )
        StatPanel(
            modifier = Modifier.weight(1f),
            title = "Счастье",
            valueText = "${state.happiness}/100",
            progress = state.happiness,
            drawableRes = R.drawable.ic_finpet_pet_smile,
            iconDescription = "Счастье",
            accentColor = AppTheme.colors.metricHappiness,
        )
    }
}
