package github.detrig.feature.savings.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.economy.domain.SavingsGoalProgress

interface SavingsApi {
    fun open(firstRunOnboarding: Boolean = false, suggestedGoalId: String? = null)
    fun entries(): EntryHostProviderInstaller
    suspend fun createGoal(draft: SavingsGoalDraft): SavingsGoal
    suspend fun getActiveGoalProgress(): SavingsGoalProgress?
    suspend fun reconcileLearning()
    suspend fun transferToActiveGoal(operationId: String, amountRub: Long): SavingsTransferResult
    suspend fun transferFromActiveGoal(operationId: String, amountRub: Long): SavingsTransferResult
}

sealed interface SavingsTransferResult {
    data object NoActiveGoal : SavingsTransferResult
    data class Completed(val financial: FinancialOperationResult) : SavingsTransferResult
}

data class SavingsGoalDraft(
    val id: String,
    val title: String,
    val targetRub: Long,
    val metadata: String? = null,
) {
    init {
        require(id.isNotBlank())
        require(title.isNotBlank())
        require(targetRub > 0)
    }
}
