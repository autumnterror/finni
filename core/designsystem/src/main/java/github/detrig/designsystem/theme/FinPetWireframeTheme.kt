package github.detrig.designsystem.theme

import androidx.compose.ui.graphics.Color

/** Монохромная тема для заменяемого макета магазина; другие экраны сохраняют свою тему. */
internal fun wireframeTheme(base: FinPetThemePack): FinPetThemePack {
    val paper = Color.White
    val ink = Color(0xFF303539)
    val muted = Color(0xFF666D72)
    val line = Color(0xFF899196)
    val feedback = FinPetFeedbackColors(paper, line, ink, ink)
    return base.copy(colors = base.colors.copy(
        actionPrimary = ink, onActionPrimary = paper, actionSecondary = paper, onActionSecondary = ink,
        surfaceBase = paper, surfaceElevated = paper, surfaceInteractive = paper,
        textPrimary = ink, textSecondary = muted, borderDefault = line, progressTrack = paper,
        currencyAccent = ink, currencyContainer = paper, currencyBorder = line,
        metricHealth = ink, metricHunger = ink, metricThirst = ink, metricHappiness = ink,
        petColorSunny = paper, petColorMint = paper, petColorCoral = paper, petColorSky = paper,
        sceneBackground = paper, sceneGround = paper, sceneBorder = line, sceneShadow = Color.Transparent,
        roomBackground = paper, onRoomBackground = ink, roomObjectSurface = paper,
        roomObjectBorder = line, roomObjectShadow = Color.Transparent,
        statusInfo = feedback, statusPositive = feedback, statusWarning = feedback, statusCritical = feedback,
        intensityLow = ink, intensityMedium = ink, intensityHigh = ink,
        storefront = FinPetStorefrontColors(
            background = paper,
            surface = paper,
            selectedSurface = paper,
            primaryAction = ink,
            onPrimaryAction = paper,
            onSurface = ink,
            outline = line,
            shadow = Color.Transparent,
        ),
    ))
}
