package github.detrig.feature.room.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.presentation.DayTransitionNoticeState
import kotlinx.coroutines.delay

@Composable
internal fun DayTransitionDialog(
    notice: DayTransitionNoticeState,
    onFinished: () -> Unit,
) {
    var visible by remember(notice) { mutableStateOf(false) }
    LaunchedEffect(notice) {
        visible = true
        delay(2_400)
        visible = false
        delay(220)
        onFinished()
    }
    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = {},
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.spacing.xl),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.96f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.98f),
            ) {
                val title = if (notice.dayOfWeek == 1) {
                    stringResource(R.string.day_transition_week_title, notice.weekNumber)
                } else {
                    val day = stringArrayResource(R.array.house_weekdays_full)[
                        (notice.dayOfWeek - 1).coerceIn(0, 6)
                    ]
                    stringResource(R.string.day_transition_title, day, notice.weekNumber)
                }
                FinPetCard(
                    containerColor = AppTheme.colors.storefront.surface,
                    contentColor = AppTheme.colors.storefront.onSurface,
                    borderColor = AppTheme.colors.storefront.outline,
                    borderWidth = AppTheme.sizes.borderStrong,
                    elevation = AppTheme.elevation.medium,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(
                            horizontal = AppTheme.spacing.xl,
                            vertical = AppTheme.spacing.lg,
                        ),
                        style = AppTheme.typography.screenTitle,
                    )
                }
            }
        }
    }
}

@Preview(name = "Новый игровой день", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun DayTransitionDialogPreview() {
    FinPetTheme {
        DayTransitionDialog(
            notice = DayTransitionNoticeState(dayOfWeek = 2, weekNumber = 6),
            onFinished = {},
        )
    }
}
