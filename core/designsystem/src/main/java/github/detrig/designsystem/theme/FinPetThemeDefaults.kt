package github.detrig.designsystem.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Наборы темы. После брендбука здесь появится brand и станет темой по умолчанию. */
object FinPetThemePacks {
    val wireframe: FinPetThemePack by lazy { wireframeTheme(prototype) }
    val prototype = FinPetThemePack(
        colors = prototypeColors,
        typography = prototypeTypography,
        spacing = prototypeSpacing,
        shapes = prototypeShapes,
        elevation = prototypeElevation,
        sizes = prototypeSizes,
        motion = prototypeMotion,
    )
}

private val prototypeColors = FinPetColors(
    intensityLow = Color(0xFF54C97F),
    intensityMedium = Color(0xFFF09636),
    intensityHigh = Color(0xFFEF6169),
    actionPrimary = Color(0xFF2F7D57),
    onActionPrimary = Color(0xFFFFFFFF),
    actionSecondary = Color(0xFFDDEFE5),
    onActionSecondary = Color(0xFF17412D),
    surfaceBase = Color(0xFFF5F0E6),
    surfaceElevated = Color(0xFFFFFCF4),
    surfaceInteractive = Color(0xFFF0E9DD),
    textPrimary = Color(0xFF21201D),
    textSecondary = Color(0xFF6E675B),
    borderDefault = Color(0xFFE2D8C7),
    progressTrack = Color(0xFFE9E0D2),
    currencyAccent = Color(0xFFE7A928),
    currencyContainer = Color(0xFFFFE9AE),
    currencyBorder = Color(0xFFE7B948),
    metricHealth = Color(0xFFE04046),
    metricHunger = Color(0xFFF09636),
    metricThirst = Color(0xFF2E95C7),
    metricHappiness = Color(0xFFE94E91),
    petColorSunny = Color(0xFFF3C85B),
    petColorMint = Color(0xFF74C997),
    petColorCoral = Color(0xFFF18484),
    petColorSky = Color(0xFF79BCEE),
    sceneBackground = Color(0xFFD9F0FF),
    sceneGround = Color(0xFF96C66E),
    sceneBorder = Color(0xFF7BA9C8),
    sceneShadow = Color(0x403F4A30),
    roomBackground = Color(0xFF102B37),
    onRoomBackground = Color(0xFFFFFCF4),
    roomObjectSurface = Color(0xB3DFCCA9),
    roomObjectBorder = Color(0xFF715537),
    roomObjectShadow = Color(0x55382211),
    flightSkyTop = Color(0xFF69BAD5),
    flightSkyBottom = Color(0xFFE5F1DD),
    flightDistant = Color(0xFF98C9C2),
    flightObstacle = Color(0xFFA37451),
    flightObstacleLight = Color(0xFFC99B6B),
    flightGrass = Color(0xFF437E62),
    flightGrassLight = Color(0xFF94C57B),
    statusInfo = FinPetFeedbackColors(
        container = Color(0xFFEFF6FF),
        border = Color(0xFFB8D8FF),
        accent = Color(0xFF1D69B3),
        onContainer = Color(0xFF143B63),
    ),
    statusPositive = FinPetFeedbackColors(
        container = Color(0xFFEAF7EF),
        border = Color(0xFFB8DEC5),
        accent = Color(0xFF2F7D57),
        onContainer = Color(0xFF1D4E36),
    ),
    statusWarning = FinPetFeedbackColors(
        container = Color(0xFFFFF5DA),
        border = Color(0xFFE6C978),
        accent = Color(0xFF9A650C),
        onContainer = Color(0xFF5B3C0A),
    ),
    statusCritical = FinPetFeedbackColors(
        container = Color(0xFFFFECEC),
        border = Color(0xFFF0B6B6),
        accent = Color(0xFFC7363E),
        onContainer = Color(0xFF7A1E24),
    ),
)

private val prototypeTypography = FinPetTypography(
    brand = prototypeTextStyle(size = 28, lineHeight = 34, weight = FontWeight.Bold),
    screenTitle = prototypeTextStyle(size = 24, lineHeight = 30, weight = FontWeight.Bold),
    sectionTitle = prototypeTextStyle(size = 18, lineHeight = 24, weight = FontWeight.SemiBold),
    body = prototypeTextStyle(size = 16, lineHeight = 22, weight = FontWeight.Normal),
    bodyStrong = prototypeTextStyle(size = 16, lineHeight = 22, weight = FontWeight.SemiBold),
    caption = prototypeTextStyle(size = 13, lineHeight = 18, weight = FontWeight.Normal),
    label = prototypeTextStyle(size = 14, lineHeight = 18, weight = FontWeight.Medium),
    button = prototypeTextStyle(size = 16, lineHeight = 20, weight = FontWeight.SemiBold),
    currency = prototypeTextStyle(size = 22, lineHeight = 26, weight = FontWeight.Bold),
    metricValue = prototypeTextStyle(size = 18, lineHeight = 22, weight = FontWeight.Bold),
    gameScore = prototypeTextStyle(size = 40, lineHeight = 48, weight = FontWeight.Bold),
    gameCountdown = prototypeTextStyle(size = 88, lineHeight = 96, weight = FontWeight.Bold),
)

private val prototypeSpacing = FinPetSpacing(
    none = 0.dp,
    xxs = 2.dp,
    xs = 4.dp,
    sm = 8.dp,
    md = 12.dp,
    lg = 16.dp,
    xl = 24.dp,
    xxl = 32.dp,
)

private val prototypeShapes = FinPetShapes(
    compact = RoundedCornerShape(4.dp),
    card = RoundedCornerShape(8.dp),
    button = RoundedCornerShape(8.dp),
    badge = RoundedCornerShape(8.dp),
    dialog = RoundedCornerShape(8.dp),
    sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
)

private val prototypeElevation = FinPetElevation(
    none = 0.dp,
    low = 2.dp,
    medium = 8.dp,
    high = 16.dp,
)

private val prototypeSizes = FinPetSizes(
    minimumTouchTarget = 48.dp,
    preferredTouchTarget = 56.dp,
    contentMaxWidth = 560.dp,
    illustrationMinHeight = 176.dp,
    illustrationMaxHeight = 228.dp,
    iconSmall = 24.dp,
    iconMedium = 30.dp,
    iconLarge = 34.dp,
    progressIndicator = 8.dp,
    borderThin = 1.dp,
    borderStrong = 2.dp,
)

private val prototypeMotion = FinPetMotion(
    durationFastMillis = 150,
    durationMediumMillis = 300,
    durationSlowMillis = 500,
    standardEasing = FastOutSlowInEasing,
    emphasizedEasing = LinearOutSlowInEasing,
)

private fun prototypeTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)
