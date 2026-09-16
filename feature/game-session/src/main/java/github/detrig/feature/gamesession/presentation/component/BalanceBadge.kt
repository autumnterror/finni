package github.detrig.feature.gamesession.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.gamesession.R

@Composable
internal fun BalanceBadge(
    balanceText: String,
    modifier: Modifier = Modifier,
) {
    FinPetCard(
        modifier = modifier,
        shape = AppTheme.shapes.badge,
        containerColor = AppTheme.colors.currencyContainer,
        borderColor = AppTheme.colors.currencyBorder,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppTheme.spacing.lg,
                vertical = AppTheme.spacing.md,
            ),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelImage(
                drawableRes = R.drawable.ic_finpet_coin,
                contentDescription = "Баланс",
                size = AppTheme.sizes.iconLarge,
            )
            Text(
                text = balanceText,
                style = AppTheme.typography.currency,
                color = AppTheme.colors.textPrimary,
            )
        }
    }
}
