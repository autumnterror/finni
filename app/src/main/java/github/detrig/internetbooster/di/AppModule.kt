package github.detrig.internetbooster.di

import github.detrig.core.di.CoreComponent
import github.detrig.internetbooster.database.AppDatabaseModule
import github.detrig.internetbooster.database.MiniGamesCommonDatabaseModule
import github.detrig.internetbooster.mediators.GameSessionMediator
import github.detrig.internetbooster.mediators.EconomyMediator
import github.detrig.internetbooster.mediators.GameStateMediator
import github.detrig.internetbooster.mediators.MiniGamesCommonMediator
import github.detrig.internetbooster.mediators.PetMediator
import github.detrig.internetbooster.mediators.RoomMediator
import github.detrig.internetbooster.mediators.PlanningMediator
import github.detrig.internetbooster.mediators.WeekMediator
import github.detrig.internetbooster.mediators.SavingsMediator
import github.detrig.internetbooster.network.AppNetworkModule

internal interface AppModule {

    fun initFeatures()
}

internal class AppModuleImpl(
    private val coreComponent: CoreComponent,
) : AppModule {

    private val databaseModule: AppDatabaseModule by lazy {
        AppDatabaseModule(coreComponent.context)
    }

    private val miniGamesCommonDatabaseModule: MiniGamesCommonDatabaseModule by lazy {
        MiniGamesCommonDatabaseModule(coreComponent.context)
    }

    private val networkModule: AppNetworkModule by lazy {
        AppNetworkModule()
    }

    private val economyMediator: EconomyMediator by lazy { EconomyMediator(databaseModule) }
    private val weekMediator: WeekMediator by lazy { WeekMediator(databaseModule, economyMediator) }
    private val planningMediator: PlanningMediator by lazy { PlanningMediator(databaseModule) }
    private val savingsMediator: SavingsMediator by lazy {
        SavingsMediator(coreComponent, economyMediator, planningMediator, weekMediator)
    }

    private val gameStateMediator: GameStateMediator by lazy {
        GameStateMediator(databaseModule, economyMediator)
    }

    private val petMediator: PetMediator by lazy {
        PetMediator(coreComponent)
    }

    private val gameSessionMediator: GameSessionMediator by lazy {
        GameSessionMediator(
            coreComponent = coreComponent,
            gameStateMediator = gameStateMediator,
            economyMediator = economyMediator,
            roomMediator = roomMediator,
            petMediator = petMediator,
        )
    }

    private val roomMediator: RoomMediator by lazy {
        RoomMediator(coreComponent, gameStateMediator, economyMediator, weekMediator, planningMediator, savingsMediator)
    }

    private val miniGamesCommonMediator: MiniGamesCommonMediator by lazy {
        MiniGamesCommonMediator(
            networkModule = networkModule,
            databaseModule = miniGamesCommonDatabaseModule,
        )
    }

    override fun initFeatures() {
        miniGamesCommonMediator.init()
        economyMediator.init()
        weekMediator.init()
        planningMediator.init()
        savingsMediator.init()
        gameStateMediator.init()
        petMediator.init()
        github.detrig.internetbooster.mediators.FlightMediator(
            coreComponent,
            gameStateMediator,
            petMediator,
        ).init()
        github.detrig.internetbooster.mediators.ProductMarketMediator(
            coreComponent,
            github.detrig.internetbooster.database.ProductMarketDatabaseModule(coreComponent.context),
            economyMediator,
            weekMediator,
            planningMediator,
            savingsMediator,
        ).init()
        github.detrig.internetbooster.mediators.FishingMediator(
            coreComponent,
            databaseModule,
            gameStateMediator,
            petMediator,
        ).init()
        roomMediator.init()
        gameSessionMediator.init()
    }
}
