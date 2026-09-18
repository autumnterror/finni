package github.detrig.feature.savings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.component.FinPetProgressIndicator
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.savings.R
import github.detrig.feature.savings.SavingsFeature
import github.detrig.feature.savings.api.SavingsGoalDraft

@Composable
internal fun SavingsScreen() {
    val viewModel: SavingsViewModel = viewModel { SavingsFeature.component().viewModel() }
    val state by viewModel.state().observeAsState(SavingsViewState())
    LaunchedEffect(viewModel) { viewModel.perform(SavingsViewEvent.Load) }
    BackHandler { viewModel.perform(SavingsViewEvent.Back) }
    SavingsContent(state, viewModel::perform)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsContent(state: SavingsViewState, onEvent: (SavingsViewEvent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.savings_title)) },
                navigationIcon = {
                    TextButton(onClick = { onEvent(SavingsViewEvent.Back) }) {
                        Text(stringResource(R.string.savings_back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            else -> Column(
                Modifier.fillMaxSize().padding(padding).padding(AppTheme.spacing.lg)
                    .verticalScroll(rememberScrollState()).testTag("savings_screen"),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            ) {
                val economy = state.economy
                if (economy != null) BalanceCard(economy.availableRub, economy.savingsRub)
                state.goal?.let { GoalCard(it) }
                if (state.goal == null) {
                    Text(stringResource(R.string.savings_choose_goal), style = AppTheme.typography.sectionTitle)
                } else {
                    Text(stringResource(R.string.savings_other_goals), style = AppTheme.typography.label)
                }
                GoalChoices(state.starterGoals, state.busy) { onEvent(SavingsViewEvent.GoalSelected(it)) }
                if (state.goal != null) {
                    Button(
                        onClick = { onEvent(SavingsViewEvent.TransferOpened(SavingsTransferDirection.DEPOSIT)) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.minimumTouchTarget)
                            .testTag("savings_deposit"),
                    ) { Text(stringResource(R.string.savings_deposit)) }
                    OutlinedButton(
                        onClick = { onEvent(SavingsViewEvent.TransferOpened(SavingsTransferDirection.WITHDRAW)) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.minimumTouchTarget)
                            .testTag("savings_withdraw"),
                    ) { Text(stringResource(R.string.savings_withdraw)) }
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
        AlertDialog(
            onDismissRequest = { onEvent(SavingsViewEvent.NoticeDismissed) },
            title = { Text(stringResource(R.string.savings_notice_title)) },
            text = { Text(notice.message()) },
            confirmButton = { TextButton(onClick = { onEvent(SavingsViewEvent.NoticeDismissed) }) {
                Text(stringResource(R.string.savings_ok))
            } },
        )
    }
}

@Composable
private fun BalanceCard(availableRub: Long, savingsRub: Long) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
            Text(stringResource(R.string.savings_wallet, availableRub), style = AppTheme.typography.bodyStrong)
            Text(stringResource(R.string.savings_total, savingsRub), style = AppTheme.typography.body)
        }
    }
}

@Composable
private fun GoalCard(progress: github.detrig.feature.economy.domain.SavingsGoalProgress) {
    ElevatedCard(Modifier.fillMaxWidth().testTag("savings_goal")) {
        Column(Modifier.padding(AppTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            Text(progress.goal.title, style = AppTheme.typography.sectionTitle)
            Text(stringResource(R.string.savings_progress, progress.savedRub, progress.goal.targetRub), style = AppTheme.typography.bodyStrong)
            FinPetProgressIndicator(
                progress = (progress.savedRub.toFloat() / progress.goal.targetRub).coerceIn(0f, 1f),
                color = AppTheme.colors.statusPositive.accent,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (progress.isReached) stringResource(R.string.savings_reached)
                else stringResource(R.string.savings_remaining, progress.remainingRub),
                style = AppTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun GoalChoices(goals: List<SavingsGoalDraft>, busy: Boolean, onGoal: (SavingsGoalDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        goals.forEach { goal ->
            OutlinedButton(
                onClick = { onGoal(goal) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.minimumTouchTarget),
            ) { Text(stringResource(R.string.savings_goal_choice, goal.title, goal.targetRub)) }
        }
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
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(if (direction == SavingsTransferDirection.DEPOSIT) R.string.savings_deposit_title else R.string.savings_withdraw_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                Text(stringResource(if (direction == SavingsTransferDirection.DEPOSIT) R.string.savings_deposit_description else R.string.savings_withdraw_description))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value -> amount = value.filter { character -> character.isDigit() } },
                    label = { Text(stringResource(R.string.savings_amount)) },
                    suffix = { Text(stringResource(R.string.savings_rub)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("savings_transfer_amount"),
                )
            }
        },
        confirmButton = {
            Button(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null && !busy) {
                Text(stringResource(if (direction == SavingsTransferDirection.DEPOSIT) R.string.savings_confirm_deposit else R.string.savings_confirm_withdraw))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.savings_cancel)) }
        },
    )
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
