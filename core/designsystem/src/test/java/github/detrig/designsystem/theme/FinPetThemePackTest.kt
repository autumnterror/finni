package github.detrig.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Test

class FinPetThemePackTest {

    private val theme = FinPetThemePacks.prototype

    @Test
    fun `touch targets meet accessibility minimum`() {
        assertTrue(theme.sizes.minimumTouchTarget >= 48.dp)
        assertTrue(theme.sizes.preferredTouchTarget >= theme.sizes.minimumTouchTarget)
        assertTrue(theme.sizes.illustrationMaxHeight >= theme.sizes.illustrationMinHeight)
    }

    @Test
    fun `spacing scale is ordered`() {
        val spacing = theme.spacing
        val scale = listOf(
            spacing.none,
            spacing.xxs,
            spacing.xs,
            spacing.sm,
            spacing.md,
            spacing.lg,
            spacing.xl,
            spacing.xxl,
        )

        assertTrue(scale.zipWithNext().all { (smaller, larger) -> smaller <= larger })
    }

    @Test
    fun `motion durations are ordered`() {
        val motion = theme.motion

        assertTrue(motion.durationFastMillis < motion.durationMediumMillis)
        assertTrue(motion.durationMediumMillis < motion.durationSlowMillis)
    }

    @Test
    fun `primary text and action colors have readable contrast`() {
        assertTrue(contrastRatio(theme.colors.textPrimary, theme.colors.surfaceBase) >= 4.5f)
        assertTrue(contrastRatio(theme.colors.onActionPrimary, theme.colors.actionPrimary) >= 4.5f)
    }

    @Test
    fun `pet customization colors are distinct`() {
        val colors = theme.colors
        val petColors = setOf(
            colors.petColorSunny,
            colors.petColorMint,
            colors.petColorCoral,
            colors.petColorSky,
        )

        assertTrue(petColors.size == 4)
    }
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    val lighter = maxOf(foreground.luminance(), background.luminance())
    val darker = minOf(foreground.luminance(), background.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
