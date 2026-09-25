package github.detrig.feature.gamestate.domain

import github.detrig.feature.gamestate.domain.progression.GameProgress
import github.detrig.feature.gamestate.domain.progression.ProgressionRules

/** Сохраняемый снимок игры, общий для всех фич. */
data class GameState(
    val pet: PetState,
    val playerLevel: Int,
    val ownedZoneIds: Set<String> = emptySet(),
    val totalXp: Int = ProgressionRules.minimumXpForLevel(playerLevel),
) {
    val progress: GameProgress get() = ProgressionRules.progress(totalXp)
}

data class PetState(
    val hunger: Int,
    val thirst: Int,
    val happiness: Int,
    val health: Int,
)
