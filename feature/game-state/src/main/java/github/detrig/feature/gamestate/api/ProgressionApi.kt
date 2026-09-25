package github.detrig.feature.gamestate.api

import github.detrig.feature.gamestate.domain.progression.GameProgress
import github.detrig.feature.gamestate.domain.progression.GrantXpResult
import kotlinx.coroutines.flow.Flow

interface ProgressionApi {
    suspend fun grantXp(
        grantId: String,
        profileId: String,
        amount: Int,
        source: String,
    ): GrantXpResult

    fun observeProgress(profileId: String): Flow<GameProgress>
}
