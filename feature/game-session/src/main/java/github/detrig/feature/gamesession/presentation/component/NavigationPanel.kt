package github.detrig.feature.gamesession.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.gamesession.presentation.GameSessionViewEvent

@Composable
internal fun NavigationPanel(onEvent: (GameSessionViewEvent) -> Unit) {
    FinPetCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                SectionButton(
                    modifier = Modifier.weight(1f),
                    text = "Магазин",
                    onClick = { onEvent(GameSessionViewEvent.ShopClicked) },
                )
                SectionButton(
                    modifier = Modifier.weight(1f),
                    text = "Задания",
                    onClick = { onEvent(GameSessionViewEvent.TasksClicked) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                SectionOutlinedButton(
                    modifier = Modifier.weight(1f),
                    text = "Банк",
                    onClick = { onEvent(GameSessionViewEvent.BankClicked) },
                )
                SectionOutlinedButton(
                    modifier = Modifier.weight(1f),
                    text = "Жилье",
                    onClick = { onEvent(GameSessionViewEvent.HomeClicked) },
                )
            }
        }
    }
}
