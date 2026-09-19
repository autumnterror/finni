package github.detrig.feature.learning

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.learning.api.LearningXpRewardGateway
import github.detrig.feature.learning.data.local.LearningDao
import github.detrig.feature.learning.domain.LearningConfig

interface LearningDependencies {
    fun learningDao(): LearningDao
    fun transactionRunner(): RoomTransactionRunner
    fun config(): LearningConfig
    fun xpRewardGateway(): LearningXpRewardGateway
}
