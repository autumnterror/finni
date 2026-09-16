package github.detrig.minigames.fishing.domain

internal interface FishingRepository {
    suspend fun load(profileId: String): FishingProgress
    suspend fun update(profileId: String, transform: (FishingProgress) -> FishingProgress): FishingProgress
}
