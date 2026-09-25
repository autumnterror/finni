package github.detrig.feature.phone.di

import github.detrig.feature.phone.PhoneDependencies
import github.detrig.feature.phone.api.PhoneApi
import github.detrig.feature.phone.api.PhoneApiImpl
import github.detrig.feature.phone.navigation.PhoneRouter
import github.detrig.feature.phone.navigation.PhoneRouterImpl
import github.detrig.feature.phone.presentation.DebugMenuViewModel
import github.detrig.feature.phone.data.PersistentMessagesRepository
import github.detrig.feature.phone.data.SharedPreferencesMessagesStore
import github.detrig.feature.phone.domain.MessagesCoordinator
import github.detrig.feature.phone.domain.SecurityEventConfig
import github.detrig.feature.phone.presentation.MessagesViewModel
import github.detrig.feature.phone.presentation.RoomNotificationsViewModel

internal class PhoneModule(
    dependencies: PhoneDependencies,
) : PhoneComponent {
    override val roomApi = dependencies.roomApi()
    override val petApi = dependencies.petApi()
    override val shopApi = dependencies.shopApi()
    private val weekApi = dependencies.weekApi()
    private val economyApi = dependencies.economyApi()
    private val messagesRepository by lazy {
        PersistentMessagesRepository(
            SharedPreferencesMessagesStore(dependencies.messagesStorage()),
        )
    }
    private val messagesCoordinator by lazy {
        MessagesCoordinator(
            repository = messagesRepository,
            weekApi = weekApi,
            learningApi = dependencies.learningApi(),
            economyApi = dependencies.economyApi(),
            minimumHelpBalanceRub = dependencies.minimumHelpBalanceRub(),
            eventConfig = SecurityEventConfig(
                dailyProbability = dependencies.dailySecurityEventProbability(),
            ),
        )
    }

    override val router: PhoneRouter by lazy {
        PhoneRouterImpl(dependencies.globalNavigator())
    }

    override val api: PhoneApi by lazy {
        PhoneApiImpl(
            router = router,
            coordinator = messagesCoordinator,
            applicationScope = dependencies.applicationScope(),
        )
    }

    override fun debugMenuViewModel() = DebugMenuViewModel(economyApi, weekApi)

    override fun messagesViewModel() = MessagesViewModel(
        repository = messagesRepository,
        coordinator = messagesCoordinator,
    )

    override fun roomNotificationsViewModel() = RoomNotificationsViewModel(
        repository = messagesRepository,
        coordinator = messagesCoordinator,
    )
}
