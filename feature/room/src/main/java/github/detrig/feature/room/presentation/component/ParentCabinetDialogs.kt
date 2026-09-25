package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import github.detrig.designsystem.component.FinPetAmountInput
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.learning.domain.AchievementStage
import github.detrig.feature.learning.domain.LearningTopicIds
import github.detrig.feature.learning.domain.ParentProgressRow
import github.detrig.feature.room.R
import github.detrig.feature.room.presentation.ParentGateState
import github.detrig.feature.room.presentation.PlanAchievementFeedback

@Composable
internal fun ParentGateDialog(
    state: ParentGateState,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    FinPetModalDialog(
        title = stringResource(R.string.parent_gate_title),
        onDismissRequest = onDismiss,
        actions = {
            FinPetButton(
                text = stringResource(R.string.parent_gate_open),
                onClick = onSubmit,
                enabled = state.answer.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
        },
    ) {
        Text(
            text = stringResource(R.string.parent_gate_hint),
            style = AppTheme.typography.body,
        )
        Text(
            text = stringResource(R.string.parent_gate_expression, state.firstNumber, state.secondNumber),
            style = AppTheme.typography.metricValue,
        )
        val answerLabel = stringResource(R.string.parent_gate_answer)
        FinPetAmountInput(
            value = state.answer,
            onValueChange = onAnswerChange,
            suffix = "",
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = answerLabel },
        )
        if (state.hasError) {
            Text(
                text = stringResource(R.string.parent_gate_error),
                style = AppTheme.typography.body,
                color = AppTheme.colors.statusCritical.accent,
            )
        }
    }
}

@Composable
internal fun ParentCabinetDialog(
    achievements: List<PlanAchievementFeedback>,
    parentRows: List<ParentProgressRow>,
    onDismiss: () -> Unit,
) {
    val unlockedCount = achievements.count { it.isUnlocked }
    FinPetModalDialog(
        title = stringResource(R.string.parent_cabinet_title),
        onDismissRequest = onDismiss,
        actions = {
            FinPetButton(
                text = stringResource(R.string.achievements_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
        },
    ) {
        FinPetModalSection(
            modifier = Modifier.fillMaxWidth(),
            tone = FinPetModalSectionTone.Highlighted,
        ) {
            Column(
                modifier = Modifier.padding(AppTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                Text(stringResource(R.string.parent_cabinet_goal), style = AppTheme.typography.body)
                Text(
                    stringResource(R.string.parent_cabinet_overall, unlockedCount, achievements.size),
                    style = AppTheme.typography.bodyStrong,
                )
            }
        }
        if (parentRows.isEmpty()) {
            Text(
                text = stringResource(R.string.parent_cabinet_empty),
                style = AppTheme.typography.body,
            )
        }
        listOf(
            LearningTopicIds.BUDGET_PLANNING to R.string.parent_topic_budget,
            LearningTopicIds.SAVINGS_BUILDING to R.string.parent_topic_savings,
            LearningTopicIds.PAYMENTS_AND_PURCHASES to R.string.parent_topic_purchases,
            LearningTopicIds.FINANCIAL_SECURITY to R.string.parent_topic_security,
        ).forEach { (topicId, titleRes) ->
            val topicRows = parentRows.filter { it.topicId == topicId }
            val total = achievements.count { it.topicId == topicId }
            FinPetModalSection(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(AppTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    Text(stringResource(titleRes), style = AppTheme.typography.sectionTitle)
                    Text(
                        stringResource(R.string.parent_cabinet_topic_progress, topicRows.size, total),
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.textSecondary,
                    )
                    if (topicRows.isEmpty()) {
                        Text(stringResource(R.string.parent_cabinet_topic_empty), style = AppTheme.typography.body)
                    } else {
                        topicRows.forEach { row ->
                            Text(row.text, style = AppTheme.typography.body)
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Проверка взрослого", widthDp = 360, heightDp = 360, showBackground = true)
@Composable
private fun ParentGatePreview() {
    FinPetTheme {
        ParentGateDialog(ParentGateState(8, 7), onAnswerChange = {}, onSubmit = {}, onDismiss = {})
    }
}

@Preview(name = "Родительский кабинет", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun ParentCabinetPreview() {
    FinPetTheme {
        ParentCabinetDialog(
            achievements = listOf(
                PlanAchievementFeedback("first-plan", LearningTopicIds.BUDGET_PLANNING, "Первый план", "", true),
                PlanAchievementFeedback("good-plan", LearningTopicIds.BUDGET_PLANNING, "Продуманный план", "", true),
            ),
            parentRows = listOf(
                ParentProgressRow("first-plan", LearningTopicIds.BUDGET_PLANNING,
                    AchievementStage.INTRODUCTION,
                    "Ребёнок познакомился с недельным планом.", 1),
                ParentProgressRow("good-plan", LearningTopicIds.BUDGET_PLANNING,
                    AchievementStage.LEARNED,
                    "Ребёнок умеет оставлять деньги на важное.", 2),
            ),
            onDismiss = {},
        )
    }
}
