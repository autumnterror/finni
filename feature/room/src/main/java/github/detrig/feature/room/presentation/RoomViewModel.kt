package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.core.audio.GameAudio
import github.detrig.core.audio.SilentGameAudio
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.room.domain.interactor.BuyRoomZoneInteractor
import github.detrig.feature.room.domain.interactor.EndDayInteractor
import github.detrig.feature.room.domain.interactor.SaveWeeklyPlanInteractor
import github.detrig.feature.room.domain.interactor.AssessWeeklyPlanInteractor
import github.detrig.feature.room.domain.interactor.WeeklyPlanLearningInteractor
import github.detrig.feature.room.domain.interactor.OpenSavingsInteractor
import github.detrig.feature.room.domain.interactor.LoadActiveSavingsGoalInteractor
import github.detrig.feature.room.domain.interactor.ReconcileSavingsLearningInteractor
import github.detrig.feature.room.domain.interactor.SaveZoneAsSavingsGoalInteractor
import github.detrig.feature.room.domain.interactor.LoadParentHelpInteractor
import github.detrig.feature.room.domain.interactor.RequestParentHelpInteractor
import github.detrig.feature.room.domain.interactor.EndWeekEarlyWithParentHelpInteractor
import github.detrig.feature.room.domain.interactor.LoadRoomImpulseWishInteractor
import github.detrig.feature.room.domain.interactor.ObserveRoomZonesInteractor
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.domain.model.RoomData
import github.detrig.feature.room.domain.interactor.WeeklyPlanAchievement
import github.detrig.feature.learning.domain.ParentProgressRow
import github.detrig.feature.room.navigation.RoomRouter
import github.detrig.feature.room.presentation.mapper.toRoomZones
import github.detrig.feature.room.domain.model.HousePositionRepository
import github.detrig.feature.room.domain.model.FirstRunOnboardingChapter
import github.detrig.feature.room.domain.model.FirstRunOnboardingProgress
import github.detrig.feature.room.domain.model.FirstRunOnboardingRepository
import github.detrig.feature.room.domain.model.ParentHelpPromptRepository
import github.detrig.feature.room.domain.model.shouldOfferAutomaticParentHelp
import github.detrig.feature.room.api.FirstRunGuideApi
import github.detrig.feature.room.api.FirstRunOnboardingStep
import github.detrig.feature.inventory.api.InventoryApi
import kotlinx.coroutines.flow.first
import github.detrig.feature.room.presentation.model.HouseLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import github.detrig.feature.planning.domain.PlanAssessment
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.economy.domain.LowBalanceRecoveryAction
import github.detrig.feature.economy.domain.lowBalanceRecoveryAction
import github.detrig.feature.economy.domain.canOfferParentHelp
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.week.domain.EarlyWeekEndResult
import github.detrig.feature.economy.domain.SavingsGoalProgress
import kotlin.random.Random

private data class ObservedRoomData(
    val roomData: RoomData,
    val achievements: List<WeeklyPlanAchievement>,
    val parentRows: List<ParentProgressRow>,
    val guideStep: FirstRunOnboardingStep,
)

internal class RoomViewModel(
    private val observeZones: ObserveRoomZonesInteractor,
    private val buyZone: BuyRoomZoneInteractor,
    private val endDay: EndDayInteractor,
    private val assessWeeklyPlan: AssessWeeklyPlanInteractor,
    private val saveWeeklyPlan: SaveWeeklyPlanInteractor,
    private val weeklyPlanLearning: WeeklyPlanLearningInteractor,
    private val openSavings: OpenSavingsInteractor,
    private val saveZoneGoal: SaveZoneAsSavingsGoalInteractor,
    private val loadActiveSavingsGoal: LoadActiveSavingsGoalInteractor,
    private val reconcileSavingsLearning: ReconcileSavingsLearningInteractor,
    private val loadParentHelpInteractor: LoadParentHelpInteractor,
    private val requestParentHelpInteractor: RequestParentHelpInteractor,
    private val endWeekEarlyWithParentHelp: EndWeekEarlyWithParentHelpInteractor,
    private val parentHelpPromptRepository: ParentHelpPromptRepository,
    private val minimumProductPriceRub: Long,
    private val loadRoomImpulseWish: LoadRoomImpulseWishInteractor,
    private val router: RoomRouter,
    private val positions: HousePositionRepository,
    private val onboardingRepository: FirstRunOnboardingRepository,
    private val firstRunGuide: FirstRunGuideApi,
    private val inventoryApi: InventoryApi,
    private val gameAudio: GameAudio = SilentGameAudio,
) : CoreViewModel<RoomViewState, RoomViewEvent>(RoomViewState.Loading) {
    private var observationJob: Job? = null
    private var buyJob: Job? = null
    private var sleepJob: Job? = null
    private var savePlanJob: Job? = null
    private var parentHelpJob: Job? = null
    private var lowBalanceJob: Job? = null
    private var onboardingRefreshJob: Job? = null
    private var firstWeekNeedHintJob: Job? = null
    private var firstWeekGoalHintJob: Job? = null
    private var impulseWishJob: Job? = null
    private val checkedImpulseWishDays = mutableSetOf<Long>()
    private val promptedSavingsRecoveryWeeks = mutableSetOf<Long>()
    private val hintedHungerDays = mutableSetOf<Long>()
    private var firstWeekGoalHintShown = false
    private val reconciledPlanWeeks = mutableSetOf<Long>()
    private var reconcilingPlanWeek: Long? = null
    // Read this small preference before the first composition of HouseScene. Loading it
    // from Dispatchers.IO after rendering caused one frame at the default center position.
    private var savedPosition = HouseLayout.restored(positions.load())
    private var lastLaunchNanos = 0L
    private var onboardingProgress = FirstRunOnboardingProgress(
        FirstRunOnboardingChapter.entries.toSet(),
    )
    private var onboardingStep = FirstRunOnboardingStep.COMPLETED
    private var onboardingGoal: SavingsGoalProgress? = null
    private var onboardingSuggestedGoalZoneId: String? = null

    init {
        require(minimumProductPriceRub > 0)
    }

    private companion object {
        const val DEFAULT_SUGGESTED_GOAL_ZONE_ID = "fishing"
        const val ROOM_GOAL_ID_PREFIX = "room-zone:"
        const val FIRST_WEEK_HUNGER_HINT_THRESHOLD = 35
    }

    override fun perform(viewEvent: RoomViewEvent) {
        when (viewEvent) {
            RoomViewEvent.MarketClicked -> launchOnce { router.openMarket() }
            RoomViewEvent.BedClicked -> showSleepConfirmation()
            RoomViewEvent.SleepConfirmed -> sleep()
            RoomViewEvent.SleepPostponed -> hideSleepConfirmation()
            RoomViewEvent.CalendarClicked -> showPlanSummary()
            RoomViewEvent.PiggyBankClicked -> openPiggyBank()
            RoomViewEvent.TestsClicked,
            RoomViewEvent.WardrobeClicked,
            RoomViewEvent.FoodClicked,
            RoomViewEvent.FeedingClicked -> router.showEntryComingSoon()
            RoomViewEvent.DishesClicked -> router.showEntryComingSoon()
            RoomViewEvent.OpenSavingsFromRecoveryPrompt -> {
                nullableState<RoomViewState.Content>()?.let {
                    updateState(it.copy(savingsRecoveryPrompt = null))
                }
                openPiggyBank()
            }
            RoomViewEvent.DismissSavingsRecoveryPrompt -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(savingsRecoveryPrompt = null))
                handleLowBalance(it.progress)
            }
            is RoomViewEvent.ParentHelpOfferClicked -> requestParentHelp(viewEvent.offerId)
            RoomViewEvent.ParentHelpDialogShown -> nullableState<RoomViewState.Content>()?.let {
                if (it.parentHelpDialog != null) parentHelpPromptRepository.markShownInWeek(it.progress.weekNumber)
            }
            RoomViewEvent.CloseParentHelpDialog -> nullableState<RoomViewState.Content>()?.let {
                parentHelpPromptRepository.markShownInWeek(it.progress.weekNumber)
                updateState(it.copy(
                    parentHelpDialog = null,
                    parentHelpPhonePrompt = if (it.parentHelpDialog?.activeHelp == null) {
                        ParentHelpPhonePromptState
                    } else {
                        null
                    },
                ))
            }
            RoomViewEvent.CloseParentHelpPhonePrompt -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(parentHelpPhonePrompt = null))
            }
            RoomViewEvent.CloseAllowanceNotice -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(allowanceNotice = null))
            }
            RoomViewEvent.CloseEarlyWeekParentHelpNotice -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(earlyWeekParentHelpNotice = null))
            }
            RoomViewEvent.CloseDayTransitionNotice -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(dayTransitionNotice = null))
            }
            RoomViewEvent.CloseImpulseWish -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(impulseWish = null))
            }
            RoomViewEvent.FirstRunOnboardingContinue -> continueFirstRunOnboarding()
            RoomViewEvent.FirstRunOpenPhone -> transitionOnboarding(FirstRunOnboardingStep.PHONE_STORE_GUIDANCE)
            RoomViewEvent.FirstRunOpenFridge -> transitionOnboarding(FirstRunOnboardingStep.FRIDGE_EXPLANATION)
            RoomViewEvent.FirstRunOpenTable -> transitionOnboarding(FirstRunOnboardingStep.FEEDING)
            RoomViewEvent.FirstRunShowWeekSummary -> showFirstWeekSummary()
            RoomViewEvent.FirstRunStartNewWeekPlan -> startNextWeekPlan()
            RoomViewEvent.FirstRunMoneyNoticeClosed -> {
                if (onboardingStep == FirstRunOnboardingStep.FIRST_MONEY) {
                    transitionOnboarding(FirstRunOnboardingStep.MONEY_EXPLANATION)
                }
            }
            is RoomViewEvent.FirstRunDepositSelected -> selectFirstDeposit(viewEvent.depositNow)
            RoomViewEvent.Resumed -> {
                refreshFirstRunOnboarding()
                nullableState<RoomViewState.Content>()?.let { content ->
                    handleLowBalance(content.progress)
                }
            }
            RoomViewEvent.Paused -> Unit
            RoomViewEvent.SavePlanClicked -> savePlan()
            RoomViewEvent.PlanTutorialNext -> advancePlanTutorial()
            RoomViewEvent.PlanDialogueFinished -> closePlanDialogue()
            RoomViewEvent.PlanDialogueEditRequested -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(planDialogue = null))
            }
            RoomViewEvent.MenuClicked -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(menuDestination = RoomMenuDestination.MENU, areMenuAchievementsExpanded = false))
            }
            RoomViewEvent.CloseMenu -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(menuDestination = RoomMenuDestination.NONE))
            }
            RoomViewEvent.ToggleMenuAchievements -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(areMenuAchievementsExpanded = !it.areMenuAchievementsExpanded))
            }
            RoomViewEvent.ShowAllAchievements -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(menuDestination = RoomMenuDestination.ALL_ACHIEVEMENTS))
            }
            RoomViewEvent.CloseAchievements -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(menuDestination = RoomMenuDestination.MENU))
            }
            RoomViewEvent.ParentCabinetClicked -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(
                    menuDestination = RoomMenuDestination.PARENT_GATE,
                    parentGate = ParentGateState(Random.nextInt(7, 16), Random.nextInt(7, 16)),
                ))
            }
            is RoomViewEvent.ParentAnswerChanged -> nullableState<RoomViewState.Content>()?.let { content ->
                content.parentGate?.let { gate ->
                    updateState(content.copy(parentGate = gate.copy(
                        answer = viewEvent.answer.filter(Char::isDigit).take(3),
                        hasError = false,
                    )))
                }
            }
            RoomViewEvent.ParentAnswerSubmitted -> nullableState<RoomViewState.Content>()?.let { content ->
                content.parentGate?.let { gate ->
                    if (gate.answer.toIntOrNull() == gate.firstNumber + gate.secondNumber) {
                        updateState(content.copy(menuDestination = RoomMenuDestination.PARENT_CABINET, parentGate = null))
                    } else {
                        updateState(content.copy(parentGate = gate.copy(answer = "", hasError = true)))
                    }
                }
            }
            RoomViewEvent.CloseParentGate -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(menuDestination = RoomMenuDestination.MENU, parentGate = null))
            }
            RoomViewEvent.CloseParentCabinet -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(menuDestination = RoomMenuDestination.MENU))
            }
            RoomViewEvent.ClosePlanSummary -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(isPlanSummaryVisible = false))
            }
            RoomViewEvent.CloseWeekResult -> closeWeekResult()
            RoomViewEvent.WeekSummaryTutorialNext -> advanceWeekSummaryTutorial()
            RoomViewEvent.CloseFirstGamePurchaseFeedback -> closeFirstGamePurchaseFeedback()
            is RoomViewEvent.FirstRunViewReadyGame -> viewReadyFirstGame(viewEvent.zoneId)
            RoomViewEvent.CloseFirstWeekNeedHint -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(firstWeekNeedHint = null))
            }
            RoomViewEvent.CloseFirstWeekGoalHint -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(firstWeekGoalHint = null))
            }
            is RoomViewEvent.PlanPercentChanged -> updatePlanPercent(viewEvent.category, viewEvent.percent)
            is RoomViewEvent.PlanReserveChanged -> updatePlanReserve(viewEvent.percent)
            is RoomViewEvent.SavePosition -> {
                savedPosition = viewEvent.position
                positions.save(savedPosition)
                nullableState<RoomViewState.Content>()?.let {
                    updateState(it.copy(initialPosition = savedPosition))
                }
            }
            is RoomViewEvent.ZonePreviewed -> {
                val zone = nullableState<RoomViewState.Content>()?.zones?.find { it.id == viewEvent.zoneId }
                if (zone != null && zone.access !is RoomZoneAccess.Open) onZoneClicked(zone.id)
            }
            RoomViewEvent.Load, RoomViewEvent.RetryClicked -> load()
            is RoomViewEvent.ZoneClicked -> onZoneClicked(viewEvent.zoneId)
            is RoomViewEvent.BuyConfirmed -> buy(viewEvent.zoneId, viewEvent.useSavings)
            is RoomViewEvent.SaveZoneAsGoal -> saveZoneAsGoal(viewEvent.zoneId, viewEvent.title)
        }
    }

    private fun load() {
        if (observationJob?.isActive == true) return
        updateState(RoomViewState.Loading)
        observationJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState(RoomViewState.Error)
                true
            },
        ) {
            onboardingProgress = onboardingRepository.load()
            onboardingStep = firstRunGuide.step.value
            onboardingSuggestedGoalZoneId = onboardingRepository.loadSuggestedGoalZoneId()
                ?.takeIf(FIRST_SAVINGS_GOAL_ZONE_IDS::contains)
            reconcileSavingsLearning()
            combine(
                observeZones(),
                weeklyPlanLearning.observeAchievements(),
                weeklyPlanLearning.observeParentRows(),
                firstRunGuide.step,
            ) { roomData, achievements, parentRows, guideStep ->
                ObservedRoomData(roomData, achievements, parentRows, guideStep)
            }.collect { (roomData, achievements, parentRows, guideStep) ->
                    onboardingStep = guideStep
                    reconcileCommittedOnboardingActions(roomData.progress)
                    val activeGoal = loadActiveSavingsGoal()
                    onboardingGoal = activeGoal
                    val current = nullableState<RoomViewState.Content>()
                    val zones = roomData.toRoomZones()
                    if (onboardingProgress.currentChapter == FirstRunOnboardingChapter.BUDGET_PLANNING &&
                        onboardingStep == FirstRunOnboardingStep.PLAN &&
                        roomData.progress.planProgress != null &&
                        savePlanJob?.isActive != true &&
                        current?.isSavingPlan != true &&
                        current?.planDialogue !is PlanDialogueState.Saved
                    ) {
                        completeOnboardingChapter(FirstRunOnboardingChapter.BUDGET_PLANNING)
                    }
                    val editor = when {
                        roomData.progress.planProgress != null -> null
                        current?.planEditor != null -> current.planEditor
                        onboardingStep == FirstRunOnboardingStep.PLAN -> PlanEditorState()
                        onboardingStep != FirstRunOnboardingStep.COMPLETED -> null
                        roomData.progress.requiresPlan -> PlanEditorState()
                        else -> null
                    }
                    val startsPlanning = editor != null && current?.planEditor == null
                    val shouldStartPlanTutorial = startsPlanning &&
                        onboardingProgress.currentChapter == FirstRunOnboardingChapter.BUDGET_PLANNING &&
                        onboardingStep == FirstRunOnboardingStep.PLAN
                    if (shouldStartPlanTutorial) weeklyPlanLearning.claimIntroduction()
                    val tutorialStep = when {
                        shouldStartPlanTutorial ->
                            PlanTutorialStep.MANDATORY
                        editor == null -> null
                        else -> current?.planTutorialStep
                    }
                    updateState(
                        RoomViewState.Content(
                            zones = zones,
                            initialPosition = savedPosition,
                            progress = roomData.progress,
                            buyingZoneId = current?.buyingZoneId,
                            savingGoalZoneId = current?.savingGoalZoneId,
                            sleepConfirmationVisible = current?.sleepConfirmationVisible ?: false,
                            sleeping = current?.sleeping ?: false,
                            planEditor = editor,
                            planTutorialStep = tutorialStep,
                            planDialogue = current?.planDialogue,
                            isSavingPlan = current?.isSavingPlan ?: false,
                            isPlanSummaryVisible = current?.isPlanSummaryVisible ?: false,
                            weekResult = current?.weekResult,
                            weekSummaryTutorialStep = current?.weekSummaryTutorialStep,
                            showFirstGamePurchaseFeedback = current?.showFirstGamePurchaseFeedback == true ||
                                shouldShowFirstGamePurchaseFeedback(roomData.progress),
                            firstGamePurchaseZoneId = current?.firstGamePurchaseZoneId
                                ?: firstPurchasedGameZoneId(zones, roomData.progress),
                            readyFirstGameZoneId = readyFirstGameZoneId(zones, activeGoal),
                            activeSavingsGoal = activeGoal,
                            firstWeekNeedHint = current?.firstWeekNeedHint,
                            firstWeekGoalHint = current?.firstWeekGoalHint,
                            achievements = achievements.map { achievement ->
                                PlanAchievementFeedback(
                                    id = achievement.id,
                                    topicId = achievement.topicId,
                                    title = achievement.title,
                                    description = achievement.description,
                                    isUnlocked = achievement.isUnlocked,
                                    xpReward = achievement.xpReward,
                                    unlockOrder = achievement.unlockOrder,
                                )
                            },
                            parentRows = parentRows,
                            menuDestination = current?.menuDestination ?: RoomMenuDestination.NONE,
                            areMenuAchievementsExpanded = current?.areMenuAchievementsExpanded ?: false,
                            parentGate = current?.parentGate,
                            parentHelpDialog = current?.parentHelpDialog?.takeIf { dialog ->
                                dialog.activeHelp != null || canOfferParentHelp(
                                    availableRub = roomData.progress.balanceRub.toLong(),
                                    savingsRub = roomData.progress.savingsRub,
                                    debtRub = roomData.progress.debtRub,
                                    hasActiveParentHelp = false,
                                    minimumRequiredBalanceRub = minimumProductPriceRub,
                                )
                            },
                            isRequestingParentHelp = current?.isRequestingParentHelp ?: false,
                            parentHelpPhonePrompt = current?.parentHelpPhonePrompt,
                            savingsRecoveryPrompt = current?.savingsRecoveryPrompt,
                            allowanceNotice = current?.allowanceNotice,
                            earlyWeekParentHelpNotice = current?.earlyWeekParentHelpNotice,
                            dayTransitionNotice = current?.dayTransitionNotice,
                            impulseWish = current?.impulseWish,
                            onboarding = onboardingUiState(),
                        ),
                    )
                    showFirstWeekNeedHintIfNeeded(roomData.progress)
                roomData.progress.planProgress?.let(::reconcilePlanLearning)
                handleLowBalance(roomData.progress)
                if (onboardingStep == FirstRunOnboardingStep.COMPLETED) {
                    loadImpulseWishForDay(roomData.progress.absoluteDay)
                }
            }
        }
    }

    private fun loadImpulseWishForDay(absoluteDay: Long) {
        if (absoluteDay in checkedImpulseWishDays || impulseWishJob?.isActive == true) return
        checkedImpulseWishDays += absoluteDay
        impulseWishJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                checkedImpulseWishDays -= absoluteDay
                impulseWishJob = null
                true
            },
        ) {
            val wish = loadRoomImpulseWish()
            nullableState<RoomViewState.Content>()?.let { content ->
                if (content.progress.absoluteDay == absoluteDay && content.onboarding == null) {
                    updateState(content.copy(impulseWish = wish))
                }
            }
            impulseWishJob = null
        }
    }

    private fun onboardingUiState(): FirstRunOnboardingState? {
        if (onboardingStep == FirstRunOnboardingStep.COMPLETED ||
            onboardingStep == FirstRunOnboardingStep.WAITING_FOR_WEEK_END ||
            onboardingStep == FirstRunOnboardingStep.WEEK_SUMMARY_VIEW
        ) return null
        return FirstRunOnboardingState(
            step = onboardingStep,
            suggestedGoalZoneId = onboardingSuggestedGoalZoneId,
            goalTitle = onboardingGoal?.goal?.title,
            goalTargetRub = onboardingGoal?.goal?.targetRub,
            goalSavedRub = onboardingGoal?.savedRub ?: 0,
        )
    }

    private fun completeOnboardingChapter(
        chapter: FirstRunOnboardingChapter,
        nextStep: FirstRunOnboardingStep? = null,
    ) {
        onboardingProgress = onboardingRepository.markChapterCompleted(chapter)
        onboardingStep = nextStep ?: onboardingProgress.firstStep
        firstRunGuide.moveTo(onboardingStep)
        if (onboardingProgress.isCompleted) {
            onboardingSuggestedGoalZoneId = null
            onboardingRepository.saveSuggestedGoalZoneId(null)
        }
    }

    private fun transitionOnboarding(step: FirstRunOnboardingStep) {
        onboardingStep = step
        firstRunGuide.moveTo(step)
        nullableState<RoomViewState.Content>()?.let { content ->
            updateState(content.copy(onboarding = onboardingUiState()))
            if (step == FirstRunOnboardingStep.COMPLETED) {
                loadImpulseWishForDay(content.progress.absoluteDay)
                handleLowBalance(content.progress)
            }
            if (step == FirstRunOnboardingStep.WAITING_FOR_WEEK_END) {
                showFirstWeekNeedHintIfNeeded(content.progress)
                loadFirstWeekGoalHintIfNeeded()
            }
        }
    }

    private fun continueFirstRunOnboarding() {
        when (onboardingStep) {
            FirstRunOnboardingStep.INTRODUCTION -> transitionOnboarding(FirstRunOnboardingStep.FIRST_MONEY)
            FirstRunOnboardingStep.WISH -> transitionOnboarding(FirstRunOnboardingStep.FIRST_MONEY)
            FirstRunOnboardingStep.MONEY_EXPLANATION ->
                transitionOnboarding(FirstRunOnboardingStep.PLAN_TRANSITION)
            FirstRunOnboardingStep.PLAN_TRANSITION -> startFirstPlan()
            FirstRunOnboardingStep.GAME_DISCOVERY ->
                transitionOnboarding(FirstRunOnboardingStep.GAME_DISCOVERY_DETAILS)
            FirstRunOnboardingStep.GAME_DISCOVERY_DETAILS ->
                transitionOnboarding(FirstRunOnboardingStep.GAME_SELECTION)
            FirstRunOnboardingStep.GAME_SELECTED -> {
                completeOnboardingChapter(FirstRunOnboardingChapter.MINI_GAMES_DISCOVERY)
                transitionOnboarding(onboardingProgress.firstStep)
            }
            FirstRunOnboardingStep.PIGGY_BANK -> transitionOnboarding(FirstRunOnboardingStep.PIGGY_TAP)
            FirstRunOnboardingStep.PIGGY_TAP -> {
                completeOnboardingChapter(FirstRunOnboardingChapter.PIGGY_BANK_DISCOVERY)
                transitionOnboarding(onboardingProgress.firstStep)
            }
            FirstRunOnboardingStep.GOAL_CREATED -> transitionOnboarding(
                if ((onboardingGoal?.savedRub ?: 0) > 0) {
                    FirstRunOnboardingStep.DEPOSIT_DONE
                } else {
                    FirstRunOnboardingStep.FIRST_DEPOSIT
                },
            )
            FirstRunOnboardingStep.DEPOSIT_DONE,
            FirstRunOnboardingStep.DEPOSIT_SKIPPED,
            -> {
                completeOnboardingChapter(FirstRunOnboardingChapter.FIRST_GOAL_SELECTION)
                transitionOnboarding(onboardingProgress.firstStep)
            }
            FirstRunOnboardingStep.HUNGER_INTRO ->
                transitionOnboarding(FirstRunOnboardingStep.HUNGER_FIND_FOOD)
            FirstRunOnboardingStep.HUNGER_FIND_FOOD -> guideToAvailableFood()
            FirstRunOnboardingStep.PHONE_GUIDANCE ->
                transitionOnboarding(FirstRunOnboardingStep.WAITING_FOR_PHONE)
            FirstRunOnboardingStep.FRIDGE_GUIDANCE ->
                transitionOnboarding(FirstRunOnboardingStep.WAITING_FOR_FRIDGE)
            FirstRunOnboardingStep.PURCHASE_READY ->
                transitionOnboarding(FirstRunOnboardingStep.FRIDGE_GUIDANCE)
            FirstRunOnboardingStep.PURCHASE_STORAGE_HINT ->
                transitionOnboarding(FirstRunOnboardingStep.FRIDGE_GUIDANCE)
            FirstRunOnboardingStep.TABLE_PROMPT ->
                transitionOnboarding(FirstRunOnboardingStep.TABLE_GUIDANCE)
            FirstRunOnboardingStep.BEDTIME_LATE ->
                transitionOnboarding(FirstRunOnboardingStep.BEDTIME_GUIDANCE)
            FirstRunOnboardingStep.BEDTIME_GUIDANCE ->
                transitionOnboarding(FirstRunOnboardingStep.WAITING_FOR_BED)
            FirstRunOnboardingStep.SECOND_DAY_MORNING -> {
                completeOnboardingChapter(FirstRunOnboardingChapter.SECOND_DAY_MORNING)
                transitionOnboarding(onboardingProgress.firstStep)
            }
            FirstRunOnboardingStep.NEW_WEEK_INTRO ->
                transitionOnboarding(FirstRunOnboardingStep.NEW_WEEK_PLAN_GUIDANCE)
            FirstRunOnboardingStep.GAMES,
            FirstRunOnboardingStep.FINISH,
            -> transitionOnboarding(onboardingProgress.firstStep)
            FirstRunOnboardingStep.FIRST_MONEY,
            FirstRunOnboardingStep.PLAN,
            FirstRunOnboardingStep.GAME_SELECTION,
            FirstRunOnboardingStep.WAITING_FOR_PIGGY,
            FirstRunOnboardingStep.WAITING_FOR_GOAL,
            FirstRunOnboardingStep.FIRST_DEPOSIT,
            FirstRunOnboardingStep.WAITING_FOR_DEPOSIT,
            FirstRunOnboardingStep.WAITING_FOR_HUNGER,
            FirstRunOnboardingStep.WAITING_FOR_PHONE,
            FirstRunOnboardingStep.PHONE_STORE_GUIDANCE,
            FirstRunOnboardingStep.WAITING_FOR_STORE,
            FirstRunOnboardingStep.SHOP_PRICE_GUIDANCE,
            FirstRunOnboardingStep.SHOP_FOOD_GUIDANCE,
            FirstRunOnboardingStep.SHOP_FOOD_SELECTED,
            FirstRunOnboardingStep.WAITING_FOR_FRIDGE,
            FirstRunOnboardingStep.FRIDGE_FOUND,
            FirstRunOnboardingStep.FRIDGE_EXPLANATION,
            FirstRunOnboardingStep.FRIDGE_PICK_FOOD,
            FirstRunOnboardingStep.WAITING_FOR_FRIDGE_CLOSE,
            FirstRunOnboardingStep.TABLE_GUIDANCE,
            FirstRunOnboardingStep.FEEDING,
            FirstRunOnboardingStep.FEEDING_DONE,
            FirstRunOnboardingStep.WAITING_FOR_BED,
            FirstRunOnboardingStep.WAITING_FOR_WEEK_END,
            FirstRunOnboardingStep.WEEK_END_INTRO,
            FirstRunOnboardingStep.WEEK_SUMMARY_VIEW,
            FirstRunOnboardingStep.NEW_WEEK_PLAN_GUIDANCE,
            FirstRunOnboardingStep.COMPLETED,
            -> Unit
        }
    }

    private fun reconcileCommittedOnboardingActions(
        progress: github.detrig.feature.room.domain.model.RoomProgress,
    ) {
        var reconciled: Boolean
        do {
            reconciled = when (onboardingProgress.currentChapter) {
                FirstRunOnboardingChapter.FIRST_BEDTIME -> progress.absoluteDay > 1L
                FirstRunOnboardingChapter.SECOND_DAY_MORNING -> progress.absoluteDay > 2L
                FirstRunOnboardingChapter.FIRST_WEEK_SUMMARY -> progress.weekNumber > 1L
                else -> false
            }
            if (reconciled) {
                completeOnboardingChapter(checkNotNull(onboardingProgress.currentChapter))
            }
        } while (reconciled)
    }

    private fun guideToAvailableFood() {
        launchCoroutine {
            val hasFood = inventoryApi.observeStock().first().any { it.quantity > 0 }
            transitionOnboarding(
                if (hasFood) FirstRunOnboardingStep.FRIDGE_GUIDANCE
                else FirstRunOnboardingStep.PHONE_GUIDANCE,
            )
        }
    }

    private fun showFirstWeekNeedHintIfNeeded(
        progress: github.detrig.feature.room.domain.model.RoomProgress,
    ) {
        if (onboardingStep != FirstRunOnboardingStep.WAITING_FOR_WEEK_END ||
            progress.petHunger > FIRST_WEEK_HUNGER_HINT_THRESHOLD ||
            progress.absoluteDay in hintedHungerDays ||
            firstWeekNeedHintJob?.isActive == true
        ) return
        hintedHungerDays += progress.absoluteDay
        firstWeekNeedHintJob = launchCoroutine {
            val fridgeIsEmpty = inventoryApi.observeStock().first().none { it.quantity > 0 }
            nullableState<RoomViewState.Content>()?.let { content ->
                if (content.progress.absoluteDay == progress.absoluteDay &&
                    content.firstWeekNeedHint == null
                ) {
                    updateState(content.copy(firstWeekNeedHint = FirstWeekNeedHint(fridgeIsEmpty)))
                }
            }
            firstWeekNeedHintJob = null
        }
    }

    private fun loadFirstWeekGoalHintIfNeeded() {
        if (firstWeekGoalHintShown || firstWeekGoalHintJob?.isActive == true) return
        firstWeekGoalHintJob = launchCoroutine {
            val goal = loadActiveSavingsGoal()
            val threshold = goal?.goal?.targetRub?.div(5)?.coerceAtLeast(50L) ?: 0L
            if (goal != null && goal.remainingRub in 1..threshold) {
                firstWeekGoalHintShown = true
                nullableState<RoomViewState.Content>()?.let { content ->
                    updateState(content.copy(firstWeekGoalHint = FirstWeekGoalHint(goal.remainingRub)))
                }
            }
            firstWeekGoalHintJob = null
        }
    }

    private fun startFirstPlan() {
        val content = nullableState<RoomViewState.Content>() ?: return
        completeOnboardingChapter(
            chapter = FirstRunOnboardingChapter.INTRODUCTION_AND_FIRST_MONEY,
            nextStep = FirstRunOnboardingStep.PLAN,
        )
        if (content.progress.planProgress != null) {
            completeOnboardingChapter(FirstRunOnboardingChapter.BUDGET_PLANNING)
            transitionOnboarding(onboardingProgress.firstStep)
            return
        }
        updateState(content.copy(
            onboarding = onboardingUiState(),
            planEditor = content.planEditor ?: PlanEditorState(),
            planTutorialStep = PlanTutorialStep.MANDATORY,
        ))
        launchCoroutine {
            weeklyPlanLearning.claimIntroduction()
        }
    }

    private fun openPiggyBank() {
        if (onboardingStep == FirstRunOnboardingStep.WAITING_FOR_PIGGY) {
            transitionOnboarding(FirstRunOnboardingStep.WAITING_FOR_GOAL)
            val suggestedZoneId = onboardingSuggestedGoalZoneId ?: DEFAULT_SUGGESTED_GOAL_ZONE_ID
            openSavings(
                firstRunOnboarding = true,
                suggestedGoalId = "$ROOM_GOAL_ID_PREFIX$suggestedZoneId",
            )
            return
        }
        if (onboardingStep != FirstRunOnboardingStep.COMPLETED &&
            onboardingStep != FirstRunOnboardingStep.WAITING_FOR_WEEK_END
        ) return
        openSavings()
    }

    private fun selectFirstDeposit(depositNow: Boolean) {
        if (onboardingStep != FirstRunOnboardingStep.FIRST_DEPOSIT) return
        if (depositNow) {
            transitionOnboarding(FirstRunOnboardingStep.WAITING_FOR_DEPOSIT)
            openSavings()
        } else {
            transitionOnboarding(FirstRunOnboardingStep.DEPOSIT_SKIPPED)
        }
    }

    private fun refreshFirstRunOnboarding() {
        val externalStep = firstRunGuide.step.value
        if (externalStep != onboardingStep) {
            onboardingStep = externalStep
            transitionOnboarding(externalStep)
        }
        if (externalStep == FirstRunOnboardingStep.WAITING_FOR_WEEK_END) {
            loadFirstWeekGoalHintIfNeeded()
        }
        if (onboardingRefreshJob?.isActive == true) return
        if (onboardingStep != FirstRunOnboardingStep.WAITING_FOR_GOAL &&
            onboardingStep != FirstRunOnboardingStep.WAITING_FOR_DEPOSIT
        ) return
        onboardingRefreshJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                onboardingRefreshJob = null
                true
            },
        ) {
            val goal = loadActiveSavingsGoal()
            onboardingGoal = goal
            when (onboardingStep) {
                FirstRunOnboardingStep.WAITING_FOR_GOAL -> if (goal == null) {
                    transitionOnboarding(FirstRunOnboardingStep.WAITING_FOR_PIGGY)
                } else {
                    completeOnboardingChapter(FirstRunOnboardingChapter.FIRST_GOAL_SELECTION)
                    transitionOnboarding(onboardingProgress.firstStep)
                }
                FirstRunOnboardingStep.WAITING_FOR_DEPOSIT -> if (goal == null) {
                    transitionOnboarding(FirstRunOnboardingStep.WAITING_FOR_PIGGY)
                } else {
                    completeOnboardingChapter(FirstRunOnboardingChapter.FIRST_GOAL_SELECTION)
                    transitionOnboarding(onboardingProgress.firstStep)
                }
                else -> Unit
            }
            onboardingRefreshJob = null
        }
    }

    private fun onZoneClicked(zoneId: String) {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.buyingZoneId != null) return
        val zone = content.zones.find { it.id == zoneId } ?: return
        if (onboardingStep == FirstRunOnboardingStep.GAME_SELECTION) {
            if (zoneId !in FIRST_SAVINGS_GOAL_ZONE_IDS || zone.access !is RoomZoneAccess.Buyable) return
            onboardingSuggestedGoalZoneId = zoneId
            onboardingRepository.saveSuggestedGoalZoneId(zoneId)
            commands.onNext(RoomCommand.ShowBuyConfirmation(zoneId))
            return
        }
        if (onboardingStep != FirstRunOnboardingStep.COMPLETED &&
            onboardingStep != FirstRunOnboardingStep.WAITING_FOR_WEEK_END
        ) return
        when (val access = zone.access) {
            RoomZoneAccess.Open -> launchOnce { router.openGame(zone.gameId) }
            is RoomZoneAccess.Unavailable -> router.showLevelRequired(access.requiredLevel)
            is RoomZoneAccess.Buyable -> commands.onNext(RoomCommand.ShowBuyConfirmation(zoneId))
        }
    }

    private fun launchOnce(action: () -> Unit) {
        val now = System.nanoTime()
        if (lastLaunchNanos != 0L && now - lastLaunchNanos < 700_000_000L) return
        lastLaunchNanos = now
        action()
    }

    private fun buy(zoneId: String, useSavings: Boolean) {
        if (buyJob?.isActive == true) return
        val content = nullableState<RoomViewState.Content>() ?: return
        val zone = content.zones.find { it.id == zoneId } ?: return
        if (zone.access !is RoomZoneAccess.Buyable) return
        updateState(content.copy(buyingZoneId = zoneId))
        buyJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                router.showBuyError()
                true
            },
        ) {
            try {
                when (val result = buyZone(zoneId, useSavings)) {
                    ZoneBuyResult.Bought -> {
                        commands.onNext(RoomCommand.CloseBuyConfirmation(zoneId))
                        router.showBought()
                        if (zoneId in FIRST_SAVINGS_GOAL_ZONE_IDS &&
                            !onboardingRepository.isFirstGamePurchaseExplained()
                        ) {
                            nullableState<RoomViewState.Content>()?.let { latest ->
                                updateState(latest.copy(
                                    firstGamePurchaseZoneId = zoneId,
                                    showFirstGamePurchaseFeedback = true,
                                ))
                            }
                        }
                    }
                    ZoneBuyResult.AlreadyOwned -> commands.onNext(RoomCommand.CloseBuyConfirmation(zoneId))
                    is ZoneBuyResult.NotEnoughMoney -> router.showNotEnoughMoney(result.missingRub)
                    is ZoneBuyResult.LevelTooLow -> {
                        commands.onNext(RoomCommand.CloseBuyConfirmation(zoneId))
                        router.showLevelRequired(result.requiredLevel)
                    }
                }
            } finally {
                nullableState<RoomViewState.Content>()?.let { updateState(it.copy(buyingZoneId = null)) }
            }
        }
    }

    private fun saveZoneAsGoal(zoneId: String, title: String) {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.savingGoalZoneId != null || content.buyingZoneId != null) return
        val isFirstRunGoal = onboardingStep == FirstRunOnboardingStep.GAME_SELECTION
        updateState(content.copy(savingGoalZoneId = zoneId))
        launchCoroutine(
            handleAction = ExceptionConsumer {
                nullableState<RoomViewState.Content>()?.let { updateState(it.copy(savingGoalZoneId = null)) }
                router.showSavingsGoalError()
                true
            },
        ) {
            try {
                saveZoneGoal(zoneId, title)
                commands.onNext(RoomCommand.CloseBuyConfirmation(zoneId))
                if (isFirstRunGoal) {
                    completeOnboardingChapter(FirstRunOnboardingChapter.MINI_GAMES_DISCOVERY)
                    transitionOnboarding(onboardingProgress.firstStep)
                } else {
                    openSavings()
                }
            } finally {
                nullableState<RoomViewState.Content>()?.let { updateState(it.copy(savingGoalZoneId = null)) }
            }
        }
    }

    private fun showSleepConfirmation() {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.sleeping || content.buyingZoneId != null) return
        if (onboardingStep != FirstRunOnboardingStep.COMPLETED &&
            onboardingStep != FirstRunOnboardingStep.WAITING_FOR_BED &&
            onboardingStep != FirstRunOnboardingStep.WAITING_FOR_WEEK_END
        ) return
        updateState(content.copy(sleepConfirmationVisible = true))
    }

    private fun hideSleepConfirmation() {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.sleeping) return
        updateState(content.copy(sleepConfirmationVisible = false))
    }

    private fun sleep() {
        if (sleepJob?.isActive == true) return
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.sleeping || content.buyingZoneId != null) return
        if (content.progress.petHunger <= 0) {
            updateState(content.copy(sleepConfirmationVisible = true))
            return
        }
        val expectedDay = content.progress.absoluteDay
        updateState(content.copy(sleepConfirmationVisible = false, sleeping = true))
        sleepJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                router.showSleepError()
                true
            },
        ) {
            try {
                delay(800)
                if (content.progress.dayOfWeek == 7 &&
                    onboardingStep == FirstRunOnboardingStep.WAITING_FOR_WEEK_END
                ) {
                    transitionOnboarding(FirstRunOnboardingStep.WEEK_END_INTRO)
                    return@launchCoroutine
                }
                if (content.progress.dayOfWeek == 7 && content.progress.planProgress != null) {
                    nullableState<RoomViewState.Content>()?.let { latest ->
                        updateState(latest.copy(
                            sleeping = false,
                            weekResult = content.progress.planProgress,
                        ))
                    }
                    return@launchCoroutine
                }
                val result = advanceDay(expectedDay)
                if (result is EndDayResult.Advanced &&
                    onboardingStep == FirstRunOnboardingStep.WAITING_FOR_BED
                ) {
                    completeOnboardingChapter(FirstRunOnboardingChapter.FIRST_BEDTIME)
                    transitionOnboarding(onboardingProgress.firstStep)
                }
            } finally {
                nullableState<RoomViewState.Content>()?.let { updateState(it.copy(sleeping = false)) }
            }
        }
    }

    private fun closeWeekResult() {
        if (sleepJob?.isActive == true) return
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.weekResult == null) return
        val expectedDay = content.progress.absoluteDay
        updateState(content.copy(weekResult = null, sleeping = true))
        sleepJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                router.showSleepError()
                true
            },
        ) {
            try {
                advanceDay(expectedDay, showAllowanceNotice = onboardingStep != FirstRunOnboardingStep.WEEK_SUMMARY_VIEW)
                if (onboardingStep == FirstRunOnboardingStep.WEEK_SUMMARY_VIEW) {
                    completeOnboardingChapter(FirstRunOnboardingChapter.FIRST_WEEK_SUMMARY)
                    transitionOnboarding(onboardingProgress.firstStep)
                }
            } finally {
                nullableState<RoomViewState.Content>()?.let { updateState(it.copy(sleeping = false)) }
            }
        }
    }

    private suspend fun advanceDay(
        expectedDay: Long,
        showAllowanceNotice: Boolean = true,
    ): EndDayResult {
        val result = endDay(expectedDay)
        if (result is EndDayResult.Advanced) gameAudio.play(RoomAudioCues.Sleep)
        if (showAllowanceNotice && onboardingStep == FirstRunOnboardingStep.COMPLETED &&
            result is EndDayResult.Advanced
        ) {
            nullableState<RoomViewState.Content>()?.let { latest ->
                updateState(latest.copy(
                    dayTransitionNotice = DayTransitionNoticeState(
                        dayOfWeek = result.state.dayOfWeek,
                        weekNumber = result.state.weekNumber,
                    ),
                    allowanceNotice = if (result.allowanceGrossRub > 0) {
                        AllowanceNoticeState(
                            grossRub = result.allowanceGrossRub,
                            parentHelpRepaidRub = result.parentHelpRepaidRub,
                            receivedRub = result.allowanceReceivedRub,
                        )
                    } else {
                        latest.allowanceNotice
                    },
                ))
            }
        }
        return result
    }

    private fun showFirstWeekSummary() {
        if (onboardingStep != FirstRunOnboardingStep.WEEK_END_INTRO) return
        val content = nullableState<RoomViewState.Content>() ?: return
        val progress = content.progress.planProgress ?: return
        transitionOnboarding(FirstRunOnboardingStep.WEEK_SUMMARY_VIEW)
        nullableState<RoomViewState.Content>()?.let { latest ->
            updateState(latest.copy(
                weekResult = progress,
                weekSummaryTutorialStep = WeekSummaryTutorialStep.INCOME,
                onboarding = null,
            ))
        }
    }

    private fun advanceWeekSummaryTutorial() {
        val content = nullableState<RoomViewState.Content>() ?: return
        val next = when (content.weekSummaryTutorialStep) {
            WeekSummaryTutorialStep.INCOME -> WeekSummaryTutorialStep.EXPENSES
            WeekSummaryTutorialStep.EXPENSES -> WeekSummaryTutorialStep.REMAINDER
            WeekSummaryTutorialStep.REMAINDER -> null
            null -> return
        }
        updateState(content.copy(weekSummaryTutorialStep = next))
    }

    private fun shouldShowFirstGamePurchaseFeedback(progress: github.detrig.feature.room.domain.model.RoomProgress): Boolean {
        if (onboardingRepository.isFirstGamePurchaseExplained()) return false
        if (onboardingStep != FirstRunOnboardingStep.WAITING_FOR_WEEK_END &&
            onboardingStep != FirstRunOnboardingStep.COMPLETED
        ) return false
        return progress.ownedZoneIds.any(FIRST_SAVINGS_GOAL_ZONE_IDS::contains)
    }

    private fun closeFirstGamePurchaseFeedback() {
        val content = nullableState<RoomViewState.Content>() ?: return
        onboardingRepository.markFirstGamePurchaseExplained()
        updateState(content.copy(
            showFirstGamePurchaseFeedback = false,
            firstGamePurchaseZoneId = null,
        ))
    }

    private fun firstPurchasedGameZoneId(
        zones: List<github.detrig.feature.room.presentation.model.RoomZoneUiModel>,
        progress: github.detrig.feature.room.domain.model.RoomProgress,
    ): String? = zones.firstOrNull { zone ->
        zone.id == onboardingSuggestedGoalZoneId && zone.id in progress.ownedZoneIds
    }?.id ?: zones.firstOrNull { zone ->
        zone.id in FIRST_SAVINGS_GOAL_ZONE_IDS && zone.id in progress.ownedZoneIds
    }?.id

    private fun isSelectedGameGoalReached(zoneId: String, priceRub: Int, goal: SavingsGoalProgress?): Boolean =
        goal?.let { it.goal.id == "$ROOM_GOAL_ID_PREFIX$zoneId" && it.savedRub >= priceRub } == true

    private fun readyFirstGameZoneId(
        zones: List<github.detrig.feature.room.presentation.model.RoomZoneUiModel>,
        goal: SavingsGoalProgress?,
    ): String? {
        if (onboardingStep != FirstRunOnboardingStep.WAITING_FOR_WEEK_END ||
            onboardingRepository.isFirstGameReadyIntroduced()
        ) return null
        val suggested = onboardingSuggestedGoalZoneId ?: return null
        return zones.firstOrNull { zone ->
            zone.id == suggested && zone.access is RoomZoneAccess.Buyable &&
                isSelectedGameGoalReached(zone.id, zone.access.priceRub, goal)
        }?.id
    }

    private fun viewReadyFirstGame(zoneId: String) {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.readyFirstGameZoneId != zoneId) return
        val zone = content.zones.firstOrNull { it.id == zoneId } ?: return
        if (zone.access !is RoomZoneAccess.Buyable ||
            !isSelectedGameGoalReached(zone.id, zone.access.priceRub, content.activeSavingsGoal)
        ) return
        onboardingRepository.markFirstGameReadyIntroduced()
        updateState(content.copy(readyFirstGameZoneId = null))
        commands.onNext(RoomCommand.ShowBuyConfirmation(zoneId))
    }

    private fun startNextWeekPlan() {
        if (onboardingStep != FirstRunOnboardingStep.NEW_WEEK_PLAN_GUIDANCE) return
        val content = nullableState<RoomViewState.Content>() ?: return
        completeOnboardingChapter(FirstRunOnboardingChapter.NEXT_WEEK_PLANNING)
        updateState(content.copy(
            onboarding = null,
            planEditor = content.planEditor ?: PlanEditorState(),
            planTutorialStep = null,
        ))
    }

    private fun updatePlanPercent(category: github.detrig.feature.planning.domain.PlanCategory, percent: Int) {
        val content = nullableState<RoomViewState.Content>() ?: return
        val editor = content.planEditor ?: return
        if (content.isSavingPlan || content.planTutorialStep != null) return
        updateState(content.copy(planEditor = editor.update(category, percent)))
    }

    private fun updatePlanReserve(percent: Int) {
        val content = nullableState<RoomViewState.Content>() ?: return
        val editor = content.planEditor ?: return
        if (content.isSavingPlan || content.planTutorialStep != null) return
        updateState(content.copy(planEditor = editor.updateReserve(percent)))
    }

    private fun advancePlanTutorial() {
        val content = nullableState<RoomViewState.Content>() ?: return
        val step = content.planTutorialStep ?: return
        updateState(content.copy(planTutorialStep =
            if (step == PlanTutorialStep.RESERVE) null else step.nextOrNull()))
    }

    private fun closePlanDialogue() {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.planDialogue is PlanDialogueState.NeedsChanges) {
            persistPlan(content.copy(planDialogue = null))
            return
        }
        if (content.planDialogue is PlanDialogueState.Saved &&
            onboardingStep == FirstRunOnboardingStep.PLAN
        ) {
            completeOnboardingChapter(
                chapter = FirstRunOnboardingChapter.BUDGET_PLANNING,
            )
        }
        updateState(content.copy(
            planDialogue = null,
            onboarding = onboardingUiState(),
        ))
    }

    private fun savePlan() {
        if (savePlanJob?.isActive == true) return
        val content = nullableState<RoomViewState.Content>() ?: return
        val editor = content.planEditor ?: return
        if (editor.total > 100 || content.planTutorialStep != null) return
        when (val assessment = assessWeeklyPlan(editor.toPercentages())) {
            PlanAssessment.Adequate -> Unit
            is PlanAssessment.NeedsChanges -> {
                updateState(content.copy(
                    planDialogue = PlanDialogueState.NeedsChanges(
                        reason = assessment.reason,
                        recommendedPercent = assessment.recommendedPercent,
                    ),
                ))
                return
            }
        }
        persistPlan(content)
    }

    private fun persistPlan(content: RoomViewState.Content) {
        if (savePlanJob?.isActive == true) return
        val editor = content.planEditor ?: return
        updateState(content.copy(isSavingPlan = true))
        savePlanJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                nullableState<RoomViewState.Content>()?.let { latest ->
                    updateState(latest.copy(isSavingPlan = false))
                }
                router.showPlanSaveError()
                true
            },
        ) {
            val outcome = saveWeeklyPlan(
                weekNumber = content.progress.weekNumber,
                availableRub = content.progress.balanceRub.toLong(),
                percentages = editor.toPercentages(),
            )
            val plan = outcome.progress
            val feedback = outcome.learningFeedback
            val regularDialogue = PlanDialogueState.Saved(
                showSuccessExplanation = feedback.showSuccessExplanation,
                hasSavings = plan.plan.plannedRub(github.detrig.feature.planning.domain.PlanCategory.SAVINGS) > 0,
            ).takeIf { it.showSuccessExplanation }
            nullableState<RoomViewState.Content>()?.let { latest ->
                val isFirstRunPlan = onboardingStep == FirstRunOnboardingStep.PLAN
                if (isFirstRunPlan && regularDialogue == null) {
                    completeOnboardingChapter(
                        chapter = FirstRunOnboardingChapter.BUDGET_PLANNING,
                    )
                }
                updateState(latest.copy(
                    progress = latest.progress.copy(planProgress = plan, requiresPlan = false),
                    planEditor = null,
                    planTutorialStep = null,
                    planDialogue = regularDialogue,
                    onboarding = onboardingUiState(),
                    isSavingPlan = false,
                ))
            }
        }
    }

    private fun reconcilePlanLearning(progress: WeeklyPlanProgress) {
        val weekNumber = progress.plan.weekNumber
        if (savePlanJob?.isActive == true) return
        if (assessWeeklyPlan(progress.plan.percentages) !is PlanAssessment.Adequate) return
        if (weekNumber in reconciledPlanWeeks || reconcilingPlanWeek == weekNumber) return
        reconcilingPlanWeek = weekNumber
        launchCoroutine(
            handleAction = ExceptionConsumer {
                reconcilingPlanWeek = null
                true
            },
        ) {
            weeklyPlanLearning.reconcile(progress.plan)
            reconciledPlanWeeks += weekNumber
            reconcilingPlanWeek = null
        }
    }

    private fun showPlanSummary() {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.progress.planProgress == null) {
            router.showPlanNotReady()
            return
        }
        updateState(content.copy(isPlanSummaryVisible = true))
    }

    private fun requestParentHelp(offerId: String) {
        if (parentHelpJob?.isActive == true) return
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.parentHelpDialog == null || content.isRequestingParentHelp) return
        updateState(content.copy(isRequestingParentHelp = true))
        parentHelpJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                nullableState<RoomViewState.Content>()?.let { latest ->
                    updateState(latest.copy(isRequestingParentHelp = false))
                }
                true
            },
        ) {
            val result = requestParentHelpInteractor(offerId)
            val activeHelp = when (result) {
                is ParentHelpRequestResult.Accepted -> result.help
                is ParentHelpRequestResult.AlreadyActive -> result.help
                is ParentHelpRequestResult.Rejected -> loadParentHelpInteractor()
            }
            nullableState<RoomViewState.Content>()?.let { latest ->
                updateState(latest.copy(
                    parentHelpDialog = latest.parentHelpDialog?.copy(activeHelp = activeHelp),
                    isRequestingParentHelp = false,
                ))
            }
        }
    }

    private fun handleLowBalance(progress: github.detrig.feature.room.domain.model.RoomProgress) {
        val recoveryAction = lowBalanceRecoveryAction(
            availableRub = progress.balanceRub.toLong(),
            savingsRub = progress.savingsRub,
            minimumRequiredBalanceRub = minimumProductPriceRub,
        )
        if (recoveryAction == LowBalanceRecoveryAction.NONE ||
            (recoveryAction == LowBalanceRecoveryAction.ASK_PARENTS &&
                onboardingStep != FirstRunOnboardingStep.COMPLETED) ||
            lowBalanceJob?.isActive == true
        ) return
        lowBalanceJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                lowBalanceJob = null
                true
            },
        ) {
            if (progress.savingsRub > 0) {
                val current = nullableState<RoomViewState.Content>()
                if (current?.savingsRecoveryPrompt != null) {
                    lowBalanceJob = null
                    return@launchCoroutine
                }
                if (progress.weekNumber !in promptedSavingsRecoveryWeeks &&
                    current != null &&
                    current.earlyWeekParentHelpNotice == null
                ) {
                    promptedSavingsRecoveryWeeks += progress.weekNumber
                    updateState(current.copy(
                        savingsRecoveryPrompt = SavingsRecoveryPromptState,
                    ))
                    lowBalanceJob = null
                    return@launchCoroutine
                }
            }
            if (recoveryAction == LowBalanceRecoveryAction.USE_SAVINGS) {
                lowBalanceJob = null
                return@launchCoroutine
            }

            val activeHelp = loadParentHelpInteractor()
            if (activeHelp == null) {
                val canOfferHelp = shouldOfferAutomaticParentHelp(
                    onboardingCompleted = onboardingStep == FirstRunOnboardingStep.COMPLETED,
                    availableRub = progress.balanceRub.toLong(),
                    savingsRub = progress.savingsRub,
                    debtRub = progress.debtRub,
                    hasActiveParentHelp = false,
                    minimumRequiredBalanceRub = minimumProductPriceRub,
                    alreadyShownInWeek = parentHelpPromptRepository.wasShownInWeek(progress.weekNumber),
                )
                val current = nullableState<RoomViewState.Content>()
                if (canOfferHelp &&
                    current?.parentHelpDialog == null &&
                    current?.earlyWeekParentHelpNotice == null
                ) {
                    val offers = loadParentHelpInteractor.offers()
                    if (offers.isNotEmpty()) updateState(checkNotNull(current).copy(
                        parentHelpDialog = ParentHelpDialogState(
                            offers = offers,
                            activeHelp = null,
                            availableRub = progress.balanceRub.toLong(),
                            savingsRub = progress.savingsRub,
                            debtRub = progress.debtRub,
                            minimumRequiredBalanceRub = minimumProductPriceRub,
                        ),
                    ))
                }
            } else {
                when (val result = endWeekEarlyWithParentHelp(
                    expectedAbsoluteDay = progress.absoluteDay,
                    minimumProductPriceRub = minimumProductPriceRub,
                )) {
                    is EarlyWeekEndResult.Completed -> nullableState<RoomViewState.Content>()?.let { latest ->
                        updateState(latest.copy(
                            earlyWeekParentHelpNotice = EarlyWeekParentHelpNoticeState,
                            allowanceNotice = AllowanceNoticeState(
                                grossRub = result.allowanceGrossRub,
                                parentHelpRepaidRub = result.parentHelpRepaidRub,
                                receivedRub = result.allowanceReceivedRub,
                            ),
                        ))
                    }
                    is EarlyWeekEndResult.AlreadyCompleted,
                    is EarlyWeekEndResult.NotNeeded,
                    -> Unit
                }
            }
            lowBalanceJob = null
        }
    }
}
