package github.detrig.feature.planning.domain

/** Балансировочные правила недельного плана. */
data class PlanningConfig(
    val minimumFoodBudgetRub: Long = 400,
    val smallReserveRub: Long = 50,
    val minimumReserveRub: Long = 100,
    val strongReserveRub: Long = 150,
) {
    init {
        require(minimumFoodBudgetRub > 0)
        require(smallReserveRub > 0)
        require(minimumReserveRub > smallReserveRub)
        require(strongReserveRub > minimumReserveRub)
    }

    fun reserveLevel(amountRub: Long): ReserveLevel = when {
        amountRub < smallReserveRub -> ReserveLevel.ALMOST_NONE
        amountRub < minimumReserveRub -> ReserveLevel.SMALL
        amountRub < strongReserveRub -> ReserveLevel.ADEQUATE
        else -> ReserveLevel.STRONG
    }
}

enum class ReserveLevel { ALMOST_NONE, SMALL, ADEQUATE, STRONG }
