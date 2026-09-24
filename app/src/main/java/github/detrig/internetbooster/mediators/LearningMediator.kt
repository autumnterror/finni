package github.detrig.internetbooster.mediators

import github.detrig.core.Mediator
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.learning.LearningDependencies
import github.detrig.feature.learning.LearningFeature
import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.learning.api.LearningXpRewardGateway
import github.detrig.feature.learning.api.XpGrantResult
import github.detrig.feature.learning.data.local.LearningDao
import github.detrig.feature.learning.domain.LearningConfig
import github.detrig.feature.learning.domain.BudgetPlanningLearning
import github.detrig.feature.learning.domain.SavingsLearning
import github.detrig.feature.learning.domain.PurchaseLearning
import github.detrig.feature.learning.domain.FinancialSecurityLearning
import github.detrig.internetbooster.database.AppDatabaseModule

internal class LearningMediator(
    private val databaseModule: AppDatabaseModule,
) : Mediator<LearningApi> {
    fun init() {
        LearningFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : LearningDependencies {
                override fun learningDao(): LearningDao = databaseModule.learningDao
                override fun transactionRunner(): RoomTransactionRunner = databaseModule.transactionRunner

                override fun config(): LearningConfig = LearningConfig(
                    metricRules = listOf(BudgetPlanningLearning.reasonablePlanRule()) +
                        SavingsLearning.rules() +
                        PurchaseLearning.rules() +
                        FinancialSecurityLearning.rules(),
                )

                // XP остаётся в outbox до появления ProgressionApi.
                override fun xpRewardGateway(): LearningXpRewardGateway = DeferredXpRewardGateway
            }
        }
    }

    override fun getApi(): LearningApi = LearningFeature.getApi()

    private object DeferredXpRewardGateway : LearningXpRewardGateway {
        override suspend fun grantXp(
            grantId: String,
            profileId: String,
            amount: Int,
        ): XpGrantResult = XpGrantResult.RetryLater
    }
}
