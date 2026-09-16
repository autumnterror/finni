package github.detrig.feature.gamesession.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetProgressIndicator
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.gamesession.R
import github.detrig.feature.gamesession.presentation.GameSessionViewState

@Composable
internal fun StatusHud(
    state: GameSessionViewState.Content,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        StatusIndicator(
            drawableRes = R.drawable.ic_finpet_status_health,
            iconDescription = "Здоровье",
            progress = state.health,
            accentColor = AppTheme.colors.metricHealth,
        )
        StatusIndicator(
            drawableRes = R.drawable.ic_finpet_status_food,
            iconDescription = "Еда",
            progress = state.hunger,
            accentColor = AppTheme.colors.metricHunger,
        )
        StatusIndicator(
            drawableRes = R.drawable.ic_finpet_status_water,
            iconDescription = "Вода",
            progress = state.thirst,
            accentColor = AppTheme.colors.metricThirst,
        )
        StatusIndicator(
            drawableRes = R.drawable.ic_finpet_status_happiness,
            iconDescription = "Настроение",
            progress = state.happiness,
            accentColor = AppTheme.colors.metricHappiness,
        )
    }
}

@Composable
private fun StatusIndicator(
    @DrawableRes drawableRes: Int,
    iconDescription: String,
    progress: Int,
    accentColor: Color,
) {
    FinPetCard(
        elevation = AppTheme.elevation.low,
    ) {
        Row(
            modifier = Modifier
                .width(124.dp)
                .height(48.dp)
                .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelImage(
                drawableRes = drawableRes,
                contentDescription = iconDescription,
                size = AppTheme.sizes.iconMedium,
            )
            FinPetProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.width(66.dp),
                color = accentColor,
            )
        }
    }
}
