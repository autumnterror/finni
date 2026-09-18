package github.detrig.feature.room.di

import github.detrig.feature.room.RoomDependencies
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.room.api.RoomApiImpl
import github.detrig.feature.room.data.catalog.RoomZoneCatalog
import github.detrig.feature.room.data.repository.RoomRepositoryImpl
import github.detrig.feature.room.domain.interactor.BuyRoomZoneInteractor
import github.detrig.feature.room.domain.interactor.EndDayInteractor
import github.detrig.feature.room.domain.interactor.SaveWeeklyPlanInteractor
import github.detrig.feature.room.domain.interactor.ObserveRoomZonesInteractor
import github.detrig.feature.room.domain.interactor.ResolveRoomZoneAccessInteractor
import github.detrig.feature.room.navigation.RoomRouterImpl
import github.detrig.feature.room.presentation.RoomViewModel

internal class RoomModule(dependencies: RoomDependencies) : RoomComponent {
    private val positions by lazy { github.detrig.feature.room.data.local.HousePositionStorage(dependencies.housePreferences()) }
    private val previewRequests = github.detrig.feature.room.navigation.RoomPreviewRequests()
    override val api: RoomApi by lazy { RoomApiImpl(previewRequests) }
    private val repository by lazy {
        RoomRepositoryImpl(
            RoomZoneCatalog(), dependencies.gameStateApi(), dependencies.economyApi(), dependencies.weekApi(),
            dependencies.planningApi(),
        )
    }
    private val router by lazy {
        RoomRouterImpl(dependencies.gameLauncher(), dependencies.globalMessageController(), dependencies.resources(),
            dependencies.marketLauncher())
    }
    private val observeZones by lazy {
        ObserveRoomZonesInteractor(repository, ResolveRoomZoneAccessInteractor())
    }
    private val buyZone by lazy { BuyRoomZoneInteractor(repository) }
    private val endDay by lazy { EndDayInteractor(repository) }
    private val saveWeeklyPlan by lazy { SaveWeeklyPlanInteractor(repository) }

    override fun getRoomViewModel() = RoomViewModel(observeZones, buyZone, endDay, saveWeeklyPlan, router, positions)
}
