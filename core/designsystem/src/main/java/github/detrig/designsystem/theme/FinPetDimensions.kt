package github.detrig.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
data class FinPetSpacing(
    val none: Dp,
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
)

@Immutable
data class FinPetElevation(
    val none: Dp,
    val low: Dp,
    val medium: Dp,
    val high: Dp,
)

@Immutable
data class FinPetSizes(
    val minimumTouchTarget: Dp,
    val preferredTouchTarget: Dp,
    val contentMaxWidth: Dp,
    val illustrationMinHeight: Dp,
    val illustrationMaxHeight: Dp,
    val iconSmall: Dp,
    val iconMedium: Dp,
    val iconLarge: Dp,
    val dialoguePortrait: Dp,
    val progressIndicator: Dp,
    val borderThin: Dp,
    val borderStrong: Dp,
)
