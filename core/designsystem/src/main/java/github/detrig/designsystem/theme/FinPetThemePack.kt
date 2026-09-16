package github.detrig.designsystem.theme

import androidx.compose.runtime.Immutable

/** Полный визуальный контракт приложения, заменяемый одним объектом. */
@Immutable
data class FinPetThemePack(
    val colors: FinPetColors,
    val typography: FinPetTypography,
    val spacing: FinPetSpacing,
    val shapes: FinPetShapes,
    val elevation: FinPetElevation,
    val sizes: FinPetSizes,
    val motion: FinPetMotion,
)
