package github.detrig.internetbooster.mediators

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.MainThread
import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.phone.PhoneDependencies
import github.detrig.feature.phone.PhoneFeature
import github.detrig.feature.phone.api.PhoneApi
import github.detrig.feature.phone.api.PhoneMessagesStorage
import github.detrig.internetbooster.startup.AppExperienceModeStorage

internal class PhoneMediator(
    private val coreComponent: CoreComponent,
    private val roomMediator: RoomMediator,
    private val petMediator: PetMediator,
    private val shopMediator: ShopMediator,
    private val wardrobeMediator: WardrobeMediator,
    private val economyMediator: EconomyMediator,
    private val weekMediator: WeekMediator,
    private val learningMediator: LearningMediator,
    private val gameStateMediator: GameStateMediator,
    private val inventoryMediator: InventoryMediator,
    private val resetDemoProgress: suspend (skipOnboarding: Boolean) -> Unit,
    private val experienceModeStorage: AppExperienceModeStorage,
    private val dailyRoomEvents: DailyRoomEventSchedule,
) : Mediator<PhoneApi> {

    @MainThread
    fun init() {
        PhoneFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : PhoneDependencies {
                override fun globalNavigator() = coreComponent.globalNavigator
                override fun applicationScope() = coreComponent.applicationScope
                override fun roomApi() = roomMediator.getApi()
                override fun petApi() = petMediator.getApi()
                override fun shopApi() = shopMediator.getApi()
                override fun wardrobeApi() = wardrobeMediator.getApi()
                override fun economyApi() = economyMediator.getApi()
                override fun weekApi() = weekMediator.getApi()
                override fun learningApi() = learningMediator.getApi()
                override fun progressionApi() = gameStateMediator.getProgressionApi()
                override fun gameStateApi() = gameStateMediator.getApi()
                override fun inventoryApi() = inventoryMediator.getApi()
                override suspend fun resetDemoProgress(skipOnboarding: Boolean) =
                    this@PhoneMediator.resetDemoProgress(skipOnboarding)
                override fun isDemoMode() = experienceModeStorage.isDemoMode()
                override fun messagesStorage(): PhoneMessagesStorage = DurablePhoneMessagesStorage(
                    preferences = coreComponent.context.getSharedPreferences(
                        MESSAGES_PREFERENCES,
                        Context.MODE_PRIVATE,
                    ),
                )
                override fun isSecurityEventScheduled(absoluteDay: Long) =
                    dailyRoomEvents.eventForDay(absoluteDay) == DailyRoomEventSchedule.Kind.SECURITY
                override fun minimumDaysBetweenSecurityEvents() =
                    DailyRoomEventSchedule.MINIMUM_SECURITY_INTERVAL_DAYS
                override fun minimumHelpBalanceRub() = shopMediator.minimumGroceryPriceRub()
                override fun globalMessageController() = coreComponent.globalMessageController
                override suspend fun createRandomWishForDebug() =
                    shopMediator.createRandomWishForDebug()
            }
        }
        PhoneFeature.getApi().initialize()
    }

    @MainThread
    override fun getApi(): PhoneApi = PhoneFeature.getApi()

    private companion object {
        const val MESSAGES_PREFERENCES = "phone_messages"

    }

    private class DurablePhoneMessagesStorage(
        private val preferences: SharedPreferences,
    ) : SharedStorage(preferences), PhoneMessagesStorage {
        override fun readPayload(): String = readString(STATE_KEY, defaultValue = "")

        override fun writePayload(payload: String) {
            check(preferences.edit().putString(STATE_KEY, payload).commit()) {
                "Failed to persist phone messages"
            }
        }

        private companion object {
            const val STATE_KEY = "messages_state_v1"
        }
    }
}
