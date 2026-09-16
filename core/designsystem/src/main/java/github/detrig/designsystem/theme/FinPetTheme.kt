package github.detrig.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalFinPetColors = staticCompositionLocalOf { FinPetThemePacks.prototype.colors }
private val LocalFinPetTypography = staticCompositionLocalOf { FinPetThemePacks.prototype.typography }
private val LocalFinPetSpacing = staticCompositionLocalOf { FinPetThemePacks.prototype.spacing }
private val LocalFinPetShapes = staticCompositionLocalOf { FinPetThemePacks.prototype.shapes }
private val LocalFinPetElevation = staticCompositionLocalOf { FinPetThemePacks.prototype.elevation }
private val LocalFinPetSizes = staticCompositionLocalOf { FinPetThemePacks.prototype.sizes }
private val LocalFinPetMotion = staticCompositionLocalOf { FinPetThemePacks.prototype.motion }

object AppTheme {
    val colors: FinPetColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFinPetColors.current

    val typography: FinPetTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalFinPetTypography.current

    val spacing: FinPetSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalFinPetSpacing.current

    val shapes: FinPetShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalFinPetShapes.current

    val elevation: FinPetElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalFinPetElevation.current

    val sizes: FinPetSizes
        @Composable
        @ReadOnlyComposable
        get() = LocalFinPetSizes.current

    val motion: FinPetMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalFinPetMotion.current
}

@Composable
fun FinPetTheme(
    themePack: FinPetThemePack = FinPetThemePacks.prototype,
    content: @Composable () -> Unit,
) {
    val colors = themePack.colors
    val shapes = themePack.shapes

    CompositionLocalProvider(
        LocalFinPetColors provides colors,
        LocalFinPetTypography provides themePack.typography,
        LocalFinPetSpacing provides themePack.spacing,
        LocalFinPetShapes provides shapes,
        LocalFinPetElevation provides themePack.elevation,
        LocalFinPetSizes provides themePack.sizes,
        LocalFinPetMotion provides themePack.motion,
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = colors.actionPrimary,
                onPrimary = colors.onActionPrimary,
                primaryContainer = colors.actionSecondary,
                onPrimaryContainer = colors.onActionSecondary,
                secondary = colors.actionSecondary,
                onSecondary = colors.onActionSecondary,
                background = colors.surfaceBase,
                onBackground = colors.textPrimary,
                surface = colors.surfaceElevated,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.surfaceInteractive,
                onSurfaceVariant = colors.textSecondary,
                outline = colors.borderDefault,
                error = colors.statusCritical.accent,
                onError = colors.onActionPrimary,
                errorContainer = colors.statusCritical.container,
                onErrorContainer = colors.statusCritical.onContainer,
            ),
            typography = themePack.typography.toMaterialTypography(),
            shapes = Shapes(
                extraSmall = shapes.compact,
                small = shapes.button,
                medium = shapes.card,
                large = shapes.dialog,
                extraLarge = shapes.sheet,
            ),
            content = content,
        )
    }
}
