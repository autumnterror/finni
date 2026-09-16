package github.detrig.feature.gamesession.presentation.component

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetOutlinedButton
import github.detrig.designsystem.theme.AppTheme

@Composable
internal fun SectionButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    FinPetButton(
        text = text,
        onClick = onClick,
        modifier = modifier.height(AppTheme.sizes.minimumTouchTarget),
    )
}

@Composable
internal fun SectionOutlinedButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    FinPetOutlinedButton(
        text = text,
        onClick = onClick,
        modifier = modifier.height(AppTheme.sizes.minimumTouchTarget),
    )
}
