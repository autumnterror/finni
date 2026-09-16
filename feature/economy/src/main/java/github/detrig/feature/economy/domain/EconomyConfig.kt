package github.detrig.feature.economy.domain

data class EconomyConfig(
    val initialAvailableRub: Long = 500,
    val initialSavingsRub: Long = 0,
    val periodicIncomeAmountRub: Long = 500,
    val firstPeriodicIncomeDelayMillis: Long = 2L * 24 * 60 * 60 * 1_000,
    val periodicIncomePeriodMillis: Long = 7L * 24 * 60 * 60 * 1_000,
    val maximumDebtRub: Long = 1_000,
) {
    init {
        require(initialAvailableRub >= 0)
        require(initialSavingsRub >= 0)
        require(periodicIncomeAmountRub > 0)
        require(firstPeriodicIncomeDelayMillis > 0)
        require(periodicIncomePeriodMillis > 0)
        require(maximumDebtRub > 0)
    }

    fun initialState(nowMillis: Long) = EconomyState(
        availableRub = initialAvailableRub,
        savingsRub = initialSavingsRub,
        debtRub = 0,
        periodicIncome = PeriodicIncome(
            amountRub = periodicIncomeAmountRub,
            periodMillis = periodicIncomePeriodMillis,
            nextAtMillis = Math.addExact(nowMillis, firstPeriodicIncomeDelayMillis),
        ),
    )
}
