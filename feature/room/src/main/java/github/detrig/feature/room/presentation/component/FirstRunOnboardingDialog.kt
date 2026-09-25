package github.detrig.feature.room.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import github.detrig.designsystem.component.FinPetDialogueAction
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.component.FinPetStorefrontProgressIndicator
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.room.R
import github.detrig.feature.room.api.FirstRunOnboardingStep
import github.detrig.feature.room.presentation.FirstRunOnboardingState

@Composable
internal fun FirstRunOnboardingDialog(
    state: FirstRunOnboardingState,
    petName: String,
    petPortrait: @Composable (Modifier) -> Unit,
    topInset: Dp = 0.dp,
    onContinue: () -> Unit,
    onDepositSelected: (Boolean) -> Unit,
    onOpenPhone: () -> Unit = {},
    onOpenFridge: () -> Unit = {},
    onOpenTable: () -> Unit = {},
    onGoToBed: () -> Unit = {},
    onShowWeekSummary: () -> Unit = {},
    onStartNewWeekPlan: () -> Unit = {},
    onPageChanged: (Int) -> Unit = {},
) {
    val cards = state.cards(petName) ?: return
    val isDepositChoice = state.step == FirstRunOnboardingStep.FIRST_DEPOSIT
    val destinationAction = when (state.step) {
        FirstRunOnboardingStep.PHONE_GUIDANCE -> FinPetDialogueAction(
            id = OPEN_PHONE_ACTION_ID,
            label = stringResource(R.string.onboarding_open_phone),
        )
        FirstRunOnboardingStep.FRIDGE_GUIDANCE -> FinPetDialogueAction(
            id = OPEN_FRIDGE_ACTION_ID,
            label = stringResource(R.string.onboarding_open_fridge),
        )
        FirstRunOnboardingStep.TABLE_GUIDANCE -> FinPetDialogueAction(
            id = OPEN_TABLE_ACTION_ID,
            label = stringResource(R.string.onboarding_open_table),
        )
        FirstRunOnboardingStep.BEDTIME_GUIDANCE -> FinPetDialogueAction(
            id = GO_TO_BED_ACTION_ID,
            label = stringResource(R.string.onboarding_go_to_bed),
        )
        FirstRunOnboardingStep.WEEK_END_INTRO -> FinPetDialogueAction(
            id = SHOW_WEEK_SUMMARY_ACTION_ID,
            label = stringResource(R.string.onboarding_show_week_summary),
        )
        FirstRunOnboardingStep.NEW_WEEK_PLAN_GUIDANCE -> FinPetDialogueAction(
            id = START_NEW_WEEK_PLAN_ACTION_ID,
            label = stringResource(R.string.onboarding_start_new_week_plan),
        )
        else -> null
    }
    FinPetDialogueDialog(
        speakerName = petName,
        cards = cards,
        portrait = petPortrait,
        topInset = topInset,
        onPageChanged = onPageChanged,
        dismissOnBackPress = false,
        advanceOnTap = !isDepositChoice && destinationAction == null,
        actions = when {
            isDepositChoice -> listOf(
                FinPetDialogueAction(
                    id = DEPOSIT_NOW_ACTION_ID,
                    label = stringResource(R.string.onboarding_deposit_now),
                ),
                FinPetDialogueAction(
                    id = DEPOSIT_LATER_ACTION_ID,
                    label = stringResource(R.string.onboarding_deposit_later),
                ),
            )
            destinationAction != null -> listOf(destinationAction)
            else -> emptyList()
        },
        onActionSelected = { action ->
            when (action.id) {
                DEPOSIT_NOW_ACTION_ID -> onDepositSelected(true)
                DEPOSIT_LATER_ACTION_ID -> onDepositSelected(false)
                OPEN_PHONE_ACTION_ID -> onOpenPhone()
                OPEN_FRIDGE_ACTION_ID -> onOpenFridge()
                OPEN_TABLE_ACTION_ID -> onOpenTable()
                GO_TO_BED_ACTION_ID -> onGoToBed()
                SHOW_WEEK_SUMMARY_ACTION_ID -> onShowWeekSummary()
                START_NEW_WEEK_PLAN_ACTION_ID -> onStartNewWeekPlan()
            }
        },
        additionalContent = {
            if (state.step == FirstRunOnboardingStep.GOAL_CREATED) {
                GoalProgressCard(state)
            }
        },
        onFinished = onContinue,
    )
}

private const val DEPOSIT_NOW_ACTION_ID = "deposit_now"
private const val DEPOSIT_LATER_ACTION_ID = "deposit_later"
private const val OPEN_PHONE_ACTION_ID = "open_phone"
private const val OPEN_FRIDGE_ACTION_ID = "open_fridge"
private const val OPEN_TABLE_ACTION_ID = "open_table"
private const val GO_TO_BED_ACTION_ID = "go_to_bed"
private const val SHOW_WEEK_SUMMARY_ACTION_ID = "show_week_summary"
private const val START_NEW_WEEK_PLAN_ACTION_ID = "start_new_week_plan"

@Composable
private fun FirstRunOnboardingState.cards(petName: String): List<String>? = when (step) {
    FirstRunOnboardingStep.INTRODUCTION -> listOf(
        stringResource(R.string.onboarding_intro_1, petName),
        stringResource(R.string.onboarding_intro_2),
        stringResource(R.string.onboarding_intro_3),
    )
    FirstRunOnboardingStep.MONEY_EXPLANATION -> listOf(
        stringResource(R.string.onboarding_money_1),
        stringResource(R.string.onboarding_money_2),
    )
    FirstRunOnboardingStep.PLAN_TRANSITION -> listOf(
        stringResource(R.string.onboarding_plan_transition_1),
    )
    FirstRunOnboardingStep.GAME_DISCOVERY -> listOf(
        stringResource(R.string.onboarding_games_discovery_1),
    )
    FirstRunOnboardingStep.GAME_DISCOVERY_DETAILS -> listOf(
        stringResource(R.string.onboarding_games_discovery_2),
        stringResource(R.string.onboarding_game_selected_1),
        stringResource(R.string.onboarding_game_selected_2),
        stringResource(R.string.onboarding_game_selected_3),
    )
    FirstRunOnboardingStep.PIGGY_BANK -> listOf(
        stringResource(R.string.onboarding_piggy_1),
    )
    FirstRunOnboardingStep.PIGGY_TAP -> listOf(
        stringResource(R.string.onboarding_piggy_5),
    )
    FirstRunOnboardingStep.GOAL_CREATED -> listOf(
        stringResource(R.string.onboarding_goal_created_1, goalTitle.orEmpty()),
        stringResource(
            if (goalSavedRub > 0) {
                R.string.onboarding_goal_created_with_savings
            } else {
                R.string.onboarding_goal_created_2
            },
        ),
        stringResource(R.string.onboarding_goal_created_3),
    )
    FirstRunOnboardingStep.FIRST_DEPOSIT -> listOf(
        stringResource(R.string.onboarding_deposit_question),
    )
    FirstRunOnboardingStep.DEPOSIT_DONE -> listOf(
        stringResource(R.string.onboarding_deposit_done),
    )
    FirstRunOnboardingStep.DEPOSIT_SKIPPED -> listOf(
        stringResource(R.string.onboarding_deposit_skipped),
    )
    FirstRunOnboardingStep.HUNGER_INTRO -> listOf(stringResource(R.string.onboarding_hunger_intro))
    FirstRunOnboardingStep.HUNGER_FIND_FOOD -> listOf(stringResource(R.string.onboarding_hunger_find_food))
    FirstRunOnboardingStep.PHONE_GUIDANCE -> listOf(stringResource(R.string.onboarding_phone_guidance))
    FirstRunOnboardingStep.PURCHASE_READY -> listOf(stringResource(R.string.onboarding_purchase_ready))
    FirstRunOnboardingStep.PURCHASE_STORAGE_HINT ->
        listOf(stringResource(R.string.onboarding_purchase_storage_hint))
    FirstRunOnboardingStep.FRIDGE_GUIDANCE -> listOf(stringResource(R.string.onboarding_fridge_guidance))
    FirstRunOnboardingStep.TABLE_PROMPT -> listOf(stringResource(R.string.onboarding_table_prompt))
    FirstRunOnboardingStep.TABLE_GUIDANCE -> listOf(stringResource(R.string.onboarding_table_guidance))
    FirstRunOnboardingStep.BEDTIME_LATE -> listOf(stringResource(R.string.onboarding_bedtime_late))
    FirstRunOnboardingStep.BEDTIME_GUIDANCE -> listOf(stringResource(R.string.onboarding_bedtime_guidance))
    FirstRunOnboardingStep.SECOND_DAY_MORNING -> listOf(
        stringResource(R.string.onboarding_second_day_morning),
        stringResource(R.string.onboarding_second_day_free),
    )
    FirstRunOnboardingStep.WEEK_END_INTRO -> listOf(
        stringResource(R.string.onboarding_week_end_wait),
        stringResource(R.string.onboarding_week_end_finished),
        stringResource(R.string.onboarding_week_end_guidance),
    )
    FirstRunOnboardingStep.NEW_WEEK_INTRO -> listOf(stringResource(R.string.onboarding_new_week_intro))
    FirstRunOnboardingStep.NEW_WEEK_PLAN_GUIDANCE ->
        listOf(stringResource(R.string.onboarding_new_week_plan_guidance))
    FirstRunOnboardingStep.WISH,
    FirstRunOnboardingStep.FIRST_MONEY,
    FirstRunOnboardingStep.PLAN,
    FirstRunOnboardingStep.GAME_SELECTION,
    FirstRunOnboardingStep.GAME_SELECTED,
    FirstRunOnboardingStep.WAITING_FOR_PIGGY,
    FirstRunOnboardingStep.WAITING_FOR_GOAL,
    FirstRunOnboardingStep.WAITING_FOR_DEPOSIT,
    FirstRunOnboardingStep.GAMES,
    FirstRunOnboardingStep.FINISH,
    FirstRunOnboardingStep.WAITING_FOR_HUNGER,
    FirstRunOnboardingStep.PHONE_STORE_GUIDANCE,
    FirstRunOnboardingStep.SHOP_PRICE_GUIDANCE,
    FirstRunOnboardingStep.SHOP_FOOD_GUIDANCE,
    FirstRunOnboardingStep.FRIDGE_FOUND,
    FirstRunOnboardingStep.FRIDGE_EXPLANATION,
    FirstRunOnboardingStep.WAITING_FOR_FRIDGE_CLOSE,
    FirstRunOnboardingStep.FEEDING,
    FirstRunOnboardingStep.FEEDING_DONE,
    FirstRunOnboardingStep.WAITING_FOR_BED,
    FirstRunOnboardingStep.WAITING_FOR_WEEK_END,
    FirstRunOnboardingStep.WEEK_SUMMARY_VIEW,
    FirstRunOnboardingStep.COMPLETED,
    -> null
}

@Composable
private fun GoalProgressCard(state: FirstRunOnboardingState) {
    val target = state.goalTargetRub?.coerceAtLeast(1) ?: 1
    FinPetModalSection(
        modifier = Modifier.fillMaxWidth(),
        tone = FinPetModalSectionTone.Highlighted,
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Text(
                text = state.goalTitle.orEmpty(),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.storefront.onSurface,
            )
            Text(
                text = stringResource(R.string.onboarding_goal_progress, state.goalSavedRub, target),
                style = AppTheme.typography.body,
            )
            FinPetStorefrontProgressIndicator(
                progress = (state.goalSavedRub.toFloat() / target).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Знакомство с копилкой", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun FirstRunOnboardingDialogPreview() {
    FinPetTheme {
        FirstRunOnboardingDialog(
            state = FirstRunOnboardingState(FirstRunOnboardingStep.PIGGY_TAP),
            petName = "Финни",
            petPortrait = { modifier ->
                Box(modifier.background(AppTheme.colors.actionSecondary), contentAlignment = Alignment.Center) {
                    Text("🐹", style = AppTheme.typography.screenTitle)
                }
            },
            onContinue = {},
            onDepositSelected = {},
        )
    }
}
