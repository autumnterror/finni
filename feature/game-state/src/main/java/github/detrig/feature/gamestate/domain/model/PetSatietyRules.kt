package github.detrig.feature.gamestate.domain.model

/** Сон меняет сытость в игровом времени, независимо от часов устройства. */
object PetSatietyRules {
    const val INITIAL = 100
    const val SLEEP_COST = 30

    fun afterCost(current: Int, cost: Int): Int {
        require(current in 0..100)
        require(cost >= 0)
        return (current - cost).coerceAtLeast(0)
    }
}
