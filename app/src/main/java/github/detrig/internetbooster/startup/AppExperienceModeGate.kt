package github.detrig.internetbooster.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.internetbooster.R
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun AppExperienceModeGate(
    selectedMode: StateFlow<AppExperienceMode?>,
    onModeSelected: (AppExperienceMode) -> Unit,
    content: @Composable () -> Unit,
) {
    val mode by selectedMode.collectAsState()
    if (mode == null) {
        AppExperienceModeSelection(onModeSelected = onModeSelected)
    } else {
        content()
    }
}

@Composable
private fun AppExperienceModeSelection(onModeSelected: (AppExperienceMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.storefront.background)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.app_mode_title),
            style = AppTheme.typography.screenTitle,
            color = AppTheme.colors.storefront.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.app_mode_description),
            modifier = Modifier.padding(top = AppTheme.spacing.sm, bottom = AppTheme.spacing.xl),
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        FinPetButton(
            onClick = { onModeSelected(AppExperienceMode.GAME) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
            style = FinPetButtonDefaults.storefrontPrimaryStyle(),
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(vertical = AppTheme.spacing.sm),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.app_mode_game_title),
                    style = AppTheme.typography.sectionTitle,
                )
                Text(
                    text = stringResource(R.string.app_mode_game_description),
                    style = AppTheme.typography.label,
                )
            }
        }
        FinPetButton(
            onClick = { onModeSelected(AppExperienceMode.DEMONSTRATION) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp).padding(top = AppTheme.spacing.md),
            style = FinPetButtonDefaults.storefrontOutlinedStyle(),
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(vertical = AppTheme.spacing.sm),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.app_mode_demo_title),
                    style = AppTheme.typography.sectionTitle,
                )
                Text(
                    text = stringResource(R.string.app_mode_demo_description),
                    style = AppTheme.typography.label,
                )
            }
        }
    }
}

@Preview(name = "Выбор режима", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun AppExperienceModeSelectionPreview() {
    FinPetTheme {
        AppExperienceModeSelection(onModeSelected = {})
    }
}
