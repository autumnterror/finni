package github.detrig.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetOutlinedButton
import github.detrig.designsystem.component.FinPetProgressIndicator

@Preview(name = "Prototype theme", widthDp = 360, heightDp = 640)
@Composable
private fun PrototypeThemePreview() {
    FinPetTheme {
        ThemeContractSample()
    }
}

@Preview(name = "Theme swap proof", widthDp = 360, heightDp = 640)
@Composable
private fun ThemeSwapPreview() {
    FinPetTheme(themePack = previewContrastThemePack) {
        ThemeContractSample()
    }
}

@Composable
private fun ThemeContractSample() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.surfaceBase)
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        Text(
            text = "FinPet",
            style = AppTheme.typography.brand,
            color = AppTheme.colors.textPrimary,
        )
        Text(
            text = "Theme contract preview",
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        FinPetCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(AppTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                Text(
                    text = "Balance: 7 350",
                    style = AppTheme.typography.currency,
                )
                FinPetProgressIndicator(
                    progress = 0.72f,
                    color = AppTheme.colors.metricHappiness,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            FinPetButton(
                text = "Continue",
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            FinPetOutlinedButton(
                text = "Later",
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val previewContrastThemePack = FinPetThemePacks.prototype.let { prototype ->
    prototype.copy(
        colors = prototype.colors.copy(
            actionPrimary = Color(0xFF3153A4),
            onActionPrimary = Color(0xFFFFFFFF),
            actionSecondary = Color(0xFFDCE5FF),
            onActionSecondary = Color(0xFF172B57),
            surfaceBase = Color(0xFFF4F7FF),
            surfaceElevated = Color(0xFFFFFFFF),
            surfaceInteractive = Color(0xFFE8EDFA),
            textPrimary = Color(0xFF171C29),
            textSecondary = Color(0xFF535D73),
            borderDefault = Color(0xFFC9D2E8),
        ),
    )
}
