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
import github.detrig.internetbooster.mediators.ShopMediator
import github.detrig.internetbooster.mediators.PhoneMediator
import github.detrig.internetbooster.mediators.InventoryMediator
import github.detrig.internetbooster.mediators.FridgeMediator
import github.detrig.internetbooster.mediators.LearningMediator
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
    private val learningMediator: LearningMediator by lazy { LearningMediator(databaseModule) }
    private val inventoryMediator: InventoryMediator by lazy { InventoryMediator(coreComponent) }
    private val savingsMediator: SavingsMediator by lazy {
        SavingsMediator(
            core = coreComponent,
            economy = economyMediator,
            planning = planningMediator,
            week = weekMediator,
            pet = petMediator,
            roomApiProvider = { roomMediator.getApi() },
        )
    }
    private val shopMediator: ShopMediator by lazy {
        ShopMediator(
            coreComponent = coreComponent,
            economyMediator = economyMediator,
            weekMediator = weekMediator,
            inventoryApi = inventoryMediator.getApi(),
        )
    }

    private val gameStateMediator: GameStateMediator by lazy {
        GameStateMediator(databaseModule, economyMediator)
    }

    private val phoneMediator: PhoneMediator by lazy {
        PhoneMediator(
            coreComponent = coreComponent,
            roomMediator = roomMediator,
            petMediator = petMediator,
            shopMediator = shopMediator,
        )
    }

    private val fridgeMediator: FridgeMediator by lazy {
        FridgeMediator(
            coreComponent = coreComponent,
            roomMediator = roomMediator,
            petMediator = petMediator,
            inventoryMediator = inventoryMediator,
            gameStateMediator = gameStateMediator,
            shopMediator = shopMediator,
        )
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
            phoneMediator = phoneMediator,
            fridgeMediator = fridgeMediator,
        )
    }

    private val roomMediator: RoomMediator by lazy {
        RoomMediator(
            coreComponent,
            gameStateMediator,
            economyMediator,
            weekMediator,
            planningMediator,
            learningMediator,
            savingsMediator,
            shopMediator,
        )
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
        learningMediator.init()
        inventoryMediator.init()
        savingsMediator.init()
        shopMediator.init()
        gameStateMediator.init()
        petMediator.init()
        github.detrig.internetbooster.mediators.FlightMediator(
            coreComponent,
            gameStateMediator,
            petMediator,
        ).init()
        github.detrig.internetbooster.mediators.FishingMediator(
            coreComponent,
            databaseModule,
            gameStateMediator,
            petMediator,
        ).init()
        roomMediator.init()
        phoneMediator.init()
        fridgeMediator.init()
        gameSessionMediator.init()
    }
}
