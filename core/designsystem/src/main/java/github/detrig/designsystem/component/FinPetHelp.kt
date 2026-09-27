package github.detrig.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme

/** Accessible question-mark control for opening a short in-context hint. */
@Composable
fun FinPetHelpButton(
    contentDescription: String,
    onClick: () -> Unit,
    outlined: Boolean = false,
    modifier: Modifier = Modifier,
) {
    FinPetIconButton(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        val mark: @Composable () -> Unit = {
            Text(
                text = "?",
                style = AppTheme.typography.screenTitle.copy(fontSize = 24.sp),
                color = AppTheme.colors.actionPrimary,
                textAlign = TextAlign.Center,
            )
        }
        if (outlined) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = AppTheme.colors.storefront.surface,
                border = BorderStroke(AppTheme.sizes.borderStrong, AppTheme.colors.storefront.outline),
                shadowElevation = AppTheme.elevation.low,
            ) {
                Box(contentAlignment = Alignment.Center) { mark() }
            }
        } else {
            mark()
        }
    }
}

/** Shared modal presentation for brief child-facing help. */
@Composable
fun FinPetHelpDialog(
    title: String,
    message: String,
    dismissText: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FinPetModalDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        actions = {
            FinPetButton(
                text = dismissText,
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
        },
    ) {
        FinPetModalSection(
            modifier = Modifier.fillMaxWidth(),
            tone = FinPetModalSectionTone.Highlighted,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.55f)
                    .verticalScroll(rememberScrollState())
                    .padding(AppTheme.spacing.md),
            ) {
                Text(
                    text = message,
                    style = AppTheme.typography.body.copy(fontSize = 16.sp),
                    color = AppTheme.colors.storefront.onSurface,
                )
            }
        }
    }
}

@Preview(name = "Подсказка", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun FinPetHelpPreview() {
    FinPetTheme {
        FinPetHelpDialog(
            title = "Копилка",
            message = "Выбери цель и откладывай деньги. Накопленное можно вернуть в кошелёк.",
            dismissText = "Понятно",
            onDismissRequest = {},
        )
    }
}
