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
import github.detrig.feature.room.domain.interactor.SaveZoneAsSavingsGoalInteractor
import github.detrig.feature.room.domain.interactor.LoadParentHelpInteractor
import github.detrig.feature.room.domain.interactor.RequestParentHelpInteractor
import github.detrig.feature.room.domain.interactor.ProvideZeroBalanceHelpInteractor
import github.detrig.feature.room.domain.interactor.ObserveRoomZonesInteractor
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.navigation.RoomRouter
import github.detrig.feature.room.presentation.mapper.toRoomZones
import github.detrig.feature.room.domain.model.HousePositionRepository
import github.detrig.feature.room.presentation.model.HouseLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import github.detrig.feature.planning.domain.PlanAssessment
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.economy.domain.ZeroBalanceHelpResult

internal class RoomViewModel(
    private val observeZones: ObserveRoomZonesInteractor,
    private val buyZone: BuyRoomZoneInteractor,
    private val endDay: EndDayInteractor,
    private val assessWeeklyPlan: AssessWeeklyPlanInteractor,
    private val saveWeeklyPlan: SaveWeeklyPlanInteractor,
    private val weeklyPlanLearning: WeeklyPlanLearningInteractor,
    private val openSavings: OpenSavingsInteractor,
    private val saveZoneGoal: SaveZoneAsSavingsGoalInteractor,
    private val loadParentHelpInteractor: LoadParentHelpInteractor,
    private val requestParentHelpInteractor: RequestParentHelpInteractor,
    private val provideZeroBalanceHelpInteractor: ProvideZeroBalanceHelpInteractor,
    private val router: RoomRouter,
    private val positions: HousePositionRepository,
) : CoreViewModel<RoomViewState, RoomViewEvent>(RoomViewState.Loading) {
    private var observationJob: Job? = null
    private var buyJob: Job? = null
    private var sleepJob: Job? = null
    private var savePlanJob: Job? = null
    private var parentHelpJob: Job? = null
    private var zeroBalanceHelpJob: Job? = null
    private var achievementBannerJob: Job? = null
    private val reconciledPlanWeeks = mutableSetOf<Long>()
    private var reconcilingPlanWeek: Long? = null
    private var savedPosition = HouseLayout.initialPosition()
    private var lastLaunchNanos = 0L

    private companion object {
        const val ACHIEVEMENT_BANNER_DURATION_MS = 5_000L
    }

    override fun perform(viewEvent: RoomViewEvent) {
        when (viewEvent) {
            RoomViewEvent.MarketClicked -> launchOnce { router.openMarket() }
            RoomViewEvent.BedClicked -> sleep()
            RoomViewEvent.CalendarClicked -> showPlanSummary()
            RoomViewEvent.PiggyBankClicked -> openSavings()
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
            RoomViewEvent.CloseZeroBalanceHelpNotice -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(zeroBalanceHelpNotice = null))
            }
            RoomViewEvent.SavePlanClicked -> savePlan()
            RoomViewEvent.PlanTutorialNext -> advancePlanTutorial()
            RoomViewEvent.PlanDialogueFinished -> closePlanDialogue()
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
            savedPosition = HouseLayout.restored(withContext(Dispatchers.IO) { positions.load() })
            combine(
                observeZones(),
                weeklyPlanLearning.observeAchievements(),
            ) { roomData, achievements -> roomData to achievements }
                .collect { (roomData, achievements) ->
                val current = nullableState<RoomViewState.Content>()
                val editor = when {
                    roomData.progress.planProgress != null -> null
                    current?.planEditor != null -> current.planEditor
                    roomData.progress.requiresPlan -> PlanEditorState()
                    else -> null
                }
                val startsPlanning = editor != null && current?.planEditor == null
                val tutorialStep = when {
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
                        sleeping = current?.sleeping ?: false,
                        planEditor = editor,
                        planTutorialStep = tutorialStep,
                        planDialogue = current?.planDialogue,
                        isSavingPlan = current?.isSavingPlan ?: false,
                        isPlanSummaryVisible = current?.isPlanSummaryVisible ?: false,
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
                        zeroBalanceHelpNotice = current?.zeroBalanceHelpNotice,
                    ),
                )
                roomData.progress.planProgress?.let(::reconcilePlanLearning)
                if (roomData.progress.balanceRub == 0) provideZeroBalanceHelp()
            }
        }
    }

    private fun onZoneClicked(zoneId: String) {
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.buyingZoneId != null) return
        val zone = content.zones.find { it.id == zoneId } ?: return
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

    private fun sleep() {
        if (sleepJob?.isActive == true) return
        val content = nullableState<RoomViewState.Content>() ?: return
        if (content.sleeping || content.buyingZoneId != null) return
        val expectedDay = content.progress.absoluteDay
        updateState(content.copy(sleeping = true))
        sleepJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                router.showSleepError()
                true
            },
        ) {
            try {
                delay(800)
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
            } finally {
                nullableState<RoomViewState.Content>()?.let { updateState(it.copy(sleeping = false)) }
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
        val nextBanner = content.achievementBanner ?: content.pendingAchievementBanners.firstOrNull()
        val promotedBanner = content.achievementBanner == null && nextBanner != null
        val pending = if (promotedBanner) {
            content.pendingAchievementBanners.drop(1)
        } else {
            content.pendingAchievementBanners
        }
        updateState(content.copy(
            planDialogue = null,
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
            val dialogue = PlanDialogueState.Saved(
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

    private fun provideZeroBalanceHelp() {
        if (zeroBalanceHelpJob?.isActive == true) return
        zeroBalanceHelpJob = launchCoroutine(
            handleAction = ExceptionConsumer { true },
        ) {
            when (val result = provideZeroBalanceHelpInteractor()) {
                is ZeroBalanceHelpResult.Granted -> nullableState<RoomViewState.Content>()?.let { latest ->
                    updateState(latest.copy(zeroBalanceHelpNotice = ZeroBalanceHelpNoticeState(result.amountRub)))
                }
                is ZeroBalanceHelpResult.NotNeeded -> Unit
            }
        }
    }
}
