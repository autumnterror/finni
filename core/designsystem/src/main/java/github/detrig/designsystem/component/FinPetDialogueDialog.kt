package github.detrig.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import github.detrig.designsystem.R
import github.detrig.designsystem.theme.AppTheme

/** Общая карточка реплик. Источник текста и портрета задаёт вызывающая фича. */
@Composable
fun FinPetDialogueDialog(
    speakerName: String,
    cards: List<String>,
    portrait: @Composable (Modifier) -> Unit,
    onFinished: () -> Unit,
) {
    require(cards.isNotEmpty()) { "Dialogue must contain at least one card" }
    var pageIndex by rememberSaveable(cards) { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    LaunchedEffect(pageIndex) { scrollState.scrollTo(0) }
    val lastIndex = cards.lastIndex
    val maxCardHeight = LocalConfiguration.current.screenHeightDp.dp * 0.75f
    val interactionSource = remember { MutableInteractionSource() }
    val tapHint = stringResource(
        if (pageIndex < lastIndex) R.string.dialogue_tap_next else R.string.dialogue_tap_finish,
    )

    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = onFinished,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClickLabel = tapHint,
                    role = Role.Button,
                ) {
                    if (pageIndex < lastIndex) pageIndex++ else onFinished()
                }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.xl)
                .testTag("finpet_dialogue"),
            contentAlignment = Alignment.TopCenter,
        ) {
            FinPetCard(
                modifier = Modifier.fillMaxWidth().widthIn(max = AppTheme.sizes.contentMaxWidth),
                shape = AppTheme.shapes.dialog,
                borderColor = AppTheme.colors.actionPrimary,
                borderWidth = AppTheme.sizes.borderStrong,
                elevation = AppTheme.elevation.high,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxCardHeight)
                        .verticalScroll(scrollState)
                        .padding(AppTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(AppTheme.sizes.dialoguePortrait)
                                .clip(CircleShape)
                                .background(AppTheme.colors.actionSecondary),
                            contentAlignment = Alignment.Center,
                        ) {
                            portrait(Modifier.fillMaxSize())
                        }
                        Column(
                            modifier = Modifier.weight(1f).heightIn(min = AppTheme.sizes.dialoguePortrait),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = speakerName,
                                style = AppTheme.typography.bodyStrong,
                                color = AppTheme.colors.actionPrimary,
                            )
                            Text(
                                text = cards[pageIndex],
                                style = AppTheme.typography.body,
                                color = AppTheme.colors.textPrimary,
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tapHint,
                            style = AppTheme.typography.caption,
                            color = AppTheme.colors.textSecondary,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.dialogue_progress, pageIndex + 1, cards.size),
                            style = AppTheme.typography.label,
                            color = AppTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
