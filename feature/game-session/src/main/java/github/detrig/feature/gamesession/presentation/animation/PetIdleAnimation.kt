package github.detrig.feature.gamesession.presentation.animation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import github.detrig.designsystem.theme.AppTheme

@Composable
internal fun Modifier.petIdleAnimation(
    animationState: PetAnimationState,
    enabled: Boolean = true,
): Modifier {
    if (enabled.not()) {
        return this
    }

    val transition = rememberInfiniteTransition(label = "pet_idle_transition")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationState.durationMillis,
                easing = AppTheme.motion.standardEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pet_idle_progress",
    )
    val targetTranslationY = with(LocalDensity.current) {
        animationState.targetTranslationY.toPx()
    }

    return graphicsLayer {
        transformOrigin = TransformOrigin(
            pivotFractionX = 0.5f,
            pivotFractionY = 0.92f,
        )
        scaleX = 1f + (animationState.targetScaleX - 1f) * progress
        scaleY = 1f + (animationState.targetScaleY - 1f) * progress
        translationY = targetTranslationY * progress
        rotationZ = animationState.targetRotationZ * progress
    }
}
