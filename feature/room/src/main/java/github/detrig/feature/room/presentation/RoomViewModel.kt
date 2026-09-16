package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.gamestate.domain.model.ZoneBuyResult
import github.detrig.feature.room.domain.interactor.BuyRoomZoneInteractor
import github.detrig.feature.room.domain.interactor.ObserveRoomZonesInteractor
import github.detrig.feature.room.domain.model.RoomZoneAccess
import github.detrig.feature.room.navigation.RoomRouter
import github.detrig.feature.room.presentation.mapper.toRoomZones
import github.detrig.feature.room.domain.model.HousePositionRepository
import github.detrig.feature.room.presentation.model.HouseLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job

internal class RoomViewModel(
    private val observeZones: ObserveRoomZonesInteractor,
    private val buyZone: BuyRoomZoneInteractor,
    private val router: RoomRouter,
    private val positions: HousePositionRepository,
) : CoreViewModel<RoomViewState, RoomViewEvent>(RoomViewState.Loading) {
    private var observationJob: Job? = null
    private var buyJob: Job? = null
    private var savedPosition = HouseLayout.initialPosition()
    private var lastLaunchNanos = 0L

    override fun perform(viewEvent: RoomViewEvent) {
        when (viewEvent) {
            RoomViewEvent.MarketClicked -> launchOnce { router.openMarket() }
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
                updateState(
                    RoomViewState.Content(
                        zones = roomData.toRoomZones(),
                        initialPosition = savedPosition,
                        progress = roomData.progress,
                        buyingZoneId = nullableState<RoomViewState.Content>()?.buyingZoneId,
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
}
