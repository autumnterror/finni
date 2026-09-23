package github.detrig.feature.savings.di

import github.detrig.feature.savings.SavingsDependencies
import github.detrig.feature.savings.api.SavingsApi
import github.detrig.feature.savings.api.SavingsApiImpl
import github.detrig.feature.savings.domain.CreateSavingsGoalInteractor
import github.detrig.feature.savings.domain.TransferFromSavingsInteractor
import github.detrig.feature.savings.domain.TransferToSavingsInteractor
import github.detrig.feature.savings.navigation.SavingsRouterImpl
import github.detrig.feature.savings.presentation.SavingsViewModel

internal class SavingsModule(private val dependencies: SavingsDependencies) : SavingsComponent {
    private val router by lazy { SavingsRouterImpl(dependencies.globalNavigator()) }
    private val createGoal by lazy { CreateSavingsGoalInteractor(dependencies.economyApi()) }
    private val transferTo by lazy {
        TransferToSavingsInteractor(dependencies.economyApi(), dependencies.planningApi(), dependencies.weekApi())
    }
    private val transferFrom by lazy { TransferFromSavingsInteractor(dependencies.economyApi()) }

    override val api: SavingsApi by lazy {
        SavingsApiImpl(
            router,
            createGoal,
            dependencies.economyApi(),
            transferTo,
            transferFrom,
            dependencies.petApi(),
            dependencies.roomBackdrop(),
        )
    }
    override fun viewModel(firstRunOnboarding: Boolean, suggestedGoalId: String?) = SavingsViewModel(
        economy = dependencies.economyApi(),
        configuration = dependencies.configuration(),
        createGoal = createGoal,
        transferTo = transferTo,
        transferFrom = transferFrom,
        router = router,
        firstRunOnboarding = firstRunOnboarding,
        suggestedGoalId = suggestedGoalId,
    )
}
