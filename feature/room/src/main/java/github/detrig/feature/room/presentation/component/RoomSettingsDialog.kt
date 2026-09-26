package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.R

@Composable
internal fun RoomSettingsDialog(
    isSoundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    FinPetModalDialog(
        title = stringResource(R.string.room_settings_title),
        onDismissRequest = onDismiss,
        actions = {
            FinPetButton(
                text = stringResource(R.string.room_settings_back),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontOutlinedStyle(),
            )
        },
    ) {
        FinPetModalSection(
            modifier = Modifier.fillMaxWidth(),
            tone = FinPetModalSectionTone.Highlighted,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.room_settings_sound),
                    style = AppTheme.typography.sectionTitle,
                )
                Text(
                    text = stringResource(
                        if (isSoundEnabled) R.string.room_settings_sound_enabled
                        else R.string.room_settings_sound_disabled,
                    ),
                    style = AppTheme.typography.body,
                )
                FinPetButton(
                    text = stringResource(
                        if (isSoundEnabled) R.string.room_settings_sound_turn_off
                        else R.string.room_settings_sound_turn_on,
                    ),
                    onClick = { onSoundEnabledChange(!isSoundEnabled) },
                    modifier = Modifier.fillMaxWidth().testTag("settings_sound_toggle"),
                    style = FinPetButtonDefaults.storefrontPrimaryStyle(),
                )
            }
        }
    }
}

@Preview(name = "Настройки", widthDp = 360, heightDp = 740, showBackground = true)
@Preview(name = "Настройки, крупный шрифт", widthDp = 320, heightDp = 740, fontScale = 1.3f)
@Composable
private fun RoomSettingsDialogPreview() {
    FinPetTheme {
        RoomSettingsDialog(
            isSoundEnabled = true,
            onSoundEnabledChange = {},
            onDismiss = {},
        )
    }
}
