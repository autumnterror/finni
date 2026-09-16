package github.detrig.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetFeedbackColors

enum class FinPetFeedbackTone {
    Info,
    Positive,
    Warning,
    Critical,
}

@Composable
fun FinPetFeedbackSurface(
    tone: FinPetFeedbackTone,
    modifier: Modifier = Modifier,
    content: @Composable (FinPetFeedbackStyle) -> Unit,
) {
    val style = tone.style()
    FinPetCard(
        modifier = modifier,
        containerColor = style.container,
        contentColor = style.content,
        borderColor = style.border,
    ) {
        content(style)
    }
}

@Immutable
data class FinPetFeedbackStyle(
    val container: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
)

@Composable
private fun FinPetFeedbackTone.style(): FinPetFeedbackStyle {
    val colors = AppTheme.colors
    val feedback = when (this) {
        FinPetFeedbackTone.Info -> colors.statusInfo
        FinPetFeedbackTone.Positive -> colors.statusPositive
        FinPetFeedbackTone.Warning -> colors.statusWarning
        FinPetFeedbackTone.Critical -> colors.statusCritical
    }
    return feedback.toStyle()
}

private fun FinPetFeedbackColors.toStyle() = FinPetFeedbackStyle(
    container = container,
    border = border,
    accent = accent,
    content = onContainer,
)
