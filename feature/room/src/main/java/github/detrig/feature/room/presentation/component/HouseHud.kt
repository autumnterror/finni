package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetProgressIndicator
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.domain.model.RoomProgress

@Composable
internal fun HouseHud(progress: RoomProgress, active: Boolean, modifier: Modifier = Modifier) {
    val balance = stringResource(R.string.house_balance_accessibility, progress.balanceRub)
    Row(
        modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(AppTheme.spacing.sm).testTag("house_hud"),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        FinPetCard(Modifier.weight(1f)) {
            Column(Modifier.padding(AppTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Text(stringResource(R.string.house_level, progress.playerLevel), style = AppTheme.typography.label)
                Text(stringResource(R.string.house_day_counter, progress.weekNumber, progress.dayOfWeek),
                    style = AppTheme.typography.bodyStrong, modifier = Modifier.testTag("house_day_counter"))
                HouseMetric(stringResource(R.string.house_hunger), progress.petHunger, AppTheme.colors.metricHunger)
                HouseMetric(stringResource(R.string.house_happiness), progress.petHappiness, AppTheme.colors.metricHappiness)
            }
        }
        FinPetCard(Modifier.weight(1.15f)) {
            Column(Modifier.padding(AppTheme.spacing.sm), horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Text(stringResource(R.string.room_money, progress.balanceRub),
                    style = AppTheme.typography.currency, modifier = Modifier.semantics { contentDescription = balance })
                Text(stringResource(R.string.house_money_in_days, progress.daysUntilAllowance),
                    style = AppTheme.typography.caption)
            }
        }
    }
}

@Composable
private fun HouseMetric(label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        Text(label, style = AppTheme.typography.caption, modifier = Modifier.weight(1f))
        Text(value.toString(), style = AppTheme.typography.caption)
    }
    FinPetProgressIndicator(value / 100f, color, Modifier.fillMaxWidth())
}
