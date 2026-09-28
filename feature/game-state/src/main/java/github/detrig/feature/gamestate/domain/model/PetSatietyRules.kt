package github.detrig.feature.gamestate.domain.model

/** Сон меняет сытость в игровом времени, независимо от часов устройства. */
object PetSatietyRules {
    const val INITIAL = 70
    const val SLEEP_COST = 50

    fun canSleep(satiety: Int): Boolean {
        require(satiety in 0..100)
        return satiety >= SLEEP_COST
    }

    fun afterCost(current: Int, cost: Int): Int {
        require(current in 0..100)
        require(cost >= 0)
        return (current - cost).coerceAtLeast(0)
    }
}

object PetHappinessRules {
    const val SLEEP_COST = 10
    const val SAVINGS_PROTECTED_SLEEP_COST = 5
    const val SAVINGS_PROTECTION_SLEEP_COUNT = 3
}
