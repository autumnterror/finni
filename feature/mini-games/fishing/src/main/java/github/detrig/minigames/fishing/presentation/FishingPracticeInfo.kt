package github.detrig.minigames.fishing.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import github.detrig.designsystem.component.*
import github.detrig.designsystem.theme.AppTheme
import github.detrig.minigames.fishing.R
import github.detrig.minigames.fishing.domain.*

@Composable
internal fun FishingPracticeInfo(state: FishingViewState, action: (FishingViewEvent) -> Unit) {
    var bomb by remember { mutableStateOf(false) }
    var lifted by remember(bomb) { mutableStateOf(false) }
    val colors = AppTheme.colors
    val fraction by animateFloatAsState(if (lifted) 1f else 0f,
        tween(if (state.progress?.preferences?.reducedMotion == true) 0 else AppTheme.motion.durationSlowMillis * 2), label = "practiceObject")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.fishing_how), style = AppTheme.typography.screenTitle)
        Text(stringResource(R.string.fishing_home_hint), style = AppTheme.typography.body)
        Text(stringResource(if (bomb) R.string.fishing_bomb_demo else R.string.fishing_junk_demo), style = AppTheme.typography.bodyStrong)
        FinPetCard(Modifier.fillMaxWidth()) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1.35f)) {
                val at = Offset(size.width * .5f, size.height * (.75f - .45f * fraction))
                drawLine(colors.statusInfo.accent, Offset(0f, size.height * .30f), Offset(size.width, size.height * .30f), size.width * .006f)
                drawLine(colors.textSecondary, Offset(size.width * .5f, 0f), at, size.width * .003f)
                fishingObject(if (bomb) ObjectKind.BOMB else ObjectKind.BOOT, at, size.width * .18f, colors, bomb && lifted)
                if (bomb && fraction >= 1f) drawCircle(colors.statusInfo.accent, size.width * .29f, at, style = Stroke(size.width * .012f))
            }
        }
        if (lifted) Text(stringResource(if (bomb) R.string.fishing_bomb_demo_result else R.string.fishing_junk_demo_result), style = AppTheme.typography.body)
        FinPetButton(stringResource(R.string.fishing_demo_reel), { lifted = true }, Modifier.fillMaxWidth(), !lifted)
        FinPetOutlinedButton(stringResource(if (bomb) R.string.fishing_understood else R.string.fishing_next),
            { if (bomb) action(FishingViewEvent.FinishPractice) else bomb = true }, Modifier.fillMaxWidth())
    }
}
