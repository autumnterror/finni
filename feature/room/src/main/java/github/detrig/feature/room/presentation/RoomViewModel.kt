package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.room.domain.interactor.BuyRoomZoneInteractor
import github.detrig.feature.room.domain.interactor.EndDayInteractor
import github.detrig.feature.room.domain.interactor.SaveWeeklyPlanInteractor
import github.detrig.feature.room.domain.interactor.AssessWeeklyPlanInteractor
import github.detrig.feature.room.domain.interactor.WeeklyPlanLearningInteractor
import github.detrig.feature.room.domain.interactor.OpenSavingsInteractor
import github.detrig.feature.room.domain.interactor.LoadActiveSavingsGoalInteractor
import github.detrig.feature.room.domain.interactor.SaveZoneAsSavingsGoalInteractor
import github.detrig.feature.room.domain.interactor.LoadParentHelpInteractor
import github.detrig.feature.room.domain.interactor.RequestParentHelpInteractor
import github.detrig.feature.room.domain.interactor.EndWeekEarlyWithParentHelpInteractor
import github.detrig.feature.room.domain.interactor.ObserveRoomZonesInteractor
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.navigation.RoomRouter
import github.detrig.feature.room.presentation.mapper.toRoomZones
import github.detrig.feature.room.domain.model.HousePositionRepository
import github.detrig.feature.room.domain.model.FirstRunOnboardingRepository
import github.detrig.feature.room.domain.model.FirstRunOnboardingStep
import github.detrig.feature.room.presentation.model.HouseLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import github.detrig.feature.planning.domain.PlanAssessment
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.week.domain.EarlyWeekEndResult
import github.detrig.feature.economy.domain.SavingsGoalProgress

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
    private val loadParentHelpInteractor: LoadParentHelpInteractor,
    private val requestParentHelpInteractor: RequestParentHelpInteractor,
    private val endWeekEarlyWithParentHelp: EndWeekEarlyWithParentHelpInteractor,
    private val minimumProductPriceRub: Long,
    private val router: RoomRouter,
    private val positions: HousePositionRepository,
    private val onboardingRepository: FirstRunOnboardingRepository,
) : CoreViewModel<RoomViewState, RoomViewEvent>(RoomViewState.Loading) {
    private var observationJob: Job? = null
    private var buyJob: Job? = null
    private var sleepJob: Job? = null
    private var savePlanJob: Job? = null
    private var parentHelpJob: Job? = null
    private var lowBalanceJob: Job? = null
    private var onboardingRefreshJob: Job? = null
    private var achievementBannerJob: Job? = null
    private val reconciledPlanWeeks = mutableSetOf<Long>()
    private var reconcilingPlanWeek: Long? = null
    // Read this small preference before the first composition of HouseScene. Loading it
    // from Dispatchers.IO after rendering caused one frame at the default center position.
    private var savedPosition = HouseLayout.restored(positions.load())
    private var lastLaunchNanos = 0L
    private var onboardingStep = FirstRunOnboardingStep.COMPLETED
    private var onboardingGoal: SavingsGoalProgress? = null
    private var onboardingSuggestedGoalZoneId: String? = null

    init {
        require(minimumProductPriceRub > 0)
    }

    private companion object {
        const val ACHIEVEMENT_BANNER_DURATION_MS = 5_000L
        const val DEFAULT_SUGGESTED_GOAL_ZONE_ID = "fishing"
        const val ROOM_GOAL_ID_PREFIX = "room-zone:"
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
            RoomViewEvent.DishesClicked -> showParentHelp()
            is RoomViewEvent.ParentHelpOfferClicked -> requestParentHelp(viewEvent.offerId)
            RoomViewEvent.CloseParentHelpDialog -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(parentHelpDialog = null))
            }
            RoomViewEvent.CloseAllowanceNotice -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(allowanceNotice = null))
            }
            RoomViewEvent.CloseEarlyWeekParentHelpNotice -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(earlyWeekParentHelpNotice = null))
            }
            RoomViewEvent.FirstRunOnboardingContinue -> continueFirstRunOnboarding()
            RoomViewEvent.FirstRunMoneyNoticeClosed -> transitionOnboarding(
                FirstRunOnboardingStep.MONEY_EXPLANATION,
            )
            is RoomViewEvent.FirstRunDepositSelected -> selectFirstDeposit(viewEvent.depositNow)
            RoomViewEvent.Resumed -> refreshFirstRunOnboarding()
            RoomViewEvent.SavePlanClicked -> savePlan()
            RoomViewEvent.PlanTutorialNext -> advancePlanTutorial()
            RoomViewEvent.PlanDialogueFinished -> closePlanDialogue()
            RoomViewEvent.PlanDialogueEditRequested -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(planDialogue = null))
            }
            RoomViewEvent.AchievementsClicked -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(isAchievementsVisible = true))
            }
            RoomViewEvent.CloseAchievements -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(isAchievementsVisible = false))
            }
            RoomViewEvent.AchievementBannerDismissed -> dismissAchievementBanner()
            RoomViewEvent.ClosePlanSummary -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(isPlanSummaryVisible = false))
            }
            RoomViewEvent.CloseWeekResult -> closeWeekResult()
            is RoomViewEvent.PlanPercentChanged -> updatePlanPercent(viewEvent.category, viewEvent.percent)
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
            is RoomViewEvent.BuyConfirmed -> buy(viewEvent.zoneId)
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
            onboardingStep = onboardingRepository.load()
            onboardingSuggestedGoalZoneId = onboardingRepository.loadSuggestedGoalZoneId()
                ?.takeIf(FIRST_SAVINGS_GOAL_ZONE_IDS::contains)
            if (onboardingStep.needsGoalProgress()) {
                onboardingGoal = loadActiveSavingsGoal()
            }
            if (onboardingStep == FirstRunOnboardingStep.WISH) {
                persistOnboardingStep(FirstRunOnboardingStep.FIRST_MONEY)
            }
            if (onboardingStep.isLegacyRoomSavingsDialogue()) {
                persistOnboardingStep(
                    if (onboardingGoal == null) {
                        FirstRunOnboardingStep.PIGGY_TAP
                    } else {
                        FirstRunOnboardingStep.COMPLETED
                    },
                )
            }
            if (onboardingStep == FirstRunOnboardingStep.GAMES ||
                onboardingStep == FirstRunOnboardingStep.FINISH
            ) {
                persistOnboardingStep(FirstRunOnboardingStep.COMPLETED)
            }
            combine(
                observeZones(),
                weeklyPlanLearning.observeAchievements(),
            ) { roomData, achievements -> roomData to achievements }
                .collect { (roomData, achievements) ->
                    val current = nullableState<RoomViewState.Content>()
                    if (onboardingStep == FirstRunOnboardingStep.PLAN && roomData.progress.planProgress != null) {
                        persistOnboardingStep(FirstRunOnboardingStep.PLAN_SAVED)
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
                    if (onboardingStep == FirstRunOnboardingStep.PLAN && startsPlanning) {
                        weeklyPlanLearning.claimIntroduction(roomData.progress.weekNumber)
                    }
                    val tutorialStep = when {
                        onboardingStep == FirstRunOnboardingStep.PLAN && startsPlanning ->
                            PlanTutorialStep.INTRODUCTION
                        startsPlanning && weeklyPlanLearning.claimIntroduction(roomData.progress.weekNumber) ->
                            PlanTutorialStep.INTRODUCTION
                        editor == null -> null
                        else -> current?.planTutorialStep
                    }
                    updateState(
                        RoomViewState.Content(
                            zones = roomData.toRoomZones(),
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
                            achievements = achievements.map { achievement ->
                                PlanAchievementFeedback(
                                    id = achievement.id,
                                    title = achievement.title,
                                    description = achievement.description,
                                    isUnlocked = achievement.isUnlocked,
                                )
                            },
                            isAchievementsVisible = current?.isAchievementsVisible ?: false,
                            achievementBanner = current?.achievementBanner,
                            pendingAchievementBanners = current?.pendingAchievementBanners.orEmpty(),
                            parentHelpDialog = current?.parentHelpDialog,
                            isRequestingParentHelp = current?.isRequestingParentHelp ?: false,
                            allowanceNotice = current?.allowanceNotice,
                            earlyWeekParentHelpNotice = current?.earlyWeekParentHelpNotice,
                            onboarding = onboardingUiState(),
                        ),
                    )
                roomData.progress.planProgress?.let(::reconcilePlanLearning)
                handleLowBalance(roomData.progress)
            }
        }
    }

    private fun onboardingUiState(): FirstRunOnboardingState? {
        if (onboardingStep == FirstRunOnboardingStep.COMPLETED) return null
        return FirstRunOnboardingState(
            step = onboardingStep,
            suggestedGoalZoneId = onboardingSuggestedGoalZoneId,
            goalTitle = onboardingGoal?.goal?.title,
            goalTargetRub = onboardingGoal?.goal?.targetRub,
            goalSavedRub = onboardingGoal?.savedRub ?: 0,
        )
    }

    private fun persistOnboardingStep(step: FirstRunOnboardingStep) {
        onboardingStep = step
        onboardingRepository.save(step)
        if (step == FirstRunOnboardingStep.COMPLETED) {
            onboardingSuggestedGoalZoneId = null
            onboardingRepository.saveSuggestedGoalZoneId(null)
        }
    }

    private fun transitionOnboarding(step: FirstRunOnboardingStep) {
        persistOnboardingStep(step)
        nullableState<RoomViewState.Content>()?.let { content ->
            updateState(content.copy(onboarding = onboardingUiState()))
        }
        if (step == FirstRunOnboardingStep.COMPLETED) {
            promoteNextAchievementBanner()
        }
    }

    private fun continueFirstRunOnboarding() {
        when (onboardingStep) {
            FirstRunOnboardingStep.INTRODUCTION -> transitionOnboarding(FirstRunOnboardingStep.FIRST_MONEY)
            FirstRunOnboardingStep.WISH -> transitionOnboarding(FirstRunOnboardingStep.FIRST_MONEY)
            FirstRunOnboardingStep.MONEY_EXPLANATION ->
                transitionOnboarding(FirstRunOnboardingStep.PLAN_TRANSITION)
            FirstRunOnboardingStep.PLAN_TRANSITION -> startFirstPlan()
            FirstRunOnboardingStep.PLAN_SAVED -> transitionOnboarding(FirstRunOnboardingStep.GAME_DISCOVERY)
            FirstRunOnboardingStep.GAME_DISCOVERY ->
                transitionOnboarding(FirstRunOnboardingStep.GAME_SELECTION)
            FirstRunOnboardingStep.PIGGY_BANK -> transitionOnboarding(FirstRunOnboardingStep.PIGGY_TAP)
            FirstRunOnboardingStep.GOAL_CREATED -> transitionOnboarding(
                if ((onboardingGoal?.savedRub ?: 0) > 0) {
                    FirstRunOnboardingStep.DEPOSIT_DONE
                } else {
                    FirstRunOnboardingStep.FIRST_DEPOSIT
                },
            )
            FirstRunOnboardingStep.DEPOSIT_DONE,
            FirstRunOnboardingStep.DEPOSIT_SKIPPED,
            FirstRunOnboardingStep.GAMES,
            -> transitionOnboarding(FirstRunOnboardingStep.COMPLETED)
            FirstRunOnboardingStep.FINISH -> transitionOnboarding(FirstRunOnboardingStep.COMPLETED)
            FirstRunOnboardingStep.FIRST_MONEY,
            FirstRunOnboardingStep.PLAN,
            FirstRunOnboardingStep.GAME_SELECTION,
            FirstRunOnboardingStep.PIGGY_TAP,
            FirstRunOnboardingStep.WAITING_FOR_GOAL,
            FirstRunOnboardingStep.FIRST_DEPOSIT,
            FirstRunOnboardingStep.WAITING_FOR_DEPOSIT,
            FirstRunOnboardingStep.COMPLETED,
            -> Unit
        }
    }

    private fun startFirstPlan() {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.progress.planProgress != null) {
            transitionOnboarding(FirstRunOnboardingStep.PLAN_SAVED)
            return
        }
        persistOnboardingStep(FirstRunOnboardingStep.PLAN)
        updateState(content.copy(
            onboarding = onboardingUiState(),
            planEditor = content.planEditor ?: PlanEditorState(),
            planTutorialStep = PlanTutorialStep.INTRODUCTION,
        ))
        launchCoroutine { weeklyPlanLearning.claimIntroduction(content.progress.weekNumber) }
    }

    private fun openPiggyBank() {
        if (onboardingStep == FirstRunOnboardingStep.PIGGY_TAP) {
            transitionOnboarding(FirstRunOnboardingStep.WAITING_FOR_GOAL)
            val suggestedZoneId = onboardingSuggestedGoalZoneId ?: DEFAULT_SUGGESTED_GOAL_ZONE_ID
            openSavings(
                firstRunOnboarding = true,
                suggestedGoalId = "$ROOM_GOAL_ID_PREFIX$suggestedZoneId",
            )
            return
        }
        if (onboardingStep != FirstRunOnboardingStep.COMPLETED) return
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
                FirstRunOnboardingStep.WAITING_FOR_GOAL -> transitionOnboarding(
                    if (goal == null) FirstRunOnboardingStep.PIGGY_TAP
                    else FirstRunOnboardingStep.COMPLETED,
                )
                FirstRunOnboardingStep.WAITING_FOR_DEPOSIT -> transitionOnboarding(
                    if (goal != null && goal.savedRub > 0) FirstRunOnboardingStep.DEPOSIT_DONE
                    else FirstRunOnboardingStep.DEPOSIT_SKIPPED,
                )
                else -> Unit
            }
            onboardingRefreshJob = null
        }
    }

    private fun FirstRunOnboardingStep.needsGoalProgress(): Boolean = when (this) {
        FirstRunOnboardingStep.GOAL_CREATED,
        FirstRunOnboardingStep.FIRST_DEPOSIT,
        FirstRunOnboardingStep.WAITING_FOR_DEPOSIT,
        FirstRunOnboardingStep.DEPOSIT_DONE,
        FirstRunOnboardingStep.DEPOSIT_SKIPPED,
        FirstRunOnboardingStep.GAMES,
        FirstRunOnboardingStep.FINISH,
        -> true
        else -> false
    }

    private fun FirstRunOnboardingStep.isLegacyRoomSavingsDialogue(): Boolean = when (this) {
        FirstRunOnboardingStep.GOAL_CREATED,
        FirstRunOnboardingStep.FIRST_DEPOSIT,
        FirstRunOnboardingStep.WAITING_FOR_DEPOSIT,
        FirstRunOnboardingStep.DEPOSIT_DONE,
        FirstRunOnboardingStep.DEPOSIT_SKIPPED,
        -> true
        else -> false
    }

    private fun onZoneClicked(zoneId: String) {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.buyingZoneId != null) return
        val zone = content.zones.find { it.id == zoneId } ?: return
        if (onboardingStep == FirstRunOnboardingStep.GAME_SELECTION) {
            if (zoneId !in FIRST_SAVINGS_GOAL_ZONE_IDS || zone.access !is RoomZoneAccess.Buyable) return
            onboardingSuggestedGoalZoneId = zoneId
            onboardingRepository.saveSuggestedGoalZoneId(zoneId)
            transitionOnboarding(FirstRunOnboardingStep.PIGGY_BANK)
            return
        }
        if (onboardingStep != FirstRunOnboardingStep.COMPLETED) return
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

    private fun buy(zoneId: String) {
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
                when (val result = buyZone(zoneId)) {
                    ZoneBuyResult.Bought -> {
                        commands.onNext(RoomCommand.CloseBuyConfirmation(zoneId))
                        router.showBought()
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
        updateState(content.copy(savingGoalZoneId = zoneId))
        launchCoroutine(
            handleAction = ExceptionConsumer {
                nullableState<RoomViewState.Content>()?.let { updateState(it.copy(savingGoalZoneId = null)) }
                router.showBuyError()
                true
            },
        ) {
            try {
                saveZoneGoal(zoneId, title)
            } finally {
                nullableState<RoomViewState.Content>()?.let { updateState(it.copy(savingGoalZoneId = null)) }
            }
        }
    }

    private fun showSleepConfirmation() {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.sleeping || content.buyingZoneId != null) return
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
                if (content.progress.dayOfWeek == 7 && content.progress.planProgress != null) {
                    nullableState<RoomViewState.Content>()?.let { latest ->
                        updateState(latest.copy(
                            sleeping = false,
                            weekResult = content.progress.planProgress,
                        ))
                    }
                    return@launchCoroutine
                }
                advanceDay(expectedDay)
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
                advanceDay(expectedDay)
            } finally {
                nullableState<RoomViewState.Content>()?.let { updateState(it.copy(sleeping = false)) }
            }
        }
    }

    private suspend fun advanceDay(expectedDay: Long) {
        val result = endDay(expectedDay)
        if (result is EndDayResult.Advanced && result.allowanceGrossRub > 0) {
            nullableState<RoomViewState.Content>()?.let { latest ->
                updateState(latest.copy(allowanceNotice = AllowanceNoticeState(
                    grossRub = result.allowanceGrossRub,
                    parentHelpRepaidRub = result.parentHelpRepaidRub,
                    receivedRub = result.allowanceReceivedRub,
                )))
            }
        }
    }

    private fun updatePlanPercent(category: github.detrig.feature.planning.domain.PlanCategory, percent: Int) {
        val content = nullableState<RoomViewState.Content>() ?: return
        val editor = content.planEditor ?: return
        if (content.isSavingPlan || content.planTutorialStep != null) return
        updateState(content.copy(planEditor = editor.update(category, percent)))
    }

    private fun advancePlanTutorial() {
        val content = nullableState<RoomViewState.Content>() ?: return
        val step = content.planTutorialStep ?: return
        updateState(content.copy(planTutorialStep = step.nextOrNull()))
    }

    private fun closePlanDialogue() {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.planDialogue is PlanDialogueState.NeedsChanges) {
            updateState(content.copy(planDialogue = null))
            persistPlan(content.copy(planDialogue = null))
            return
        }
        updateState(content.copy(planDialogue = null))
        promoteNextAchievementBanner()
    }

    private fun promoteNextAchievementBanner() {
        val content = nullableState<RoomViewState.Content>() ?: return
        val nextBanner = content.achievementBanner ?: content.pendingAchievementBanners.firstOrNull()
        val promotedBanner = content.achievementBanner == null && nextBanner != null
        val pending = if (promotedBanner) {
            content.pendingAchievementBanners.drop(1)
        } else {
            content.pendingAchievementBanners
        }
        updateState(content.copy(
            achievementBanner = nextBanner,
            pendingAchievementBanners = pending,
        ))
        if (promotedBanner) scheduleAchievementBannerDismissal(nextBanner.id)
    }

    private fun dismissAchievementBanner() {
        val content = nullableState<RoomViewState.Content>() ?: return
        achievementBannerJob?.cancel()
        achievementBannerJob = null
        val nextBanner = content.pendingAchievementBanners.firstOrNull()
        updateState(content.copy(
            achievementBanner = nextBanner,
            pendingAchievementBanners = content.pendingAchievementBanners.drop(1),
        ))
        nextBanner?.let { scheduleAchievementBannerDismissal(it.id) }
    }

    private fun scheduleAchievementBannerDismissal(achievementId: String) {
        achievementBannerJob?.cancel()
        achievementBannerJob = launchCoroutine {
            delay(ACHIEVEMENT_BANNER_DURATION_MS)
            val content = nullableState<RoomViewState.Content>()
            if (content?.achievementBanner?.id == achievementId) {
                achievementBannerJob = null
                dismissAchievementBanner()
            }
        }
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
            ).takeIf { it.showSuccessExplanation }
            val unlocked = feedback.newlyUnlocked.map { unlock ->
                PlanAchievementFeedback(
                    id = unlock.definition.achievementId,
                    title = unlock.definition.childTitle,
                    description = unlock.definition.childDescription,
                )
            }
            nullableState<RoomViewState.Content>()?.let { latest ->
                val queuedBanners = latest.pendingAchievementBanners + unlocked
                val isFirstRunPlan = onboardingStep == FirstRunOnboardingStep.PLAN
                if (isFirstRunPlan) persistOnboardingStep(FirstRunOnboardingStep.PLAN_SAVED)
                val dialogue = regularDialogue.takeUnless { isFirstRunPlan }
                val visibleBanner = if (dialogue == null && latest.achievementBanner == null) {
                    queuedBanners.firstOrNull()
                } else {
                    latest.achievementBanner
                }
                val promotedBanner = latest.achievementBanner == null && visibleBanner != null
                updateState(latest.copy(
                    progress = latest.progress.copy(planProgress = plan, requiresPlan = false),
                    planEditor = null,
                    planTutorialStep = null,
                    planDialogue = dialogue,
                    onboarding = onboardingUiState(),
                    achievementBanner = visibleBanner,
                    pendingAchievementBanners = if (promotedBanner) {
                        queuedBanners.drop(1)
                    } else {
                        queuedBanners
                    },
                    isSavingPlan = false,
                ))
                if (promotedBanner) scheduleAchievementBannerDismissal(visibleBanner.id)
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

    private fun showParentHelp() {
        if (parentHelpJob?.isActive == true) return
        val content = nullableState<RoomViewState.Content>() ?: return
        parentHelpJob = launchCoroutine(
            handleAction = ExceptionConsumer { true },
        ) {
            val dialog = ParentHelpDialogState(
                offers = loadParentHelpInteractor.offers(),
                activeHelp = loadParentHelpInteractor(),
            )
            nullableState<RoomViewState.Content>()?.let { latest ->
                updateState(latest.copy(parentHelpDialog = dialog))
            }
        }
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
        if (progress.balanceRub >= minimumProductPriceRub || lowBalanceJob?.isActive == true) return
        lowBalanceJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                lowBalanceJob = null
                true
            },
        ) {
            val activeHelp = loadParentHelpInteractor()
            if (activeHelp == null) {
                nullableState<RoomViewState.Content>()?.let { latest ->
                    if (latest.progress.balanceRub < minimumProductPriceRub &&
                        latest.parentHelpDialog == null && latest.earlyWeekParentHelpNotice == null
                    ) {
                        updateState(latest.copy(parentHelpDialog = ParentHelpDialogState(
                            offers = loadParentHelpInteractor.offers(),
                            activeHelp = null,
                        )))
                    }
                }
            } else {
                when (val result = endWeekEarlyWithParentHelp(
                    expectedAbsoluteDay = progress.absoluteDay,
                    minimumProductPriceRub = minimumProductPriceRub,
                )) {
                    is EarlyWeekEndResult.Completed -> nullableState<RoomViewState.Content>()?.let { latest ->
                        updateState(latest.copy(
                            parentHelpDialog = null,
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
