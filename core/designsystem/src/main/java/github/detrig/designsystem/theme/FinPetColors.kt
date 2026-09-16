package github.detrig.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class FinPetFeedbackColors(
    val container: Color,
    val border: Color,
    val accent: Color,
    val onContainer: Color,
)

/** Семантические цвета приложения. Названия не зависят от текущей палитры. */
@Immutable
data class FinPetColors(
    val actionPrimary: Color,
    val onActionPrimary: Color,
    val actionSecondary: Color,
    val onActionSecondary: Color,
    val surfaceBase: Color,
    val surfaceElevated: Color,
    val surfaceInteractive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val borderDefault: Color,
    val progressTrack: Color,
    val currencyAccent: Color,
    val currencyContainer: Color,
    val currencyBorder: Color,
    val metricHealth: Color,
    val metricHunger: Color,
    val metricThirst: Color,
    val metricHappiness: Color,
    val petColorSunny: Color,
    val petColorMint: Color,
    val petColorCoral: Color,
    val petColorSky: Color,
    val sceneBackground: Color,
    val sceneGround: Color,
    val sceneBorder: Color,
    val sceneShadow: Color,
    val roomBackground: Color,
    val onRoomBackground: Color,
    val roomObjectSurface: Color,
    val roomObjectBorder: Color,
    val roomObjectShadow: Color,
    val statusInfo: FinPetFeedbackColors,
    val statusPositive: FinPetFeedbackColors,
    val statusWarning: FinPetFeedbackColors,
    val statusCritical: FinPetFeedbackColors,
    val intensityLow: Color = statusPositive.accent,
    val intensityMedium: Color = statusWarning.accent,
    val intensityHigh: Color = statusCritical.accent,
    val flightSkyTop: Color = sceneBackground,
    val flightSkyBottom: Color = surfaceElevated,
    val flightCloud: Color = onActionPrimary,
    val flightSun: Color = currencyContainer,
    val flightDistant: Color = statusInfo.border,
    val flightObstacle: Color = roomObjectBorder,
    val flightObstacleLight: Color = roomObjectSurface,
    val flightGrass: Color = actionPrimary,
    val flightGrassLight: Color = sceneGround,
    val house: FinPetHouseColors = FinPetHouseColors(),
)
