package github.detrig.feature.savings.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.SavingsGoalProgress
import github.detrig.feature.savings.domain.CreateSavingsGoalInteractor
import github.detrig.feature.savings.domain.TransferFromSavingsInteractor
import github.detrig.feature.savings.domain.TransferToSavingsInteractor
import github.detrig.feature.savings.domain.SavingsLearningInteractor
import github.detrig.feature.savings.navigation.SavingsRoute
import github.detrig.feature.savings.navigation.SavingsRouter
import github.detrig.feature.savings.presentation.SavingsScreen
import github.detrig.feature.savings.SavingsRoomBackdrop
import github.detrig.feature.pet.api.PetApi

internal class SavingsApiImpl(
    private val router: SavingsRouter,
    private val createGoal: CreateSavingsGoalInteractor,
    private val economy: EconomyApi,
    private val transferTo: TransferToSavingsInteractor,
    private val transferFrom: TransferFromSavingsInteractor,
    private val learning: SavingsLearningInteractor,
    private val petApi: PetApi,
    private val roomBackdrop: SavingsRoomBackdrop,
) : SavingsApi {
    override fun open(firstRunOnboarding: Boolean, suggestedGoalId: String?) =
        router.open(firstRunOnboarding, suggestedGoalId)

    override fun entries(): EntryHostProviderInstaller = {
        composable<SavingsRoute.Home> { route ->
            SavingsScreen(
                firstRunOnboarding = route.firstRunOnboarding,
                suggestedGoalId = route.suggestedGoalId,
                petApi = petApi,
                roomBackdrop = roomBackdrop,
            )
        }
    }
    override suspend fun createGoal(draft: SavingsGoalDraft): SavingsGoal = createGoal(draft)
    override suspend fun getActiveGoalProgress(): SavingsGoalProgress? =
        economy.getActiveGoal()?.let { economy.getGoalProgress(it.id) }
    override suspend fun reconcileLearning() = learning.reconcile()

    override suspend fun transferToActiveGoal(operationId: String, amountRub: Long): SavingsTransferResult {
        val goal = economy.getActiveGoal() ?: return SavingsTransferResult.NoActiveGoal
        return SavingsTransferResult.Completed(transferTo(operationId, goal.id, amountRub))
    }

    override suspend fun transferFromActiveGoal(operationId: String, amountRub: Long): SavingsTransferResult {
        val goal = economy.getActiveGoal() ?: return SavingsTransferResult.NoActiveGoal
        return SavingsTransferResult.Completed(transferFrom(operationId, goal.id, amountRub))
    }
}
