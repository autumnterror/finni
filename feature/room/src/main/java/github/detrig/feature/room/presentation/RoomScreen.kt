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
import github.detrig.feature.room.presentation.component.ParentHelpDialog
import github.detrig.feature.room.presentation.component.WeeklyPlanEditorDialog
import github.detrig.feature.room.presentation.component.WeeklyPlanProgressDialog
import github.detrig.feature.room.presentation.component.AllowanceReceiptDialog
import github.detrig.feature.room.presentation.component.EarlyWeekParentHelpDialog
import github.detrig.feature.room.presentation.component.HouseHud
import github.detrig.feature.room.presentation.component.AchievementsDialog
import github.detrig.feature.room.presentation.component.RoomMenuDialog
import github.detrig.feature.room.presentation.component.ParentGateDialog
import github.detrig.feature.room.presentation.component.ParentCabinetDialog
import github.detrig.feature.room.presentation.component.FirstRunOnboardingDialog
import github.detrig.feature.room.presentation.component.RoomImpulseWishDialog
import github.detrig.feature.room.presentation.component.TutorialSpotlight
import github.detrig.feature.room.presentation.component.SleepConfirmationDialog
import github.detrig.feature.room.presentation.component.DayTransitionDialog
import github.detrig.designsystem.component.FinPetDialogueDialog
import github.detrig.designsystem.component.FinPetDialogueAction
import github.detrig.designsystem.component.FinPetStorefrontBalanceBadge
import github.detrig.designsystem.component.FinPetCard
import github.detrig.designsystem.component.FinPetStorefrontCard
import github.detrig.feature.room.domain.model.RoomProgress
import github.detrig.feature.room.presentation.preview.RoomPreviewData
import github.detrig.feature.room.api.FirstRunOnboardingStep
import github.detrig.feature.room.api.RoomPetInteraction
import github.detrig.feature.planning.domain.PlanAdjustmentReason
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import github.detrig.feature.room.R
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import kotlinx.coroutines.delay

@Composable
internal fun RoomScreen(
    modifier: Modifier = Modifier,
    petName: String,
    canShowDialogs: Boolean,
    petContent: @Composable (Modifier, RoomPetInteraction) -> Unit = { _, _ -> },
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
    showHud: Boolean = false,
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
    val openPhoneFromTutorial = {
        viewModel.perform(RoomViewEvent.FirstRunOpenPhone)
        onPhoneClick()
    }
    val openTableFromTutorial = {
        viewModel.perform(RoomViewEvent.FirstRunOpenTable)
        onFeedingClick()
    }
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
            onPhoneClick = if (onboarding?.step == FirstRunOnboardingStep.WAITING_FOR_PHONE) {
                openPhoneFromTutorial
            } else onPhoneClick,
            phoneUnreadCount = phoneUnreadCount,
            onFoodClick = if (onboarding?.step == FirstRunOnboardingStep.WAITING_FOR_FRIDGE) {
                {
                    viewModel.perform(RoomViewEvent.FirstRunOpenFridge)
                    onFoodClick()
                }
            } else onFoodClick,
            onFeedingClick = if (onboarding?.step == FirstRunOnboardingStep.TABLE_GUIDANCE) {
                openTableFromTutorial
            } else onFeedingClick,
            tableFoodContent = tableFoodContent,
            active = externalActive && canShowDialogs && resumed &&
                (focused || hasAllowedOnboardingObjects) && dialogZoneId == null &&
                content?.planEditor == null &&
                content?.isPlanSummaryVisible != true && content?.menuDestination == RoomMenuDestination.NONE &&
                content?.weekResult == null &&
                content?.weekSummaryTutorialStep == null &&
                content?.showFirstGamePurchaseFeedback != true &&
                content?.readyFirstGameZoneId == null &&
                content?.firstWeekNeedHint == null &&
                content?.firstWeekGoalHint == null &&
                content?.parentHelpDialog == null && content?.allowanceNotice == null &&
                content?.dayTransitionNotice == null &&
                content?.savingsRecoveryPrompt == null &&
                content?.parentHelpPhonePrompt == null &&
                content?.earlyWeekParentHelpNotice == null && content?.planDialogue == null &&
                content?.impulseWish == null && content?.sleepConfirmationVisible != true &&
                !showPhoneNotificationPrompt &&
                (onboarding == null || hasAllowedOnboardingObjects),
            previewZoneId = requestedZoneId,
            focusObjectId = activeFocusObjectId,
            isFeedingScene = focusObjectId == "dining_table",
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
        if ((externalActive || showHud) && content != null) {
            HouseHud(
                progress = content.progress,
                showMenu = externalActive && onboarding == null,
                showDetails = true,
                onMenuClick = { viewModel.perform(RoomViewEvent.MenuClicked) },
                modifier = Modifier.align(Alignment.TopCenter),
            )
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
    }
    val zone = content?.zones?.find { it.id == dialogZoneId }
    val readyFirstGame = content?.zones?.find { it.id == content.readyFirstGameZoneId }
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
        val tableTapInstruction = firstRun.step == FirstRunOnboardingStep.TABLE_GUIDANCE
        canShowDialogs &&
            (tableTapInstruction || firstRun.focusObjectId == null || focusedObjectId == firstRun.focusObjectId) &&
            (tableTapInstruction || !spotlightRequired || spotlightBounds != null)
    }
    when {
        content?.sleepConfirmationVisible == true && canShowDialogs -> {
            SleepConfirmationDialog(
                canSleep = content.progress.petHunger > 0,
                onConfirm = { viewModel.perform(RoomViewEvent.SleepConfirmed) },
                onPostpone = { viewModel.perform(RoomViewEvent.SleepPostponed) },
            )
        }
        zone?.access is RoomZoneAccess.Buyable && canShowDialogs -> {
            val priceRub = (zone.access as RoomZoneAccess.Buyable).priceRub
            val activeGoal = content.activeSavingsGoal
            RoomBuyDialog(zone, content.progress, content.buyingZoneId != null,
                onConfirm = { useSavings -> viewModel.perform(RoomViewEvent.BuyConfirmed(zone.id, useSavings)) },
                showBuyNow = onboarding?.step != FirstRunOnboardingStep.GAME_SELECTION,
                canBuyFromSavings = activeGoal?.let {
                    it.goal.id == "room-zone:${zone.id}" && it.savedRub >= priceRub
                } == true,
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
        content?.savingsRecoveryPrompt != null && canShowDialogs -> {
            FinPetDialogueDialog(
                speakerName = petName,
                cards = listOf(stringResource(R.string.savings_recovery_prompt)),
                portrait = petPortrait,
                topInset = 0.dp,
                advanceOnTap = false,
                actions = listOf(
                    FinPetDialogueAction(id = "open", label = stringResource(R.string.savings_recovery_open)),
                    FinPetDialogueAction(id = "later", label = stringResource(R.string.savings_recovery_later)),
                ),
                onActionSelected = { action ->
                    viewModel.perform(
                        if (action.id == "open") {
                            RoomViewEvent.OpenSavingsFromRecoveryPrompt
                        } else {
                            RoomViewEvent.DismissSavingsRecoveryPrompt
                        },
                    )
                },
                onFinished = { viewModel.perform(RoomViewEvent.DismissSavingsRecoveryPrompt) },
            )
        }
        content?.parentHelpDialog != null && canShowDialogs && externalActive && resumed &&
            onboarding == null && content.planEditor == null && content.planDialogue == null &&
            content.weekResult == null && content.dayTransitionNotice == null &&
            content.menuDestination == RoomMenuDestination.NONE && !showPhoneNotificationPrompt -> {
            LaunchedEffect(content.progress.weekNumber) {
                viewModel.perform(RoomViewEvent.ParentHelpDialogShown)
            }
            ParentHelpDialog(
                state = content.parentHelpDialog,
                isRequesting = content.isRequestingParentHelp,
                onOfferSelected = { viewModel.perform(RoomViewEvent.ParentHelpOfferClicked(it)) },
                onDismiss = { viewModel.perform(RoomViewEvent.CloseParentHelpDialog) },
            )
        }
        content?.parentHelpPhonePrompt != null && canShowDialogs -> {
            FinPetDialogueDialog(
                speakerName = petName,
                cards = listOf(stringResource(R.string.parent_help_phone_prompt)),
                portrait = petPortrait,
                topInset = 0.dp,
                onFinished = { viewModel.perform(RoomViewEvent.CloseParentHelpPhonePrompt) },
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
        content?.firstWeekNeedHint != null && canShowDialogs -> {
            FinPetDialogueDialog(
                speakerName = petName,
                cards = buildList {
                    add(stringResource(R.string.onboarding_first_week_hungry_hint))
                    if (content.firstWeekNeedHint.fridgeIsEmpty) {
                        add(stringResource(R.string.onboarding_first_week_empty_fridge_hint))
                    }
                },
                portrait = petPortrait,
                onFinished = { viewModel.perform(RoomViewEvent.CloseFirstWeekNeedHint) },
            )
        }
        content?.firstWeekGoalHint != null && canShowDialogs -> {
            FinPetDialogueDialog(
                speakerName = petName,
                cards = listOf(stringResource(
                    R.string.onboarding_first_week_goal_almost_reached,
                    content.firstWeekGoalHint.remainingRub,
                )),
                portrait = petPortrait,
                onFinished = { viewModel.perform(RoomViewEvent.CloseFirstWeekGoalHint) },
            )
        }
        readyFirstGame != null && canShowDialogs -> {
            FinPetDialogueDialog(
                speakerName = petName,
                cards = listOf(
                    stringResource(R.string.onboarding_first_game_ready) + "\n\n" +
                        stringResource(R.string.onboarding_first_game_can_open),
                ),
                portrait = petPortrait,
                advanceOnTap = false,
                actions = listOf(FinPetDialogueAction(
                    id = "view_game",
                    label = stringResource(R.string.onboarding_first_game_view),
                )),
                onActionSelected = {
                    viewModel.perform(RoomViewEvent.FirstRunViewReadyGame(readyFirstGame.id))
                },
                onFinished = {},
            )
        }
        content?.showFirstGamePurchaseFeedback == true && canShowDialogs -> {
            FinPetDialogueDialog(
                speakerName = petName,
                cards = listOf(stringResource(R.string.onboarding_first_game_play_offer)),
                portrait = petPortrait,
                onFinished = {
                    viewModel.perform(RoomViewEvent.CloseFirstGamePurchaseFeedback)
                },
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
                feedbackCards = (content.planDialogue as? PlanDialogueState.NeedsChanges)?.cards(
                    availableRub = content.progress.balanceRub.toLong(),
                ),
                dialogueTopInset = 0.dp,
                onTutorialNext = { viewModel.perform(RoomViewEvent.PlanTutorialNext) },
                onFeedbackEdit = { viewModel.perform(RoomViewEvent.PlanDialogueEditRequested) },
                onFeedbackFinished = { viewModel.perform(RoomViewEvent.PlanDialogueFinished) },
                onPercentChanged = { category, percent ->
                    viewModel.perform(RoomViewEvent.PlanPercentChanged(category, percent))
                },
                onReserveChanged = { percent ->
                    viewModel.perform(RoomViewEvent.PlanReserveChanged(percent))
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
                remainingRub = content.progress.balanceRub.toLong(),
                onDismiss = { viewModel.perform(RoomViewEvent.CloseWeekResult) },
            )
        }
        content?.menuDestination == RoomMenuDestination.MENU && canShowDialogs -> {
            RoomMenuDialog(
                progress = content.progress,
                achievements = content.achievements,
                showAllUnlocked = content.areMenuAchievementsExpanded,
                onToggleUnlocked = { viewModel.perform(RoomViewEvent.ToggleMenuAchievements) },
                onShowAllAchievements = { viewModel.perform(RoomViewEvent.ShowAllAchievements) },
                onParentCabinet = { viewModel.perform(RoomViewEvent.ParentCabinetClicked) },
                onDismiss = { viewModel.perform(RoomViewEvent.CloseMenu) },
            )
        }
        content?.menuDestination == RoomMenuDestination.ALL_ACHIEVEMENTS && canShowDialogs -> {
            AchievementsDialog(
                achievements = content.achievements,
                onDismiss = { viewModel.perform(RoomViewEvent.CloseAchievements) },
            )
        }
        content?.menuDestination == RoomMenuDestination.PARENT_GATE && content.parentGate != null && canShowDialogs -> {
            ParentGateDialog(
                state = content.parentGate,
                onAnswerChange = { viewModel.perform(RoomViewEvent.ParentAnswerChanged(it)) },
                onSubmit = { viewModel.perform(RoomViewEvent.ParentAnswerSubmitted) },
                onDismiss = { viewModel.perform(RoomViewEvent.CloseParentGate) },
            )
        }
        content?.menuDestination == RoomMenuDestination.PARENT_CABINET && canShowDialogs -> {
            ParentCabinetDialog(
                achievements = content.achievements,
                parentRows = content.parentRows,
                onDismiss = { viewModel.perform(RoomViewEvent.CloseParentCabinet) },
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
                onShowWeekSummary = {
                    viewModel.perform(RoomViewEvent.FirstRunShowWeekSummary)
                },
                onStartNewWeekPlan = {
                    viewModel.perform(RoomViewEvent.FirstRunStartNewWeekPlan)
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
    if (content?.dayTransitionNotice != null && canShowDialogs) {
        DayTransitionDialog(content.dayTransitionNotice) {
            viewModel.perform(RoomViewEvent.CloseDayTransitionNotice)
        }
    }
    if (content?.weekSummaryTutorialStep != null && canShowDialogs) {
        FinPetDialogueDialog(
            speakerName = petName,
            cards = listOf(stringResource(when (content.weekSummaryTutorialStep) {
                WeekSummaryTutorialStep.INCOME -> R.string.onboarding_week_summary_income
                WeekSummaryTutorialStep.EXPENSES -> R.string.onboarding_week_summary_expenses
                WeekSummaryTutorialStep.REMAINDER -> R.string.onboarding_week_summary_remainder
            })),
            portrait = petPortrait,
            advanceOnTap = false,
            actions = listOf(FinPetDialogueAction(
                id = "next",
                label = stringResource(R.string.onboarding_next),
            )),
            onActionSelected = {
                viewModel.perform(RoomViewEvent.WeekSummaryTutorialNext)
            },
            onFinished = { viewModel.perform(RoomViewEvent.WeekSummaryTutorialNext) },
        )
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
    FirstRunOnboardingStep.PHONE_GUIDANCE,
    FirstRunOnboardingStep.WAITING_FOR_PHONE,
    FirstRunOnboardingStep.FRIDGE_GUIDANCE,
    FirstRunOnboardingStep.WAITING_FOR_FRIDGE,
    FirstRunOnboardingStep.TABLE_PROMPT,
    FirstRunOnboardingStep.TABLE_GUIDANCE,
    FirstRunOnboardingStep.BEDTIME_LATE,
    FirstRunOnboardingStep.BEDTIME_GUIDANCE,
    FirstRunOnboardingStep.WAITING_FOR_BED,
)

@Composable
private fun PlanDialogueState.cards(availableRub: Long = 0): List<String> = when (this) {
    is PlanDialogueState.NeedsChanges -> listOf(
        when (reason) {
            PlanAdjustmentReason.MANDATORY_TOO_LOW -> stringResource(
                R.string.plan_feedback_mandatory,
                recommendedPercent,
            )
            PlanAdjustmentReason.RESERVE_TOO_LOW -> stringResource(
                R.string.plan_feedback_reserve,
                availableRub * recommendedPercent / 100,
            )
            PlanAdjustmentReason.SAVINGS_TOO_LOW -> stringResource(
                R.string.plan_feedback_savings,
                availableRub * recommendedPercent / 100,
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
            ).cards(availableRub = 500),
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
