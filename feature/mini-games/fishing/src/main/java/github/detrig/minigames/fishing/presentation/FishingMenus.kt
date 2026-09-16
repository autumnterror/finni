package github.detrig.minigames.fishing.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import github.detrig.designsystem.component.*
import github.detrig.designsystem.theme.AppTheme
import github.detrig.minigames.fishing.R
import github.detrig.minigames.fishing.domain.*

@Composable
internal fun FishingRecords(state: FishingViewState, config: FishingConfig, action: (FishingViewEvent) -> Unit) {
    val progress = state.progress ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
        MenuHeader(R.string.fishing_records, action)
        val best = progress.records.firstOrNull { it.rulesVersion == config.rulesVersion && !it.assisted }
        Text(best?.let { formatFishingMass(it.grams) } ?: stringResource(R.string.fishing_first_ahead), style = AppTheme.typography.brand)
        if (best != null) {
            Text(stringResource(R.string.fishing_count, best.count), style = AppTheme.typography.body)
            Text(formatFishingDate(best.achievedAtMillis), style = AppTheme.typography.caption)
        }
        val old = progress.records.filterNot { it.rulesVersion == config.rulesVersion }.sortedByDescending { it.rulesVersion }
        if (old.isNotEmpty()) {
            Text(stringResource(R.string.fishing_old_records), style = AppTheme.typography.sectionTitle)
            old.forEach {
                if (it.assisted) Text(stringResource(R.string.fishing_assisted), style = AppTheme.typography.caption)
                Text(stringResource(R.string.fishing_rules_version, it.rulesVersion, formatFishingMass(it.grams)), style = AppTheme.typography.body)
                Text(formatFishingDate(it.achievedAtMillis), style = AppTheme.typography.caption)
            }
        }
    }
}

@Composable
internal fun FishingResults(
    state: FishingViewState,
    petBitmap: ImageBitmap?,
    action: (FishingViewEvent) -> Unit,
) {
    val result = state.progress?.lastResult ?: return
    val colors = AppTheme.colors
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.surfaceBase, colors.statusInfo.container)))) {
        Column(Modifier.widthIn(max = AppTheme.sizes.preferredTouchTarget * 8).fillMaxWidth()
            .align(Alignment.Center).verticalScroll(rememberScrollState()).padding(AppTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            petBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.size(AppTheme.sizes.preferredTouchTarget * 1.5f),
                )
            }
            Text(stringResource(R.string.fishing_result_title), style = AppTheme.typography.screenTitle)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Text(formatFishingMass(result.grams), style = AppTheme.typography.brand, modifier = Modifier.testTag("fishing_result_weight"))
                Text(stringResource(R.string.fishing_count, result.count), style = AppTheme.typography.bodyStrong,
                    color = colors.textSecondary, modifier = Modifier.testTag("fishing_result_count"))
            }
            run {
                FinPetCard(Modifier.fillMaxWidth(), containerColor = if (result.newRecord) colors.statusPositive.container else colors.surfaceElevated) {
                    Column(Modifier.fillMaxWidth().padding(AppTheme.spacing.lg), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                        Text(stringResource(if (result.newRecord) R.string.fishing_new_record else R.string.fishing_record),
                            style = AppTheme.typography.bodyStrong, color = if (result.newRecord) colors.statusPositive.onContainer else colors.textSecondary)
                        Text(formatFishingMass(result.recordGrams), style = AppTheme.typography.metricValue,
                            modifier = Modifier.testTag("fishing_result_record"))
                        if (result.newRecord && result.previousRecordGrams > 0) Text(formatFishingMass(result.previousRecordGrams),
                            style = AppTheme.typography.body, textDecoration = TextDecoration.LineThrough,
                            color = colors.textSecondary, modifier = Modifier.testTag("fishing_previous_record"))
                    }
                }
            }
            FinPetButton(stringResource(R.string.fishing_again),
                { action(FishingViewEvent.Start) },
                Modifier.fillMaxWidth().testTag("fishing_again"), !state.busy)
            TextButton(onClick = { action(FishingViewEvent.Exit) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.fishing_room)) }
        }
    }
}

@Composable
internal fun MenuHeader(title: Int, action: (FishingViewEvent) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Text(stringResource(title), Modifier.weight(1f), style = AppTheme.typography.screenTitle)
        FinPetOutlinedButton(stringResource(R.string.fishing_back), { action(FishingViewEvent.Back) })
    }
}
