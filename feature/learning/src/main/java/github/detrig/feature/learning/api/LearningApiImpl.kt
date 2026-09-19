package github.detrig.feature.learning.api

import github.detrig.feature.learning.data.LearningRepository
import github.detrig.feature.learning.domain.AchievementProgress
import github.detrig.feature.learning.domain.LearningAction
import github.detrig.feature.learning.domain.ParentProgressRow
import github.detrig.feature.learning.domain.PendingXpReward
import github.detrig.feature.learning.domain.RecordLearningResult
import github.detrig.feature.learning.domain.XpDeliveryResult
import kotlinx.coroutines.flow.Flow

internal class LearningApiImpl(
    private val repository: LearningRepository,
    private val xpRewardGateway: LearningXpRewardGateway,
) : LearningApi {
    override suspend fun record(action: LearningAction): RecordLearningResult {
        val result = repository.record(action)
        if (result is RecordLearningResult.Processed || result is RecordLearningResult.AlreadyProcessed) {
            deliverPendingXpRewards(action.profileId)
        }
        return result
    }

    override fun observeAchievements(profileId: String): Flow<List<AchievementProgress>> {
        require(profileId.isNotBlank()) { "Profile ID must not be blank" }
        return repository.observeAchievements(profileId)
    }

    override fun observeParentRows(profileId: String): Flow<List<ParentProgressRow>> {
        require(profileId.isNotBlank()) { "Profile ID must not be blank" }
        return repository.observeParentRows(profileId)
    }

    override fun observePendingXpRewards(profileId: String): Flow<List<PendingXpReward>> {
        require(profileId.isNotBlank()) { "Profile ID must not be blank" }
        return repository.observePendingXpRewards(profileId)
    }

    override suspend fun claimFirstExplanation(profileId: String, explanationId: String): Boolean =
        repository.claimFirstExplanation(profileId, explanationId)

    override suspend fun deliverPendingXpRewards(profileId: String): XpDeliveryResult {
        require(profileId.isNotBlank()) { "Profile ID must not be blank" }
        var delivered = 0
        repository.pendingXpRewards(profileId).forEach { reward ->
            when (xpRewardGateway.grantXp(reward.grantId, reward.profileId, reward.amount)) {
                XpGrantResult.Granted,
                XpGrantResult.AlreadyGranted,
                -> if (repository.markXpRewardDelivered(reward.grantId)) delivered += 1

                XpGrantResult.RetryLater -> Unit
            }
        }
        return XpDeliveryResult(
            delivered = delivered,
            remaining = repository.pendingXpRewards(profileId).size,
        )
    }

    override suspend fun resetProfile(profileId: String) = repository.resetProfile(profileId)
}
