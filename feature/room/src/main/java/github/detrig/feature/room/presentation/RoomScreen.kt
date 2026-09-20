package github.detrig.feature.room.presentation

import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.core.mvvm.command.CommandsQueueEffect
import github.detrig.core.mvvm.command.ImmutableCommandsQueue
import github.detrig.feature.room.RoomFeature
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.navigation.RoomPreviewRequests
import github.detrig.feature.room.presentation.component.RoomBuyDialog
import github.detrig.feature.room.presentation.component.WeeklyPlanEditorDialog
import github.detrig.feature.room.presentation.component.WeeklyPlanProgressDialog
import github.detrig.feature.room.presentation.component.ParentHelpDialog
import github.detrig.feature.room.presentation.component.AllowanceReceiptDialog
import github.detrig.feature.room.presentation.component.ZeroBalanceHelpDialog
import github.detrig.feature.room.presentation.component.AchievementMenuButton
import github.detrig.feature.room.presentation.component.AchievementUnlockedBanner
import github.detrig.feature.room.presentation.component.AchievementsDialog
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.feature.planning.domain.PlanAdjustmentReason
import androidx.compose.ui.res.stringResource
import github.detrig.feature.room.R
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme

@Composable
internal fun RoomScreen(
    modifier: Modifier = Modifier,
    petName: String,
    canShowDialogs: Boolean,
    petContent: @Composable (Modifier) -> Unit = {},
    petPortrait: @Composable (Modifier) -> Unit = {},
    onMirrorClick: () -> Unit = {},
    onPhoneClick: () -> Unit = {},
    externalActive: Boolean = true,
    previewRequests: RoomPreviewRequests,
) {
    val viewModel: RoomViewModel = viewModel { RoomFeature.component().getRoomViewModel() }
    val state by viewModel.state().observeAsState(RoomViewState.Loading)
    var dialogZoneId by rememberSaveable { mutableStateOf<String?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val focused = LocalWindowInfo.current.isWindowFocused
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val commands = remember(viewModel) { ImmutableCommandsQueue(viewModel.commands<RoomCommand>()) }
    CommandsQueueEffect(commands) { command ->
        when (command) {
            is RoomCommand.ShowBuyConfirmation -> dialogZoneId = command.zoneId
            is RoomCommand.CloseBuyConfirmation -> if (dialogZoneId == command.zoneId) dialogZoneId = null
        }
    }
    LaunchedEffect(viewModel) { viewModel.perform(RoomViewEvent.Load) }
    val requestedZoneId by previewRequests.zoneId.collectAsState()
    val content = state as? RoomViewState.Content
    Box(modifier = modifier) {
        RoomContent(
            state = state,
            onEvent = viewModel::perform,
            modifier = Modifier.fillMaxSize(),
            petContent = petContent,
            onMirrorClick = onMirrorClick,
            onPhoneClick = onPhoneClick,
            active = externalActive && canShowDialogs && resumed && focused && dialogZoneId == null &&
                content?.planEditor == null &&
                content?.isPlanSummaryVisible != true && content?.isAchievementsVisible != true &&
                content?.parentHelpDialog == null && content?.allowanceNotice == null &&
                content?.zeroBalanceHelpNotice == null && content?.planDialogue == null,
            previewZoneId = requestedZoneId,
            onPreviewReady = { id ->
                previewRequests.consume(id)
                viewModel.perform(RoomViewEvent.ZonePreviewed(id))
            },
        )
        if (externalActive && content != null && content.achievementBanner == null) {
            AchievementMenuButton(
                onClick = { viewModel.perform(RoomViewEvent.AchievementsClicked) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(AppTheme.spacing.md),
            )
        }
    }
    val zone = content?.zones?.find { it.id == dialogZoneId }
    if (zone?.access is RoomZoneAccess.Buyable) {
        RoomBuyDialog(zone, content.progress, content.buyingZoneId != null,
            onConfirm = { viewModel.perform(RoomViewEvent.BuyConfirmed(zone.id)) },
            isSavingGoal = content.savingGoalZoneId == zone.id,
            onSaveAsGoal = { title -> viewModel.perform(RoomViewEvent.SaveZoneAsGoal(zone.id, title)) },
            onDismiss = { dialogZoneId = null })
    }
    content?.allowanceNotice?.let { notice ->
        AllowanceReceiptDialog(notice) { viewModel.perform(RoomViewEvent.CloseAllowanceNotice) }
    }
    content?.zeroBalanceHelpNotice?.let { notice ->
        ZeroBalanceHelpDialog(notice) { viewModel.perform(RoomViewEvent.CloseZeroBalanceHelpNotice) }
    }
    content?.parentHelpDialog?.takeIf { content.zeroBalanceHelpNotice == null }?.let { dialog ->
        ParentHelpDialog(
            state = dialog,
            isRequesting = content.isRequestingParentHelp,
            onOfferSelected = { viewModel.perform(RoomViewEvent.ParentHelpOfferClicked(it)) },
            onDismiss = { viewModel.perform(RoomViewEvent.CloseParentHelpDialog) },
        )
    }
    content?.planEditor?.takeIf {
        canShowDialogs &&
        content.allowanceNotice == null && content.zeroBalanceHelpNotice == null
    }?.let { editor ->
        WeeklyPlanEditorDialog(
            editor = editor,
            availableRub = content.progress.balanceRub.toLong(),
            isSaving = content.isSavingPlan,
            petName = petName,
            petPortrait = petPortrait,
            tutorialStep = content.planTutorialStep,
            feedbackCards = (content.planDialogue as? PlanDialogueState.NeedsChanges)?.cards(),
            onTutorialNext = { viewModel.perform(RoomViewEvent.PlanTutorialNext) },
            onFeedbackFinished = { viewModel.perform(RoomViewEvent.PlanDialogueFinished) },
            onPercentChanged = { category, percent ->
                viewModel.perform(RoomViewEvent.PlanPercentChanged(category, percent))
            },
            onSave = { viewModel.perform(RoomViewEvent.SavePlanClicked) },
        )
    }
    content?.progress?.planProgress?.takeIf { content.isPlanSummaryVisible }?.let { plan ->
        WeeklyPlanProgressDialog(plan) { viewModel.perform(RoomViewEvent.ClosePlanSummary) }
    }
    content?.takeIf { it.isAchievementsVisible && canShowDialogs }?.let {
        AchievementsDialog(
            achievements = it.achievements,
            onDismiss = { viewModel.perform(RoomViewEvent.CloseAchievements) },
        )
    }
    content?.planDialogue?.takeIf { dialogue ->
        canShowDialogs && dialogue is PlanDialogueState.Saved
    }?.let { dialogue ->
        FinPetDialogueDialog(
            speakerName = petName,
            cards = dialogue.cards(),
            portrait = petPortrait,
            onFinished = { viewModel.perform(RoomViewEvent.PlanDialogueFinished) },
        )
    }
    content?.achievementBanner?.takeIf { canShowDialogs }?.let { achievement ->
        AchievementUnlockedBanner(
            achievement = achievement,
            onDismiss = { viewModel.perform(RoomViewEvent.AchievementBannerDismissed) },
        )
    }
    LaunchedEffect(zone?.access, content != null) {
        if (content != null && zone?.access !is RoomZoneAccess.Buyable) dialogZoneId = null
    }
}

@Composable
private fun PlanDialogueState.cards(): List<String> = when (this) {
    is PlanDialogueState.NeedsChanges -> listOf(
        when (reason) {
            PlanAdjustmentReason.MANDATORY_TOO_LOW -> stringResource(
                R.string.plan_feedback_mandatory,
                recommendedPercent,
            )
            PlanAdjustmentReason.RESERVE_TOO_LOW -> stringResource(
                R.string.plan_feedback_reserve,
                recommendedPercent,
            )
            PlanAdjustmentReason.SAVINGS_TOO_LOW -> stringResource(
                R.string.plan_feedback_savings,
                recommendedPercent,
            )
        },
    )
    is PlanDialogueState.Saved -> listOf(stringResource(R.string.plan_feedback_success))
}

@Preview(name = "Обучение плану", widthDp = 360, heightDp = 740, showBackground = true)
@Composable
private fun PlanDialoguePreview() {
    FinPetTheme {
        FinPetDialogueDialog(
            speakerName = "Барсик",
            cards = PlanDialogueState.NeedsChanges(
                reason = PlanAdjustmentReason.MANDATORY_TOO_LOW,
                recommendedPercent = 40,
            ).cards(),
            portrait = { modifier ->
                Box(
                    modifier = modifier.background(AppTheme.colors.actionSecondary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🐾", style = AppTheme.typography.screenTitle)
                }
            },
            onFinished = {},
        )
    }
}
