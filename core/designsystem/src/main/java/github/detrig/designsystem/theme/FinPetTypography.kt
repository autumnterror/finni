package github.detrig.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle

/** Семантические роли текста, не зависящие от конкретного шрифта брендбука. */
@Immutable
data class FinPetTypography(
    val brand: TextStyle,
    val screenTitle: TextStyle,
    val sectionTitle: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val currency: TextStyle,
    val metricValue: TextStyle,
    val gameScore: TextStyle = brand,
    val gameCountdown: TextStyle = brand,
)

internal fun FinPetTypography.toMaterialTypography(): Typography {
    return Typography(
        displaySmall = brand,
        headlineSmall = screenTitle,
        titleMedium = sectionTitle,
        titleSmall = bodyStrong,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = caption,
        labelLarge = button,
        labelMedium = label,
        labelSmall = caption,
    )
}
