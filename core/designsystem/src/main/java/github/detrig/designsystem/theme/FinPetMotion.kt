package github.detrig.designsystem.theme

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

@Immutable
data class FinPetMotion(
    val durationFastMillis: Int,
    val durationMediumMillis: Int,
    val durationSlowMillis: Int,
    val standardEasing: Easing,
    val emphasizedEasing: Easing,
)
