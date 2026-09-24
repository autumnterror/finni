package github.detrig.feature.room.presentation

import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import github.detrig.feature.room.presentation.component.EarlyWeekParentHelpDialog
import github.detrig.feature.room.presentation.component.AchievementMenuButton
import github.detrig.feature.room.presentation.component.AchievementsDialog
import github.detrig.feature.room.presentation.component.FirstRunOnboardingDialog
import github.detrig.feature.room.presentation.component.RoomImpulseWishDialog
import github.detrig.feature.room.presentation.component.TutorialSpotlight
import github.detrig.feature.room.presentation.component.SleepConfirmationDialog
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.FinPetDialogueAction
import github.detrig.designsystem.component.FinPetStorefrontBalanceBadge
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetStorefrontCard
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.presentation.preview.RoomPreviewData
import github.detrig.feature.room.domain.model.FirstRunOnboardingStep
import github.detrig.feature.planning.domain.PlanAdjustmentReason
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import github.detrig.feature.room.R
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import kotlinx.coroutines.delay

@Composable
internal fun RoomScreen(
    modifier: Modifier = Modifier,
    petName: String,
    canShowDialogs: Boolean,
    petContent: @Composable (Modifier) -> Unit = {},
    petPortrait: @Composable (Modifier) -> Unit = {},
    onMirrorClick: () -> Unit = {},
    onPhoneClick: () -> Unit = {},
    phoneUnreadCount: Int = 0,
    phoneNotificationPrompt: String? = null,
    onPhonePromptOpen: () -> Unit = {},
    onPhonePromptDismiss: () -> Unit = {},
    onFoodClick: () -> Unit = {},
    onFeedingClick: () -> Unit = {},
    tableFoodContent: @Composable (Modifier) -> Unit = {},
    externalActive: Boolean = true,
    previewRequests: RoomPreviewRequests,
    focusObjectId: String? = null,
    petAnchorObjectId: String? = null,
    petZIndex: Float = 3f,
    petBaselineFraction: Float? = null,
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
    val onboarding = content?.onboarding
    var petLookingAround by remember { mutableStateOf(false) }
    LaunchedEffect(petLookingAround) {
        if (petLookingAround) {
            delay(1_200)
            petLookingAround = false
        }
    }
    val activeFocusObjectId = onboarding?.focusObjectId ?: focusObjectId ?: requestedZoneId
    var focusedObjectId by remember { mutableStateOf<String?>(null) }
    var spotlightBoundsInWindow by remember { mutableStateOf<Rect?>(null) }
    var roomOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    val spotlightBounds = spotlightBoundsInWindow?.let { bounds ->
        Rect(
            left = bounds.left - roomOriginInWindow.x,
            top = bounds.top - roomOriginInWindow.y,
            right = bounds.right - roomOriginInWindow.x,
            bottom = bounds.bottom - roomOriginInWindow.y,
        )
    }
    LaunchedEffect(activeFocusObjectId) {
        focusedObjectId = null
        spotlightBoundsInWindow = null
    }
    LaunchedEffect(resumed, externalActive) {
        viewModel.perform(if (resumed && externalActive) RoomViewEvent.Resumed else RoomViewEvent.Paused)
    }
    val hasAllowedOnboardingObjects = onboarding?.allowedObjectIds?.isNotEmpty() == true
    val showPhoneNotificationPrompt = phoneNotificationPrompt != null && onboarding == null &&
        content != null && !content.sleeping && canShowDialogs
    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            roomOriginInWindow = coordinates.positionInWindow()
        },
    ) {
        RoomContent(
            state = state,
            onEvent = viewModel::perform,
            modifier = Modifier.fillMaxSize(),
            petContent = petContent,
            petLookingAround = petLookingAround,
            onMirrorClick = onMirrorClick,
            onPhoneClick = onPhoneClick,
            phoneUnreadCount = phoneUnreadCount,
            onFoodClick = onFoodClick,
            onFeedingClick = onFeedingClick,
            tableFoodContent = tableFoodContent,
            active = externalActive && canShowDialogs && resumed &&
                (focused || hasAllowedOnboardingObjects) && dialogZoneId == null &&
                content?.planEditor == null &&
                content?.isPlanSummaryVisible != true && content?.isAchievementsVisible != true &&
                content?.weekResult == null &&
                content?.parentHelpDialog == null && content?.allowanceNotice == null &&
                content?.earlyWeekParentHelpNotice == null && content?.planDialogue == null &&
                content?.impulseWish == null && content?.sleepConfirmationVisible != true &&
                !showPhoneNotificationPrompt &&
                (onboarding == null || hasAllowedOnboardingObjects),
            previewZoneId = requestedZoneId,
            focusObjectId = activeFocusObjectId,
            petAnchorObjectId = petAnchorObjectId,
            petZIndex = petZIndex,
            petBaselineFraction = petBaselineFraction,
            highlightedObjectIds = onboarding?.highlightedObjectIds.orEmpty(),
            allowedObjectIds = onboarding?.allowedObjectIds.orEmpty(),
            onHighlightedObjectBoundsChanged = { spotlightBoundsInWindow = it },
            onPreviewReady = { id ->
                if (activeFocusObjectId == id) focusedObjectId = id
                if (requestedZoneId == id) {
                    previewRequests.consume(id)
                    viewModel.perform(RoomViewEvent.ZonePreviewed(id))
                }
            },
        )
        if (onboarding?.step?.let(spotlightSteps::contains) == true &&
            focusedObjectId == activeFocusObjectId && spotlightBounds != null
        ) {
            TutorialSpotlight(spotlightBounds)
        }
        if (onboarding?.step == FirstRunOnboardingStep.GAME_SELECTION) {
            FinPetStorefrontCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(AppTheme.spacing.md)
                    .widthIn(max = 400.dp),
            ) {
                Row(
                    modifier = Modifier.padding(AppTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(56.dp)) { petPortrait(Modifier.fillMaxSize()) }
                    Text(
                        stringResource(R.string.onboarding_games_selection),
                        modifier = Modifier.padding(start = AppTheme.spacing.md),
                        style = AppTheme.typography.bodyStrong,
                    )
                }
            }
        }
        if (externalActive && content != null) {
            RoomTopStatus(
                progress = content.progress,
                showDay = onboarding?.step != FirstRunOnboardingStep.GAME_SELECTION,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
        if (externalActive && content != null && !content.sleeping && onboarding == null) {
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
    val firstMoneyNotice = if (
        canShowDialogs && content != null && onboarding?.step == FirstRunOnboardingStep.FIRST_MONEY
    ) {
        content.allowanceNotice ?: AllowanceNoticeState(
            grossRub = content.progress.balanceRub.toLong(),
            parentHelpRepaidRub = 0,
            receivedRub = content.progress.balanceRub.toLong(),
        )
    } else {
        null
    }
    val visibleOnboarding = onboarding?.takeIf { firstRun ->
        val spotlightRequired = firstRun.step in spotlightSteps
        canShowDialogs &&
            (firstRun.focusObjectId == null || focusedObjectId == firstRun.focusObjectId) &&
            (!spotlightRequired || spotlightBounds != null)
    }
    when {
        content?.sleepConfirmationVisible == true && canShowDialogs -> {
            SleepConfirmationDialog(
                onConfirm = { viewModel.perform(RoomViewEvent.SleepConfirmed) },
                onPostpone = { viewModel.perform(RoomViewEvent.SleepPostponed) },
            )
        }
        zone?.access is RoomZoneAccess.Buyable && content != null && canShowDialogs -> {
            RoomBuyDialog(zone, content.progress, content.buyingZoneId != null,
                onConfirm = { viewModel.perform(RoomViewEvent.BuyConfirmed(zone.id)) },
                isSavingGoal = content.savingGoalZoneId == zone.id,
                onSaveAsGoal = { title -> viewModel.perform(RoomViewEvent.SaveZoneAsGoal(zone.id, title)) },
                onDismiss = { dialogZoneId = null })
        }
        firstMoneyNotice != null -> {
            AllowanceReceiptDialog(
                notice = firstMoneyNotice,
                firstRun = true,
                onDismiss = {
                    if (content?.allowanceNotice != null) {
                        viewModel.perform(RoomViewEvent.CloseAllowanceNotice)
                    }
                    viewModel.perform(RoomViewEvent.FirstRunMoneyNoticeClosed)
                },
            )
        }
        content?.allowanceNotice != null && content.weekResult == null && canShowDialogs -> {
            AllowanceReceiptDialog(content.allowanceNotice) {
                viewModel.perform(RoomViewEvent.CloseAllowanceNotice)
            }
        }
        content?.earlyWeekParentHelpNotice != null && canShowDialogs -> {
            EarlyWeekParentHelpDialog {
                viewModel.perform(RoomViewEvent.CloseEarlyWeekParentHelpNotice)
            }
        }
        content?.parentHelpDialog != null && canShowDialogs -> {
            ParentHelpDialog(
                state = content.parentHelpDialog,
                isRequesting = content.isRequestingParentHelp,
                onOfferSelected = { viewModel.perform(RoomViewEvent.ParentHelpOfferClicked(it)) },
                onDismiss = { viewModel.perform(RoomViewEvent.CloseParentHelpDialog) },
            )
        }
        content?.impulseWish != null && canShowDialogs -> {
            RoomImpulseWishDialog(
                wish = content.impulseWish,
                petName = petName,
                petPortrait = petPortrait,
                onFinished = { viewModel.perform(RoomViewEvent.CloseImpulseWish) },
            )
        }
        content?.planEditor != null && canShowDialogs && !content.sleeping &&
            content.allowanceNotice == null && content.earlyWeekParentHelpNotice == null &&
            content.weekResult == null -> {
            WeeklyPlanEditorDialog(
                editor = content.planEditor,
                weekNumber = content.progress.weekNumber,
                availableRub = content.progress.balanceRub.toLong(),
                isSaving = content.isSavingPlan,
                petName = petName,
                petPortrait = petPortrait,
                tutorialStep = content.planTutorialStep,
                feedbackCards = (content.planDialogue as? PlanDialogueState.NeedsChanges)?.cards(),
                dialogueTopInset = 0.dp,
                onTutorialNext = { viewModel.perform(RoomViewEvent.PlanTutorialNext) },
                onFeedbackEdit = { viewModel.perform(RoomViewEvent.PlanDialogueEditRequested) },
                onFeedbackFinished = { viewModel.perform(RoomViewEvent.PlanDialogueFinished) },
                onPercentChanged = { category, percent ->
                    viewModel.perform(RoomViewEvent.PlanPercentChanged(category, percent))
                },
                onSave = { viewModel.perform(RoomViewEvent.SavePlanClicked) },
            )
        }
        content?.isPlanSummaryVisible == true && content.progress.planProgress != null -> {
            WeeklyPlanProgressDialog(content.progress.planProgress) {
                viewModel.perform(RoomViewEvent.ClosePlanSummary)
            }
        }
        content?.weekResult != null && canShowDialogs -> {
            WeeklyPlanProgressDialog(
                progress = content.weekResult,
                isWeekResult = true,
                onDismiss = { viewModel.perform(RoomViewEvent.CloseWeekResult) },
            )
        }
        content?.isAchievementsVisible == true && canShowDialogs -> {
            AchievementsDialog(
                achievements = content.achievements,
                onDismiss = { viewModel.perform(RoomViewEvent.CloseAchievements) },
            )
        }
        content?.planDialogue is PlanDialogueState.Saved && canShowDialogs -> {
            FinPetDialogueDialog(
                speakerName = petName,
                cards = content.planDialogue.cards(),
                portrait = petPortrait,
                topInset = 0.dp,
                onFinished = { viewModel.perform(RoomViewEvent.PlanDialogueFinished) },
            )
        }
        visibleOnboarding != null -> {
            FirstRunOnboardingDialog(
                state = visibleOnboarding,
                selectedZone = content.zones.firstOrNull { it.id == visibleOnboarding.suggestedGoalZoneId },
                petName = petName,
                petPortrait = petPortrait,
                topInset = 0.dp,
                onContinue = { viewModel.perform(RoomViewEvent.FirstRunOnboardingContinue) },
                onPageChanged = { page ->
                    if (visibleOnboarding.step == FirstRunOnboardingStep.INTRODUCTION && page == 2) {
                        petLookingAround = true
                    }
                },
                onDepositSelected = {
                    viewModel.perform(RoomViewEvent.FirstRunDepositSelected(it))
                },
            )
        }
        showPhoneNotificationPrompt -> {
            FinPetDialogueDialog(
                speakerName = petName,
                cards = listOf(checkNotNull(phoneNotificationPrompt)),
                portrait = petPortrait,
                advanceOnTap = false,
                actions = listOf(
                    FinPetDialogueAction(id = "open", label = "Посмотреть"),
                    FinPetDialogueAction(id = "later", label = "Позже"),
                ),
                onActionSelected = { action ->
                    onPhonePromptDismiss()
                    if (action.id == "open") onPhonePromptOpen()
                },
                onFinished = onPhonePromptDismiss,
            )
        }
    }
    LaunchedEffect(zone?.access, content != null) {
        if (content != null && zone?.access !is RoomZoneAccess.Buyable) dialogZoneId = null
    }
}

@Composable
private fun RoomTopStatus(
    progress: RoomProgress,
    showDay: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        val balanceDescription = stringResource(R.string.house_balance_accessibility, progress.balanceRub)
        FinPetStorefrontBalanceBadge(
            balanceRub = progress.balanceRub.toLong(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(AppTheme.spacing.md)
                .semantics { contentDescription = balanceDescription }
                .testTag("house_balance"),
        )
        if (showDay) {
            FinPetCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(AppTheme.spacing.md)
                    .testTag("house_day_counter"),
            ) {
                Text(
                    text = stringResource(R.string.house_current_day, progress.dayOfWeek),
                    modifier = Modifier.padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
                    style = AppTheme.typography.bodyStrong,
                )
            }
        }
    }
}

@Preview(name = "Баланс и день комнаты", widthDp = 360, heightDp = 120, showBackground = true)
@Composable
private fun RoomTopStatusPreview() {
    FinPetTheme {
        RoomTopStatus(RoomPreviewData.state.progress)
    }
}

private val spotlightSteps = setOf(
    FirstRunOnboardingStep.GAME_DISCOVERY,
    FirstRunOnboardingStep.GAME_DISCOVERY_DETAILS,
    FirstRunOnboardingStep.GAME_SELECTED,
    FirstRunOnboardingStep.PIGGY_BANK,
    FirstRunOnboardingStep.PIGGY_TAP,
    FirstRunOnboardingStep.WAITING_FOR_PIGGY,
)

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
    is PlanDialogueState.Saved -> listOf(stringResource(
        if (hasSavings) R.string.plan_feedback_success else R.string.plan_feedback_success_no_savings,
    ))
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
