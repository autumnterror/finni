package github.detrig.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import github.detrig.designsystem.R
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FinPetAchievementBanner(
    title: String,
    onDismiss: () -> Unit,
    onHeightChanged: (Dp) -> Unit = {},
) {
    val closeLabel = stringResource(R.string.achievement_banner_close)
    val density = LocalDensity.current
    Popup(
        alignment = Alignment.TopCenter,
        properties = PopupProperties(focusable = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = AppTheme.spacing.xl, vertical = AppTheme.spacing.xs)
                .onSizeChanged { size ->
                    onHeightChanged(with(density) { size.height.toDp() })
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = ACHIEVEMENT_BANNER_MAX_WIDTH),
            ) {
                AchievementBurst(Modifier.matchParentSize())
                FinPetCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = ACHIEVEMENT_CARD_SIDE_INSET,
                            top = ACHIEVEMENT_RIBBON_OVERLAP,
                            end = ACHIEVEMENT_CARD_SIDE_INSET,
                        )
                        .shadow(
                            elevation = AppTheme.elevation.high,
                            shape = AppTheme.shapes.storefrontControl,
                            ambientColor = AppTheme.colors.storefront.shadow,
                            spotColor = AppTheme.colors.storefront.shadow,
                        )
                        .clip(AppTheme.shapes.storefrontControl)
                        .clickable(
                            onClickLabel = closeLabel,
                            role = Role.Button,
                            onClick = onDismiss,
                        ),
                    shape = AppTheme.shapes.storefrontControl,
                    containerColor = AppTheme.colors.surfaceElevated,
                    contentColor = AppTheme.colors.storefront.onSurface,
                    borderColor = AppTheme.colors.storefront.outline,
                    borderWidth = AppTheme.sizes.borderStrong * 2,
                ) {
                    Box(
                        modifier = Modifier.background(
                            Brush.horizontalGradient(
                                listOf(
                                    AppTheme.colors.surfaceElevated,
                                    AppTheme.colors.currencyContainer,
                                    AppTheme.colors.surfaceElevated,
                                ),
                            ),
                        ),
                    ) {
                        AchievementSparkles(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(
                                    top = AppTheme.spacing.xl,
                                    end = AppTheme.spacing.md,
                                )
                                .size(width = 38.dp, height = 28.dp),
                        )
                        Row(
                            modifier = Modifier
                                .padding(
                                    start = AppTheme.spacing.lg,
                                    top = ACHIEVEMENT_CARD_TOP_PADDING,
                                    end = ACHIEVEMENT_CARD_END_PADDING,
                                    bottom = ACHIEVEMENT_CARD_BOTTOM_PADDING,
                                ),
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AchievementMedal(
                                modifier = Modifier.size(ACHIEVEMENT_MEDAL_SIZE)
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .wrapContentHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = AppTheme.typography.sectionTitle.copy(
                                        platformStyle = PlatformTextStyle(
                                            includeFontPadding = false
                                        )
                                    ),
                                    color = AppTheme.colors.storefront.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(ACHIEVEMENT_RIBBON_WIDTH_FRACTION)
                        .shadow(
                            elevation = AppTheme.elevation.medium,
                            shape = AppTheme.shapes.storefrontControl,
                            ambientColor = AppTheme.colors.storefront.shadow,
                            spotColor = AppTheme.colors.storefront.shadow,
                        )
                        .clip(AppTheme.shapes.storefrontControl)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    AppTheme.colors.currencyContainer,
                                    AppTheme.colors.currencyAccent,
                                ),
                            ),
                        )
                        .padding(
                            horizontal = AppTheme.spacing.lg,
                            vertical = AppTheme.spacing.xs,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.achievement_banner_title),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.storefront.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
                FinPetIconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = ACHIEVEMENT_RIBBON_OVERLAP, end = ACHIEVEMENT_CARD_SIDE_INSET)
                        .semantics { contentDescription = closeLabel },
                ) {
                    Text(
                        text = "×",
                        style = AppTheme.typography.sectionTitle,
                        color = AppTheme.colors.storefront.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementBurst(modifier: Modifier = Modifier) {
    val color = AppTheme.colors.currencyAccent
    val borderWidth = AppTheme.sizes.borderStrong
    Canvas(modifier) {
        val stroke = borderWidth.toPx() * 2f
        val rayLength = 12.dp.toPx()
        val outerInset = 4.dp.toPx()
        listOf(0.4f, 0.62f, 0.82f).forEachIndexed { index, yFraction ->
            val slant = when (index) {
                0 -> -rayLength * 0.7f
                2 -> rayLength * 0.7f
                else -> 0f
            }
            val y = size.height * yFraction
            drawLine(
                color = color,
                start = Offset(outerInset, y + slant),
                end = Offset(outerInset + rayLength, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(size.width - outerInset, y + slant),
                end = Offset(size.width - outerInset - rayLength, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun AchievementMedal(modifier: Modifier = Modifier) {
    val gold = AppTheme.colors.currencyAccent
    val goldLight = AppTheme.colors.currencyContainer
    val outline = AppTheme.colors.storefront.outline
    val ribbon = AppTheme.colors.statusCritical.accent
    val borderWidth = AppTheme.sizes.borderStrong
    Canvas(modifier) {
        val center = Offset(size.width * 0.5f, size.height * 0.4f)
        val radius = size.minDimension * 0.31f
        val ribbonStroke = Stroke(
            width = borderWidth.toPx(),
        )
        val leftRibbon = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.49f)
            lineTo(size.width * 0.18f, size.height * 0.94f)
            lineTo(size.width * 0.37f, size.height * 0.84f)
            lineTo(size.width * 0.5f, size.height * 0.98f)
            lineTo(size.width * 0.55f, size.height * 0.49f)
            close()
        }
        val rightRibbon = Path().apply {
            moveTo(size.width * 0.45f, size.height * 0.49f)
            lineTo(size.width * 0.5f, size.height * 0.98f)
            lineTo(size.width * 0.64f, size.height * 0.84f)
            lineTo(size.width * 0.82f, size.height * 0.94f)
            lineTo(size.width * 0.72f, size.height * 0.49f)
            close()
        }
        drawPath(leftRibbon, ribbon)
        drawPath(leftRibbon, outline, style = ribbonStroke)
        drawPath(rightRibbon, ribbon)
        drawPath(rightRibbon, outline, style = ribbonStroke)
        drawCircle(color = outline, radius = radius + 4.dp.toPx(), center = center)
        drawCircle(color = gold, radius = radius, center = center)
        drawCircle(color = goldLight, radius = radius * 0.73f, center = center)
        drawCircle(
            color = gold,
            radius = radius * 0.73f,
            center = center,
            style = Stroke(width = borderWidth.toPx() * 2f),
        )
        drawPath(
            path = starPath(center, radius * 0.54f, radius * 0.26f),
            color = gold,
        )
    }
}

@Composable
private fun AchievementSparkles(modifier: Modifier = Modifier) {
    val color = AppTheme.colors.currencyAccent
    Canvas(modifier) {
        drawSparkle(
            center = Offset(size.width * 0.32f, size.height * 0.36f),
            radius = size.minDimension * 0.25f,
            color = color,
        )
        drawSparkle(
            center = Offset(size.width * 0.72f, size.height * 0.7f),
            radius = size.minDimension * 0.13f,
            color = color,
        )
    }
}

private fun starPath(center: Offset, outerRadius: Float, innerRadius: Float): Path = Path().apply {
    repeat(10) { index ->
        val angle = -PI / 2 + index * PI / 5
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val point = Offset(
            x = center.x + cos(angle).toFloat() * radius,
            y = center.y + sin(angle).toFloat() * radius,
        )
        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
    close()
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkle(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.25f, center.y - radius * 0.25f)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + radius * 0.25f, center.y + radius * 0.25f)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.25f, center.y + radius * 0.25f)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - radius * 0.25f, center.y - radius * 0.25f)
        close()
    }
    drawPath(path, color)
}

private val ACHIEVEMENT_BANNER_MAX_WIDTH = 460.dp
private val ACHIEVEMENT_CARD_SIDE_INSET = 24.dp
private val ACHIEVEMENT_RIBBON_OVERLAP = 8.dp
private val ACHIEVEMENT_CARD_TOP_PADDING = 26.dp
private val ACHIEVEMENT_CARD_BOTTOM_PADDING = 20.dp
private val ACHIEVEMENT_CARD_END_PADDING = 52.dp
private val ACHIEVEMENT_MEDAL_SIZE = 52.dp
private const val ACHIEVEMENT_RIBBON_WIDTH_FRACTION = 0.76f

@Preview(name = "Открыто достижение", widthDp = 360, heightDp = 220, showBackground = true)
@Composable
private fun FinPetAchievementBannerPreview() {
    FinPetTheme {
        FinPetAchievementBanner(title = "Первый план", onDismiss = {})
    }
}
