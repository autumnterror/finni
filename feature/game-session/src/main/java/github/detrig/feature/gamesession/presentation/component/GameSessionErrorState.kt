package github.detrig.feature.gamesession.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetFeedbackSurface
import github.detrig.designsystem.component.FinPetFeedbackTone
import github.detrig.designsystem.theme.AppTheme

@Composable
internal fun GameSessionErrorState(
    onRetryClick: () -> Unit,
) {
    FinPetFeedbackSurface(
        tone = FinPetFeedbackTone.Critical,
        modifier = Modifier.fillMaxWidth(),
    ) { style ->
        Column(
            modifier = Modifier.padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Text(
                text = "Не удалось загрузить игровую сессию",
                style = AppTheme.typography.sectionTitle,
                color = style.accent,
            )
            Text(
                text = "Проверь состояние приложения и попробуй еще раз.",
                style = AppTheme.typography.body,
                color = style.content,
            )
            FinPetButton(
                text = "Повторить",
                onClick = onRetryClick,
            )
        }
    }
}
