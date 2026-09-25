package github.detrig.feature.room.di

import github.detrig.feature.room.RoomDependencies
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.room.api.RoomApiImpl
import github.detrig.feature.room.data.catalog.RoomZoneCatalog
import github.detrig.feature.room.data.repository.RoomRepositoryImpl
import github.detrig.feature.room.domain.interactor.BuyRoomZoneInteractor
import github.detrig.feature.room.domain.interactor.EndDayInteractor
import github.detrig.feature.room.domain.interactor.SaveWeeklyPlanInteractor
import github.detrig.feature.room.domain.interactor.AssessWeeklyPlanInteractor
import github.detrig.feature.room.domain.interactor.WeeklyPlanLearningInteractor
import github.detrig.feature.room.domain.interactor.ObserveRoomZonesInteractor
import github.detrig.feature.room.domain.interactor.ResolveRoomZoneAccessInteractor
import github.detrig.feature.room.domain.interactor.OpenSavingsInteractor
import github.detrig.feature.room.domain.interactor.LoadActiveSavingsGoalInteractor
import github.detrig.feature.room.domain.interactor.ReconcileSavingsLearningInteractor
import github.detrig.feature.room.domain.interactor.SaveZoneAsSavingsGoalInteractor
import github.detrig.feature.room.domain.interactor.LoadParentHelpInteractor
import github.detrig.feature.room.domain.interactor.RequestParentHelpInteractor
import github.detrig.feature.room.domain.interactor.EndWeekEarlyWithParentHelpInteractor
import github.detrig.feature.room.domain.interactor.LoadRoomImpulseWishInteractor
import github.detrig.feature.room.navigation.RoomRouterImpl
import github.detrig.feature.room.presentation.RoomViewModel

internal class RoomModule(private val dependencies: RoomDependencies) : RoomComponent {
    private val minimumProductPriceRub = dependencies.minimumProductPriceRub()
    private val positions by lazy { github.detrig.feature.room.data.local.HousePositionStorage(dependencies.housePreferences()) }
    private val onboarding by lazy {
        github.detrig.feature.room.data.local.FirstRunOnboardingStorage(dependencies.housePreferences())
    }
    private val previewRequests = github.detrig.feature.room.navigation.RoomPreviewRequests()
    override val api: RoomApi by lazy { RoomApiImpl(previewRequests, dependencies.resources()) }
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
    private val weeklyPlanLearning by lazy { WeeklyPlanLearningInteractor(dependencies.learningApi()) }
    private val assessWeeklyPlan by lazy { AssessWeeklyPlanInteractor(repository) }
    private val saveWeeklyPlan by lazy { SaveWeeklyPlanInteractor(repository, weeklyPlanLearning) }
    private val openSavings by lazy { OpenSavingsInteractor(dependencies.savingsApi()) }
    private val loadActiveSavingsGoal by lazy { LoadActiveSavingsGoalInteractor(dependencies.savingsApi()) }
    private val reconcileSavingsLearning by lazy { ReconcileSavingsLearningInteractor(dependencies.savingsApi()) }
    private val saveZoneAsGoal by lazy { SaveZoneAsSavingsGoalInteractor(repository, dependencies.savingsApi()) }
    private val loadParentHelp by lazy { LoadParentHelpInteractor(repository) }
    private val requestParentHelp by lazy { RequestParentHelpInteractor(repository) }
    private val endWeekEarlyWithParentHelp by lazy { EndWeekEarlyWithParentHelpInteractor(repository) }
    private val loadImpulseWish by lazy { LoadRoomImpulseWishInteractor(dependencies.impulseWishSource()) }

    override fun getRoomViewModel() = RoomViewModel(
        observeZones, buyZone, endDay, assessWeeklyPlan, saveWeeklyPlan, weeklyPlanLearning, openSavings, saveZoneAsGoal,
        loadActiveSavingsGoal, reconcileSavingsLearning, loadParentHelp, requestParentHelp,
        endWeekEarlyWithParentHelp, minimumProductPriceRub, loadImpulseWish, router, positions, onboarding,
        dependencies.gameAudio(),
    )
}
