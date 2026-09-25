package github.detrig.feature.savings.di

import github.detrig.feature.savings.SavingsDependencies
import github.detrig.feature.savings.api.SavingsApi
import github.detrig.feature.savings.api.SavingsApiImpl
import github.detrig.feature.savings.domain.CreateSavingsGoalInteractor
import github.detrig.feature.savings.domain.TransferFromSavingsInteractor
import github.detrig.feature.savings.domain.TransferToSavingsInteractor
import github.detrig.feature.savings.domain.SavingsLearningInteractor
import github.detrig.feature.savings.domain.PurchaseSavingsGoalInteractor
import github.detrig.feature.savings.navigation.SavingsRouterImpl
import github.detrig.feature.savings.presentation.SavingsViewModel

internal class SavingsModule(private val dependencies: SavingsDependencies) : SavingsComponent {
    private val router by lazy { SavingsRouterImpl(dependencies.globalNavigator()) }
    private val learning by lazy {
        SavingsLearningInteractor(
            dependencies.economyApi(),
            dependencies.weekApi(),
            dependencies.learningApi(),
            dependencies.progressionApi(),
        )
    }
    private val createGoal by lazy { CreateSavingsGoalInteractor(dependencies.economyApi(), learning) }
    private val transferTo by lazy {
        TransferToSavingsInteractor(dependencies.economyApi(), dependencies.planningApi(), learning)
    }
    private val transferFrom by lazy {
        TransferFromSavingsInteractor(dependencies.economyApi(), dependencies.planningApi(), learning)
    }
    private val purchaseGoal by lazy {
        PurchaseSavingsGoalInteractor(
            dependencies.economyApi(),
            dependencies.goalPurchaser(),
            dependencies.transactionRunner(),
        )
    }

    override val api: SavingsApi by lazy {
        SavingsApiImpl(
            router,
            createGoal,
            dependencies.economyApi(),
            transferTo,
            transferFrom,
            learning,
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
        purchaseGoalInteractor = purchaseGoal,
        learning = learning,
        router = router,
        firstRunOnboarding = firstRunOnboarding,
        suggestedGoalId = suggestedGoalId,
        gameAudio = dependencies.gameAudio(),
    )
}
