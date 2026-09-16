package github.detrig.feature.gamestate.domain

/** Сохраняемый снимок игры, общий для всех фич. */
data class GameState(
    val pet: PetState,
    val playerLevel: Int,
    val ownedZoneIds: Set<String> = emptySet(),
)

data class PetState(
    val hunger: Int,
    val thirst: Int,
    val happiness: Int,
    val health: Int,
)
