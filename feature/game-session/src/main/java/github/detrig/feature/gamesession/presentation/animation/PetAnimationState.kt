package github.detrig.feature.gamesession.presentation.animation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class PetAnimationState(
    val targetScaleX: Float,
    val targetScaleY: Float,
    val targetTranslationY: Dp,
    val targetRotationZ: Float,
    val durationMillis: Int,
) {
    CALM(
        targetScaleX = 1.006f,
        targetScaleY = 1.018f,
        targetTranslationY = (-3).dp,
        targetRotationZ = 0f,
        durationMillis = 1500,
    ),
    HAPPY(
        targetScaleX = 1.014f,
        targetScaleY = 1.028f,
        targetTranslationY = (-7).dp,
        targetRotationZ = 1.1f,
        durationMillis = 900,
    ),
    LOW_ENERGY(
        targetScaleX = 1.002f,
        targetScaleY = 1.010f,
        targetTranslationY = 1.dp,
        targetRotationZ = (-0.4f),
        durationMillis = 2200,
    ),
    SICK(
        targetScaleX = 0.998f,
        targetScaleY = 1.008f,
        targetTranslationY = 0.dp,
        targetRotationZ = (-0.9f),
        durationMillis = 1800,
    ),
}
