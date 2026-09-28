package github.detrig.internetbooster.di

import github.detrig.core.di.CoreComponent
import github.detrig.core.audio.AndroidGameAudio
import github.detrig.core.audio.GameAudio
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
import github.detrig.internetbooster.mediators.DailyRoomEventSchedule
import github.detrig.internetbooster.mediators.InventoryMediator
import github.detrig.internetbooster.mediators.FridgeMediator
import github.detrig.internetbooster.mediators.WardrobeMediator
import github.detrig.internetbooster.mediators.LearningMediator
import github.detrig.internetbooster.mediators.LearningTestsMediator
import github.detrig.internetbooster.network.AppNetworkModule
import github.detrig.internetbooster.time.HungerNotificationDispatcher
import github.detrig.internetbooster.startup.AppExperienceMode
import github.detrig.internetbooster.startup.AppExperienceModeStorage
import kotlinx.coroutines.flow.StateFlow
import github.detrig.internetbooster.audio.AppAudioCues
import github.detrig.internetbooster.mediators.FlightMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal interface AppModule {

    val gameAudio: GameAudio
    val selectedExperienceMode: StateFlow<AppExperienceMode?>
    fun initFeatures()
    fun selectExperienceMode(mode: AppExperienceMode)
    suspend fun reconcileTimedEvents()
    suspend fun hasPetProfile(): Boolean
    suspend fun resetDemoProgress(skipOnboarding: Boolean)
}

internal class AppModuleImpl(
    private val coreComponent: CoreComponent,
) : AppModule {

    override val gameAudio: GameAudio by lazy { AndroidGameAudio(coreComponent.context) }

    private val dailyRoomEvents by lazy {
        DailyRoomEventSchedule(coreComponent.context.getSharedPreferences(
            DailyRoomEventSchedule.PREFERENCES_NAME, android.content.Context.MODE_PRIVATE,
        ))
    }

    private val databaseModule: AppDatabaseModule by lazy {
        AppDatabaseModule(coreComponent.context)
    }

    private val miniGamesCommonDatabaseModule: MiniGamesCommonDatabaseModule by lazy {
        MiniGamesCommonDatabaseModule(coreComponent.context)
    }
    private val experienceModeStorage by lazy { AppExperienceModeStorage(coreComponent.context) }

    override val selectedExperienceMode: StateFlow<AppExperienceMode?>
        get() = experienceModeStorage.selectedMode

    override fun selectExperienceMode(mode: AppExperienceMode) = experienceModeStorage.select(mode)

    private val networkModule: AppNetworkModule by lazy {
        AppNetworkModule()
    }

    private val economyMediator: EconomyMediator by lazy { EconomyMediator(databaseModule) }
    private val weekMediator: WeekMediator by lazy {
        WeekMediator(databaseModule, economyMediator, gameStateMediator, planningMediator, learningMediator)
    }
    private val planningMediator: PlanningMediator by lazy { PlanningMediator(databaseModule) }
    private val learningMediator: LearningMediator by lazy {
        LearningMediator(databaseModule, gameStateMediator)
    }
    private val learningTestsMediator: LearningTestsMediator by lazy {
        LearningTestsMediator(
            coreComponent = coreComponent,
            databaseModule = databaseModule,
            economyMediator = economyMediator,
            weekMediator = weekMediator,
            gameStateMediator = gameStateMediator,
            petMediator = petMediator,
        )
    }
    private val inventoryMediator: InventoryMediator by lazy { InventoryMediator(coreComponent) }
    private val savingsMediator: SavingsMediator by lazy {
        SavingsMediator(
            core = coreComponent,
            economy = economyMediator,
            planning = planningMediator,
            week = weekMediator,
            pet = petMediator,
            roomApiProvider = { roomMediator.getApi() },
            learning = learningMediator,
            gameState = gameStateMediator,
            gameAudio = gameAudio,
        )
    }
    private val shopMediator: ShopMediator by lazy {
        ShopMediator(
            coreComponent = coreComponent,
            economyMediator = economyMediator,
            weekMediator = weekMediator,
            planningMediator = planningMediator,
            inventoryApi = inventoryMediator.getApi(),
            learningMediator = learningMediator,
            petMediator = petMediator,
            gameStateMediator = gameStateMediator,
            savingsMediator = savingsMediator,
            gameAudio = gameAudio,
            interiorWishCandidates = { roomMediator.getApi().interiorWishCandidates() },
            dailyRoomEvents = dailyRoomEvents,
        )
    }

    private val gameStateMediator: GameStateMediator by lazy {
        GameStateMediator(databaseModule, economyMediator, coreComponent.context)
    }

    private val hungerNotifications: HungerNotificationDispatcher by lazy {
        HungerNotificationDispatcher(coreComponent.context, gameStateMediator.getApi())
    }

    override suspend fun reconcileTimedEvents() {
        learningMediator.getApi().deliverPendingXpRewards("current")
        hungerNotifications.dispatch()
    }

    override suspend fun hasPetProfile(): Boolean = petMediator.getApi().observeProfile().first() != null

    override suspend fun resetDemoProgress(skipOnboarding: Boolean) {
        phoneMediator.getApi().resetProgress {
            dailyRoomEvents.resetProgress()
            withContext(Dispatchers.IO) {
                databaseModule.database.clearAllTables()
                flightMediator.resetProgress()
            }
            inventoryMediator.getApi().resetProgress()
            roomMediator.getApi().resetProgress(skipOnboarding)
        }
        petMediator.getApi().resetProfile()
        experienceModeStorage.clearSelection()
    }

    private val phoneMediator: PhoneMediator by lazy {
        PhoneMediator(
            coreComponent = coreComponent,
            roomMediator = roomMediator,
            petMediator = petMediator,
            shopMediator = shopMediator,
            wardrobeMediator = wardrobeMediator,
            economyMediator = economyMediator,
            weekMediator = weekMediator,
            learningMediator = learningMediator,
            gameStateMediator = gameStateMediator,
            inventoryMediator = inventoryMediator,
            resetDemoProgress = ::resetDemoProgress,
            experienceModeStorage = experienceModeStorage,
            dailyRoomEvents = dailyRoomEvents,
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
            gameAudio = gameAudio,
        )
    }

    private val petMediator: PetMediator by lazy {
        PetMediator(coreComponent, gameStateMediator)
    }
    private val wardrobeMediator: WardrobeMediator by lazy {
        WardrobeMediator(coreComponent, petMediator, economyMediator, planningMediator, weekMediator, gameStateMediator, shopMediator)
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
            petMediator,
            wardrobeMediator,
            inventoryMediator,
            gameAudio,
            learningTestsMediator,
            ::resetDemoProgress,
        )
    }

    private val miniGamesCommonMediator: MiniGamesCommonMediator by lazy {
        MiniGamesCommonMediator(
            networkModule = networkModule,
            databaseModule = miniGamesCommonDatabaseModule,
        )
    }

    private val flightMediator: FlightMediator by lazy {
        FlightMediator(
            coreComponent,
            gameStateMediator,
            petMediator,
            gameAudio,
        )
    }

    override fun initFeatures() {
        gameAudio.preload(listOf(AppAudioCues.Purchase))
        miniGamesCommonMediator.init()
        economyMediator.init()
        gameStateMediator.init()
        planningMediator.init()
        learningMediator.init()
        weekMediator.init()
        learningTestsMediator.init()
        inventoryMediator.init()
        savingsMediator.init()
        shopMediator.init()
        petMediator.init()
        wardrobeMediator.init()
        flightMediator.init()
        github.detrig.internetbooster.mediators.FishingMediator(
            coreComponent,
            databaseModule,
            gameStateMediator,
            weekMediator,
            petMediator,
            gameAudio,
        ).init()
        roomMediator.init()
        phoneMediator.init()
        fridgeMediator.init()
        gameSessionMediator.init()
    }
}
