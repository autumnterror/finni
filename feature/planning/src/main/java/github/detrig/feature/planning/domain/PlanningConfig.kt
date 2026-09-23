package github.detrig.feature.planning.domain

/** Балансировочные правила недельного плана. */
data class PlanningConfig(
    val minimumMandatoryPercent: Int = 40,
    val minimumSavingsPercent: Int = 10,
    val minimumReservePercent: Int = 10,
) {
    init {
        require(minimumMandatoryPercent in 0..PlanPercentages.TOTAL_PERCENT)
        require(minimumSavingsPercent in 0..PlanPercentages.TOTAL_PERCENT)
        require(minimumReservePercent in 0..PlanPercentages.TOTAL_PERCENT)
        require(minimumMandatoryPercent + minimumSavingsPercent + minimumReservePercent <= PlanPercentages.TOTAL_PERCENT)
    }
}
