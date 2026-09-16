package github.detrig.feature.gamestate.domain

/** Стартовые настройки прототипа. Применяются только к новой игре. */
data class GameStateInitialConfig(
    val hunger: Int = 20,
    val thirst: Int = 80,
    val happiness: Int = 70,
    val health: Int = 100,
    val playerLevel: Int = 1,
) {

    init {
        require(hunger in 0..100)
        require(thirst in 0..100)
        require(happiness in 0..100)
        require(health in 0..100)
        require(playerLevel >= 1)
    }

    internal fun createState(): GameState {
        return GameState(
            pet = PetState(
                hunger = hunger,
                thirst = thirst,
                happiness = happiness,
                health = health,
            ),
            playerLevel = playerLevel,
        )
    }
}
