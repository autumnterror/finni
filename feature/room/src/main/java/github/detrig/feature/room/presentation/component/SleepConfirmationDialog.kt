package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetOutlinedButton
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.R
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import github.detrig.designsystem.theme.AppTheme

@Composable
internal fun SleepConfirmationDialog(
    canSleep: Boolean,
    onConfirm: () -> Unit,
    onPostpone: () -> Unit,
) {
    FinPetModalDialog(
        title = stringResource(if (canSleep) R.string.room_sleep_title else R.string.room_sleep_hungry_title),
        onDismissRequest = onPostpone,
        actions = {
            if (canSleep) {
                FinPetButton(
                    text = stringResource(R.string.room_sleep_confirm),
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    style = FinPetButtonDefaults.storefrontPrimaryStyle(),
                )
            }
            FinPetOutlinedButton(
                text = stringResource(if (canSleep) R.string.room_sleep_postpone else R.string.room_sleep_hungry_ok),
                onClick = onPostpone,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontOutlinedStyle(),
            )
        },
        content = {
            if (!canSleep) {
                Text(
                    text = stringResource(R.string.room_sleep_hungry_message),
                    modifier = Modifier.padding(horizontal = AppTheme.spacing.sm),
                    style = AppTheme.typography.body,
                )
            }
        },
    )
}

@Preview(name = "Подтверждение сна", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun SleepConfirmationDialogPreview() {
    FinPetTheme {
        SleepConfirmationDialog(canSleep = false, onConfirm = {}, onPostpone = {})
    }
}
