package github.detrig.feature.learning.data

import github.detrig.feature.learning.domain.AchievementProgress
import github.detrig.feature.learning.domain.LearningAction
import github.detrig.feature.learning.domain.ParentProgressRow
import github.detrig.feature.learning.domain.PendingXpReward
import github.detrig.feature.learning.domain.RecordLearningResult
import kotlinx.coroutines.flow.Flow

internal interface LearningRepository {
    suspend fun record(action: LearningAction): RecordLearningResult
    fun observeAchievements(profileId: String): Flow<List<AchievementProgress>>
    fun observeParentRows(profileId: String): Flow<List<ParentProgressRow>>
    fun observePendingXpRewards(profileId: String): Flow<List<PendingXpReward>>
    suspend fun pendingXpRewards(profileId: String): List<PendingXpReward>
    suspend fun markXpRewardDelivered(grantId: String): Boolean
    suspend fun claimFirstExplanation(profileId: String, explanationId: String): Boolean
    suspend fun resetProfile(profileId: String)
}
