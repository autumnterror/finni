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

@Composable
internal fun SleepConfirmationDialog(
    onConfirm: () -> Unit,
    onPostpone: () -> Unit,
) {
    FinPetModalDialog(
        title = stringResource(R.string.room_sleep_title),
        onDismissRequest = onPostpone,
        actions = {
            FinPetButton(
                text = stringResource(R.string.room_sleep_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
            FinPetOutlinedButton(
                text = stringResource(R.string.room_sleep_postpone),
                onClick = onPostpone,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontOutlinedStyle(),
            )
        },
        content = {},
    )
}

@Preview(name = "Подтверждение сна", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun SleepConfirmationDialogPreview() {
    FinPetTheme {
        SleepConfirmationDialog(onConfirm = {}, onPostpone = {})
    }
}
