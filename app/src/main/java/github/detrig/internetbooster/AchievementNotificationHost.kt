package github.detrig.internetbooster

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetAchievementBanner
import github.detrig.designsystem.component.FinPetDialogueAction
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.LocalFinPetDialogueTopInset
import github.detrig.designsystem.component.LocalFinPetModalVisibilityReporter
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.learning.LearningFeature
import kotlinx.coroutines.delay

private data class AchievementNotice(val id: String, val title: String)

/** Единственная очередь уведомлений о достижениях поверх всей навигации приложения. */
@Composable
internal fun AchievementNotificationHost(content: @Composable () -> Unit) {
    val learningApi = remember { LearningFeature.getApi() }
    var knownUnlockedIds by remember { mutableStateOf<Set<String>?>(null) }
    var notices by remember { mutableStateOf<List<AchievementNotice>>(emptyList()) }
    var bannerHeight by remember { mutableStateOf(0.dp) }
    var activeModalCount by remember { mutableIntStateOf(0) }
    val reportModalVisibility: (Boolean) -> Unit = remember {
        { visible ->
            activeModalCount = (activeModalCount + if (visible) 1 else -1).coerceAtLeast(0)
        }
    }

    LaunchedEffect(learningApi) {
        learningApi.observeAchievements(CURRENT_PROFILE_ID).collect { achievements ->
            val unlocked = achievements.filter { it.isUnlocked }
            val ids = unlocked.map { it.definition.achievementId }.toSet()
            val previous = knownUnlockedIds
            if (previous != null) {
                val queuedIds = notices.map { it.id }.toSet()
                notices += unlocked.filter { progress ->
                    progress.definition.achievementId !in previous &&
                        progress.definition.achievementId !in queuedIds
                }.map { progress ->
                    AchievementNotice(
                        id = progress.definition.achievementId,
                        title = progress.definition.childTitle,
                    )
                }
            }
            knownUnlockedIds = ids
        }
    }

    val current = notices.firstOrNull().takeIf { activeModalCount == 0 }
    LaunchedEffect(current?.id) {
        if (current != null) {
            delay(ACHIEVEMENT_NOTICE_DURATION_MS)
            notices = notices.filterNot { it.id == current.id }
        }
    }

    val dialogueTopInset = if (current == null) {
        0.dp
    } else {
        maxOf(bannerHeight, MINIMUM_BANNER_HEIGHT) + AppTheme.spacing.md
    }
    CompositionLocalProvider(
        LocalFinPetDialogueTopInset provides dialogueTopInset,
        LocalFinPetModalVisibilityReporter provides reportModalVisibility,
    ) {
        content()
    }
    current?.let { notice ->
        FinPetAchievementBanner(
            title = notice.title,
            onDismiss = { notices = notices.filterNot { it.id == notice.id } },
            onHeightChanged = { bannerHeight = it },
        )
    }
}

private const val CURRENT_PROFILE_ID = "current"
private const val ACHIEVEMENT_NOTICE_DURATION_MS = 5_000L
private val MINIMUM_BANNER_HEIGHT = 112.dp

@Preview(name = "Диалог под достижением", widthDp = 360, heightDp = 640)
@Composable
private fun AchievementNotificationPreview() {
    FinPetTheme {
        CompositionLocalProvider(LocalFinPetDialogueTopInset provides 140.dp) {
            FinPetDialogueDialog(
                speakerName = "Финни",
                cards = listOf("Хочешь положить первые деньги в копилку сейчас?"),
                portrait = { modifier -> Box(modifier.background(AppTheme.colors.currencyContainer)) },
                actions = listOf(
                    FinPetDialogueAction("now", "Положить деньги"),
                    FinPetDialogueAction("later", "Сделаю позже"),
                ),
                onFinished = {},
            )
        }
        FinPetAchievementBanner(title = "Первый план", onDismiss = {})
    }
}
