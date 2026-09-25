package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetStorefrontBalanceBadge
import github.detrig.designsystem.component.FinPetProgressIndicator
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.domain.model.RoomProgress

/** Неподвижный слой над комнатой: меню и потребности слева, деньги справа. */
@Composable
internal fun HouseHud(
    progress: RoomProgress,
    showMenu: Boolean,
    showDetails: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm)
            .testTag("house_hud"),
    ) {
        val ringSize = when {
            maxWidth < 320.dp -> 56.dp
            maxWidth < 390.dp -> 68.dp
            else -> 74.dp
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
            if (showDetails) {
                val xpDescription = progress.nextLevelXp?.let { nextXp ->
                    stringResource(R.string.hud_experience_description, progress.currentLevelXp, nextXp)
                } ?: stringResource(R.string.hud_experience_max_description, progress.totalXp)
                FinPetProgressIndicator(
                    progress = progress.experienceProgress,
                    color = AppTheme.colors.currencyAccent,
                    modifier = Modifier.fillMaxWidth()
                        .semantics { contentDescription = xpDescription }
                        .testTag("hud_experience"),
                )
                Text(
                    text = progress.xpUntilNextLevel?.let { remaining ->
                        stringResource(R.string.hud_experience_remaining, remaining)
                    } ?: stringResource(R.string.hud_experience_max_level),
                    modifier = Modifier.align(Alignment.End),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.onRoomBackground,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                if (showDetails || showMenu) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showMenu) RoomMenuButton(onClick = onMenuClick)
                        if (showDetails) {
                            HouseNeedRing(
                                value = progress.petHappiness,
                                label = stringResource(R.string.house_happiness),
                                color = AppTheme.colors.house.sunnyAccent,
                                size = ringSize,
                                icon = HouseNeedIcon.SUN,
                                modifier = Modifier.testTag("hud_happiness"),
                            )
                            HouseNeedRing(
                                value = progress.petHunger,
                                label = stringResource(R.string.hud_satiety),
                                color = AppTheme.colors.house.leafShade,
                                size = ringSize,
                                icon = HouseNeedIcon.APPLE,
                                modifier = Modifier.testTag("hud_satiety"),
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val balanceDescription = stringResource(R.string.house_balance_accessibility, progress.balanceRub)
                    FinPetStorefrontBalanceBadge(
                        balanceRub = progress.balanceRub.toLong(),
                        transparent = true,
                        modifier = Modifier.semantics { contentDescription = balanceDescription }.testTag("house_balance"),
                    )
                    if (showDetails) {
                        Spacer(Modifier.height(AppTheme.spacing.sm))
                        HouseDayBadge(progress.weekNumber, progress.dayOfWeek, Modifier.testTag("hud_day"))
                    }
                }
            }
        }
    }
}

@Composable
private fun HouseDayBadge(weekNumber: Long, dayOfWeek: Int, modifier: Modifier = Modifier) {
    FinPetCard(
        modifier = modifier,
        containerColor = AppTheme.colors.roomBackground.copy(alpha = .62f),
        contentColor = AppTheme.colors.onRoomBackground,
        borderColor = AppTheme.colors.onRoomBackground.copy(alpha = .75f),
    ) {
        Text(
            text = stringResource(R.string.house_day_counter, weekNumber, dayOfWeek),
            modifier = Modifier.padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
            style = AppTheme.typography.caption,
        )
    }
}

private enum class HouseNeedIcon { APPLE, SUN }

@Composable
private fun HouseNeedRing(
    value: Int,
    label: String,
    color: Color,
    size: Dp,
    icon: HouseNeedIcon,
    modifier: Modifier = Modifier,
) {
    val percentage = value.coerceIn(0, 100)
    val description = stringResource(R.string.hud_metric_description, label, percentage)
    FinPetCard(
        modifier = modifier.size(size).clearAndSetSemantics { contentDescription = description },
        shape = CircleShape,
        containerColor = AppTheme.colors.roomBackground.copy(alpha = .62f),
        contentColor = AppTheme.colors.onRoomBackground,
        borderColor = AppTheme.colors.onRoomBackground.copy(alpha = .75f),
        borderWidth = AppTheme.sizes.borderStrong,
        elevation = AppTheme.elevation.low,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val track = AppTheme.colors.onRoomBackground.copy(alpha = .28f)
            Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (percentage > 0) drawArc(
                    color, -90f, percentage * 3.6f, false, Offset(inset, inset), arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HouseNeedArtwork(icon, Modifier.size(25.dp))
                Text("$percentage%", style = AppTheme.typography.caption, maxLines = 1)
            }
        }
    }
}

@Composable
private fun HouseNeedArtwork(icon: HouseNeedIcon, modifier: Modifier = Modifier) {
    val outline = AppTheme.colors.house.outline
    val leaf = AppTheme.colors.house.leaf
    val sun = AppTheme.colors.house.sunnyAccent
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        when (icon) {
            HouseNeedIcon.APPLE -> {
                val apple = Path().apply {
                    moveTo(w * .5f, h * .31f)
                    cubicTo(w * .18f, h * .12f, w * .08f, h * .43f, w * .2f, h * .73f)
                    cubicTo(w * .3f, h * .98f, w * .43f, h * .94f, w * .5f, h * .86f)
                    cubicTo(w * .59f, h * .96f, w * .76f, h * .96f, w * .84f, h * .72f)
                    cubicTo(w * .96f, h * .42f, w * .79f, h * .13f, w * .5f, h * .31f)
                    close()
                }
                drawPath(apple, leaf)
                drawPath(apple, outline, style = Stroke(stroke))
                drawLine(outline, Offset(w * .5f, h * .3f), Offset(w * .54f, h * .1f), stroke)
                drawOval(leaf, Offset(w * .57f, h * .1f), Size(w * .25f, h * .13f))
            }
            HouseNeedIcon.SUN -> {
                val center = Offset(w / 2f, h / 2f)
                repeat(8) { index ->
                    val angle = Math.PI * index / 4.0
                    val dx = kotlin.math.cos(angle).toFloat()
                    val dy = kotlin.math.sin(angle).toFloat()
                    drawLine(
                        outline,
                        center + Offset(dx * w * .36f, dy * h * .36f),
                        center + Offset(dx * w * .47f, dy * h * .47f),
                        stroke,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(sun, w * .29f, center)
                drawCircle(outline, w * .29f, center, style = Stroke(stroke))
                drawCircle(outline, w * .023f, Offset(w * .41f, h * .46f))
                drawCircle(outline, w * .023f, Offset(w * .59f, h * .46f))
                drawArc(
                    outline, 15f, 150f, false,
                    Offset(w * .39f, h * .48f), Size(w * .22f, h * .2f),
                    style = Stroke(stroke * .75f),
                )
            }
        }
    }
}

@Preview(name = "HUD комнаты", widthDp = 393, heightDp = 220, showBackground = true)
@Preview(name = "HUD, узкий экран", widthDp = 320, heightDp = 220, fontScale = 1.3f)
@Composable
private fun HouseHudPreview() {
    FinPetTheme {
        Box(Modifier.fillMaxSize()) {
            val stripe = AppTheme.colors.house.hallStripe
            val wall = AppTheme.colors.house.wallHighlight
            Canvas(Modifier.fillMaxSize()) {
                drawRect(wall)
                repeat(5) { index ->
                    drawRect(stripe, Offset(size.width * (index * .22f), 0f), Size(size.width * .1f, size.height))
                }
            }
            HouseHud(
                progress = RoomProgress(
                    balanceRub = 1399, playerLevel = 1, ownedZoneIds = emptySet(),
                    absoluteDay = 3, weekNumber = 1, dayOfWeek = 3, daysUntilAllowance = 5,
                    petHunger = 54, petHappiness = 68,
                    totalXp = 180, currentLevelXp = 80, nextLevelXp = 150,
                    experienceProgress = 80f / 150f,
                ),
                showMenu = true, showDetails = true,
                onMenuClick = {},
            )
        }
    }
}
