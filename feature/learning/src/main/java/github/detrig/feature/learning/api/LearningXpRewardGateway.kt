package github.detrig.feature.learning.api

/**
 * Узкий контракт игровой прогрессии. Реализация обязана быть идемпотентной по grantId.
 */
interface LearningXpRewardGateway {
    suspend fun grantXp(
        grantId: String,
        profileId: String,
        amount: Int,
    ): XpGrantResult
}

sealed interface XpGrantResult {
    data object Granted : XpGrantResult
    data object AlreadyGranted : XpGrantResult
    data object RetryLater : XpGrantResult
}
