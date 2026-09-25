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
import github.detrig.internetbooster.BuildConfig

internal class PhoneMediator(
    private val coreComponent: CoreComponent,
    private val roomMediator: RoomMediator,
    private val petMediator: PetMediator,
    private val shopMediator: ShopMediator,
    private val economyMediator: EconomyMediator,
    private val weekMediator: WeekMediator,
    private val learningMediator: LearningMediator,
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
                override fun economyApi() = economyMediator.getApi()
                override fun weekApi() = weekMediator.getApi()
                override fun learningApi() = learningMediator.getApi()
                override fun messagesStorage(): PhoneMessagesStorage = DurablePhoneMessagesStorage(
                    preferences = coreComponent.context.getSharedPreferences(
                        MESSAGES_PREFERENCES,
                        Context.MODE_PRIVATE,
                    ),
                )
                override fun dailySecurityEventProbability() = if (BuildConfig.DEBUG) {
                    DEBUG_SECURITY_EVENT_DAILY_PROBABILITY
                } else {
                    SECURITY_EVENT_DAILY_PROBABILITY
                }
                override fun minimumHelpBalanceRub() = shopMediator.minimumGroceryPriceRub()
                override fun globalMessageController() = coreComponent.globalMessageController
            }
        }
        PhoneFeature.getApi().initialize()
    }

    @MainThread
    override fun getApi(): PhoneApi = PhoneFeature.getApi()

    private companion object {
        const val MESSAGES_PREFERENCES = "phone_messages"

        // Отдельные точки настройки: в debug событие гарантировано для быстрой проверки.
        const val DEBUG_SECURITY_EVENT_DAILY_PROBABILITY = 1.0
        const val SECURITY_EVENT_DAILY_PROBABILITY = 0.45
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
