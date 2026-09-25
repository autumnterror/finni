package github.detrig.feature.gamestate.api

import github.detrig.feature.gamestate.domain.GameStateRepository

internal class ProgressionApiImpl(
    private val repository: GameStateRepository,
) : ProgressionApi {
    override suspend fun grantXp(
        grantId: String,
        profileId: String,
        amount: Int,
        source: String,
    ) = repository.grantXp(grantId, profileId, amount, source)

    override fun observeProgress(profileId: String) = repository.observeProgress(profileId)
}
