package github.detrig.minigames.fishing.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import github.detrig.designsystem.component.*
import github.detrig.designsystem.theme.AppTheme
import github.detrig.minigames.fishing.R
import github.detrig.minigames.fishing.domain.*

/** Весь свободный участок сцены принимает один жест; HUD не перехватывает заброс. */
@Composable
internal fun Modifier.fishingInput(state: FishingViewState, action: (FishingViewEvent) -> Unit): Modifier {
    val s = state.hud ?: return this
    val enabled by rememberUpdatedState(!s.paused && s.resumeSeconds <= 0 && state.error == null &&
        s.phase in listOf(FishingPhase.READY, FishingPhase.CHARGING, FishingPhase.SEARCHING, FishingPhase.FIGHTING, FishingPhase.HAULING))
    val currentAction by rememberUpdatedState(action)
    val hint = stringResource(when {
        s.phase == FishingPhase.FIGHTING || s.phase == FishingPhase.HAULING -> if (s.pulling) R.string.fishing_stop_reel else R.string.fishing_hold_reel
        s.warning -> R.string.fishing_relax
        s.phase == FishingPhase.CHARGING -> R.string.fishing_release_cast
        s.phase == FishingPhase.SEARCHING -> R.string.fishing_lift_hook
        else -> R.string.fishing_hold_cast
    })
    return this.testTag("fishing_action").semantics {
        role = Role.Button
        contentDescription = hint
        if (!enabled) disabled()
        onClick { if (enabled) { currentAction(FishingViewEvent.Toggle); true } else false }
    }.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown()
            if (!enabled) return@awaitEachGesture
            down.consume()
                currentAction(FishingViewEvent.Press)
                var ended = false
                try {
                    while (!ended) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                        if (change == null || change.position.x !in 0f..size.width.toFloat() ||
                            change.position.y !in 0f..size.height.toFloat()) {
                            currentAction(FishingViewEvent.CancelPress); ended = true
                        } else if (!change.pressed) {
                            change.consume(); currentAction(FishingViewEvent.Release); ended = true
                        } else change.consume()
                    }
                } finally {
                    if (!ended) currentAction(FishingViewEvent.CancelPress)
                }
        }
    }
}

/** Шкала поверх сцены: цвет, положение маркера и число сообщают одно состояние. */
@Composable
internal fun FishingControls(state: FishingViewState, config: FishingConfig, modifier: Modifier = Modifier) {
    val s = state.hud ?: return
    val colors = AppTheme.colors
    val fighting = s.phase == FishingPhase.FIGHTING || s.phase == FishingPhase.CATCH_PENDING
    val casting = s.phase in listOf(FishingPhase.COUNTDOWN, FishingPhase.READY, FishingPhase.CHARGING, FishingPhase.FLYING)
    val label = stringResource(when {
        fighting -> R.string.fishing_tension
        casting -> R.string.fishing_power
        s.attachedObject?.kind == ObjectKind.BOMB -> R.string.fishing_bomb_hooked
        s.phase == FishingPhase.HAULING -> R.string.fishing_junk
        s.phase == FishingPhase.EMPTY_RETURN -> R.string.fishing_returning
        s.phase == FishingPhase.RELEASING -> R.string.fishing_release_fish
        else -> R.string.fishing_depth
    })
    Column(modifier.background(colors.textPrimary.copy(alpha = .68f), AppTheme.shapes.card)
        .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm).testTag("fishing_hud"),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        val value = (if (fighting) s.tension / config.fight.maxTension else if (s.phase == FishingPhase.READY || s.phase == FishingPhase.COUNTDOWN) 0.0 else if (casting) s.power else s.hook.y / config.world.maxDepth).toFloat().coerceIn(0f, 1f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = AppTheme.typography.caption, color = colors.onActionPrimary)
            Text(if (fighting && s.warning) "! ${(value * 100).toInt()}%" else if (casting || fighting) "${(value * 100).toInt()}%"
                else stringResource(R.string.fishing_depth_value, (s.hook.y.coerceAtLeast(0.0) * 10).toInt()),
                style = AppTheme.typography.caption, color = colors.onActionPrimary)
        }
        Canvas(Modifier.fillMaxWidth().height(AppTheme.sizes.progressIndicator * 1.5f)
            .testTag(if (fighting) "fishing_tension" else "fishing_power").semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
            }) {
            val radius = CornerRadius(size.height / 2)
            drawRoundRect(colors.onActionPrimary.copy(alpha = .2f), cornerRadius = radius)
            val gradient = Brush.horizontalGradient(listOf(colors.intensityLow, colors.intensityMedium, colors.intensityHigh))
            clipRect(right = size.width * value) {
                if (fighting || casting) drawRoundRect(gradient, cornerRadius = radius)
                else drawRoundRect(colors.onActionPrimary, cornerRadius = radius)
            }
            val x = (size.width * value).coerceIn(size.height / 2, size.width - size.height / 2)
            drawCircle(colors.onActionPrimary, size.height * .65f, Offset(x, size.height / 2))
        }
    }
}

@Composable
internal fun FishingSettings(preferences: FishingPreferences, enabled: Boolean, action: (FishingViewEvent) -> Unit) {
    Text(stringResource(R.string.fishing_settings), style = AppTheme.typography.sectionTitle)
    SettingSwitch(R.string.fishing_sound, preferences.sound, enabled) { action(FishingViewEvent.Preferences(preferences.copy(sound = it))) }
    SettingSwitch(R.string.fishing_haptics, preferences.haptics, enabled) { action(FishingViewEvent.Preferences(preferences.copy(haptics = it))) }
    SettingSwitch(R.string.fishing_reduced_motion, preferences.reducedMotion, enabled) { action(FishingViewEvent.Preferences(preferences.copy(reducedMotion = it))) }
}

@Composable
private fun SettingSwitch(label: Int, checked: Boolean, enabled: Boolean, tag: String = "", change: (Boolean) -> Unit) {
    val text = stringResource(label)
    Row(Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.minimumTouchTarget), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        Text(text, Modifier.weight(1f), style = AppTheme.typography.body)
        Switch(checked, change, Modifier.semantics { contentDescription = text }.testTag(tag), enabled = enabled)
    }
}
