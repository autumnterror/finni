package github.detrig.feature.gamestate.domain.model

/** One concrete food portion consumed by the pet. */
data class PetFeedingCompletion(
    val operationId: String,
    val satietyPercent: Int,
    val happinessPoints: Int,
) {
    init {
        require(operationId.isNotBlank()) { "Feeding operation id must not be blank" }
        require(satietyPercent in 0..100) { "Satiety must be between 0 and 100" }
        require(happinessPoints in 0..100) { "Happiness must be between 0 and 100" }
    }
}

/** The persisted pet state after an idempotent feeding operation. */
data class PetFeedingResult(
    val hunger: Int,
    val happiness: Int,
)
