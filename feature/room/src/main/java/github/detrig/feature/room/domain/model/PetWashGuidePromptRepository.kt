package github.detrig.feature.room.domain.model

internal interface PetWashGuidePromptRepository {
    fun wasShownForCurrentDirtEpisode(): Boolean
    fun tryMarkShownForCurrentDirtEpisode(): Boolean
    fun resetForCleanPet()
}
