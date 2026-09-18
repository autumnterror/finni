package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.room.domain.interactor.BuyRoomZoneInteractor
import github.detrig.feature.room.domain.interactor.EndDayInteractor
import github.detrig.feature.room.domain.interactor.SaveWeeklyPlanInteractor
import github.detrig.feature.room.domain.interactor.OpenSavingsInteractor
import github.detrig.feature.room.domain.interactor.SaveZoneAsSavingsGoalInteractor
import github.detrig.feature.room.domain.interactor.LoadParentHelpInteractor
import github.detrig.feature.room.domain.interactor.RequestParentHelpInteractor
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
import github.detrig.feature.planning.domain.SavePlanResult
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.week.domain.EndDayResult

internal class RoomViewModel(
    private val observeZones: ObserveRoomZonesInteractor,
    private val buyZone: BuyRoomZoneInteractor,
    private val endDay: EndDayInteractor,
    private val saveWeeklyPlan: SaveWeeklyPlanInteractor,
    private val openSavings: OpenSavingsInteractor,
    private val saveZoneGoal: SaveZoneAsSavingsGoalInteractor,
    private val loadParentHelpInteractor: LoadParentHelpInteractor,
    private val requestParentHelpInteractor: RequestParentHelpInteractor,
    private val router: RoomRouter,
    private val positions: HousePositionRepository,
) : CoreViewModel<RoomViewState, RoomViewEvent>(RoomViewState.Loading) {
    private var observationJob: Job? = null
    private var buyJob: Job? = null
    private var sleepJob: Job? = null
    private var savePlanJob: Job? = null
    private var parentHelpJob: Job? = null
    private var savedPosition = HouseLayout.initialPosition()
    private var lastLaunchNanos = 0L

    override fun perform(viewEvent: RoomViewEvent) {
        when (viewEvent) {
            RoomViewEvent.MarketClicked -> launchOnce { router.openMarket() }
            RoomViewEvent.BedClicked -> sleep()
            RoomViewEvent.CalendarClicked -> showPlanSummary()
            RoomViewEvent.PiggyBankClicked -> openSavings()
            RoomViewEvent.ParentHelpBoardClicked -> showParentHelp()
            is RoomViewEvent.ParentHelpOfferClicked -> requestParentHelp(viewEvent.offerId)
            RoomViewEvent.CloseParentHelpDialog -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(parentHelpDialog = null))
            }
            RoomViewEvent.CloseAllowanceNotice -> nullableState<RoomViewState.Content>()?.let {
                updateState(it.copy(allowanceNotice = null))
            }
            RoomViewEvent.SavePlanClicked -> savePlan()
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
            observeZones().collect { roomData ->
                val current = nullableState<RoomViewState.Content>()
                val editor = when {
                    roomData.progress.planProgress != null -> null
                    current?.planEditor != null -> current.planEditor
                    roomData.progress.requiresPlan -> PlanEditorState()
                    else -> null
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
                        isSavingPlan = current?.isSavingPlan ?: false,
                        isPlanSummaryVisible = current?.isPlanSummaryVisible ?: false,
                        parentHelpDialog = current?.parentHelpDialog,
                        isRequestingParentHelp = current?.isRequestingParentHelp ?: false,
                        allowanceNotice = current?.allowanceNotice,
                    ),
                )
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
        if (content.isSavingPlan) return
        updateState(content.copy(planEditor = editor.update(category, percent)))
    }

    private fun savePlan() {
        if (savePlanJob?.isActive == true) return
        val content = nullableState<RoomViewState.Content>() ?: return
        val editor = content.planEditor ?: return
        if (editor.total != 100) return
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
            val result = saveWeeklyPlan(
                weekNumber = content.progress.weekNumber,
                availableRub = content.progress.balanceRub.toLong(),
                percentages = editor.toPercentages(),
            )
            val plan = when (result) {
                is SavePlanResult.Saved -> result.progress
                is SavePlanResult.AlreadySaved -> result.progress
            }
            nullableState<RoomViewState.Content>()?.let { latest ->
                updateState(latest.copy(
                    progress = latest.progress.copy(planProgress = plan, requiresPlan = false),
                    planEditor = null,
                    isSavingPlan = false,
                ))
            }
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
}
