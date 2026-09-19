package github.detrig.feature.learning.api

import github.detrig.feature.learning.domain.AchievementProgress
import github.detrig.feature.learning.domain.LearningAction
import github.detrig.feature.learning.domain.ParentProgressRow
import github.detrig.feature.learning.domain.PendingXpReward
import github.detrig.feature.learning.domain.RecordLearningResult
import github.detrig.feature.learning.domain.XpDeliveryResult
import kotlinx.coroutines.flow.Flow

interface LearningApi {
    suspend fun record(action: LearningAction): RecordLearningResult
    fun observeAchievements(profileId: String): Flow<List<AchievementProgress>>
    fun observeParentRows(profileId: String): Flow<List<ParentProgressRow>>
    fun observePendingXpRewards(profileId: String): Flow<List<PendingXpReward>>
    suspend fun claimFirstExplanation(profileId: String, explanationId: String): Boolean
    suspend fun deliverPendingXpRewards(profileId: String): XpDeliveryResult

    /** Вызывается только общим reset-сценарием тестового профиля вместе со сбросом XP. */
    suspend fun resetProfile(profileId: String)
}
