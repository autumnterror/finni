package github.detrig.feature.learning.di

import github.detrig.feature.learning.LearningDependencies
import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.learning.api.LearningApiImpl
import github.detrig.feature.learning.data.LearningRepositoryImpl
import github.detrig.feature.learning.domain.LearningRuleEngine
import github.detrig.feature.learning.domain.MvpAchievementCatalog

internal class LearningModule(
    private val dependencies: LearningDependencies,
) : LearningComponent {
    private val config by lazy { dependencies.config() }
    private val catalog by lazy { MvpAchievementCatalog.create(config) }
    private val ruleEngine by lazy { LearningRuleEngine(config.metricRules) }
    private val repository by lazy {
        LearningRepositoryImpl(
            dao = dependencies.learningDao(),
            transactionRunner = dependencies.transactionRunner(),
            config = config,
            catalog = catalog,
            ruleEngine = ruleEngine,
        )
    }

    override val api: LearningApi by lazy {
        LearningApiImpl(
            repository = repository,
            xpRewardGateway = dependencies.xpRewardGateway(),
        )
    }
}
