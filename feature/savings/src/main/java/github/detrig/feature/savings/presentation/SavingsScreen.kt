package github.detrig.feature.savings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.component.FinPetAmountInput
import github.detrig.designsystem.component.FinPetBackButton
import github.detrig.designsystem.component.FinPetButton
import github.detrig.designsystem.component.FinPetButtonDefaults
import github.detrig.designsystem.component.FinPetDialogueAction
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.FinPetModalDialog
import github.detrig.designsystem.component.FinPetModalSection
import github.detrig.designsystem.component.FinPetModalSectionTone
import github.detrig.designsystem.component.FinPetMoneyAmount
import github.detrig.designsystem.component.FinPetOutlinedButton
import github.detrig.designsystem.component.FinPetStorefrontCard
import github.detrig.designsystem.component.FinPetStorefrontProgressIndicator
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.feature.economy.domain.EconomyState
import github.detrig.feature.economy.domain.PeriodicIncome
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.economy.domain.SavingsGoalProgress
import github.detrig.feature.savings.R
import github.detrig.feature.savings.SavingsFeature
import github.detrig.feature.savings.SavingsRoomBackdrop
import github.detrig.feature.savings.api.SavingsGoalDraft
import github.detrig.feature.pet.api.PetApi
import kotlinx.coroutines.launch

@Composable
internal fun SavingsScreen(
    firstRunOnboarding: Boolean,
    suggestedGoalId: String?,
    petApi: PetApi,
    roomBackdrop: SavingsRoomBackdrop,
) {
    val viewModel: SavingsViewModel = viewModel {
        SavingsFeature.component().viewModel(firstRunOnboarding, suggestedGoalId)
    }
    val state by viewModel.state().observeAsState(SavingsViewState())
    LaunchedEffect(viewModel) { viewModel.perform(SavingsViewEvent.Load) }
    BackHandler { viewModel.perform(SavingsViewEvent.Back) }
    petApi.RequirePet(modifier = Modifier.fillMaxSize()) { profile, _, _, _ ->
        Box(Modifier.fillMaxSize()) {
            roomBackdrop.Content(
                modifier = Modifier.fillMaxSize(),
                petContent = { modifier -> petApi.Content(profile, modifier) },
            )
            SavingsContent(
                state = state,
                petName = profile.name,
                petPortrait = { modifier -> petApi.Portrait(profile, modifier) },
                onEvent = viewModel::perform,
            )
        }
    }
}

@Composable
private fun SavingsContent(
    state: SavingsViewState,
    petName: String,
    petPortrait: @Composable (Modifier) -> Unit,
    onEvent: (SavingsViewEvent) -> Unit,
) {
    val entranceOffset = remember { Animatable(0.9f) }
    val entranceRotation = remember { Animatable(8f) }
    LaunchedEffect(Unit) {
        launch {
            entranceOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 760
                    0.9f at 0
                    0.34f at 230
                    -0.04f at 505
                    0f at 760
                },
            )
        }
        entranceRotation.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 760
                8f at 0
                3f at 230
                -2f at 505
                0f at 760
            },
        )
    }
    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.BottomCenter,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .widthIn(max = AppTheme.sizes.contentMaxWidth)
                    .graphicsLayer {
                        translationY = size.height * entranceOffset.value
                        rotationZ = entranceRotation.value
                    },
            ) {
                val horizontalContentInset = maxWidth * 0.14f
                val topContentInset = maxHeight * 0.12f
                val bottomContentInset = maxHeight * 0.115f
                val frameBackgroundHorizontalInset = maxWidth * 0.12f
                val frameBackgroundTopInset = maxHeight * 0.105f
                val frameBackgroundBottomInset = maxHeight * 0.10f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = frameBackgroundHorizontalInset,
                            top = frameBackgroundTopInset,
                            end = frameBackgroundHorizontalInset,
                            bottom = frameBackgroundBottomInset,
                        )
                        .background(
                            color = AppTheme.colors.surfaceBase,
                            shape = AppTheme.shapes.storefrontControl,
                        ),
                )
                Image(
                    painter = painterResource(R.drawable.savings_pig_frame),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = horizontalContentInset,
                            top = topContentInset,
                            end = horizontalContentInset,
                            bottom = bottomContentInset,
                        ),
                ) {
                    SavingsHeader(onBack = { onEvent(SavingsViewEvent.Back) })
                    when {
                        state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = AppTheme.colors.storefront.outline)
                        }

                        else -> SavingsBody(state = state, onEvent = onEvent)
                    }
                }
            }
        }
    }
    state.transferDirection?.let { direction ->
        TransferDialog(direction, state.busy,
            onDismiss = { onEvent(SavingsViewEvent.TransferDismissed) },
            onConfirm = { onEvent(SavingsViewEvent.TransferConfirmed(it)) })
    }
    state.notice?.let { notice ->
        val dismissNotice = { onEvent(SavingsViewEvent.NoticeDismissed) }
        FinPetModalDialog(
            title = stringResource(R.string.savings_notice_title),
            onDismissRequest = dismissNotice,
            modifier = Modifier.testTag("savings_notice"),
            actions = {
                FinPetButton(
                    text = stringResource(R.string.savings_ok),
                    onClick = dismissNotice,
                    modifier = Modifier.fillMaxWidth(),
                    style = FinPetButtonDefaults.storefrontPrimaryStyle(),
                )
            },
        ) {
            FinPetModalSection(
                modifier = Modifier.fillMaxWidth(),
                tone = notice.sectionTone(),
            ) {
                Text(
                    text = notice.message(),
                    modifier = Modifier.padding(AppTheme.spacing.md),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.storefront.onSurface,
                )
            }
        }
    }
    if (state.notice == null && state.transferDirection == null) {
        SavingsOnboardingDialog(
            state = state,
            petName = petName,
            petPortrait = petPortrait,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun ColumnScope.SavingsBody(
    state: SavingsViewState,
    onEvent: (SavingsViewEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(top = AppTheme.spacing.sm, bottom = AppTheme.spacing.sm)
            .testTag("savings_screen"),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        state.economy?.let { BalanceCard(it.availableRub, it.savingsRub) }
        state.goal?.let { GoalCard(it) }
        Text(
            text = stringResource(
                if (state.goal == null) R.string.savings_choose_goal else R.string.savings_other_goals,
            ),
            style = AppTheme.typography.sectionTitle,
            color = AppTheme.colors.storefront.onSurface,
        )
        GoalChoices(
            goals = state.starterGoals,
            activeGoalId = state.goal?.goal?.id,
            suggestedGoalId = state.suggestedGoalId,
            busy = state.busy,
            onGoal = { onEvent(SavingsViewEvent.GoalSelected(it)) },
        )
        if (state.goal != null) {
            FinPetButton(
                text = stringResource(R.string.savings_deposit),
                onClick = {
                    onEvent(SavingsViewEvent.TransferOpened(SavingsTransferDirection.DEPOSIT))
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().testTag("savings_deposit"),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
            FinPetOutlinedButton(
                text = stringResource(R.string.savings_withdraw),
                onClick = {
                    onEvent(SavingsViewEvent.TransferOpened(SavingsTransferDirection.WITHDRAW))
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().testTag("savings_withdraw"),
                style = FinPetButtonDefaults.storefrontOutlinedStyle(),
            )
        }
    }
}

@Composable
private fun SavingsOnboardingDialog(
    state: SavingsViewState,
    petName: String,
    petPortrait: @Composable (Modifier) -> Unit,
    onEvent: (SavingsViewEvent) -> Unit,
) {
    val step = state.onboardingStep ?: return
    val cards = when (step) {
        SavingsOnboardingStep.INTRODUCTION -> listOf(
            stringResource(R.string.savings_onboarding_intro_1),
            stringResource(R.string.savings_onboarding_intro_2),
            stringResource(R.string.savings_onboarding_intro_3),
        )
        SavingsOnboardingStep.GOAL_CREATED -> listOf(
            stringResource(R.string.savings_onboarding_goal_created),
            stringResource(R.string.savings_onboarding_goal_hint),
        )
        SavingsOnboardingStep.FIRST_DEPOSIT -> listOf(
            stringResource(R.string.savings_onboarding_deposit_question),
        )
        SavingsOnboardingStep.DEPOSIT_DONE -> listOf(
            stringResource(R.string.savings_onboarding_deposit_done),
        )
        SavingsOnboardingStep.DEPOSIT_SKIPPED -> listOf(
            stringResource(R.string.savings_onboarding_deposit_skipped),
        )
        SavingsOnboardingStep.SELECT_GOAL,
        SavingsOnboardingStep.WAITING_FOR_DEPOSIT,
        -> return
    }
    val isDepositChoice = step == SavingsOnboardingStep.FIRST_DEPOSIT
    FinPetDialogueDialog(
        speakerName = petName,
        cards = cards,
        portrait = petPortrait,
        dismissOnBackPress = false,
        advanceOnTap = !isDepositChoice,
        actions = if (isDepositChoice) {
            listOf(
                FinPetDialogueAction(
                    id = ONBOARDING_DEPOSIT_NOW_ACTION_ID,
                    label = stringResource(R.string.savings_onboarding_deposit_now),
                ),
                FinPetDialogueAction(
                    id = ONBOARDING_DEPOSIT_LATER_ACTION_ID,
                    label = stringResource(R.string.savings_onboarding_deposit_later),
                ),
            )
        } else {
            emptyList()
        },
        onActionSelected = { action ->
            onEvent(
                SavingsViewEvent.OnboardingDepositSelected(
                    depositNow = action.id == ONBOARDING_DEPOSIT_NOW_ACTION_ID,
                ),
            )
        },
        onFinished = { onEvent(SavingsViewEvent.OnboardingContinue) },
    )
}

private const val ONBOARDING_DEPOSIT_NOW_ACTION_ID = "deposit_now"
private const val ONBOARDING_DEPOSIT_LATER_ACTION_ID = "deposit_later"

@Composable
private fun SavingsHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(AppTheme.sizes.preferredTouchTarget),
    ) {
        FinPetBackButton(
            onClick = onBack,
            contentDescription = stringResource(R.string.savings_back),
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = stringResource(R.string.savings_title),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppTheme.sizes.preferredTouchTarget),
            style = AppTheme.typography.screenTitle,
            color = AppTheme.colors.storefront.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BalanceCard(availableRub: Long, savingsRub: Long) {
    FinPetStorefrontCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = AppTheme.colors.storefront.selectedSurface,
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.savings_total_label),
                    style = AppTheme.typography.sectionTitle,
                    color = AppTheme.colors.storefront.onSurface,
                )
                FinPetMoneyAmount(savingsRub.toString())
                Text(
                    text = stringResource(R.string.savings_encouragement),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.storefront.onSurface,
                )
                Text(
                    text = stringResource(R.string.savings_wallet, availableRub),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
            }
            Image(
                painter = painterResource(R.drawable.savings_coins),
                contentDescription = null,
                modifier = Modifier.size(82.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun GoalCard(progress: SavingsGoalProgress) {
    FinPetStorefrontCard(Modifier.fillMaxWidth().testTag("savings_goal")) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.savings_goal_label),
                style = AppTheme.typography.sectionTitle,
                color = AppTheme.colors.storefront.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(savingsGoalArtwork(progress.goal.id, progress.goal.title)),
                    contentDescription = null,
                    modifier = Modifier.size(58.dp),
                    contentScale = ContentScale.Fit,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                ) {
                    Text(
                        text = progress.goal.title,
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.storefront.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.savings_progress,
                            progress.savedRub,
                            progress.goal.targetRub,
                        ),
                        style = AppTheme.typography.bodyStrong,
                    )
                    FinPetStorefrontProgressIndicator(
                        progress = (progress.savedRub.toFloat() / progress.goal.targetRub)
                            .coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Text(
                text = if (progress.isReached) {
                    stringResource(R.string.savings_reached)
                } else {
                    stringResource(R.string.savings_remaining, progress.remainingRub)
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = AppTheme.typography.caption,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GoalChoices(
    goals: List<SavingsGoalDraft>,
    activeGoalId: String?,
    suggestedGoalId: String?,
    busy: Boolean,
    onGoal: (SavingsGoalDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        goals.filterNot { it.id == activeGoalId }.forEach { goal ->
            FinPetOutlinedButton(
                onClick = { onGoal(goal) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontOutlinedStyle(),
            ) {
                Image(
                    painter = painterResource(savingsGoalArtwork(goal.id, goal.title)),
                    contentDescription = null,
                    modifier = Modifier.size(AppTheme.sizes.iconLarge),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(AppTheme.spacing.md))
                Text(
                    text = if (goal.id == suggestedGoalId) {
                        stringResource(R.string.savings_goal_suggested_title, goal.title)
                    } else {
                        goal.title
                    },
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.storefront.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(AppTheme.spacing.sm))
                Text(
                    text = stringResource(R.string.savings_goal_price, goal.targetRub),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.storefront.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun savingsGoalArtwork(goalId: String, title: String): Int {
    val searchable = "$goalId $title".lowercase()
    return when {
        "music" in searchable || "музык" in searchable -> R.drawable.savings_goal_music
        "fish" in searchable || "рыбал" in searchable -> R.drawable.savings_goal_fishing
        "draw" in searchable || "рисован" in searchable -> R.drawable.savings_goal_drawing
        else -> R.drawable.savings_goal_dream
    }
}

@Composable
private fun TransferDialog(
    direction: SavingsTransferDirection,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var amount by rememberSaveable(direction) { mutableStateOf("") }
    val parsed = amount.toLongOrNull()?.takeIf { it > 0 }
    FinPetModalDialog(
        title = stringResource(
            if (direction == SavingsTransferDirection.DEPOSIT) {
                R.string.savings_deposit_title
            } else {
                R.string.savings_withdraw_title
            },
        ),
        onDismissRequest = onDismiss,
        dismissEnabled = !busy,
        modifier = Modifier.testTag("savings_transfer_dialog"),
        actions = {
            FinPetButton(
                text = stringResource(
                    if (direction == SavingsTransferDirection.DEPOSIT) {
                        R.string.savings_confirm_deposit
                    } else {
                        R.string.savings_confirm_withdraw
                    },
                ),
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null && !busy,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontPrimaryStyle(),
            )
            FinPetOutlinedButton(
                text = stringResource(R.string.savings_cancel),
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                style = FinPetButtonDefaults.storefrontOutlinedStyle(),
            )
        },
    ) {
        Text(
            stringResource(
                if (direction == SavingsTransferDirection.DEPOSIT) {
                    R.string.savings_deposit_description
                } else {
                    R.string.savings_withdraw_description
                },
            ),
            style = AppTheme.typography.body,
        )
        Text(
            text = stringResource(R.string.savings_amount),
            style = AppTheme.typography.bodyStrong,
            color = AppTheme.colors.storefront.onSurface,
        )
        FinPetAmountInput(
            value = amount,
            onValueChange = { value -> amount = value.filter { character -> character.isDigit() } },
            enabled = !busy,
            suffix = stringResource(R.string.savings_rub),
            modifier = Modifier.fillMaxWidth().testTag("savings_transfer_amount"),
        )
    }
}

private fun SavingsNotice.sectionTone(): FinPetModalSectionTone = when (this) {
    is SavingsNotice.Rejected -> FinPetModalSectionTone.Warning
    SavingsNotice.GoalSaved,
    is SavingsNotice.TransferCompleted -> FinPetModalSectionTone.Highlighted
}

@Composable
private fun SavingsNotice.message(): String = when (this) {
    is SavingsNotice.TransferCompleted -> stringResource(
        if (direction == SavingsTransferDirection.DEPOSIT) R.string.savings_deposit_done else R.string.savings_withdraw_done,
        amountRub,
    )
    SavingsNotice.GoalSaved -> stringResource(R.string.savings_goal_saved)
    is SavingsNotice.Rejected -> when (reason) {
        RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS -> stringResource(R.string.savings_not_enough_wallet, missingRub)
        RejectionReason.INSUFFICIENT_SAVINGS -> stringResource(R.string.savings_not_enough_savings, missingRub)
        else -> stringResource(R.string.savings_transfer_error)
    }
}

@Preview(name = "Копилка", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun SavingsContentPreview() {
    val income = PeriodicIncome(
        amountRub = 500,
        periodMillis = 604_800_000,
        nextAtMillis = 1_700_000_000_000,
    )
    FinPetTheme {
        SavingsContent(
            state = SavingsViewState(
                economy = EconomyState(
                    availableRub = 480,
                    savingsRub = 160,
                    debtRub = 0,
                    periodicIncome = income,
                ),
                goal = SavingsGoalProgress(
                    goal = SavingsGoal(
                        id = "starter:drawing-set",
                        title = "Набор для рисования",
                        targetRub = 500,
                    ),
                    savedRub = 160,
                    remainingRub = 340,
                    isReached = false,
                    nextPeriodicIncome = income,
                ),
                starterGoals = listOf(
                    SavingsGoalDraft("room-zone:fishing", "Рыбалка", 400),
                    SavingsGoalDraft("starter:drawing-set", "Набор для рисования", 500),
                    SavingsGoalDraft("starter:big-dream", "Большая мечта", 800),
                ),
                loading = false,
            ),
            petName = "Финни",
            petPortrait = { modifier ->
                Box(modifier = modifier, contentAlignment = Alignment.Center) {
                    Text("🐹", style = AppTheme.typography.screenTitle)
                }
            },
            onEvent = {},
        )
    }
}
