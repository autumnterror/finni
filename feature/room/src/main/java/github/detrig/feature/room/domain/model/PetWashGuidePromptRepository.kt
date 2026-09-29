package github.detrig.feature.room.domain.model

internal interface PetWashGuidePromptRepository {
    fun wasShownForCurrentDirtEpisode(): Boolean
    fun tryMarkShownForCurrentDirtEpisode(): Boolean
    fun wasBathGuidanceCompleted(): Boolean
    fun markBathGuidanceCompleted()
    fun resetForCleanPet()
    fun resetAll()
}
